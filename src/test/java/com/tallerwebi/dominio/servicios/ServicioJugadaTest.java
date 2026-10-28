package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.TiradaNoPermitidaException;
import com.tallerwebi.dominio.resultados.ResultadoEvento;
import com.tallerwebi.dominio.resultados.ResultadoJugada;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioMovimiento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pruebas unitarias de la coordinación de jugadas, incluyendo la pausa por eventos globales. */
public class ServicioJugadaTest {

    private static final String CODIGO = "MATANZA123";

    private ServicioPartida partidasMock;
    private ServicioCasillero casillerosMock;
    private ServicioPropiedad propiedadesMock;
    private ServicioEventoCasillero eventosMock;
    private ServicioGestionQuiebra quiebrasMock;
    private ServicioMovimiento movimientoMock;
    private ServicioParticipacion participacionMock;
    private ServicioDado dadosMock;
    private ServicioJugadaImpl servicioJugada;
    private Partida partida;
    private Jugador jugador;
    private Jugador rival;

    private void prepararJugadaValida() throws Exception {
        partidasMock = mock(ServicioPartida.class);
        casillerosMock = mock(ServicioCasillero.class);
        propiedadesMock = mock(ServicioPropiedad.class);
        eventosMock = mock(ServicioEventoCasillero.class);
        quiebrasMock = mock(ServicioGestionQuiebra.class);
        movimientoMock = mock(ServicioMovimiento.class);
        participacionMock = mock(ServicioParticipacion.class);
        dadosMock = mock(ServicioDado.class);

        jugador = crearJugador(1L, "Jugador uno", 0, 1500);
        rival = crearJugador(2L, "Jugador dos", 0, 2000);

        partida = new Partida();
        partida.setCodigoUnico(CODIGO);
        partida.setJugadores(List.of(jugador, rival));
        partida.setIndiceTurnoActual(0);

        servicioJugada = new ServicioJugadaImpl(
            partidasMock, casillerosMock, propiedadesMock, eventosMock,
            quiebrasMock, movimientoMock, participacionMock, dadosMock
        );

        when(partidasMock.obtenerPartida(CODIGO)).thenReturn(partida);
        when(partidasMock.obtenerJugadorActual(CODIGO)).thenReturn(jugador);
        when(participacionMock.estaActivo(jugador)).thenReturn(true);
        when(dadosMock.tirarDados()).thenReturn(new ResultadoTirada(2, 3));
        when(movimientoMock.avanzar(jugador, 5)).thenAnswer(invocacion -> {jugador.setPosicionActual(5);return 0;});
        when(eventosMock.aplicarSiCorresponde(CODIGO, jugador)).thenReturn(Optional.empty());
    }

    private Jugador crearJugador(Long id, String apodo, Integer posicion, Integer dinero) {
        Jugador nuevo = new Jugador();
        nuevo.setId(id);
        nuevo.setApodo(apodo);
        nuevo.setPosicionActual(posicion);
        nuevo.setDinero(dinero);
        return nuevo;
    }

    private CasilleroPropiedad prepararPropiedad(Jugador propietario) {
        Propiedad propiedad = new Propiedad();
        propiedad.setId(10L);
        propiedad.setNombre("Terreno de San Justo");
        propiedad.setPropietario(propietario);
        
        CasilleroPropiedad casillero = new CasilleroPropiedad();
        casillero.setPropiedad(propiedad);
        
        when(casillerosMock.buscarOCrearPropiedadEnPosicion(partida, 5)).thenReturn(casillero);
        
        return casillero;
    }

    @Test
    public void dadoQueLaPartidaNoExisteCuandoSeTiranDadosYDaPartidaNoEncontradaException() throws Exception {
        prepararJugadaValida();
        when(partidasMock.obtenerPartida(CODIGO)).thenReturn(null);

        assertThrows(PartidaNoEncontradaException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        verifyNoInteractions(dadosMock);
        verify(partidasMock, never()).actualizarJugador(jugador);
    }

    @Test
    public void dadoQueElJugadorNoPerteneceALaPartidaCuandoTiraYDaJugadorNoEncontradoException()throws Exception {
        prepararJugadaValida();

        assertThrows(JugadorNoEncontradoException.class, () -> servicioJugada.tirarYMover(CODIGO, 99L));

        verifyNoInteractions(dadosMock);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElIdentificadorEsNuloCuandoTiraYDaJugadorNoEncontradoException() throws Exception {
        prepararJugadaValida();

        assertThrows(JugadorNoEncontradoException.class, () -> servicioJugada.tirarYMover(CODIGO, null));

        verifyNoInteractions(dadosMock);
    }

    @Test
    public void dadoQueHayEventoGlobalActivoCuandoSeTiranDadosYDaTiradaNoPermitidaException() throws Exception {
        prepararJugadaValida();
        when(partidasMock.verificarSiHayEventoGlobalActivo(CODIGO)).thenReturn(true);

        TiradaNoPermitidaException error = assertThrows(TiradaNoPermitidaException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        assertEquals("La partida está pausada por un evento global.", error.getMessage());
        assertEquals(false, jugador.getTiroEnTurno());
        verifyNoInteractions(dadosMock, movimientoMock);
        verify(partidasMock, never()).actualizarJugador(jugador);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElJugadorNoEstaActivoCuandoTiraYDaTiradaNoPermitidaException()throws Exception {
        prepararJugadaValida();
        when(participacionMock.estaActivo(jugador)).thenReturn(false);

        assertThrows(TiradaNoPermitidaException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        verifyNoInteractions(dadosMock);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueNoHayJugadorActualCuandoSeTiranDadosYDaTiradaNoPermitidaException() throws Exception {
        prepararJugadaValida();
        when(partidasMock.obtenerJugadorActual(CODIGO)).thenReturn(null);

        assertThrows(TiradaNoPermitidaException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        verifyNoInteractions(dadosMock);
    }

    @Test
    public void dadoQueEsTurnoDeOtroJugadorCuandoSeTiranDadosYDaTiradaNoPermitidaException() throws Exception {
        prepararJugadaValida();
        when(partidasMock.obtenerJugadorActual(CODIGO)).thenReturn(rival);

        assertThrows(TiradaNoPermitidaException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        verifyNoInteractions(dadosMock);
    }

    @Test
    public void dadoQueElJugadorYaTiroCuandoVuelveATirarYDaTiradaNoPermitidaException() throws Exception {
        prepararJugadaValida();
        jugador.setTiroEnTurno(true);

        TiradaNoPermitidaException error = assertThrows(TiradaNoPermitidaException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        assertEquals("Ya tiraste los dados en este turno.", error.getMessage());
        verifyNoInteractions(dadosMock);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueHayDeudaPendienteAntesDeTirarCuandoTiraYDaTiradaNoPermitidaException() throws Exception {
        prepararJugadaValida();
        
        when(quiebrasMock.obtenerDeudaPendiente(CODIGO, 1L)).thenReturn(new DeudaPendiente());

        TiradaNoPermitidaException error = assertThrows(TiradaNoPermitidaException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        assertEquals("Tenés una deuda pendiente. Debés resolverla antes de volver a tirar los dados.", error.getMessage());
        verifyNoInteractions(dadosMock);
    }

    @Test
    public void dadoQueElJugadorActualEsOtraInstanciaConElMismoIdCuandoTiraYDaJugadaValida()throws Exception {
        prepararJugadaValida();
        
        Jugador otroObjetoMismoId = crearJugador(1L, "Otra instancia", 0, 1500);
        
        when(partidasMock.obtenerJugadorActual(CODIGO)).thenReturn(otroObjetoMismoId);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, 1L);

        assertEquals(5, resultado.nuevaPosicion());
        verify(dadosMock).tirarDados();
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueLaJugadaEsValidaCuandoSeTiranDadosYDaMovimientoYAvanceDeTurno()throws Exception {
        prepararJugadaValida();

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(1L, resultado.idJugador());
        assertEquals(0, resultado.posicionAnterior());
        assertEquals(5, resultado.nuevaPosicion());
        assertEquals(5, resultado.posicionCaidaEvento());
        assertEquals(2, resultado.tirada().getDado1());
        assertEquals(3, resultado.tirada().getDado2());
        assertEquals(0, resultado.dineroPorInicio());
        assertEquals(null, resultado.evento());
        assertEquals(null, resultado.alquiler());
        assertEquals(true, jugador.getTiroEnTurno());
        verify(movimientoMock).avanzar(jugador, 5);
        verify(partidasMock).actualizarJugador(jugador);
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueHayDosJugadoresCuandoSeResuelveLaJugadaYDaLosDosSaldos()throws Exception {
        prepararJugadaValida();

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(2, resultado.saldos().size());
        assertEquals(1L, resultado.saldos().get(0).idJugador());
        assertEquals(1500, resultado.saldos().get(0).dinero());
        assertEquals(false, resultado.saldos().get(0).enBancarrota());
        assertEquals(2L, resultado.saldos().get(1).idJugador());
        assertEquals(2000, resultado.saldos().get(1).dinero());
    }

    @Test
    public void dadoQueLaPartidaActualizadaNoExisteCuandoTerminaLaJugadaYDaSaldosOriginales()throws Exception {
        prepararJugadaValida();
       
        when(partidasMock.obtenerPartida(CODIGO)).thenReturn(partida, (Partida) null);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(2, resultado.saldos().size());
        assertEquals(1500, resultado.saldos().get(0).dinero());
        verify(partidasMock, times(2)).obtenerPartida(CODIGO);
    }

    @Test
    public void dadoQueLaPartidaSeActualizoCuandoTerminaLaJugadaYDaLosSaldosActualizados()throws Exception {
        prepararJugadaValida();
        
        Jugador actualizado = crearJugador(1L, "Jugador uno", 5, 1750);
        
        Partida partidaActualizada = new Partida();
        
        partidaActualizada.setCodigoUnico(CODIGO);
        partidaActualizada.setJugadores(List.of(actualizado, rival));
        
        when(partidasMock.obtenerPartida(CODIGO)).thenReturn(partida, partidaActualizada);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(1750, resultado.saldos().get(0).dinero());
        assertEquals(2000, resultado.saldos().get(1).dinero());
    }

    @Test
    public void dadoQueSeActivaUnEventoDeCasilleroCuandoSeTiranDadosYDaEventoEnResultado()throws Exception {
        prepararJugadaValida();
        
        EventoCasillero evento = new EventoCasillero(3, "Quini", "Ganaste el Quini",
            TipoEfectoEvento.SUMA, 100, null, null);
        
            when(eventosMock.aplicarSiCorresponde(CODIGO, jugador)).thenReturn(Optional.of(new ResultadoEvento(evento, 80)));

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals("Quini", resultado.evento().nombre());
        assertEquals("Ganaste el Quini", resultado.evento().descripcion());
        assertEquals("SUMA", resultado.evento().tipoEfecto());
        assertEquals(80, resultado.dineroPorInicio());
        verify(eventosMock).aplicarSiCorresponde(CODIGO, jugador);
    }

    @Test
    public void dadoQueMovimientoYEventoDanPremiosCuandoTiraYDaSumaDeLosPremios()throws Exception {
        prepararJugadaValida();
        
        EventoCasillero evento = new EventoCasillero(4, "Bono", "Premio adicional",
            TipoEfectoEvento.SUMA, 80, null, null);
        
            when(movimientoMock.avanzar(jugador, 5)).thenAnswer(invocacion -> {
            jugador.setPosicionActual(5);
            return 200;
        });
        when(eventosMock.aplicarSiCorresponde(CODIGO, jugador)).thenReturn(Optional.of(new ResultadoEvento(evento, 80)));

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(280, resultado.dineroPorInicio());
        verify(movimientoMock).avanzar(jugador, 5);
    }

    @Test
    public void dadoQueUnEventoMueveLaFichaCuandoTerminaLaJugadaYDaCaidaYDestinoDiferentes() throws Exception {
        prepararJugadaValida();
        EventoCasillero evento = new EventoCasillero(5, "Avance", "Avanzaste cuatro",
            TipoEfectoEvento.AVANZA_CASILLEROS, null, 4, null);
        
            when(eventosMock.aplicarSiCorresponde(CODIGO, jugador)).thenAnswer(invocacion -> {
            jugador.setPosicionActual(9);
            return Optional.of(new ResultadoEvento(evento, 0));
        });

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(5, resultado.posicionCaidaEvento());
        assertEquals(9, resultado.nuevaPosicion());
        verify(casillerosMock).buscarOCrearPropiedadEnPosicion(partida, 9);
    }

    @Test
    public void dadoQueNoHayServicioDeEventosCuandoTiraYDaJugadaSinEvento()throws Exception {
        prepararJugadaValida();
        
        ServicioJugadaImpl servicioSinEventos = new ServicioJugadaImpl(
            partidasMock, casillerosMock, propiedadesMock, null,
            quiebrasMock, movimientoMock, participacionMock, dadosMock
        );

        ResultadoJugada resultado = servicioSinEventos.tirarYMover(CODIGO, jugador.getId());

        assertEquals(null, resultado.evento());
        assertEquals(0, resultado.dineroPorInicio());
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElEventoHacePerderTurnoCuandoSeResuelveElDestinoYDaTurnoAvanzado() throws Exception {
        prepararJugadaValida();
        when(eventosMock.aplicarSiCorresponde(CODIGO, jugador)).thenAnswer(invocacion -> {
            jugador.setPierdeTurno(true);
            return Optional.empty();});

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(true, jugador.getPierdeTurno());
        assertEquals(null, resultado.alquiler());
        verify(casillerosMock, never()).buscarOCrearPropiedadEnPosicion(partida, 5);
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueJugadorQuedaInactivoDespuesDelEventoCuandoSeResuelveYDaTurnoSinAvanzar()throws Exception {
        prepararJugadaValida();
        when(participacionMock.estaActivo(jugador)).thenReturn(true, false, false);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(null, resultado.alquiler());
        verify(casillerosMock, never()).buscarOCrearPropiedadEnPosicion(partida, 5);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElJugadorDejaDeEstarActivoAlResolverTurnoCuandoTerminaYDaTurnoSinAvanzar()throws Exception {
        prepararJugadaValida();
        when(participacionMock.estaActivo(jugador)).thenReturn(true, true, false);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(5, resultado.nuevaPosicion());
        verify(casillerosMock).buscarOCrearPropiedadEnPosicion(partida, 5);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueNoHayPropiedadEnLaPosicionCuandoSeResuelveDestinoYDaTurnoAvanzado() throws Exception {
        prepararJugadaValida();

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(null, resultado.alquiler());
        verify(casillerosMock).buscarOCrearPropiedadEnPosicion(partida, 5);
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElCasilleroTienePropiedadLibreCuandoSeResuelveYDaDecisionDeCompra() throws Exception {
        prepararJugadaValida();
       
        CasilleroPropiedad casillero = prepararPropiedad(null);
       
        when(propiedadesMock.estaDisponible(casillero.getPropiedad())).thenReturn(true);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(null, resultado.alquiler());
        assertEquals(true, jugador.getTiroEnTurno());
        verify(propiedadesMock).estaDisponible(casillero.getPropiedad());
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueLaPropiedadNoEstaDisponiblePeroNoTieneDuenioCuandoTiraYDaTurnoAvanzado() throws Exception {
        prepararJugadaValida();
        CasilleroPropiedad casillero = prepararPropiedad(null);
        when(propiedadesMock.estaDisponible(casillero.getPropiedad())).thenReturn(false);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(null, resultado.alquiler());
        verify(quiebrasMock, never()).procesarAlquiler(any(), any(), any(), any());
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueLaPropiedadEsDelMismoJugadorCuandoTiraYDaTurnoSinPagarAlquiler() throws Exception {
        prepararJugadaValida();
        prepararPropiedad(jugador);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(null, resultado.alquiler());
        verify(quiebrasMock, never()).procesarAlquiler(any(), any(), any(), any());
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElPropietarioTieneElMismoIdCuandoSeTiranDadosYDaSinAlquiler()  throws Exception {
        prepararJugadaValida();
        
        Jugador otroObjetoDelMismoPropietario = crearJugador(1L, "Dueño", 0, 1500);
        
        prepararPropiedad(otroObjetoDelMismoPropietario);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(null, resultado.alquiler());
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElAlquilerQuedaPendienteCuandoCaeEnPropiedadAjenaYDaDecisionDeDeuda()throws Exception {
        prepararJugadaValida();
        
        CasilleroPropiedad casillero = prepararPropiedad(rival);
        
        when(propiedadesMock.calcularAlquiler(casillero, casillero.getPropiedad())).thenReturn(300);
        when(quiebrasMock.procesarAlquiler(CODIGO, casillero.getPropiedad(), jugador, 300)).thenReturn(ResultadoDeuda.PENDIENTE);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(300, resultado.alquiler().monto());
        assertEquals(1L, resultado.alquiler().idJugadorPagador());
        assertEquals("Jugador uno", resultado.alquiler().pagador());
        assertEquals("Jugador dos", resultado.alquiler().propietario());
        assertEquals("Terreno de San Justo", resultado.alquiler().propiedad());
        verify(quiebrasMock).procesarAlquiler(CODIGO, casillero.getPropiedad(), jugador, 300);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElAlquilerSeResuelveCuandoCaeEnPropiedadAjenaYDaTurnoAvanzado()throws Exception {
        prepararJugadaValida();
        
        CasilleroPropiedad casillero = prepararPropiedad(rival);
        
        when(propiedadesMock.calcularAlquiler(casillero, casillero.getPropiedad())).thenReturn(70);
        when(quiebrasMock.procesarAlquiler(CODIGO, casillero.getPropiedad(), jugador, 70)).thenReturn(ResultadoDeuda.RESUELTA);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(70, resultado.alquiler().monto());
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElAlquilerNoGeneraDeudaCuandoCaeEnPropiedadAjenaYDaTurnoAvanzado() throws Exception {
        prepararJugadaValida();
        
        CasilleroPropiedad casillero = prepararPropiedad(rival);
        
        when(propiedadesMock.calcularAlquiler(casillero, casillero.getPropiedad())).thenReturn(45);
        when(quiebrasMock.procesarAlquiler(CODIGO, casillero.getPropiedad(), jugador, 45)).thenReturn(ResultadoDeuda.SIN_DEUDA);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(45, resultado.alquiler().monto());
        verify(partidasMock).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElAlquilerProvocaQuiebraCuandoCaeEnPropiedadAjenaYDaSinAvanzarTurno()            throws Exception {
        prepararJugadaValida();
        
        CasilleroPropiedad casillero = prepararPropiedad(rival);
        
        when(propiedadesMock.calcularAlquiler(casillero, casillero.getPropiedad())).thenReturn(500);
        when(quiebrasMock.procesarAlquiler(CODIGO, casillero.getPropiedad(), jugador, 500)).thenReturn(ResultadoDeuda.QUIEBRA);
        when(participacionMock.estaActivo(jugador)).thenReturn(true, true, false);

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(500, resultado.alquiler().monto());
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueApareceDeudaDespuesDelMovimientoCuandoTerminaJugadaYDaTurnoSinAvanzar() throws Exception {
        prepararJugadaValida();
        
        when(quiebrasMock.obtenerDeudaPendiente(CODIGO, 1L)).thenReturn(null, new DeudaPendiente());

        ResultadoJugada resultado = servicioJugada.tirarYMover(CODIGO, jugador.getId());

        assertEquals(5, resultado.nuevaPosicion());
        verify(quiebrasMock, times(2)).obtenerDeudaPendiente(CODIGO, 1L);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueElEventoDeCasilleroFallaCuandoSeResuelveLaJugadaYDaExcepcionPropagada()throws Exception {
        prepararJugadaValida();
        
        when(eventosMock.aplicarSiCorresponde(CODIGO, jugador)).thenThrow(new EventoInexistenteException("Evento no encontrado"));

        EventoInexistenteException error = assertThrows(EventoInexistenteException.class, () -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        assertEquals("Evento no encontrado", error.getMessage());
        verify(partidasMock, never()).actualizarJugador(jugador);
        verify(partidasMock, never()).avanzarTurno(CODIGO);
    }

    @Test
    public void dadoQueFallaElAvanceDeTurnoCuandoTerminaLaJugadaYDaExcepcionPropagada()throws Exception {
        prepararJugadaValida();
        
        doThrow(new PartidaNoEncontradaException()).when(partidasMock).avanzarTurno(CODIGO);

        assertThrows(PartidaNoEncontradaException.class,() -> servicioJugada.tirarYMover(CODIGO, jugador.getId()));

        verify(partidasMock).actualizarJugador(jugador);
        verify(partidasMock).avanzarTurno(CODIGO);
    }

}
