package com.tallerwebi.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioIntercambioPropiedadTest {
    private ServicioPartida partidas;
    private ServicioCasillero casilleros;
    private RepositorioPropiedad propiedades;
    private ServicioIntercambioPropiedadImpl servicio;
    private Partida partida;
    private Jugador ana;
    private Jugador luna;
    private CasilleroPropiedad ofrecida;
    private CasilleroPropiedad solicitada;
    private Clock reloj;
    private static final String CODIGO = "CAMBIO";
    private static final Instant AHORA = Instant.parse("2026-11-05T12:00:00Z");

    @BeforeEach
    void preparar() {
        partidas = mock(ServicioPartida.class);
        casilleros = mock(ServicioCasillero.class);
        propiedades = mock(RepositorioPropiedad.class);
        reloj = mock(Clock.class);
        when(reloj.instant()).thenReturn(AHORA);
        servicio = new ServicioIntercambioPropiedadImpl(partidas, casilleros, propiedades,
            new ServicioParticipacionImpl(), reloj);
        ana = jugador(1L, "Ana", 900);
        luna = jugador(2L, "Luna", 1200);
        partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(List.of(ana, luna));
        when(partidas.obtenerPartida(CODIGO)).thenReturn(partida);
        ofrecida = casillero(10L, 4, "San Justo", ana, 2);
        solicitada = casillero(11L, 9, "Isidro Casanova", luna, 1);
        when(casilleros.buscarCasilleroPropiedad(partida, 4)).thenReturn(ofrecida);
        when(casilleros.buscarCasilleroPropiedad(partida, 9)).thenReturn(solicitada);
    }

    @Test
    void proponerNoTransfiereYLaPropuestaSoloLlegaAlDestinatario() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        assertEquals(List.of(propuesta), servicio.pendientes(CODIGO, 2L));
        assertTrue(servicio.pendientes(CODIGO, 1L).isEmpty());
        assertSame(ana, ofrecida.getPropiedad().getPropietario());
        assertSame(luna, solicitada.getPropiedad().getPropietario());
        assertEquals(AHORA.plusSeconds(300), propuesta.vence());
        verifyNoInteractions(propiedades);
    }

    @Test
    void aceptarIntercambiaAmbasPropiedadesConservandoMejorasYDinero() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        servicio.aceptar(CODIGO, propuesta.id(), 2L);
        assertSame(luna, ofrecida.getPropiedad().getPropietario());
        assertSame(ana, solicitada.getPropiedad().getPropietario());
        assertEquals(2, ofrecida.getPropiedad().getNivelMejora());
        assertEquals(1, solicitada.getPropiedad().getNivelMejora());
        assertEquals(900, ana.getDinero());
        assertEquals(1200, luna.getDinero());
        verify(propiedades).guardarPropiedad(ofrecida.getPropiedad());
        verify(propiedades).guardarPropiedad(solicitada.getPropiedad());
        assertTrue(servicio.pendientes(CODIGO, 2L).isEmpty());
    }

    @Test
    void rechazarNoModificaLosDuenios() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        servicio.rechazar(CODIGO, propuesta.id(), 2L);
        assertSame(ana, ofrecida.getPropiedad().getPropietario());
        assertSame(luna, solicitada.getPropiedad().getPropietario());
        assertTrue(servicio.pendientes(CODIGO, 2L).isEmpty());
        verifyNoInteractions(propiedades);
    }

    @Test
    void otroJugadorNoPuedeResponderUnaPropuestaAjena() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(CODIGO, propuesta.id(), 1L));
        assertThrows(IllegalStateException.class, () -> servicio.rechazar(CODIGO, propuesta.id(), 1L));
        verifyNoInteractions(propiedades);
    }

    @Test
    void siSeVendioLaPropiedadSolicitadaNoTransfiereNinguna() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        solicitada.getPropiedad().setPropietario(null);
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(CODIGO, propuesta.id(), 2L));
        assertSame(ana, ofrecida.getPropiedad().getPropietario());
        verifyNoInteractions(propiedades);
    }

    @Test
    void siSeVendioLaPropiedadOfrecidaNoTransfiereLaSolicitada() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        ofrecida.getPropiedad().setPropietario(null);
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(CODIGO, propuesta.id(), 2L));
        assertSame(luna, solicitada.getPropiedad().getPropietario());
        verifyNoInteractions(propiedades);
    }

    @Test
    void noSePuedeAceptarDosVeces() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        servicio.aceptar(CODIGO, propuesta.id(), 2L);
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(CODIGO, propuesta.id(), 2L));
        verify(propiedades, times(2)).guardarPropiedad(any());
    }

    @Test
    void lasPropuestasVencidasNoSeAceptan() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        when(reloj.instant()).thenReturn(AHORA.plusSeconds(300));
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(CODIGO, propuesta.id(), 2L));
        assertTrue(servicio.pendientes(CODIGO, 2L).isEmpty());
        verifyNoInteractions(propiedades);
    }

    @Test
    void noPermiteAutointercambiosNiOfrecerUnaPropiedadAjena() {
        assertThrows(IllegalArgumentException.class, () -> servicio.proponer(CODIGO, 1L, 1L, 4, 4));
        assertThrows(IllegalStateException.class, () -> servicio.proponer(CODIGO, 1L, 2L, 9, 4));
        verifyNoInteractions(propiedades);
    }

    @Test
    void unJugadorEnQuiebraNoPuedeAceptar() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        luna.setEnBancarrota(true);
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(CODIGO, propuesta.id(), 2L));
        assertTrue(servicio.pendientes(CODIGO, 1L).isEmpty());
        verifyNoInteractions(propiedades);
    }

    @Test
    void unaPartidaFinalizadaNoPermiteIntercambios() {
        var propuesta = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        partida.setEstado(EstadoPartida.FINALIZADA);
        assertThrows(IllegalStateException.class, () -> servicio.aceptar(CODIGO, propuesta.id(), 2L));
        verifyNoInteractions(propiedades);
    }

    @Test
    void unaPropiedadNoPuedeTenerDosPropuestasPendientesAunquePidaOtraACambio() {
        servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        when(casilleros.buscarCasilleroPropiedad(partida, 5)).thenReturn(casillero(12L, 5, "Otra", luna, 0));
        assertThrows(IllegalStateException.class, () -> servicio.proponer(CODIGO, 1L, 2L, 4, 9));
        assertThrows(IllegalStateException.class, () -> servicio.proponer(CODIGO, 1L, 2L, 4, 5));
        assertEquals(1, servicio.enviadas(CODIGO, 1L).size());
        assertTrue(servicio.enviadas(CODIGO, 2L).isEmpty());
    }

    @Test
    void rechazarLiberaLaPropiedadParaUnaNuevaPropuesta() {
        var primera = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        servicio.rechazar(CODIGO, primera.id(), 2L);
        var segunda = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        assertNotEquals(primera.id(), segunda.id());
        assertEquals(1, servicio.enviadas(CODIGO, 1L).size());
    }

    @Test
    void vencerLiberaLaPropiedadParaUnaNuevaPropuesta() {
        var primera = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        when(reloj.instant()).thenReturn(AHORA.plusSeconds(300));
        var segunda = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        assertNotEquals(primera.id(), segunda.id());
        assertEquals(List.of(segunda), servicio.enviadas(CODIGO, 1L));
    }

    @Test
    void aceptarPermiteQueElNuevoPropietarioPropongaOtroIntercambio() {
        var primera = servicio.proponer(CODIGO, 1L, 2L, 4, 9);
        servicio.aceptar(CODIGO, primera.id(), 2L);
        assertNotNull(servicio.proponer(CODIGO, 2L, 1L, 4, 9));
    }

    private Jugador jugador(Long id, String nombre, int dinero) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo(nombre);
        jugador.setDinero(dinero);
        return jugador;
    }

    private CasilleroPropiedad casillero(Long id, int posicion, String nombre, Jugador propietario, int nivel) {
        Propiedad propiedad = new Propiedad(id, nombre, 300, 50, propietario, GrupoPropiedades.SAN_JUSTO);
        propiedad.setNivelMejora(nivel);
        return new CasilleroPropiedad(id, posicion, nombre, "Localidad", propiedad);
    }
}
