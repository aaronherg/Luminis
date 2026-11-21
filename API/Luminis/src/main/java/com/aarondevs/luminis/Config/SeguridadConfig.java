package com.aarondevs.luminis.Config;

import com.aarondevs.luminis.Componentes.KeystoreLoader;
import com.aarondevs.luminis.Security.Cifrador;
import com.aarondevs.luminis.Security.JwtService;
import com.aarondevs.luminis.Security.JwtValidationFilter;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SeguridadConfig {
    private final KeystoreLoader keystoreLoader;

    public SeguridadConfig(KeystoreLoader keystoreLoader) {
        this.keystoreLoader = keystoreLoader;
    }

    @Bean
    @ConditionalOnProperty(
            name = "app.seguridad.migracion-cifrado-enabled",
            havingValue = "true"
    )
    public Cifrador cifrador() throws Exception {
        return new Cifrador(keystoreLoader.cargarClave());
    }

    @Bean
    @ConditionalOnProperty(name = "app.jwt.enabled", havingValue = "true")
    public JwtValidationFilter jwtValidationFilter(
            JwtService jwtService,
            @Value("${app.jwt.public-paths}") String rutasPublicas
    ) {
        List<String> publicPaths = Arrays.stream(rutasPublicas.split(","))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .toList();
        return new JwtValidationFilter(jwtService, publicPaths);
    }

    @Bean
    @ConditionalOnProperty(name = "app.jwt.enabled", havingValue = "true")
    public FilterRegistrationBean<JwtValidationFilter> jwtFilterServletRegistration(
            JwtValidationFilter jwtValidationFilter
    ) {
        FilterRegistrationBean<JwtValidationFilter> registration =
                new FilterRegistrationBean<>(jwtValidationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtValidationFilter> jwtValidationFilter
    ) throws Exception {
        http.csrf(configurer -> configurer.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/**").permitAll());
        jwtValidationFilter.ifAvailable(filter -> http.addFilterBefore(
                filter,
                UsernamePasswordAuthenticationFilter.class
        ));
        return http.build();
    }
}
