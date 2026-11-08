package com.tallerwebi.integracion.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.config.ConfiguracionClienteIA;
import com.tallerwebi.dominio.servicios.ClienteGeminiImpl;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGeneradorPromptTrivia;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;



@SpringJUnitConfig(classes = ConfiguracionClienteIA.class) //JUnit ejecuta el test con Spring y carga la configuración de esta clase
@TestPropertySource(properties = {"gemini.api-key=api-key-de-prueba", "gemini.modelo=modelo-de-prueba"})
public class ConfiguracionClienteIATest {
    
    @Autowired
    private ApplicationContext contexto; //funciona como un registro de los objetos que Spring administra(beans)

    @MockitoBean // Registra objetos simulados dentro del contexto de Spring
    private ServicioGeneradorPromptTrivia generadorPrompt; //Simula esta dependencias porque se las necesita para construir el cliente

    @MockitoBean
    private ObjectMapper objectMapper;


    @Test
    public void deberiaRegistrarElClienteGeminiComoBeanDeClienteIA() {

        ClienteIA clienteIA = contexto.getBean(ClienteIA.class);

        assertNotNull(clienteIA);
        assertInstanceOf(ClienteGeminiImpl.class, clienteIA);
    }
}



