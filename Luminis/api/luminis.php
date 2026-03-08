<?php

require __DIR__ . '/../workerman/vendor/autoload.php';

use Workerman\Worker;
use Workerman\Timer;

class ConnworkServer
{

	protected $clients = [];
	protected $mensajes = [];
	protected $colasPorUsuario = [];
	protected $tiempoMensajeLimiteSegundos = 10;

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

			$this->entregarMensajesOffline($connection, $origen);
		}

		/*
		MARCAR COMO VISTO
		*/

		if ($type === "seen") {

			$mensajeId = $data['mensajeId'] ?? null;

			if ($mensajeId)
				$this->marcarMensajeVisto($mensajeId);

			return;
		}

		/*
		NUEVO MENSAJE
		*/

		if (!$destino || !$mensaje) return;

		/*
		VERIFICAR SI DESTINO ESTA CONECTADO
		*/

		$mensajeId = bin2hex(random_bytes(8));

		if (!isset($this->clients[$destino]) || empty($this->clients[$destino])) {

			$connection->send(json_encode([
				'type' => 'desconocido',
				'mensajeId' => $mensajeId
			]));

			return;
		}

		$this->mensajes[$mensajeId] = [
			'mensajeId' => $mensajeId,
			'origen'    => $origen,
			'destino'   => $destino,
			'mensaje'   => $mensaje,
			'expira'    => time() + $this->tiempoMensajeLimiteSegundos,
			'entregado' => false
		];

		$this->notificarEnviado($origen, $mensajeId);

		/*
		DESTINO CONECTADO
		*/

		$this->enviarMensaje($destino, $mensajeId, $origen, $mensaje);
		$this->marcarEntregado($mensajeId);

	}

	/*
	LIMPIAR MENSAJES EXPIRADOS
	*/

	private function limpiarMensajesExpirados()
	{

		$now = time();

		foreach ($this->mensajes as $mensajeId => $msgData) {

			if ($msgData['expira'] > $now)
				continue;

			if ($msgData['entregado'])
				continue;

			$destino = $msgData['destino'];
			$origen  = $msgData['origen'];

			if (isset($this->clients[$destino]))
				continue;

			if (isset($this->clients[$origen])) {

				foreach ($this->clients[$origen] as $conn) {

					$conn->send(json_encode([
						'type' => 'desaparecido',
						'mensajeId' => $mensajeId
					]));

				}

			}

			unset($this->mensajes[$mensajeId]);

			if (isset($this->colasPorUsuario[$destino][$mensajeId]))
				unset($this->colasPorUsuario[$destino][$mensajeId]);

		}

	}

	/*
	ENTREGAR MENSAJES OFFLINE
	*/

	private function entregarMensajesOffline($connection, $userId)
	{

		if (!isset($this->colasPorUsuario[$userId])) return;

		foreach ($this->colasPorUsuario[$userId] as $mensajeId => $v) {

			$msgData = $this->mensajes[$mensajeId] ?? null;
			if (!$msgData) continue;

			$connection->send(json_encode([
				'type'      => 'message',
				'mensajeId' => $msgData['mensajeId'],
				'origen'    => $msgData['origen'],
				'mensaje'   => $msgData['mensaje']
			]));

			$this->marcarEntregado($mensajeId);

		}

		unset($this->colasPorUsuario[$userId]);

	}

	/*
	MARCAR COMO ENTREGADO
	*/

	private function marcarEntregado($mensajeId)
	{

		if (!isset($this->mensajes[$mensajeId])) return;

		$this->mensajes[$mensajeId]['entregado'] = true;

		$origen = $this->mensajes[$mensajeId]['origen'];

		if (isset($this->clients[$origen])) {

			foreach ($this->clients[$origen] as $conn) {

				$conn->send(json_encode([
					'type'      => 'delivered',
					'mensajeId' => $mensajeId
				]));

			}

		}

	}

	/*
	ENVIAR MENSAJE
	*/

	private function enviarMensaje($destino, $mensajeId, $origen, $mensaje)
	{

		foreach ($this->clients[$destino] as $conn) {

			$conn->send(json_encode([
				'type'      => 'message',
				'mensajeId' => $mensajeId,
				'origen'    => $origen,
				'mensaje'   => $mensaje
			]));

		}

	}

	/*
	NOTIFICAR SENT
	*/

	private function notificarEnviado($origen, $mensajeId)
	{

		if (!isset($this->clients[$origen])) return;

		foreach ($this->clients[$origen] as $conn) {

			$conn->send(json_encode([
				'type'      => 'sent',
				'mensajeId' => $mensajeId
			]));

		}

	}

	/*
	MARCAR MENSAJE COMO VISTO
	*/

	private function marcarMensajeVisto($mensajeId)
	{

		$msgData = $this->mensajes[$mensajeId] ?? null;
		if (!$msgData) return;

		$origen  = $msgData['origen'];

		if (isset($this->clients[$origen])) {

			foreach ($this->clients[$origen] as $conn) {

				$conn->send(json_encode([
					'type'      => 'seen',
					'mensajeId' => $mensajeId
				]));

			}

		}

		unset($this->mensajes[$mensajeId]);

	}

	public function onClose($connection)
	{

		if (!isset($connection->userId)) return;

		$userId = $connection->userId;

		if (isset($this->clients[$userId])) {

			$this->clients[$userId] = array_filter(
				$this->clients[$userId],
				fn($conn) => $conn !== $connection
			);

			if (empty($this->clients[$userId]))
				unset($this->clients[$userId]);

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