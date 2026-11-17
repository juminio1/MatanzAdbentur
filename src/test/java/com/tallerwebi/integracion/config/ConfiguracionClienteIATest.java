package com.tallerwebi.integracion.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.config.ConfiguracionClienteIA;
import com.tallerwebi.dominio.excepcion.ClienteIAException;
import com.tallerwebi.dominio.servicios.ClienteGeminiImpl;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGeneradorPromptTrivia;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.bean.override.mockito.MockitoBean;



@SpringJUnitConfig(classes = ConfiguracionClienteIA.class) //JUnit ejecuta el test con Spring y carga la configuración de esta clase
@TestPropertySource(properties = {"gemini.api-key=api-key-de-prueba", "gemini.modelo=modelo-de-prueba"})
public class ConfiguracionClienteIATest {
    
    @Autowired
    private ApplicationContext contexto; //funciona como un registro de los objetos que Spring administra(beans)

    @Autowired
    private Environment entorno; // Es una interfaz de Spring que permite consultar las propiedades disponibles en el contexto

    @MockitoBean // Registra objetos simulados dentro del contexto de Spring
    private ServicioGeneradorPromptTrivia generadorPrompt; //Simula esta dependencias porque se las necesita para construir el cliente

    @MockitoBean
    private ObjectMapper objectMapper;

    @MockitoBean
    private HttpClient clienteHttp;


    @Test
    public void deberiaRegistrarElClienteGeminiComoBeanDeClienteIA() {

        ClienteIA clienteIA = contexto.getBean(ClienteIA.class);

        assertNotNull(clienteIA);
        assertInstanceOf(ClienteGeminiImpl.class, clienteIA);
    }

    
    @Test
    public void deberiaUtilizarElClienteHttpRegistradoEnSpring() {

        HttpClient clienteHttp = contexto.getBean(HttpClient.class);

        assertNotNull(clienteHttp);
    }

    @Test 
    public void deberiaUtilizarLaApiKeyYElModeloConfiguradosEnSpring() throws IOException, InterruptedException{

        // Simula el prompt que se enviaría a Gemini.
        when(generadorPrompt.generarPromptTrivia(1, "FACIL")).thenReturn("Generar una pregunta sobre Argentina");

        when(objectMapper.writeValueAsString(any())).thenReturn("{}"); // Simula la conversión del cuerpo de la petición a JSON.

        @SuppressWarnings("unchecked") // Simula una respuesta HTTP de Gemini.
        HttpResponse<String> respuestaHttpMock = mock(HttpResponse.class);

        when(respuestaHttpMock.statusCode()).thenReturn(403);

        when(clienteHttp.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(respuestaHttpMock);

        // Es el cliente de IA creado por Spring.
        ClienteIA clienteIA = contexto.getBean(ClienteIA.class);

        assertSame(clienteHttp, contexto.getBean(HttpClient.class)); //Esto comprueba que el HttpClient que tenemos en el test sea exactamente el mismo objeto registrado en Spring

        // Ejecuta la petición, que terminará con el HTTP 403 simulado.
        ClienteIAException excepcion = assertThrows(ClienteIAException.class, () -> clienteIA.generarPreguntas("FACIL", 1));

        assertEquals("Gemini devolvió HTTP 403", excepcion.getMessage());

        ArgumentCaptor<HttpRequest> capturador = ArgumentCaptor.forClass(HttpRequest.class); // Se captura la petición que recibió el cliente HTTP simulado.

        verify(clienteHttp).send(capturador.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());

        HttpRequest peticion = capturador.getValue();

        //Verifica que Spring proporciona la api key correcto 
        assertEquals("api-key-de-prueba", peticion.headers().firstValue("x-goog-api-key").orElse(null));

        //Verifica que Spring proporciona el modelo correcto capich
        assertEquals("https://generativelanguage.googleapis.com/v1beta/models/" + "modelo-de-prueba:generateContent", peticion.uri().toString());

    }

    
    @Test
    public void deberiaObtenerLaApiKeyDesdeUnaVariableDeEntorno() {

        Environment entorno = contexto.getEnvironment(); //Es un componente q permite consultar propiedades de config 

        String apiKey = entorno.getProperty("gemini.api-key");

        assertEquals("api-key-de-prueba", apiKey);
    }


    @Test
    public void deberiaResolverLaApiKeyDesdeLaVariableDeEntorno() {

        StandardEnvironment entorno = new StandardEnvironment(); //Crea un entorno de Spring independiente del contexto del test

        //Simula una variable q se llame GEMINI_API_KEY
        entorno.getPropertySources().addFirst(new MapPropertySource("variablesDeEntornoSimuladas", Map.of("GEMINI_API_KEY", "clave-secreta-de-prueba")));

        //Declara que gemini.api-key obtiene su valor desde ${GEMINI_API_KEY}
        Properties propiedades = new Properties();
        propiedades.setProperty("gemini.api-key", "${GEMINI_API_KEY}");

        entorno.getPropertySources().addLast(new PropertiesPropertySource("configuracionGemini", propiedades));

        assertEquals("clave-secreta-de-prueba", entorno.getProperty("gemini.api-key")); //Comprueba que Spring resuelve correctamente la clave ficticia
    }


    @Test
    public void deberiaConfigurarGeminiMedianteVariablesDeEntorno() throws Exception {

        Properties propiedades = new Properties(); //Permite leer un archivo disponible en los recursos de la aplicación. Properties interpreta los pares clave=Valor

        try (var archivo = new ClassPathResource("application.properties").getInputStream()) { 

            propiedades.load(archivo);
        }

        assertEquals("${GEMINI_API_KEY:}", propiedades.getProperty("gemini.api-key"));
        //Comprueba que las propiedades tengan el mismo formato esperado
        assertEquals("${GEMINI_MODELO:gemini-3.5-flash-lite}", propiedades.getProperty("gemini.modelo")); 
    }

    @Test
    public void deberiaCargarLasPropiedadesDeGeminiDesdeApplicationProperties() throws IOException {   

    try (AnnotationConfigApplicationContext contextoPrueba = //Crea un contexto independiente del que administra los demás tests
            new AnnotationConfigApplicationContext()) {

        contextoPrueba.getEnvironment().getPropertySources().addFirst( //Simula las variables GEMINI_API_KEY y GEMINI_MODELO usando valores ficticios
                new MapPropertySource("variablesDePrueba",
                        Map.of( "GEMINI_API_KEY", "clave-ficticia-de-prueba",
                                "GEMINI_MODELO", "modelo-de-prueba")));

        contextoPrueba.getEnvironment().getPropertySources().addLast(
                new org.springframework.core.io.support.ResourcePropertySource("classpath:application.properties")); //Carga el contenido real de application.properties desde los recursos del proyecto.

        String apiKey = contextoPrueba.getEnvironment().getProperty("gemini.api-key");

        String modelo = contextoPrueba.getEnvironment().getProperty("gemini.modelo");

        assertEquals("clave-ficticia-de-prueba", apiKey);
        assertEquals("modelo-de-prueba", modelo); //Comprueba que Spring resolvió correctamente las referencias.
        }
    }
}



