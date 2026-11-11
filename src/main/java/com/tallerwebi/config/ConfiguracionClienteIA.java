
package com.tallerwebi.config;

import java.net.http.HttpClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.servicios.ClienteGeminiImpl;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGeneradorPromptTrivia;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class ConfiguracionClienteIA {

    @Bean
    public ClienteIA clienteIA(HttpClient clienteHttp, ObjectProvider<ObjectMapper> objectMapper,
                               ServicioGeneradorPromptTrivia generadorPrompt,
                               @Value("${gemini.api-key}") String apiKey, @Value("${gemini.modelo}") String modelo) {

        return new ClienteGeminiImpl(clienteHttp, objectMapper.getIfAvailable(ObjectMapper::new),
            generadorPrompt, apiKey, modelo);
    }

    @Bean
    public HttpClient clienteHttp() {
        return HttpClient.newHttpClient(); //Spring crea y adminitra un objeto HttpClient
    }
}
