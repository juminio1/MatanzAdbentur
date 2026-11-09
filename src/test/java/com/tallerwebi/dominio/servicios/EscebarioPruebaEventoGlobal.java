
package com.tallerwebi.dominio.servicios;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.eventos.EventoTurnoCambiado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.*;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class EscenarioPruebaEventoGlobal {

    final Partida partida = new Partida();
    final ServicioParticipacion participacion = new ServicioParticipacionImpl();
    final RepositorioPartida repositorio = mock(RepositorioPartida.class);
    final RepositorioEventoGlobal catalogo = mock(RepositorioEventoGlobal.class);
    final ServicioDado dados = mock(ServicioDado.class);
    final ServicioGestionQuiebra quiebras = mock(ServicioGestionQuiebra.class);
    final GestorTemporizadorPartida temporizador = mock(GestorTemporizadorPartida.class);
    final ServicioPartidaImpl partidas;
    final ServicioEventoImpl eventos;

    EscenarioPruebaEventoGlobal(
        Integer cantidadJugadores,
        TipoEventoGlobal tipoEvento,
        Integer indiceSeleccionado
    ) {
        partida.setCodigoUnico("GLOBAL");
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setRondaActual(4);
        partida.setIndiceTurnoActual(cantidadJugadores - 1);

        for (Integer indice = 0; indice < cantidadJugadores; indice++) {
            Jugador jugador = new Jugador();

            jugador.setId(Long.valueOf(indice + 1));
            jugador.setOrden(indice);
            jugador.setApodo("Jugador " + indice);
            jugador.setDinero(2000);
            jugador.setPosicionActual(8 + indice * 5);
            jugador.setUltimaRondaConOportunidad(
                Objects.equals(indice, cantidadJugadores - 1) ? 3 : 4
            );

            partida.getJugadores().add(jugador);
        }

        when(repositorio.buscarPartidaActivaPorCodigoUnico("GLOBAL"))
            .thenReturn(partida);

        when(repositorio.buscarPartidaPorCodigoUnico("GLOBAL"))
            .thenReturn(partida);

        when(repositorio.buscarPartidaPorJugador(anyLong()))
            .thenReturn(partida);

        PlatformTransactionManager transacciones = mock(
            PlatformTransactionManager.class
        );

        when(transacciones.getTransaction(any())).thenReturn(new SimpleTransactionStatus());

        PublicadorEventos<EventoTurnoCambiado> turnos = new PublicadorEventos<>(List.of());

        PublicadorEventos<EventoJugadorAbandonado> abandonos =
            new PublicadorEventos<>(List.of());

        partidas = new ServicioPartidaImpl(
            repositorio,
            mock(ServicioInicioPartida.class),
            temporizador,
            new ServicioTurnoImpl(participacion),
            turnos,
            abandonos,
            quiebras,
            mock(ServicioFinalizacionPartida.class),
            transacciones
        );

        ReflectionTestUtils.setField(
            partidas,
            "participacion",
            participacion
        );

        ServicioEconomiaImpl economia = new ServicioEconomiaImpl();

        AplicadorConsecuenciaEventoGlobal aplicador =
            new AplicadorConsecuenciaEventoGlobal(
                economia,
                new ServicioMovimientoImpl(
                    economia,
                    repositorio,
                    participacion
                ),
                quiebras,
                participacion
            );

        eventos = new ServicioEventoImpl(
            catalogo,
            repositorio,
            participacion,
            dados,
            aplicador,
            partidas,
            temporizador,
            mock(NotificadorPartida.class)
        );

        ReflectionTestUtils.setField(
            partidas,
            "eventosGlobales",
            eventos
        );

        RandomGenerator generadorAleatorio = mock(RandomGenerator.class);

        when(generadorAleatorio.nextInt(6))
            .thenReturn(tipoEvento.ordinal());

        when(generadorAleatorio.nextInt(cantidadJugadores))
            .thenReturn(indiceSeleccionado);

        eventos.usarAleatorio(generadorAleatorio);

      
        when(catalogo.obtenerTodos()).thenReturn(
            Arrays.stream(TipoEventoGlobal.values())
                .map(tipo -> {
                    EventoGlobal evento = new EventoGlobal();

                    evento.setNombre(tipo.name());
                    evento.setDescripcion(tipo.name());
                    evento.setTipoEventoGlobal(tipo);

                    Integer[] valores = {0, -3, 0, 500, 400, 1};
                    evento.setValor(valores[tipo.ordinal()]);
                    
                    return evento;
                })
                .toList()
        );


        when(dados.tirarDados()).thenReturn(
            new ResultadoTirada(6, 6),
            new ResultadoTirada(1, 1)
        );

        if (tipoEvento == TipoEventoGlobal.PIQUETE) {
            when(dados.tirarDados()).thenReturn(
                new ResultadoTirada(1, 1),
                new ResultadoTirada(6, 6)
            );
        }
    }

    Jugador jugador(Integer indice) {
        return partida.getJugadores().get(indice);
    }

    void iniciar() {
        partidas.avanzarTurno("GLOBAL");
    }

    void tirarTodos() {
        if (!partida.getEventoGlobal().estaResuelto()) {
            Integer etapaActual = partida.getEventoGlobal().getEtapa();

            for (Jugador jugador : partida.getJugadores()) {
                if (partida.getEventoGlobal().getHabilitados().contains(jugador.getId())) {
                    eventos.tirar(
                        "GLOBAL",
                        jugador.getId(),
                        5,
                        etapaActual
                    );
                }
            }
        }
    }

    void confirmarTodos() {
        partida.getJugadores()
            .stream()
            .filter(participacion::estaActivo)
            .forEach(jugador -> eventos.confirmar(
                "GLOBAL",
                jugador.getId(),
                5
            ));
    }
}
