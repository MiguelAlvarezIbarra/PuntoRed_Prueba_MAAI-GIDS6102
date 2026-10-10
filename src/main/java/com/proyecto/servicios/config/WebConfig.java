package com.proyecto.servicios.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableScheduling
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private ActividadSesionInterceptor actividadSesionInterceptor;

    /**
     * Orígenes permitidos para CORS. En Render se inyecta la variable
     * CORS_ALLOWED_ORIGINS con la URL del frontend (ej. https://mi-front.vercel.app).
     * En local funciona con el default "*" para desarrollo.
     */
    @Value("${cors.allowed-origins:*}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(actividadSesionInterceptor).excludePathPatterns("/auth/login");
    }
}
