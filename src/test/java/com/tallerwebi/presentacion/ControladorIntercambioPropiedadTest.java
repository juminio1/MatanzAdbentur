package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioIntercambioPropiedad.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControladorIntercambioPropiedadTest {
    private ServicioIntercambioPropiedad intercambios;
    private SimpMessagingTemplate mensajes;
    private MockMvc mvc;
    private Propuesta propuesta;
    private Jugador ana;
    private Jugador luna;

    @BeforeEach
    void preparar() {
        ServicioPartida partidas = mock(ServicioPartida.class);
        intercambios = mock(ServicioIntercambioPropiedad.class);
        mensajes = mock(SimpMessagingTemplate.class);
        ana = jugador(1L, "Ana");
        luna = jugador(2L, "Luna");
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(ana, luna));
        when(partidas.obtenerPartida("CAMBIO")).thenReturn(partida);
        propuesta = new Propuesta("oferta-1", "CAMBIO", 1L, 2L, 4, 9, "Ana", "San Justo", "Casanova", Instant.now().plusSeconds(300));
        ServicioCasillero casilleros = mock(ServicioCasillero.class);
        when(casilleros.obtenerDatosCatalogoPropiedad(any())).thenReturn(null);
        mvc = MockMvcBuilders.standaloneSetup(new ControladorIntercambioPropiedad(partidas, intercambios,
            new ServicioParticipacionImpl(), mensajes, casilleros, mock(ServicioPropiedad.class))).build();
    }

    @Test
    void creaLaPropuestaConElJugadorAutenticadoYNotificaAlDestinatario() throws Exception {
        when(intercambios.proponer("CAMBIO", 1L, 2L, 4, 9)).thenReturn(propuesta);
        var respuesta = mvc.perform(post("/partida/CAMBIO/intercambios")
                .sessionAttr("NOMBRE", "Ana").sessionAttr("id", 999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"idDestinatario\":2,\"posicionOfrecida\":4,\"posicionSolicitada\":9}"))
            .andExpect(status().isOk()).andReturn();
        var datos = new ObjectMapper().readTree(respuesta.getResponse().getContentAsString());
        assertEquals("pendiente", datos.get("estado").asText());
        verify(intercambios).proponer("CAMBIO", 1L, 2L, 4, 9);
        verify(mensajes).convertAndSend(eq("/topic/partida/CAMBIO/intercambios"), any(Object.class));
    }

    @Test
    void unaSesionSinNombreAutenticadoNoPuedeUsarUnIdDeJugador() throws Exception {
        mvc.perform(post("/partida/CAMBIO/intercambios").sessionAttr("id", 1L)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"idDestinatario\":2,\"posicionOfrecida\":4,\"posicionSolicitada\":9}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(intercambios, mensajes);
    }

    @Test
    void laConsultaSoloPideLasPropuestasDelDestinatarioAutenticado() throws Exception {
        when(intercambios.pendientes("CAMBIO", 2L)).thenReturn(List.of(propuesta));
        mvc.perform(get("/partida/CAMBIO/intercambios").sessionAttr("NOMBRE", "Luna"))
            .andExpect(status().isOk());
        verify(intercambios).pendientes("CAMBIO", 2L);
    }

    @Test
    void rechazarNoPublicaCambiosDePropiedad() throws Exception {
        when(intercambios.rechazar("CAMBIO", "oferta-1", 2L)).thenReturn(propuesta);
        mvc.perform(post("/partida/CAMBIO/intercambios/oferta-1/responder")
            .sessionAttr("NOMBRE", "Luna").contentType(MediaType.APPLICATION_JSON).content("{\"aceptar\":false}"))
            .andExpect(status().isOk());
        verify(intercambios).rechazar("CAMBIO", "oferta-1", 2L);
        verify(mensajes, never()).convertAndSend(eq("/topic/partida/CAMBIO/propiedades"), any(Object.class));
    }

    @Test
    void aceptarPublicaLasDosPropiedadesConSusNuevosDuenios() throws Exception {
        CasilleroPropiedad ofrecida = casillero(10L, 4, luna);
        CasilleroPropiedad solicitada = casillero(11L, 9, ana);
        when(intercambios.aceptar("CAMBIO", "oferta-1", 2L)).thenReturn(new Resultado(propuesta, ofrecida, solicitada));
        var respuesta = mvc.perform(post("/partida/CAMBIO/intercambios/oferta-1/responder")
                .sessionAttr("NOMBRE", "Luna").contentType(MediaType.APPLICATION_JSON).content("{\"aceptar\":true}"))
            .andExpect(status().isOk()).andReturn();
        var datos = new ObjectMapper().readTree(respuesta.getResponse().getContentAsString());
        assertEquals("aceptado", datos.get("estado").asText());
        assertEquals(2L, datos.get("propiedades").get(0).get("propietarioId").asLong());
        assertEquals(1L, datos.get("propiedades").get(1).get("propietarioId").asLong());
        verify(mensajes, times(2)).convertAndSend(eq("/topic/partida/CAMBIO/propiedades"), any(Object.class));
    }

    @Test
    void unaPropuestaInvalidaNoPublicaTransferencias() throws Exception {
        when(intercambios.aceptar("CAMBIO", "oferta-1", 2L)).thenThrow(new IllegalStateException("La propiedad se vendió."));
        mvc.perform(post("/partida/CAMBIO/intercambios/oferta-1/responder")
            .sessionAttr("NOMBRE", "Luna").contentType(MediaType.APPLICATION_JSON).content("{\"aceptar\":true}"))
            .andExpect(status().isConflict());
        verifyNoInteractions(mensajes);
    }

    @Test
    void unaSesionAjenaNoPuedeConsultarLasPropuestas() throws Exception {
        mvc.perform(get("/partida/CAMBIO/intercambios").sessionAttr("NOMBRE", "Ajeno"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(intercambios, mensajes);
    }

    @Test
    void noPublicaUnaPropuestaQueElServicioRechaza() throws Exception {
        when(intercambios.proponer("CAMBIO", 1L, 2L, 4, 9))
            .thenThrow(new IllegalStateException("Ya hay una propuesta pendiente"));
        mvc.perform(post("/partida/CAMBIO/intercambios").sessionAttr("NOMBRE", "Ana")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"idDestinatario\":2,\"posicionOfrecida\":4,\"posicionSolicitada\":9}"))
            .andExpect(status().isConflict());
        verifyNoInteractions(mensajes);
    }

    @Test
    void comunicaUnErrorAlConsultarLasPropuestas() throws Exception {
        when(intercambios.pendientes("CAMBIO", 2L)).thenThrow(new IllegalStateException("La partida terminó"));
        mvc.perform(get("/partida/CAMBIO/intercambios").sessionAttr("NOMBRE", "Luna"))
            .andExpect(status().isConflict());
        verifyNoInteractions(mensajes);
    }

    @Test
    void laPropuestaConfirmadaSigueSiendoExitosaSiNoSePuedeNotificar() throws Exception {
        when(intercambios.proponer("CAMBIO", 1L, 2L, 4, 9)).thenReturn(propuesta);
        doThrow(new IllegalStateException("Sin conexión")).when(mensajes)
            .convertAndSend(eq("/topic/partida/CAMBIO/intercambios"), any(Object.class));
        mvc.perform(post("/partida/CAMBIO/intercambios").sessionAttr("NOMBRE", "Ana")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"idDestinatario\":2,\"posicionOfrecida\":4,\"posicionSolicitada\":9}"))
            .andExpect(status().isOk());
        verify(intercambios, times(1)).proponer("CAMBIO", 1L, 2L, 4, 9);
    }

    private Jugador jugador(Long id, String apodo) {
        Jugador jugador = new Jugador(); jugador.setId(id); jugador.setApodo(apodo); return jugador;
    }

    private CasilleroPropiedad casillero(Long id, int posicion, Jugador propietario) {
        Propiedad propiedad = new Propiedad(id, "Propiedad", 300, 50, propietario, GrupoPropiedades.SAN_JUSTO);
        return new CasilleroPropiedad(id, posicion, "Propiedad", "Localidad", propiedad);
    }
}
