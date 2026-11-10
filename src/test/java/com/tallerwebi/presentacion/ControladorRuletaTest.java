
package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.EstadoRuleta;
import com.tallerwebi.dominio.servicios.GestorRuletaPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.presentacion.DTO.EstadoRuletaDTO;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;

import jakarta.servlet.http.HttpServletRequest;

public class ControladorRuletaTest {

    private GestorRuletaPartida gestorRuletaMock;
    private ServicioPartida servicioPartidaMock;
    private ServicioParticipacion participacionMock;
    private ControladorRuleta controladorRuleta;

    @BeforeEach
    public void init() {
        this.gestorRuletaMock = mock(GestorRuletaPartida.class);
        this.servicioPartidaMock = mock(ServicioPartida.class);
        this.participacionMock = mock(ServicioParticipacion.class);
        this.controladorRuleta = new ControladorRuleta(gestorRuletaMock, servicioPartidaMock, participacionMock);
    }

    @Test
    public void deberiaDevolverDificultadFacilCuandoElServicioLaSortea() {

        Partida partidaMock = mock(Partida.class);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(servicioPartidaMock.obtenerPartida("ABC123")).thenReturn(partidaMock);

        EstadoRuletaDTO estado = new EstadoRuletaDTO("ABC123", "ACT-1", 10L, EstadoRuleta.SORTEADA, DificultadRuleta.FACIL, 10000L);

        when(gestorRuletaMock.sortearDificultad("ABC123", "ACT-1", 10L)).thenReturn(estado);

        try (MockedStatic<ResolutorJugadorSesion> resolutorMock = mockStatic(ResolutorJugadorSesion.class)) {

            resolutorMock.when(() -> ResolutorJugadorSesion.resolverParticipante(partidaMock, request, participacionMock)).thenReturn(10L);

            ResponseEntity<EstadoRuletaDTO> respuesta = controladorRuleta.sortearDificultad("ABC123", "ACT-1", request);

            assertNotNull(respuesta.getBody());
            assertEquals(DificultadRuleta.FACIL, respuesta.getBody().dificultad());
            assertEquals(EstadoRuleta.SORTEADA, respuesta.getBody().estado());

            verify(gestorRuletaMock).sortearDificultad("ABC123", "ACT-1", 10L);
        }
    }

    @Test
    public void deberiaExponerElSorteoMediantePost() throws NoSuchMethodException {

        // Verifica que el metodo tenga las anotaciones necesarias,
        // no comprueba una petición HTTP real.

        Method metodo = ControladorRuleta.class.getMethod("sortearDificultad", String.class, String.class, HttpServletRequest.class);
        PostMapping postMapping = metodo.getAnnotation(PostMapping.class);

        assertNotNull(postMapping);
    }

   @Test
public void deberiaResponderConLaDificultadSorteadaAlRecibirUnaPeticionPost() throws Exception {

    Partida partidaMock = mock(Partida.class);

    when(servicioPartidaMock.obtenerPartida("ABC123")).thenReturn(partidaMock);

    EstadoRuletaDTO estado = new EstadoRuletaDTO("ABC123", "ACT-1", 10L, EstadoRuleta.SORTEADA, DificultadRuleta.FACIL, 10000L);

    when(gestorRuletaMock.sortearDificultad("ABC123", "ACT-1", 10L)).thenReturn(estado);

    try (MockedStatic<ResolutorJugadorSesion> resolutorMock = mockStatic(ResolutorJugadorSesion.class)) {

        resolutorMock.when(() -> ResolutorJugadorSesion.resolverParticipante(eq(partidaMock), any(HttpServletRequest.class), eq(participacionMock))).thenReturn(10L);

        // Prepara un entorno mvc de prueba utilizando nuestro controlador, sin levantar la app.
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controladorRuleta).build();

        // Simula una petición HTTP POST y verifica que el servidor responda con 200.
        String respuestaJson = mockMvc.perform(post("/ruleta/ABC123/ACT-1/sortear"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Comprueba que la respuesta contenga la dificultad sorteada.
        ObjectMapper objectMapper = new ObjectMapper();
        EstadoRuletaDTO respuesta = objectMapper.readValue(respuestaJson, EstadoRuletaDTO.class);

        assertEquals(DificultadRuleta.FACIL, respuesta.dificultad());
        assertEquals(EstadoRuleta.SORTEADA, respuesta.estado());
        assertEquals("ACT-1", respuesta.activacionId());

        // Comprueba que el controlador delegó la operación al servicio.
        verify(gestorRuletaMock).sortearDificultad("ABC123", "ACT-1", 10L);
    }
}

    @Test
    public void deberiaExponerLaConsultaDelEstadoMedianteGet() throws NoSuchMethodException {

        Method metodo = ControladorRuleta.class.getMethod("obtenerEstado", String.class, HttpServletRequest.class);
        GetMapping getMapping = metodo.getAnnotation(GetMapping.class);

        assertNotNull(getMapping);
    }

    @Test
    public void deberiaDevolverElEstadoActualDeLaRuleta() {

        Partida partidaMock = mock(Partida.class);
        MockHttpServletRequest request = new MockHttpServletRequest();

        when(servicioPartidaMock.obtenerPartida("ABC123")).thenReturn(partidaMock);

        EstadoRuletaDTO estado = new EstadoRuletaDTO("ABC123", "ACT-1", 10L, EstadoRuleta.ESPERANDO, null, 10000L);

        when(gestorRuletaMock.obtenerEstado("ABC123")).thenReturn(estado);

        try (MockedStatic<ResolutorJugadorSesion> resolutorMock = mockStatic(ResolutorJugadorSesion.class)) {

            resolutorMock.when(() -> ResolutorJugadorSesion.resolverParticipante(partidaMock, request, participacionMock)).thenReturn(10L);

            ResponseEntity<EstadoRuletaDTO> respuesta = controladorRuleta.obtenerEstado("ABC123", request);

            assertNotNull(respuesta.getBody());
            assertEquals(EstadoRuleta.ESPERANDO, respuesta.getBody().estado());

            verify(gestorRuletaMock).obtenerEstado("ABC123");
        }
    }
}
