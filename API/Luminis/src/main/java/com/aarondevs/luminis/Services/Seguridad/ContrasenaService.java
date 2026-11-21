package com.aarondevs.luminis.Services.Seguridad;

import org.springframework.stereotype.Service;

@Service
public class ContrasenaService {
    public void validar(String contrasena) {
        if (contrasena == null || contrasena.length() < 8)
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        if (contrasena.chars().noneMatch(Character::isUpperCase))
            throw new IllegalArgumentException("La contraseña debe contener una letra mayúscula");
        if (contrasena.chars().noneMatch(Character::isLowerCase))
            throw new IllegalArgumentException("La contraseña debe contener una letra minúscula");
        if (contrasena.chars().noneMatch(Character::isDigit))
            throw new IllegalArgumentException("La contraseña debe contener un número");
        if (contrasena.chars().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c)))
            throw new IllegalArgumentException("La contraseña debe contener un carácter especial");
    }
}
