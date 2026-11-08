
package com.tallerwebi.config;

import java.net.http.HttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.servicios.ClienteGeminiImpl;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGeneradorPromptTrivia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionClienteIA {

    @Bean
    public ClienteIA clienteIA(ObjectMapper objectMapper, ServicioGeneradorPromptTrivia generadorPrompt,
                                 @Value("${gemini.api-key}") String apiKey, @Value("${gemini.modelo}") String modelo) {

        return new ClienteGeminiImpl(HttpClient.newHttpClient(), objectMapper, generadorPrompt, apiKey, modelo);
    }
}
