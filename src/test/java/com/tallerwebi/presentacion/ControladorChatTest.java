package com.tallerwebi.presentacion;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControladorChatTest {
    private ServicioChat chat;
    private SimpMessagingTemplate mensajes;
    private MockMvc mvc;
    @BeforeEach void preparar() {
        ServicioPartida partidas = mock(ServicioPartida.class);
        chat = mock(ServicioChat.class); mensajes = mock(SimpMessagingTemplate.class);
        Jugador ana = new Jugador(); ana.setId(2L); ana.setApodo("Ana");
        Partida partida = new Partida(); partida.setJugadores(List.of(ana));
        when(partidas.obtenerPartida("CHAT")).thenReturn(partida);
        mvc = MockMvcBuilders.standaloneSetup(new ControladorChat(partidas, chat, mensajes)).build();
    }
    @Test void enviaConLaIdentidadDeSesionYNofalsificaElAutor() throws Exception {
        var enviado = new ServicioChat.Mensaje(1L, 2L, "Ana", "Hola", 100L);
        when(chat.enviar("CHAT", 2L, "Hola")).thenReturn(enviado);
        mvc.perform(post("/partida/CHAT/chat").sessionAttr("NOMBRE", "Ana").sessionAttr("id", 99L)
                .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Hola\",\"idJugador\":99}"))
            .andExpect(status().isOk());
        verify(chat).enviar("CHAT", 2L, "Hola");
        verify(mensajes).convertAndSend("/topic/partida/CHAT/chat", enviado);
    }
    @Test void sinParticipanteAutenticadoNoLeeNiEscribe() throws Exception {
        mvc.perform(get("/partida/CHAT/chat").sessionAttr("NOMBRE", "Ajeno")).andExpect(status().isForbidden());
        mvc.perform(post("/partida/CHAT/chat").sessionAttr("id", 2L)
            .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Hola\"}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(chat, mensajes);
    }
    @Test void mensajesInvalidosNoSeDifunden() throws Exception {
        when(chat.enviar("CHAT", 2L, " ")).thenThrow(new IllegalArgumentException("Mensaje vacío"));
        mvc.perform(post("/partida/CHAT/chat").sessionAttr("NOMBRE", "Ana")
            .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\" \"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(mensajes);
    }
    @Test void recuperarMensajesUsaLaPartidaYElJugadorDeSesion() throws Exception {
        when(chat.listar("CHAT", 2L)).thenReturn(List.of());
        mvc.perform(get("/partida/CHAT/chat").sessionAttr("NOMBRE", "Ana")).andExpect(status().isOk());
        verify(chat).listar("CHAT", 2L);
    }

    @Test void unFalloDelWebSocketNoConvierteUnMensajeGuardadoEnUnEnvioFallido() throws Exception {
        var enviado = new ServicioChat.Mensaje(1L, 2L, "Ana", "Hola", 100L);
        when(chat.enviar("CHAT", 2L, "Hola")).thenReturn(enviado);
        doThrow(new IllegalStateException("Sin conexión"))
            .when(mensajes).convertAndSend("/topic/partida/CHAT/chat", enviado);

        var resultado = mvc.perform(post("/partida/CHAT/chat").sessionAttr("NOMBRE", "Ana")
            .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"Hola\"}"))
            .andExpect(status().isOk()).andReturn();

        var json = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resultado.getResponse().getContentAsByteArray());
        assertEquals(1L, json.get("id").asLong());
        assertEquals("Hola", json.get("texto").asText());
        verify(chat, times(1)).enviar("CHAT", 2L, "Hola");
    }

    @Test void unUsuarioDeOtraPartidaNoPuedeConsultarSuChat() throws Exception {
        mvc.perform(get("/partida/OTRA/chat").sessionAttr("NOMBRE", "Ana"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(chat, mensajes);
    }
}
