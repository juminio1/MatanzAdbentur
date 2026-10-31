package com.tallerwebi.dominio;
import com.tallerwebi.dominio.servicios.ServicioEconomiaImpl;
import com.tallerwebi.dominio.servicios.ServicioTurnoImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.infraestructura.RepositorioPartida;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.servicios.ServicioPropiedadImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.tallerwebi.dominio.enums.GrupoPropiedades.SAN_JUSTO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServicioPropiedadTest {
    private final ServicioEconomia economia = org.mockito.Mockito.spy(new ServicioEconomiaImpl());
    private RepositorioPropiedad repositorioPropiedadMock;
    private RepositorioPartida servicioPartidaMock;
    private ServicioPropiedad servicioPropiedad;
    private final com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero casilleros = mock(com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero.class);

    @BeforeEach
    public void init() {
        repositorioPropiedadMock = mock(RepositorioPropiedad.class);
        servicioPartidaMock = mock(RepositorioPartida.class);
        servicioPropiedad = new ServicioPropiedadImpl(repositorioPropiedadMock,
                servicioPartidaMock,
                casilleros,
                economia,
                new ServicioTurnoImpl(new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl()), new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
    }

    @Test
    public void ambasFormasDePagarAlquilerConservanElOrdenDeValidacion() {
        Jugador jugador = new Jugador();
        Propiedad propiedad = new Propiedad();
        assertThrows(com.tallerwebi.dominio.excepcion.PropiedadInvalidaException.class,
                () -> servicioPropiedad.pagarAlquiler(null, jugador));
        assertThrows(com.tallerwebi.dominio.excepcion.PropiedadInvalidaException.class,
                () -> servicioPropiedad.pagarAlquiler(null, jugador, -1));
        assertThrows(com.tallerwebi.dominio.excepcion.JugadorInvalidoException.class,
                () -> servicioPropiedad.pagarAlquiler(propiedad, null));
        assertThrows(com.tallerwebi.dominio.excepcion.JugadorInvalidoException.class,
                () -> servicioPropiedad.pagarAlquiler(propiedad, null, -1));
        assertThrows(com.tallerwebi.dominio.excepcion.PropiedadInvalidaException.class,
                () -> servicioPropiedad.pagarAlquiler(propiedad, jugador, -1));
    }

    @Test
    public void jugadorDeberiaComprarUnaPropiedadDisponible() {
        Long idPropiedad = 1L;
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        prepararPartidaEnCurso(jugador);
        jugador.setDinero(1000);
        Propiedad propiedad = new Propiedad(1L,"Plaza San Justo",  500, 100, null, SAN_JUSTO);
        when(repositorioPropiedadMock.buscarPropiedadPorId(idPropiedad)).thenReturn(propiedad);
        // Simula el descuento del dinero al comprar.
        org.mockito.Mockito.doAnswer(invocacion -> {
            Jugador jugadorQuePaga = invocacion.getArgument(0);
            Integer monto = invocacion.getArgument(1);
            jugadorQuePaga.setDinero(jugadorQuePaga.getDinero() - monto);
            return null;
        }).when(economia).restarDinero(jugador, 500);
        servicioPropiedad.comprarPropiedad(idPropiedad, jugador);
        assertFalse(servicioPropiedad.estaDisponible(propiedad));
        assertEquals(jugador, propiedad.getPropietario());
        assertEquals(500, jugador.getDinero());
    }

    @Test
    public void jugadorDeberiaPagarAlquilerAlPropietario() {
        Jugador visitante = new Jugador();
        visitante.setId(1L);
        visitante.setDinero(1000);
        prepararPartidaEnCurso(visitante);
        Jugador propietario = new Jugador();
        propietario.setId(2L);
        propietario.setDinero(500);
        Propiedad propiedad = new Propiedad(1L,"Plaza San Justo",  500, 100, propietario,  SAN_JUSTO);

        // Simula el descuento al visitante.
        org.mockito.Mockito.doAnswer(invocacion -> {
            Jugador jugadorQuePaga = invocacion.getArgument(0);
            Integer monto = invocacion.getArgument(1);
            jugadorQuePaga.setDinero(jugadorQuePaga.getDinero() - monto);
            return null;
        }).when(economia).restarDinero(visitante, 100);

        // Simula el ingreso del alquiler al propietario.
        org.mockito.Mockito.doAnswer(invocacion -> {
            Jugador jugadorQueCobra = invocacion.getArgument(0);
            Integer monto = invocacion.getArgument(1);
            jugadorQueCobra.setDinero(jugadorQueCobra.getDinero() + monto);
            return null;
        }).when(economia).sumarDinero(propietario, 100);
        servicioPropiedad.pagarAlquiler(propiedad, visitante);
        assertEquals(900, visitante.getDinero());
        assertEquals(600, propietario.getDinero());
    }

    @Test
    public void propietarioNoDeberiaPagarAlquilerPorSuPropiedad() {
        Jugador propietario = new Jugador();
        propietario.setId(1L);
        propietario.setDinero(1000);
        prepararPartidaEnCurso(propietario);
        Propiedad propiedad = new Propiedad(1L,"Plaza San Justo", 500, 100, propietario,  SAN_JUSTO);
        servicioPropiedad.pagarAlquiler(propiedad, propietario);
        assertEquals(1000, propietario.getDinero());
    }

    @Test
    public void jugadorDeberiaMejorarPropiedadSiEsDuenioDeTodoElGrupo() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        prepararPartidaEnCurso(jugador);
        jugador.setDinero(2000);

        Propiedad propiedad1 = new Propiedad(1L, "Plaza San Justo", 500, 100, jugador, SAN_JUSTO);

        Propiedad propiedad2 = new Propiedad( 2L, "UNLAM", 600, 120, jugador, SAN_JUSTO);
        when(repositorioPropiedadMock.buscarPropiedadPorId(1L))
                .thenReturn(propiedad1);

        Propiedad propiedad3 = new Propiedad(3L, "Shopping", 600, 120, jugador, SAN_JUSTO);
        var c1 = new com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad(1L, 4, "Plaza", "", propiedad1);
        var c2 = new com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad(2L, 5, "UNLaM", "", propiedad2);
        var c3 = new com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad(3L, 7, "Shopping", "", propiedad3);
        c1.setPartidaId(1L);
        c2.setPartidaId(1L);
        c3.setPartidaId(1L);
        when(casilleros.obtenerTodosLosCasilleros()).thenReturn(List.of(c1, c2, c3));

        servicioPropiedad.mejorarPropiedad(1L, jugador);

        assertEquals(1, propiedad1.getNivelMejora());
    }

    @Test
    public void propiedadSinDuenioDeberiaEstarDisponible() {
        Propiedad propiedad = new Propiedad( 1L, "Plaza San Justo", 500, 100, null, SAN_JUSTO);
        assertTrue(servicioPropiedad.estaDisponible(propiedad));
    }

    @Test
    public void alquilerDeberiaAumentarSegunElNivelDeMejoras() {
        Propiedad propiedad = new Propiedad(1L, "Plaza San Justo", 500, 100, null, SAN_JUSTO);
        propiedad.setNivelMejora(2);
        assertEquals(300, servicioPropiedad.calcularAlquiler(propiedad));
    }

    @Test
    public void alquilerSinMejorasDeberiaMantenerElPrecioBase() {
        Propiedad propiedad = new Propiedad(1L, "Plaza San Justo", 500, 100, null, SAN_JUSTO);
        assertEquals(100, servicioPropiedad.calcularAlquiler(propiedad));
    }

    private void prepararPartidaEnCurso(Jugador jugador) {
        Partida partida = new Partida();
        partida.setId(1L);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.getJugadores().add(jugador);
        when(servicioPartidaMock.buscarPartidaPorJugador(jugador.getId())).thenReturn(partida);
    }

    @Test
    public void rechazaAlquilerCalculadoEnPartidaFinalizadaSinCambiarSaldos() {
        Jugador pagador = new Jugador();
        pagador.setId(1L);
        pagador.setDinero(1000);
        Jugador propietario = new Jugador();
        propietario.setId(2L);
        propietario.setDinero(500);
        Propiedad propiedad = new Propiedad(1L, "Plaza", 500, 100, propietario, SAN_JUSTO);
        propiedad.setNivelMejora(1);
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.FINALIZADA);
        partida.getJugadores().addAll(List.of(pagador, propietario));
        when(servicioPartidaMock.buscarPartidaPorJugador(1L)).thenReturn(partida);

        assertThrows(IllegalStateException.class,
                () -> servicioPropiedad.pagarAlquiler(propiedad, pagador));

        assertEquals(1000, pagador.getDinero());
        assertEquals(500, propietario.getDinero());
        assertSame(propietario, propiedad.getPropietario());
        assertEquals(1, propiedad.getNivelMejora());
        org.mockito.Mockito.verifyNoInteractions(economia);
        org.mockito.Mockito.verify(servicioPartidaMock, org.mockito.Mockito.never()).actualizarJugador(
                org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(repositorioPropiedadMock, org.mockito.Mockito.never()).guardarPropiedad(
                org.mockito.ArgumentMatchers.any());
    }


    @Test
    public void rechazaAlquilerConMontoEnPartidaFinalizadaSinCambiarSaldos() {
        Jugador pagador = new Jugador();
        pagador.setId(1L);
        pagador.setDinero(1000);
        Jugador propietario = new Jugador();
        propietario.setId(2L);
        propietario.setDinero(500);
        Propiedad propiedad = new Propiedad(1L, "Plaza", 500, 100, propietario, SAN_JUSTO);
        propiedad.setNivelMejora(1);
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.FINALIZADA);
        partida.getJugadores().addAll(List.of(pagador, propietario));
        when(servicioPartidaMock.buscarPartidaPorJugador(1L)).thenReturn(partida);

        assertThrows(IllegalStateException.class,
                () -> servicioPropiedad.pagarAlquiler(propiedad, pagador, 100));

        assertEquals(1000, pagador.getDinero());
        assertEquals(500, propietario.getDinero());
        assertSame(propietario, propiedad.getPropietario());
        assertEquals(1, propiedad.getNivelMejora());
        org.mockito.Mockito.verifyNoInteractions(economia);
        org.mockito.Mockito.verify(servicioPartidaMock, org.mockito.Mockito.never()).actualizarJugador(
                org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(repositorioPropiedadMock, org.mockito.Mockito.never()).guardarPropiedad(
                org.mockito.ArgumentMatchers.any());
    }


    @Test
    public void rechazaAlquilerCalculadoEnPartidaEnEsperaSinCambiarSaldos() {
        Jugador pagador = new Jugador();
        pagador.setId(1L);
        pagador.setDinero(1000);
        Jugador propietario = new Jugador();
        propietario.setId(2L);
        propietario.setDinero(500);
        Propiedad propiedad = new Propiedad(1L, "Plaza", 500, 100, propietario, SAN_JUSTO);
        propiedad.setNivelMejora(1);
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.getJugadores().addAll(List.of(pagador, propietario));
        when(servicioPartidaMock.buscarPartidaPorJugador(1L)).thenReturn(partida);

        assertThrows(IllegalStateException.class,
                () -> servicioPropiedad.pagarAlquiler(propiedad, pagador));

        assertEquals(1000, pagador.getDinero());
        assertEquals(500, propietario.getDinero());
        assertSame(propietario, propiedad.getPropietario());
        assertEquals(1, propiedad.getNivelMejora());
        org.mockito.Mockito.verifyNoInteractions(economia);
        org.mockito.Mockito.verify(servicioPartidaMock, org.mockito.Mockito.never()).actualizarJugador(
                org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(repositorioPropiedadMock, org.mockito.Mockito.never()).guardarPropiedad(
                org.mockito.ArgumentMatchers.any());
    }


    @Test
    public void rechazaAlquilerConMontoEnPartidaEnEsperaSinCambiarSaldos() {
        Jugador pagador = new Jugador();
        pagador.setId(1L);
        pagador.setDinero(1000);
        Jugador propietario = new Jugador();
        propietario.setId(2L);
        propietario.setDinero(500);
        Propiedad propiedad = new Propiedad(1L, "Plaza", 500, 100, propietario, SAN_JUSTO);
        propiedad.setNivelMejora(1);
        Partida partida = new Partida();
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.getJugadores().addAll(List.of(pagador, propietario));
        when(servicioPartidaMock.buscarPartidaPorJugador(1L)).thenReturn(partida);

        assertThrows(IllegalStateException.class,
                () -> servicioPropiedad.pagarAlquiler(propiedad, pagador, 100));

        assertEquals(1000, pagador.getDinero());
        assertEquals(500, propietario.getDinero());
        assertSame(propietario, propiedad.getPropietario());
        assertEquals(1, propiedad.getNivelMejora());
        org.mockito.Mockito.verifyNoInteractions(economia);
        org.mockito.Mockito.verify(servicioPartidaMock, org.mockito.Mockito.never()).actualizarJugador(
                org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(repositorioPropiedadMock, org.mockito.Mockito.never()).guardarPropiedad(
                org.mockito.ArgumentMatchers.any());
    }
}
