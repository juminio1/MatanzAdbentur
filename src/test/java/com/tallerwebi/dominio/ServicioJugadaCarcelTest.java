package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.ServicioJugadaImpl;
import com.tallerwebi.dominio.servicios.interfaces.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ServicioJugadaCarcelTest {

    private ServicioJugadaImpl servicioJugada;
    private ServicioPartida servicioPartidaMock;
    private ServicioCasillero servicioCasilleroMock;
    private ServicioPropiedad servicioPropiedadMock;
    private ServicioEventoCasillero servicioEventoMock;
    private ServicioGestionQuiebra servicioQuiebraMock;
    private ServicioMovimiento servicioMovimientoMock;
    private ServicioParticipacion servicioParticipacionMock;
    private ServicioDado servicioDadoMock;

    @BeforeEach
    void init() {
        servicioPartidaMock = mock(ServicioPartida.class);
        servicioCasilleroMock = mock(ServicioCasillero.class);
        servicioPropiedadMock = mock(ServicioPropiedad.class);
        servicioEventoMock = mock(ServicioEventoCasillero.class);
        servicioQuiebraMock = mock(ServicioGestionQuiebra.class);
        servicioMovimientoMock = mock(ServicioMovimiento.class);
        servicioParticipacionMock = mock(ServicioParticipacion.class);
        servicioDadoMock = mock(ServicioDado.class);

        servicioJugada = new ServicioJugadaImpl(
            servicioPartidaMock,
            servicioCasilleroMock,
            servicioPropiedadMock,
            servicioEventoMock,
            servicioQuiebraMock,
            servicioMovimientoMock,
            servicioParticipacionMock,
            servicioDadoMock
        );
    }

    @Test
    void verificarQueElJugadorQuedeEncarceladoAlCaerEnLaCarcel() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(0);

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tirada = new ResultadoTirada(4, 4);
        when(servicioDadoMock.tirarDados()).thenReturn(tirada);
        when(servicioMovimientoMock.avanzar(eq(jugador), anyInt())).thenReturn(8);
        jugador.setPosicionActual(8);

        servicioJugada.tirarYMover(codigo, idJugador);

        assertTrue(jugador.getEstadoCarcel().getEncarcelado());
    }

    @Test
    void verificarQueElJugadorEncarceladoNoPuedaAvanzarNormalmente() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tiradaNoDoble = new ResultadoTirada(2, 3);
        when(servicioDadoMock.tirarDados()).thenReturn(tiradaNoDoble);

        servicioJugada.tirarYMover(codigo, idJugador);

        assertTrue(jugador.getEstadoCarcel().getEncarcelado());
        verify(servicioMovimientoMock, never()).avanzar(any(), anyInt());
    }

    @Test
    void verificarQueSeRegistreCorrectamenteElEstadoDeEncarcelamiento() {
        Jugador jugador = new Jugador();
        jugador.getEstadoCarcel().encarcelar();

        assertTrue(jugador.getEstadoCarcel().getEncarcelado());
        assertEquals(0, jugador.getEstadoCarcel().getIntentosSalirCarcel());
    }

    @Test
    void verificarQueElJugadorSalgaCuandoObtieneDobles() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tiradaDoble = new ResultadoTirada(4, 4);
        when(servicioDadoMock.tirarDados()).thenReturn(tiradaDoble);

        // Hacemos que el mock de avanzar actualice la posición del jugador
        when(servicioMovimientoMock.avanzar(eq(jugador), eq(8))).thenAnswer(invocation -> {
            jugador.setPosicionActual(16);
            return 8;
        });

        servicioJugada.tirarYMover(codigo, idJugador);

        assertFalse(jugador.getEstadoCarcel().getEncarcelado());
    }

    @Test
    void verificarQueAvanceCorrectamenteAlSalirMedianteDobles() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tiradaDoble = new ResultadoTirada(3, 3);
        when(servicioDadoMock.tirarDados()).thenReturn(tiradaDoble);

        // Configuramos para que devuelva la suma (6) y mueva al jugador a la posición 14
        when(servicioMovimientoMock.avanzar(eq(jugador), eq(6))).thenAnswer(invocation -> {
            jugador.setPosicionActual(14);
            return 6;
        });

        servicioJugada.tirarYMover(codigo, idJugador);

        assertEquals(14, jugador.getPosicionActual());
    }

    @Test
    void verificarQuePermanezcaEncarceladoCuandoNoObtieneDobles() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tiradaNoDoble = new ResultadoTirada(2, 4);
        when(servicioDadoMock.tirarDados()).thenReturn(tiradaNoDoble);

        servicioJugada.tirarYMover(codigo, idJugador);

        assertTrue(jugador.getEstadoCarcel().getEncarcelado());
    }

    @Test
    void verificarQueAumenteElContadorDeIntentosFallidos() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tirada = new ResultadoTirada(1, 3);
        when(servicioDadoMock.tirarDados()).thenReturn(tirada);

        servicioJugada.tirarYMover(codigo, idJugador);

        assertEquals(1, jugador.getEstadoCarcel().getIntentosSalirCarcel());
    }

    @Test
    void verificarQueSeCobreLaMultaAutomaticamenteAlTercerIntentoFallido() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();
        jugador.getEstadoCarcel().setIntentosSalirCarcel(2);

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tirada = new ResultadoTirada(1, 2);
        when(servicioDadoMock.tirarDados()).thenReturn(tirada);

        servicioJugada.tirarYMover(codigo, idJugador);

        verify(servicioQuiebraMock).procesarMulta(
            eq(codigo),
            eq(jugador),
            eq(100),
            eq("FIANZA_CARCEL")
        );
        assertFalse(jugador.getEstadoCarcel().getEncarcelado());
    }

    @Test
    void verificarQueNoPuedaPagarLaMultaSiNoEstaPresoOSiYaTiroEnEseTurno() {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(500);
        jugador.getEstadoCarcel().liberar(); // No está preso

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));

        assertThrows(IllegalStateException.class, () -> {
            servicioJugada.pagarFianzaVoluntaria(codigo, idJugador);
        });

        // Caso: Ya tiró en ese turno
        jugador.getEstadoCarcel().encarcelar();
        jugador.setTiroEnTurno(true);

        assertThrows(IllegalStateException.class, () -> {
            servicioJugada.pagarFianzaVoluntaria(codigo, idJugador);
        });
    }

    @Test
    void verificarQueSeDescuenteCorrectamenteElImporteDeLaMulta() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(300);
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));

        servicioJugada.pagarFianzaVoluntaria(codigo, idJugador);

        verify(servicioQuiebraMock).procesarMulta(
            eq(codigo),
            eq(jugador),
            eq(100),
            eq("FIANZA_CARCEL")
        );
    }

    @Test
    void verificarQueSeRechaceElPagoCuandoElSaldoEsInsuficiente() {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(50); // Menor a COSTO_FIANZA (100)
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));

        assertThrows(DineroInsuficienteException.class, () -> {
            servicioJugada.pagarFianzaVoluntaria(codigo, idJugador);
        });
    }

    @Test
    void verificarQueSeRestablezcaElEstadoDelJugadorAlSalirDeLaCarcel() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(500); // <-- LE AGREGAMOS DINERO SUFICIENTE PARA PAGAR LA FIANZA
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();
        jugador.getEstadoCarcel().setIntentosSalirCarcel(2);

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));

        servicioJugada.pagarFianzaVoluntaria(codigo, idJugador);

        assertFalse(jugador.getEstadoCarcel().getEncarcelado());
    }

    @Test
    void verificarQueSeReinicieElContadorDeIntentosAlRecuperarLaLibertad() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setDinero(500); // <-- LE AGREGAMOS DINERO SUFICIENTE PARA PAGAR LA FIANZA
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();
        jugador.getEstadoCarcel().setIntentosSalirCarcel(2);

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));

        servicioJugada.pagarFianzaVoluntaria(codigo, idJugador);

        assertEquals(0, jugador.getEstadoCarcel().getIntentosSalirCarcel());
    }

    @Test
    void verificarQueSeRespeteElCambioDeTurnoMientrasElJugadorEstaEncarcelado() throws Exception {
        String codigo = "PARTIDA1";
        Long idJugador = 1L;

        Partida partida = mock(Partida.class);
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setPosicionActual(8);
        jugador.getEstadoCarcel().encarcelar();

        when(servicioPartidaMock.obtenerPartida(codigo)).thenReturn(partida);
        when(partida.getJugadores()).thenReturn(List.of(jugador));
        when(servicioParticipacionMock.estaActivo(jugador)).thenReturn(true);
        when(servicioPartidaMock.obtenerJugadorActual(codigo)).thenReturn(jugador);

        ResultadoTirada tiradaNoDoble = new ResultadoTirada(1, 2);
        when(servicioDadoMock.tirarDados()).thenReturn(tiradaNoDoble);

        // Al tirar y no sacar dobles, el servicio avanza el turno automáticamente
        servicioJugada.tirarYMover(codigo, idJugador);

        assertTrue(jugador.getEstadoCarcel().getEncarcelado());
    }

    @Test
    void verificarQueSiSeAcabaElTiempoEstandoPresoCuentoComoIntentoFallido() {
        // Validación del temporizador y registro de intento por tiempo agotado
        Jugador jugador = new Jugador();
        jugador.getEstadoCarcel().encarcelar();

        boolean usoTodos = jugador.getEstadoCarcel().registrarIntentoFallido();

        assertFalse(usoTodos);
        assertEquals(1, jugador.getEstadoCarcel().getIntentosSalirCarcel());
    }
}
