package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.servicios.ServicioGestionQuiebraImpl.DeclaracionQuiebraInvalidaException;
import org.mockito.ArgumentCaptor;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.ServicioGestionQuiebraImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServicioGestionQuiebraImplTest {

    private RepositorioDeudaPendiente repositorioDeuda;
    private RepositorioPartida repositorioPartida;
    private RepositorioPropiedad repositorioPropiedad;
    private ServicioPropiedad servicioPropiedad;
    private PublicadorEventos<EventoJugadorQuebrado> publicadorQuiebra;
    private ServicioGestionQuiebraImpl servicio;

    @BeforeEach
    void preparar() {
        repositorioDeuda = mock(RepositorioDeudaPendiente.class);
        repositorioPartida = mock(RepositorioPartida.class);
        repositorioPropiedad = mock(RepositorioPropiedad.class);
        servicioPropiedad = mock(ServicioPropiedad.class);
        publicadorQuiebra = mock(PublicadorEventos.class);

        servicio = new ServicioGestionQuiebraImpl(repositorioDeuda,
                repositorioPartida,
                servicioPropiedad,
                repositorioPropiedad,
                publicadorQuiebra,
                new com.tallerwebi.dominio.servicios.ServicioEconomiaImpl(), new com.tallerwebi.dominio.servicios.ServicioParticipacionImpl());
    }

    @Test
    void queAlVenderUnaPropiedadYReunirDineroSuficienteSePagueLaDeuda() {
        String codigo = "PARTIDA123";
        Long idJugador = 1L;
        Long idPropiedad = 10L;

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(100);

        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setJugadorDeudor(jugador);
        deuda.setMontoPendiente(200);

        when(repositorioDeuda.buscarPorPartidaYJugador(codigo, idJugador))
                .thenReturn(deuda);

        doAnswer(invocacion -> {
            jugador.setDinero(jugador.getDinero() + 150);
            return 150;
        }).when(servicioPropiedad)
                .venderPropiedadParaJugador(idPropiedad, idJugador);

        Boolean resultado = servicio.venderPropiedadYResolverDeuda(
                codigo, idJugador, idPropiedad
        );

        assertTrue(resultado);
        assertEquals(50, jugador.getDinero());

        verify(servicioPropiedad).venderPropiedadParaJugador(idPropiedad, idJugador);
        verify(repositorioPartida).actualizarJugador(jugador);
        verify(repositorioDeuda).eliminar(deuda);
        verifyNoInteractions(publicadorQuiebra);
    }

    @Test
    void queSiLaVentaNoAlcanzaYQuedanPropiedadesLaDeudaSigaPendiente() {
        String codigo = "PARTIDA123";
        Long idJugador = 1L;
        Long idPropiedad = 10L;

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(100);

        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setJugadorDeudor(jugador);
        deuda.setMontoPendiente(400);

        when(repositorioDeuda.buscarPorPartidaYJugador(codigo, idJugador))
                .thenReturn(deuda);

        doAnswer(invocacion -> {
            jugador.setDinero(jugador.getDinero() + 150);
            return 150;
        }).when(servicioPropiedad)
                .venderPropiedadParaJugador(idPropiedad, idJugador);

        when(repositorioPropiedad.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(java.util.List.of(new com.tallerwebi.dominio.entidades.Propiedad()));

        Boolean resultado = servicio.venderPropiedadYResolverDeuda(
                codigo, idJugador, idPropiedad
        );

        assertFalse(resultado);
        assertEquals(250, jugador.getDinero());

        verify(servicioPropiedad).venderPropiedadParaJugador(idPropiedad, idJugador);
        verify(repositorioDeuda, never()).eliminar(deuda);
        verifyNoInteractions(publicadorQuiebra);
    }

    @Test
    void queSiVendeSuUltimaPropiedadYNoAlcanzaEntreEnQuiebra() {
        String codigo = "PARTIDA123";
        Long idJugador = 1L;
        Long idPropiedad = 10L;

        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(100);

        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(java.util.List.of(jugador));
        when(repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigo)).thenReturn(partida);

        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setPartida(partida);
        deuda.setJugadorDeudor(jugador);
        deuda.setMontoPendiente(400);

        when(repositorioDeuda.buscarPorPartidaYJugador(codigo, idJugador))
                .thenReturn(deuda);

        doAnswer(invocacion -> {
            jugador.setDinero(jugador.getDinero() + 150);
            return 150;
        }).when(servicioPropiedad)
                .venderPropiedadParaJugador(idPropiedad, idJugador);

        when(repositorioPropiedad.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(java.util.List.of());

        Boolean resultado = servicio.venderPropiedadYResolverDeuda(
                codigo, idJugador, idPropiedad
        );

        assertFalse(resultado);
        assertEquals(250, jugador.getDinero());
        assertTrue(jugador.getEnBancarrota());

        verify(repositorioPartida).actualizarJugador(jugador);
        verify(repositorioDeuda).eliminar(deuda);
        verify(publicadorQuiebra).notificar(any(EventoJugadorQuebrado.class));
    }

    @Test
    void queSePuedaDeclararQuiebraConDeudaYPropiedadesSinTenerElTurno() {
        DeudaPendiente deuda = prepararDeudaParaQuiebra();
        Jugador jugador = deuda.getJugadorDeudor();
        com.tallerwebi.dominio.entidades.Propiedad propiedad = new com.tallerwebi.dominio.entidades.Propiedad();
        propiedad.setPropietario(jugador);
        deuda.getPartida().setIndiceTurnoActual(1);

        Partida resultado = servicio.declararQuiebra("PARTIDA123", 1L);

        assertSame(deuda.getPartida(), resultado);
        assertTrue(jugador.getEnBancarrota());
        assertFalse(jugador.getAbandono());
        assertEquals(100, jugador.getDinero());
        assertSame(jugador, propiedad.getPropietario());
        verify(repositorioPartida).actualizarJugador(jugador);
        verify(repositorioDeuda).eliminar(deuda);
        ArgumentCaptor<EventoJugadorQuebrado> evento = ArgumentCaptor.forClass(EventoJugadorQuebrado.class);
        verify(publicadorQuiebra, times(1)).notificar(evento.capture());
        assertSame(jugador, evento.getValue().jugador());
        assertSame(deuda.getPartida(), evento.getValue().partida());
        verifyNoInteractions(servicioPropiedad, repositorioPropiedad);
    }

    @Test
    void queDeclararQuiebraDevuelvaElEstadoDecididoPorLosObservadores() {
        DeudaPendiente deuda = prepararDeudaParaQuiebra();
        doAnswer(invocacion -> {
            EventoJugadorQuebrado evento = invocacion.getArgument(0);
            evento.partida().setEstado(EstadoPartida.FINALIZADA);
            return null;
        }).when(publicadorQuiebra).notificar(any(EventoJugadorQuebrado.class));

        Partida resultado = servicio.declararQuiebra("PARTIDA123", 1L);

        assertSame(deuda.getPartida(), resultado);
        assertEquals(EstadoPartida.FINALIZADA, resultado.getEstado());
    }

    @Test
    void queNoSeDeclareQuiebraSinDeudaAunqueElSaldoSeaCero() {
        DeudaPendiente deuda = prepararDeudaParaQuiebra();
        deuda.getJugadorDeudor().setDinero(0);
        when(repositorioDeuda.buscarPorPartidaYJugador("PARTIDA123", 1L)).thenReturn(null);
        verificarDeclaracionRechazada();
    }

    @Test
    void queNoSeDeclareQuiebraSiLaPartidaNoExiste() {
        when(repositorioPartida.buscarPartidaActivaPorCodigoUnico("PARTIDA123")).thenReturn(null);
        verificarDeclaracionRechazada();
    }

    @Test
    void queNoSeDeclareQuiebraSiLaPartidaEstaEnEsperaOFinalizada() {
        DeudaPendiente deuda = prepararDeudaParaQuiebra();
        deuda.getPartida().setEstado(EstadoPartida.EN_ESPERA);
        verificarDeclaracionRechazada();
        deuda.getPartida().setEstado(EstadoPartida.FINALIZADA);
        verificarDeclaracionRechazada();
    }

    @Test
    void queNoSeDeclareQuiebraDeUnJugadorAjenoALaPartida() {
        DeudaPendiente deuda = prepararDeudaParaQuiebra();
        deuda.getPartida().setJugadores(java.util.List.of());
        verificarDeclaracionRechazada();
    }

    @Test
    void queNoSeDeclareQuiebraDeUnJugadorAbandonado() {
        prepararDeudaParaQuiebra().getJugadorDeudor().setAbandono(true);
        verificarDeclaracionRechazada();
    }

    @Test
    void queNoSeDeclareQuiebraDeUnJugadorYaQuebrado() {
        prepararDeudaParaQuiebra().getJugadorDeudor().setEnBancarrota(true);
        verificarDeclaracionRechazada();
    }

    @Test
    void queLosFallosDelObservadorNoSeConfundanConSolicitudesInvalidas() {
        prepararDeudaParaQuiebra();
        IllegalStateException fallo = new IllegalStateException("Fallo del observador");
        doThrow(fallo).when(publicadorQuiebra).notificar(any(EventoJugadorQuebrado.class));
        assertSame(fallo, assertThrows(IllegalStateException.class,
                () -> servicio.declararQuiebra("PARTIDA123", 1L)));
    }

    // Prepara una partida en curso con un jugador endeudado.
    private DeudaPendiente prepararDeudaParaQuiebra() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setOrden(0);
        jugador.setDinero(100);
        Partida partida = new Partida();
        partida.setCodigoUnico("PARTIDA123");
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setJugadores(java.util.List.of(jugador));
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setPartida(partida);
        deuda.setJugadorDeudor(jugador);
        deuda.setMontoPendiente(400);
        when(repositorioPartida.buscarPartidaActivaPorCodigoUnico("PARTIDA123")).thenReturn(partida);
        when(repositorioDeuda.buscarPorPartidaYJugador("PARTIDA123", 1L)).thenReturn(deuda);
        return deuda;
    }

    private void verificarDeclaracionRechazada() {
        assertThrows(DeclaracionQuiebraInvalidaException.class,
                () -> servicio.declararQuiebra("PARTIDA123", 1L));
        verify(repositorioPartida, never()).actualizarJugador(any());
        verify(repositorioDeuda, never()).eliminar(any());
        verifyNoInteractions(publicadorQuiebra);
    }
}
