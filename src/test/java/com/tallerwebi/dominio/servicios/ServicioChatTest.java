package com.tallerwebi.dominio.servicios;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.infraestructura.RepositorioChat;
import java.util.List;
import org.junit.jupiter.api.*;

class ServicioChatTest {
    private ServicioChatImpl servicio;
    private RepositorioChat repositorio;
    private Jugador ana;
    private Partida partida;
    @BeforeEach void preparar() {
        ServicioPartida partidas = mock(ServicioPartida.class);
        repositorio = mock(RepositorioChat.class);
        ana = new Jugador(); ana.setId(2L); ana.setApodo("Ana");
        partida = new Partida(); partida.setId(9L); partida.setJugadores(List.of(ana));
        when(partidas.obtenerPartida("CHAT")).thenReturn(partida);
        servicio = new ServicioChatImpl(partidas, repositorio);
    }
    @Test void guardaElMensajeConElAutorRealSinCambiarSuSaldoNiSuTurno() {
        var resultado = servicio.enviar("CHAT", 2L, "  Hola grupo  ");
        assertEquals("Hola grupo", resultado.texto());
        assertEquals("Ana", resultado.apodo());
        assertEquals(2L, resultado.idJugador());
        assertNotNull(resultado.fecha());
        var captura = org.mockito.ArgumentCaptor.forClass(MensajeChat.class);
        verify(repositorio).guardar(captura.capture());
        assertSame(partida, captura.getValue().getPartida());
        assertSame(ana, captura.getValue().getAutor());
        assertEquals(0, ana.getDinero());
        assertEquals(0, ana.getPosicionActual());
    }
    @Test void rechazaMensajesVaciosOLargosSinGuardar() {
        for (String texto : new String[] {null, "", "  \n\t", "a".repeat(501)}) {
            assertThrows(IllegalArgumentException.class, () -> servicio.enviar("CHAT", 2L, texto));
        }
        verifyNoInteractions(repositorio);
    }
    @Test void unJugadorAjenoNoPuedeLeerNiEscribir() {
        assertThrows(SecurityException.class, () -> servicio.enviar("CHAT", 99L, "Hola"));
        assertThrows(SecurityException.class, () -> servicio.listar("CHAT", 99L));
        verifyNoInteractions(repositorio);
    }
    @Test void quienAbandonoNoPuedeEnviar() {
        ana.setAbandono(true);
        assertThrows(IllegalStateException.class, () -> servicio.enviar("CHAT", 2L, "Hola"));
        verifyNoInteractions(repositorio);
    }
    @Test void consultaSoloLaPartidaPedidaYOrdenaLosMensajes() {
        MensajeChat primero = mock(MensajeChat.class); MensajeChat segundo = mock(MensajeChat.class);
        when(primero.getId()).thenReturn(1L); when(segundo.getId()).thenReturn(2L);
        when(primero.getAutor()).thenReturn(ana); when(segundo.getAutor()).thenReturn(ana);
        when(repositorio.ultimos(9L)).thenReturn(List.of(segundo, primero));
        assertEquals(List.of(1L, 2L), servicio.listar("CHAT", 2L).stream().map(m -> m.id()).toList());
        verify(repositorio).ultimos(9L);
    }

    @Test void unaPartidaInexistenteOUnActorNuloNoPuedenUsarElChat() {
        assertThrows(SecurityException.class, () -> servicio.listar("INEXISTENTE", 2L));
        assertThrows(SecurityException.class, () -> servicio.enviar("INEXISTENTE", 2L, "Hola"));
        assertThrows(SecurityException.class, () -> servicio.listar("CHAT", null));
        assertThrows(SecurityException.class, () -> servicio.enviar("CHAT", null, "Hola"));
        verifyNoInteractions(repositorio);
    }

    @Test void aceptaElLimiteDeQuinientosCaracteresSinRecortarElMensaje() {
        String texto = "a".repeat(500);
        assertEquals(texto, servicio.enviar("CHAT", 2L, texto).texto());
        verify(repositorio).guardar(any(MensajeChat.class));
    }

    @Test void unJugadorQuebradoPuedeSeguirConversandoSinModificarSuEstado() {
        ana.setEnBancarrota(true);
        assertEquals("Sigo mirando", servicio.enviar("CHAT", 2L, "Sigo mirando").texto());
        assertTrue(ana.getEnBancarrota());
        assertEquals(0, ana.getDinero());
        verify(repositorio).guardar(any(MensajeChat.class));
    }
}
