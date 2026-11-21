package com.aarondevs.luminis.Security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtValidationFilter extends OncePerRequestFilter {
    public static final String CLAIMS_ATTRIBUTE = "jwtClaims";

    private final JwtService jwtService;
    private final List<String> publicPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtValidationFilter(
            JwtService jwtService,
            List<String> publicPaths
    ) {
        this.jwtService = jwtService;
        this.publicPaths = List.copyOf(publicPaths);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String path = rutaSinContexto(request);
        return publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern.trim(), path));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            escribirError(response, HttpStatus.UNAUTHORIZED, "Token JWT requerido");
            return;
        }

        Claims claims;
        try {
            claims = jwtService.obtenerClaims(authorization.substring(7).trim());
        } catch (Exception error) {
            escribirError(response, HttpStatus.UNAUTHORIZED, "Token JWT inválido o expirado");
            return;
        }

        request.setAttribute(CLAIMS_ATTRIBUTE, claims);
        var authentication = new UsernamePasswordAuthenticationToken(
                claims.getSubject(),
                null,
                AuthorityUtils.NO_AUTHORITIES
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            chain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"validation\":\"Error\",\"status\":" + status.value()
                        + ",\"data\":null,\"mensage\":\"" + escaparJson(mensaje) + "\"}"
        );
    }

    private String escaparJson(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String rutaSinContexto(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? uri : uri.substring(contextPath.length());
    }
}
