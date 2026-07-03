package com.example.yuca.api;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Yuca API",
                version = "0.0.1-SNAPSHOT",
                description = "API d'analyse des produits issus d'Open Food Facts."),
        servers = @Server(url = "/", description = "Serveur courant"))
public class OpenApiConfig {
}
