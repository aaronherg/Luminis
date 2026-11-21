package com.aarondevs.luminis.Security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class JwtContext {
    private JwtContext() {}

    public static Optional<Claims> claims(HttpServletRequest request) {
        Object value = request.getAttribute(JwtValidationFilter.CLAIMS_ATTRIBUTE);
        return value instanceof Claims claims ? Optional.of(claims) : Optional.empty();
    }

    public static Optional<String> claim(HttpServletRequest request, String name) {
        return claims(request).map(claims -> claims.get(name)).map(Object::toString);
    }

    public static Optional<String> subject(HttpServletRequest request) {
        return claims(request).map(Claims::getSubject);
    }
}
