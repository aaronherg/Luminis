<?php

require __DIR__ . '/../workerman/vendor/autoload.php';

use Workerman\Worker;
use Workerman\Timer;

class ConnworkServer
{

	protected $clients = [];
	protected $colasPorUsuario = [];
	protected $usuariosDesconectados = [];
	protected $usuariosConocidos = [];
	protected $tiempoMensajeLimiteSegundos = 10;

	protected $logFile;

	public function __construct()
	{
		$this->logFile = __DIR__ . "/eventos.log";
	}

	private function logEvento($texto)
	{
		$linea = "[".date("Y-m-d H:i:s")."] ".$texto.PHP_EOL;
		file_put_contents($this->logFile, $linea, FILE_APPEND | LOCK_EX);
	}

	public function iniciarTimerGlobal()
	{
		Timer::add(1, function () {
			$this->limpiarMensajesExpirados();
		});
	}

	public function onMessage($connection, $msg)
	{

		$data = json_decode($msg, true);
		if (!$data) return;

		$origen  = $data['origen']  ?? null;
		$destino = $data['destino'] ?? null;
		$mensaje = $data['mensaje'] ?? null;
		$type    = $data['type']    ?? "message";

		if (!$origen) return;

		if (!isset($this->clients[$origen]))
			$this->clients[$origen] = [];

		/*
		REGISTRAR CONEXION
		*/

		if (!in_array($connection, $this->clients[$origen], true)) {

			$this->clients[$origen][] = $connection;
			$connection->userId = $origen;

			$this->usuariosConocidos[$origen] = true;

			if (isset($this->usuariosDesconectados[$origen]))
				unset($this->usuariosDesconectados[$origen]);

			$this->logEvento("$origen CONECTADO");

			$this->entregarMensajesOffline($connection, $origen);
		}

		/*
		MARCAR COMO VISTO
		*/

		if ($type === "seen") {

			$mensajeId = $data['mensajeId'] ?? null;

			if ($mensajeId && $destino) {

				$this->logEvento("ORIGEN: $origen, DESTINO: $destino, MENSAJE: $mensajeId, ESTADO: VISTO");

				if (isset($this->clients[$destino])) {

					foreach ($this->clients[$destino] as $conn) {

						$conn->send(json_encode([
							'type'      => 'seen',
							'mensajeId' => $mensajeId,
							'origen'    => $origen
						]));

					}

				} else {

					$this->agregarEventoOffline($destino, [
						'type'      => 'seen',
						'mensajeId' => $mensajeId,
						'origen'    => $origen
					]);

				}

			}

			return;
		}

		/*
		NUEVO MENSAJE
		*/

		if (!$destino || !$mensaje) return;

		$mensajeId = bin2hex(random_bytes(8));

		/*
		DESTINO NUNCA EXISTIO
		*/

		if (!isset($this->usuariosConocidos[$destino])) {

			$this->logEvento("ORIGEN: $origen, DESTINO: $destino, MENSAJE: $mensajeId, ESTADO: DESAPARECIDO (DESTINO INEXISTENTE)");

			$connection->send(json_encode([
				'type' => 'desconocido',
				'mensajeId' => $mensajeId,
				'destino' => $destino
			]));

			return;
		}

		/*
		DESTINO DESCONECTADO MAS DE 10 SEGUNDOS
		*/

		if (isset($this->usuariosDesconectados[$destino])) {

			$tiempo = time() - $this->usuariosDesconectados[$destino];

			if ($tiempo > $this->tiempoMensajeLimiteSegundos) {

				$this->logEvento("ORIGEN: $origen, DESTINO: $destino, MENSAJE: $mensajeId, ESTADO: DESAPARECIDO");

				$this->notificarDesaparecido($origen, $mensajeId, $destino);

				return;
			}

		}

		$this->notificarEnviado($origen, $mensajeId, $destino);

		$this->logEvento("ORIGEN: $origen, DESTINO: $destino, MENSAJE: $mensajeId, ESTADO: ENVIADO");

		/*
		DESTINO CONECTADO
		*/

		if (isset($this->clients[$destino]) && !empty($this->clients[$destino])) {

			$this->enviarMensaje($destino, $mensajeId, $origen, $mensaje);
			$this->marcarEntregadoDirecto($origen, $mensajeId, $destino);

			$this->logEvento("ORIGEN: $origen, DESTINO: $destino, MENSAJE: $mensajeId, ESTADO: ENTREGADO");

			return;

		}

		/*
		DESTINO DESCONECTADO TEMPORALMENTE
		*/

		if (!isset($this->colasPorUsuario[$destino]))
			$this->colasPorUsuario[$destino] = [];

		$this->colasPorUsuario[$destino][$mensajeId] = [
			'type' => 'message',
			'mensajeId' => $mensajeId,
			'origen' => $origen,
			'destino' => $destino,
			'mensaje' => $mensaje,
			'expira' => time() + $this->tiempoMensajeLimiteSegundos
		];

		$this->logEvento("ORIGEN: $origen, DESTINO: $destino, MENSAJE: $mensajeId, ESTADO: EN COLA");

	}

	private function limpiarMensajesExpirados()
	{

		$now = time();

		foreach ($this->colasPorUsuario as $destino => $lista) {

			foreach ($lista as $mensajeId => $msgData) {

				if ($msgData['expira'] > $now)
					continue;

				$origen = $msgData['origen'];

				$this->logEvento("ORIGEN: $origen, DESTINO: $destino, MENSAJE: $mensajeId, ESTADO: DESAPARECIDO");

				$this->notificarDesaparecido($origen, $mensajeId, $destino);

				unset($this->colasPorUsuario[$destino][$mensajeId]);

			}

			if (empty($this->colasPorUsuario[$destino]))
				unset($this->colasPorUsuario[$destino]);

		}

		foreach ($this->usuariosDesconectados as $user => $time) {

			if ($now - $time > $this->tiempoMensajeLimiteSegundos) {

				$this->logEvento("$user DESCONECTADO PERMANENTEMENTE");

				unset($this->usuariosConocidos[$user]);
				unset($this->usuariosDesconectados[$user]);

			}

		}

	}

	private function entregarMensajesOffline($connection, $userId)
	{

		if (!isset($this->colasPorUsuario[$userId])) return;

		foreach ($this->colasPorUsuario[$userId] as $key => $msgData) {

			$connection->send(json_encode($msgData));

			if (($msgData['type'] ?? "message") === "message") {

				$this->marcarEntregadoDirecto(
					$msgData['origen'],
					$msgData['mensajeId'],
					$msgData['destino']
				);

				$this->logEvento("ORIGEN: {$msgData['origen']}, DESTINO: {$msgData['destino']}, MENSAJE: {$msgData['mensajeId']}, ESTADO: ENTREGADO");

			}

		}

		unset($this->colasPorUsuario[$userId]);

	}

	private function agregarEventoOffline($userId, $evento)
	{

		if (!isset($this->colasPorUsuario[$userId]))
			$this->colasPorUsuario[$userId] = [];

		$key = ($evento['mensajeId'] ?? uniqid())."_event";

		$this->colasPorUsuario[$userId][$key] = $evento;

	}

	private function notificarDesaparecido($origen, $mensajeId, $destino)
	{

		if (isset($this->clients[$origen])) {

			foreach ($this->clients[$origen] as $conn) {

				$conn->send(json_encode([
					'type'      => 'desaparecido',
					'mensajeId' => $mensajeId,
					'destino'   => $destino
				]));

			}

		}

	}

	private function marcarEntregadoDirecto($origen, $mensajeId, $destino)
	{

		if (isset($this->clients[$origen])) {

			foreach ($this->clients[$origen] as $conn) {

				$conn->send(json_encode([
					'type'      => 'delivered',
					'mensajeId' => $mensajeId,
					'destino'   => $destino
				]));

			}

		}

	}

	private function enviarMensaje($destino, $mensajeId, $origen, $mensaje)
	{

		foreach ($this->clients[$destino] as $conn) {

			$conn->send(json_encode([
				'type'      => 'message',
				'mensajeId' => $mensajeId,
				'origen'    => $origen,
				'destino'   => $destino,
				'mensaje'   => $mensaje
			]));

		}

	}

	private function notificarEnviado($origen, $mensajeId, $destino)
	{

		if (!isset($this->clients[$origen])) return;

		foreach ($this->clients[$origen] as $conn) {

			$conn->send(json_encode([
				'type'      => 'sent',
				'mensajeId' => $mensajeId,
				'destino'   => $destino
			]));

		}

	}

	public function onClose($connection)
	{

		if (!isset($connection->userId)) return;

		$userId = $connection->userId;

		$this->logEvento("$userId PRE-DESCONECTADO");

		if (isset($this->clients[$userId])) {

			$this->clients[$userId] = array_filter(
				$this->clients[$userId],
				fn($conn) => $conn !== $connection
			);

			if (empty($this->clients[$userId])) {

				unset($this->clients[$userId]);
				$this->usuariosDesconectados[$userId] = time();

			}

		}

	}

}

$worker = new Worker("websocket://0.0.0.0:2345");

$server = new ConnworkServer();

$worker->onWorkerStart = function() use ($server) {
	$server->iniciarTimerGlobal();
};

$worker->onMessage = function ($connection, $data) use ($server) {
	$server->onMessage($connection, $data);
};

$worker->onClose = function ($connection) use ($server) {
	$server->onClose($connection);
};

Worker::runAll();