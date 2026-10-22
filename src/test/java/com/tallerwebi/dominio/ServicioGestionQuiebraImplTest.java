package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
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

        servicio = new ServicioGestionQuiebraImpl(
                repositorioDeuda,
                repositorioPartida,
                servicioPropiedad,
                repositorioPropiedad,
                publicadorQuiebra
        );
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




}