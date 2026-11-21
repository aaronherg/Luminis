package com.aarondevs.luminis.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SwaggerLocalOnlyFilter extends OncePerRequestFilter {
    private final boolean soloLocal;

    public SwaggerLocalOnlyFilter(@Value("${app.swagger.local-only}") boolean soloLocal) {
        this.soloLocal = soloLocal;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return !ruta.startsWith("/swagger-ui") && !ruta.startsWith("/v3/api-docs")
                && !"/swagger-ui.html".equals(ruta);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        if (soloLocal && (request.getHeader("X-Forwarded-For") != null
                || !InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        chain.doFilter(request, response);
    }
}
