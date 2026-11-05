package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.FinalizarPartida;
import com.tallerwebi.dominio.enums.EstadoParticipante;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import com.tallerwebi.infraestructura.RepositorioFinalizarPartida;
import org.junit.jupiter.api.BeforeEach;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;
import java.util.List;
import java.util.ArrayList;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import com.tallerwebi.dominio.servicios.ServicioFinalizacionPartidaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServicioFinalizacionPartidaTest {
    private RepositorioPropiedad propiedadesCaptura;
    private RepositorioDeudaPendiente deudasCaptura;
    private RepositorioFinalizarPartida resultadosCaptura;
    private ServicioFinalizacionPartida servicioCaptura;
    private Partida partidaCaptura;
    private Jugador jugadorCaptura;
    private com.tallerwebi.infraestructura.RepositorioPartida partidasCierre;
    private List<FinalizarPartida> resultadosCierre;
    private com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida temporizadorCierre;

    @BeforeEach
    public void prepararCaptura() {
        propiedadesCaptura = mock(RepositorioPropiedad.class);
        deudasCaptura = mock(RepositorioDeudaPendiente.class);
        resultadosCaptura = mock(RepositorioFinalizarPartida.class);
        partidasCierre = mock(com.tallerwebi.infraestructura.RepositorioPartida.class);
        resultadosCierre = new java.util.ArrayList<>();
        temporizadorCierre = mock(com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida.class);
        servicioCaptura = new ServicioFinalizacionPartidaImpl(propiedadesCaptura, deudasCaptura,
                resultadosCaptura, new ServicioParticipacionImpl(), partidasCierre, temporizadorCierre);
        partidaCaptura = new Partida();
        partidaCaptura.setId(10L);
        partidaCaptura.setCodigoUnico("CAPTURA");
        jugadorCaptura = new Jugador();
        jugadorCaptura.setId(1L);
        jugadorCaptura.setDinero(1000);
        partidaCaptura.getJugadores().add(jugadorCaptura);
    }

    @Test
    public void propiedadSinMejorasCuentaComoPropiedad() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(0);
        when(propiedadesCaptura.buscarPropiedadesPorPropietario(jugadorCaptura))
                .thenReturn(List.of(propiedad));

        FinalizarPartida resultado = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, EstadoParticipante.ACTIVO);

        assertEquals(1, resultado.getCantidadPropiedades());
        assertEquals(0, resultado.getCantidadMejoras());
    }

    @Test
    public void conservaCantidadesTrasLiberacionPorQuiebra() {
        verificarCantidadesTrasLiberacion(EstadoParticipante.QUEBRADO);
    }

    @Test
    public void conservaCantidadesTrasLiberacionPorAbandono() {
        verificarCantidadesTrasLiberacion(EstadoParticipante.ABANDONADO);
    }

    private void verificarCantidadesTrasLiberacion(EstadoParticipante estado) {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(3);
        propiedad.setPropietario(jugadorCaptura);
        when(propiedadesCaptura.buscarPropiedadesPorPropietario(jugadorCaptura))
                .thenReturn(List.of(propiedad));
        FinalizarPartida original = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, estado);
        var liberacion = new com.tallerwebi.dominio.servicios.ServicioLiberacionPropiedadesImpl(
                propiedadesCaptura);
        if (estado == EstadoParticipante.QUEBRADO) {
            new com.tallerwebi.dominio.observadores.ObservadorLiberarPropiedadesQuiebra(liberacion)
                    .actualizar(new com.tallerwebi.dominio.eventos.EventoJugadorQuebrado(
                            partidaCaptura, jugadorCaptura));
        } else {
            new com.tallerwebi.dominio.observadores.ObservadorLiberarPropiedades(liberacion)
                    .actualizar(new com.tallerwebi.dominio.eventos.EventoJugadorAbandonado(
                            partidaCaptura, jugadorCaptura));
        }
        org.junit.jupiter.api.Assertions.assertNull(propiedad.getPropietario());
        assertEquals(0, propiedad.getNivelMejora());
        when(resultadosCaptura.buscarPorPartidaYJugador(10L, 1L)).thenReturn(original);
        org.mockito.Mockito.clearInvocations(propiedadesCaptura, deudasCaptura);

        FinalizarPartida repetido = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, EstadoParticipante.ACTIVO);

        assertSame(original, repetido);
        assertEquals(1, repetido.getCantidadPropiedades());
        assertEquals(3, repetido.getCantidadMejoras());
        assertEquals(estado, repetido.getEstadoParticipante());
        verify(resultadosCaptura).guardar(original);
        verifyNoInteractions(propiedadesCaptura, deudasCaptura);
    }

    @Test
    public void segundaCapturaConservaCantidadesDesconocidas() {
        FinalizarPartida previo = new FinalizarPartida();
        when(resultadosCaptura.buscarPorPartidaYJugador(10L, 1L)).thenReturn(previo);

        FinalizarPartida resultado = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, EstadoParticipante.ACTIVO);

        assertSame(previo, resultado);
        org.junit.jupiter.api.Assertions.assertNull(resultado.getCantidadPropiedades());
        org.junit.jupiter.api.Assertions.assertNull(resultado.getCantidadMejoras());
        verifyNoInteractions(propiedadesCaptura, deudasCaptura);
        verify(resultadosCaptura).buscarPorPartidaYJugador(10L, 1L);
        verifyNoMoreInteractions(resultadosCaptura);
    }

    @Test
    public void propiedadSinMejorasDeberiaTenerComoValorPatrimonialSuPrecioDeCompra() {
        //crea una propiedad de prueba.
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(0);

        //Crea el servicio de finalizar
        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(500, valorPatrimonial);
    }

    @Test
    public void propiedadConUnaMejoraDeberiaSumarSuCostoAlValorPatrimonial() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(1);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(700, valorPatrimonial);
    }

    @Test
    public void propiedadConDosMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(2);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(1000, valorPatrimonial);
    }

    @Test
    public void propiedadConTresMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(3);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(1500, valorPatrimonial);
    }

    @Test
    public void propiedadConCuatroMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(4);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(2500, valorPatrimonial);
    }

    @Test
    public void propiedadConCincoMejorasDeberiaSumarSusCostosAcumulados() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(5);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(4000, valorPatrimonial);
    }

    @Test
    public void propiedadConOtroPrecioDeCompraDeberiaUsarSuPrecioParaCalcularElPatrimonio() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(800);
        propiedad.setNivelMejora(2);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(1300, valorPatrimonial);
    }

    @Test
    public void propiedadConNivelNuloDeberiaTenerComoValorPatrimonialSuPrecioDeCompra() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(null);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        Integer valorPatrimonial = servicioFinalizacionPartida.calcularValorPatrimonial(propiedad);

        assertEquals(500, valorPatrimonial);
    }

    @Test
    public void propiedadConNivelNegativoDeberiaLanzarIllegalArgumentException() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(-1);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        assertThrows(IllegalArgumentException.class,
                () -> servicioFinalizacionPartida.calcularValorPatrimonial(propiedad));
    }

    @Test
    public void propiedadConNivelSeisDeberiaLanzarIllegalArgumentException() {
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(6);

        ServicioFinalizacionPartida servicioFinalizacionPartida = new ServicioFinalizacionPartidaImpl();

        assertThrows(IllegalArgumentException.class,
                () -> servicioFinalizacionPartida.calcularValorPatrimonial(propiedad));
    }

    @Test
    public void jugadorSinPropiedadesNiDeudasDeberiaConservarSuDinero() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1500);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of());
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(1500, patrimonio);
    }

    @Test
    public void jugadorConUnaPropiedadDeberiaSumarlaASuDinero() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1000);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(0);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1));
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(1500, patrimonio);
    }

    @Test
    public void jugadorConVariasPropiedadesDeberiaAcumularSusMejoras() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1000);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(1);
        Propiedad propiedad2 = new Propiedad();
        propiedad2.setPrecioCompra(800);
        propiedad2.setNivelMejora(2);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1, propiedad2));
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(3000, patrimonio);
    }

    @Test
    public void jugadorSinDineroDeberiaConservarElValorDeSusPropiedades() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(0);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(0);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1));
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(null);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(500, patrimonio);
    }

    @Test
    public void jugadorConDeudaDeberiaRestarSoloElMontoPendiente() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(1000);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of());
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(300);
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(deuda);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(700, patrimonio);
    }

    @Test
    public void jugadorConDeudaMayorASusBienesDeberiaTenerPatrimonioNegativo() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setDinero(100);
        String codigoPartida = "PARTIDA1";
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        RepositorioDeudaPendiente deudas = mock(RepositorioDeudaPendiente.class);
        Propiedad propiedad1 = new Propiedad();
        propiedad1.setPrecioCompra(500);
        propiedad1.setNivelMejora(3);
        when(propiedades.buscarPropiedadesPorPropietario(jugador))
                .thenReturn(List.of(propiedad1));
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(2000);
        when(deudas.buscarPorPartidaYJugador(codigoPartida, jugador.getId())).thenReturn(deuda);
        ServicioFinalizacionPartida servicio = new ServicioFinalizacionPartidaImpl(propiedades, deudas);

        Integer patrimonio = servicio.calcularPatrimonioTotal(codigoPartida, jugador);

        assertEquals(-400, patrimonio);
    }


    @Test
    public void capturaDineroPropiedadesConMejorasYDeuda() {
        Propiedad primera = new Propiedad();
        primera.setPrecioCompra(500);
        primera.setNivelMejora(1);
        Propiedad segunda = new Propiedad();
        segunda.setPrecioCompra(800);
        segunda.setNivelMejora(2);
        when(propiedadesCaptura.buscarPropiedadesPorPropietario(jugadorCaptura))
                .thenReturn(List.of(primera, segunda));
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(300);
        when(deudasCaptura.buscarPorPartidaYJugador("CAPTURA", 1L)).thenReturn(deuda);

        FinalizarPartida resultado = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, EstadoParticipante.QUEBRADO);

        assertSame(partidaCaptura, resultado.getPartida());
        assertSame(jugadorCaptura, resultado.getJugador());
        assertEquals(1000, resultado.getDineroFinal());
        assertEquals(2, resultado.getCantidadPropiedades());
        assertEquals(3, resultado.getCantidadMejoras());
        assertEquals(2000, resultado.getValorPropiedades());
        assertEquals(300, resultado.getDeudaPendiente());
        assertEquals(2700, resultado.getPatrimonioTotal());
        assertEquals(false, resultado.getGanador());
        assertEquals(EstadoParticipante.QUEBRADO, resultado.getEstadoParticipante());
        verify(resultadosCaptura).buscarPorPartidaYJugador(10L, 1L);
        verify(propiedadesCaptura).buscarPropiedadesPorPropietario(jugadorCaptura);
        verify(deudasCaptura).buscarPorPartidaYJugador("CAPTURA", 1L);
        verify(resultadosCaptura).guardar(resultado);
        verifyNoMoreInteractions(propiedadesCaptura, deudasCaptura, resultadosCaptura);
    }

    @Test
    public void capturaJugadorSinPropiedadesNiDeuda() {
        when(propiedadesCaptura.buscarPropiedadesPorPropietario(jugadorCaptura))
                .thenReturn(List.of());

        FinalizarPartida resultado = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, EstadoParticipante.ACTIVO);

        assertEquals(0, resultado.getCantidadPropiedades());
        assertEquals(0, resultado.getCantidadMejoras());
        assertEquals(1000, resultado.getDineroFinal());
        assertEquals(0, resultado.getValorPropiedades());
        assertEquals(0, resultado.getDeudaPendiente());
        assertEquals(1000, resultado.getPatrimonioTotal());
        assertEquals(false, resultado.getGanador());
        assertEquals(EstadoParticipante.ACTIVO, resultado.getEstadoParticipante());
        verify(resultadosCaptura).guardar(resultado);
    }

    @Test
    public void capturaPatrimonioNegativoDeUnJugadorAbandonado() {
        jugadorCaptura.setDinero(100);
        Propiedad propiedad = new Propiedad();
        propiedad.setPrecioCompra(500);
        propiedad.setNivelMejora(3);
        when(propiedadesCaptura.buscarPropiedadesPorPropietario(jugadorCaptura))
                .thenReturn(List.of(propiedad));
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setMontoPendiente(2000);
        when(deudasCaptura.buscarPorPartidaYJugador("CAPTURA", 1L)).thenReturn(deuda);

        FinalizarPartida resultado = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, EstadoParticipante.ABANDONADO);

        assertEquals(100, resultado.getDineroFinal());
        assertEquals(1500, resultado.getValorPropiedades());
        assertEquals(2000, resultado.getDeudaPendiente());
        assertEquals(-400, resultado.getPatrimonioTotal());
        assertEquals(false, resultado.getGanador());
        assertEquals(EstadoParticipante.ABANDONADO, resultado.getEstadoParticipante());
        verify(resultadosCaptura).guardar(resultado);
    }

    @Test
    public void conservaElResultadoPrevioSinSobrescribirNiConsultarLaEconomiaActual() {
        FinalizarPartida previo = new FinalizarPartida();
        previo.setPartida(partidaCaptura);
        previo.setJugador(jugadorCaptura);
        previo.setDineroFinal(100);
        previo.setValorPropiedades(1500);
        previo.setDeudaPendiente(2000);
        previo.setPatrimonioTotal(-400);
        previo.setEstadoParticipante(EstadoParticipante.QUEBRADO);
        when(resultadosCaptura.buscarPorPartidaYJugador(10L, 1L)).thenReturn(previo);

        FinalizarPartida resultado = servicioCaptura.conservarResultadoEconomico(
                partidaCaptura, jugadorCaptura, EstadoParticipante.ABANDONADO);

        assertSame(previo, resultado);
        assertEquals(100, resultado.getDineroFinal());
        assertEquals(1500, resultado.getValorPropiedades());
        assertEquals(2000, resultado.getDeudaPendiente());
        assertEquals(-400, resultado.getPatrimonioTotal());
        assertEquals(EstadoParticipante.QUEBRADO, resultado.getEstadoParticipante());
        assertEquals(false, resultado.getGanador());
        verify(resultadosCaptura).buscarPorPartidaYJugador(10L, 1L);
        verifyNoMoreInteractions(resultadosCaptura);
        verifyNoInteractions(propiedadesCaptura, deudasCaptura);
    }

    @Test
    public void rechazaJugadorAjenoAntesDeConsultarOGuardarResultados() {
        Jugador ajeno = new Jugador();
        ajeno.setId(2L);

        assertThrows(IllegalArgumentException.class,
                () -> servicioCaptura.conservarResultadoEconomico(
                        partidaCaptura, ajeno, EstadoParticipante.ACTIVO));

        verifyNoInteractions(propiedadesCaptura, deudasCaptura, resultadosCaptura);
    }

    @Test
    public void unUnicoActivoGanaAunqueUnEliminadoTengaMayorPatrimonio() {
        FinalizarPartida activo = prepararResultadoGanador(1L, -500, EstadoParticipante.ACTIVO);
        FinalizarPartida eliminado = prepararResultadoGanador(2L, 9000, EstadoParticipante.QUEBRADO);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(eliminado, activo));

        List<FinalizarPartida> ganadores = servicioCaptura.determinarGanadores(partidaCaptura);

        assertEquals(List.of(activo), ganadores);
        assertEquals(true, activo.getGanador());
        assertEquals(false, eliminado.getGanador());
    }

    @Test
    public void entreVariosActivosGanaElDeMayorPatrimonio() {
        FinalizarPartida menor = prepararResultadoGanador(1L, 1000, EstadoParticipante.ACTIVO);
        FinalizarPartida mayor = prepararResultadoGanador(2L, 3000, EstadoParticipante.ACTIVO);
        FinalizarPartida intermedio = prepararResultadoGanador(3L, 2000, EstadoParticipante.ACTIVO);
        menor.setGanador(true);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(menor, mayor, intermedio));

        assertEquals(List.of(mayor), servicioCaptura.determinarGanadores(partidaCaptura));

        assertEquals(false, menor.getGanador());
        assertEquals(true, mayor.getGanador());
        assertEquals(false, intermedio.getGanador());
    }

    @Test
    public void dosActivosConElMaximoPatrimonioSonGanadores() {
        FinalizarPartida primero = prepararResultadoGanador(1L, 3000, EstadoParticipante.ACTIVO);
        FinalizarPartida segundo = prepararResultadoGanador(2L, 3000, EstadoParticipante.ACTIVO);
        FinalizarPartida menor = prepararResultadoGanador(3L, 1000, EstadoParticipante.ACTIVO);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(primero, menor, segundo));

        assertEquals(List.of(primero, segundo), servicioCaptura.determinarGanadores(partidaCaptura));

        assertEquals(true, primero.getGanador());
        assertEquals(true, segundo.getGanador());
        assertEquals(false, menor.getGanador());
    }

    @Test
    public void tresActivosEmpatadosSonGanadores() {
        FinalizarPartida primero = prepararResultadoGanador(1L, 3000, EstadoParticipante.ACTIVO);
        FinalizarPartida segundo = prepararResultadoGanador(2L, 3000, EstadoParticipante.ACTIVO);
        FinalizarPartida tercero = prepararResultadoGanador(3L, 3000, EstadoParticipante.ACTIVO);
        List<FinalizarPartida> resultados = List.of(primero, segundo, tercero);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(resultados);

        assertEquals(resultados, servicioCaptura.determinarGanadores(partidaCaptura));

        assertEquals(true, primero.getGanador());
        assertEquals(true, segundo.getGanador());
        assertEquals(true, tercero.getGanador());
    }

    @Test
    public void quebradoConMayorPatrimonioNoPuedeGanar() {
        FinalizarPartida menor = prepararResultadoGanador(1L, 1000, EstadoParticipante.ACTIVO);
        FinalizarPartida mayor = prepararResultadoGanador(2L, 2000, EstadoParticipante.ACTIVO);
        FinalizarPartida quebrado = prepararResultadoGanador(3L, 9000, EstadoParticipante.QUEBRADO);
        quebrado.setGanador(true);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(quebrado, menor, mayor));

        assertEquals(List.of(mayor), servicioCaptura.determinarGanadores(partidaCaptura));

        assertEquals(false, quebrado.getGanador());
        assertEquals(false, menor.getGanador());
    }

    @Test
    public void abandonadoConMayorPatrimonioNoPuedeGanar() {
        FinalizarPartida menor = prepararResultadoGanador(1L, 1000, EstadoParticipante.ACTIVO);
        FinalizarPartida mayor = prepararResultadoGanador(2L, 2000, EstadoParticipante.ACTIVO);
        FinalizarPartida abandonado = prepararResultadoGanador(3L, 9000, EstadoParticipante.ABANDONADO);
        abandonado.setGanador(true);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(abandonado, mayor, menor));

        assertEquals(List.of(mayor), servicioCaptura.determinarGanadores(partidaCaptura));

        assertEquals(false, abandonado.getGanador());
        assertEquals(false, menor.getGanador());
    }

    @Test
    public void conPatrimoniosNegativosGanaElMayorAunqueNoSeaPositivo() {
        FinalizarPartida primero = prepararResultadoGanador(1L, -1000, EstadoParticipante.ACTIVO);
        FinalizarPartida segundo = prepararResultadoGanador(2L, -200, EstadoParticipante.ACTIVO);
        FinalizarPartida tercero = prepararResultadoGanador(3L, -500, EstadoParticipante.ACTIVO);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(primero, segundo, tercero));

        assertEquals(List.of(segundo), servicioCaptura.determinarGanadores(partidaCaptura));

        assertEquals(false, primero.getGanador());
        assertEquals(true, segundo.getGanador());
        assertEquals(false, tercero.getGanador());
    }

    @Test
    public void determinarGanadoresConservaTodosLosDatosEconomicosYElEstadoDeLaPartida() {
        FinalizarPartida activo = prepararResultadoGanador(1L, 500, EstadoParticipante.ACTIVO);
        FinalizarPartida quebrado = prepararResultadoGanador(2L, -400, EstadoParticipante.QUEBRADO);
        quebrado.setDineroFinal(100);
        quebrado.setValorPropiedades(1500);
        quebrado.setDeudaPendiente(2000);
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(activo, quebrado));

        servicioCaptura.determinarGanadores(partidaCaptura);

        assertEquals(500, activo.getDineroFinal());
        assertEquals(0, activo.getValorPropiedades());
        assertEquals(0, activo.getDeudaPendiente());
        assertEquals(500, activo.getPatrimonioTotal());
        assertEquals(EstadoParticipante.ACTIVO, activo.getEstadoParticipante());
        assertEquals(100, quebrado.getDineroFinal());
        assertEquals(1500, quebrado.getValorPropiedades());
        assertEquals(2000, quebrado.getDeudaPendiente());
        assertEquals(-400, quebrado.getPatrimonioTotal());
        assertEquals(EstadoParticipante.QUEBRADO, quebrado.getEstadoParticipante());
        assertSame(partidaCaptura, quebrado.getPartida());
        assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO, partidaCaptura.getEstado());
        verify(resultadosCaptura).buscarPorPartida(10L);
        verifyNoMoreInteractions(resultadosCaptura);
        verifyNoInteractions(propiedadesCaptura, deudasCaptura);
    }

    @Test
    public void repetirLaDeterminacionNoDuplicaResultadosNiModificaLosGanadores() {
        FinalizarPartida primero = prepararResultadoGanador(1L, 3000, EstadoParticipante.ACTIVO);
        FinalizarPartida segundo = prepararResultadoGanador(2L, 3000, EstadoParticipante.ACTIVO);
        FinalizarPartida menor = prepararResultadoGanador(3L, 1000, EstadoParticipante.ACTIVO);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(primero, segundo, menor));

        List<FinalizarPartida> iniciales = servicioCaptura.determinarGanadores(partidaCaptura);
        List<FinalizarPartida> repetidos = servicioCaptura.determinarGanadores(partidaCaptura);

        assertEquals(List.of(primero, segundo), iniciales);
        assertEquals(iniciales, repetidos);
        assertEquals(true, primero.getGanador());
        assertEquals(true, segundo.getGanador());
        assertEquals(false, menor.getGanador());
        verify(resultadosCaptura, org.mockito.Mockito.times(2)).buscarPorPartida(10L);
        verifyNoMoreInteractions(resultadosCaptura);
        verifyNoInteractions(propiedadesCaptura, deudasCaptura);
    }

    private FinalizarPartida prepararResultadoGanador(
            Long idJugador, Integer patrimonio, EstadoParticipante estado) {
        Jugador jugador = new Jugador();
        jugador.setId(idJugador);
        jugador.setEnBancarrota(estado == EstadoParticipante.QUEBRADO);
        jugador.setAbandono(estado == EstadoParticipante.ABANDONADO);
        partidaCaptura.getJugadores().add(jugador);
        FinalizarPartida resultado = new FinalizarPartida();
        resultado.setPartida(partidaCaptura);
        resultado.setJugador(jugador);
        resultado.setDineroFinal(patrimonio);
        resultado.setValorPropiedades(0);
        resultado.setDeudaPendiente(0);
        resultado.setPatrimonioTotal(patrimonio);
        resultado.setEstadoParticipante(estado);
        return resultado;
    }

    @Test
    public void cumpleLaCondicionConUnUnicoJugadorActivo() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);

        org.junit.jupiter.api.Assertions.assertTrue(
                servicioCaptura.cumpleCondicionFinalizacion(partidaCaptura));
        verifyNoInteractions(propiedadesCaptura, deudasCaptura, resultadosCaptura);
    }
    @Test
    public void noCumpleLaCondicionConVariosJugadoresActivos() {
        prepararDosActivosParaCondicion();

        assertFalse(servicioCaptura.cumpleCondicionFinalizacion(partidaCaptura));
    }

    @Test
    public void unaPartidaEnEsperaNoCumpleLaCondicionAunqueTengaUnSoloActivo() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_ESPERA);

        org.junit.jupiter.api.Assertions.assertFalse(
                servicioCaptura.cumpleCondicionFinalizacion(partidaCaptura));
    }

    @Test
    public void unaPartidaFinalizadaNoVuelveACumplirLaCondicion() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.FINALIZADA);

        org.junit.jupiter.api.Assertions.assertFalse(
                servicioCaptura.cumpleCondicionFinalizacion(partidaCaptura));
    }

    @Test
    public void sinJugadoresActivosNoCumpleLaCondicionFinalizacion() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        jugadorCaptura.setEnBancarrota(true);

        org.junit.jupiter.api.Assertions.assertFalse(
                servicioCaptura.cumpleCondicionFinalizacion(partidaCaptura));
    }

    @Test
    public void cumpleLaCondicionCuandoQuedaUnActivoDespuesDeUnaQuiebra() {
        Jugador eliminado = prepararDosActivosParaCondicion();
        eliminado.setEnBancarrota(true);

        org.junit.jupiter.api.Assertions.assertTrue(
                servicioCaptura.cumpleCondicionFinalizacion(partidaCaptura));
    }

    @Test
    public void cumpleLaCondicionCuandoQuedaUnActivoDespuesDeUnAbandono() {
        Jugador eliminado = prepararDosActivosParaCondicion();
        eliminado.setAbandono(true);

        org.junit.jupiter.api.Assertions.assertTrue(
                servicioCaptura.cumpleCondicionFinalizacion(partidaCaptura));
    }

    private Jugador prepararDosActivosParaCondicion() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        jugadorCaptura.setOrden(0);
        Jugador segundo = new Jugador();
        segundo.setId(2L);
        segundo.setOrden(1);
        partidaCaptura.getJugadores().add(segundo);
        return segundo;
    }

    @Test
    public void noCierraNiGuardaResultadosSiNoSeCumpleLaCondicion() {
        prepararDosActivosParaCondicion();

        org.junit.jupiter.api.Assertions.assertFalse(servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO, partidaCaptura.getEstado());
        verifyNoInteractions(propiedadesCaptura, deudasCaptura, resultadosCaptura, partidasCierre);
    }

    @Test
    public void cierraConUnActivoCapturandoSuResultadoYDeterminandoElGanador() {
        prepararRepositorioParaCierre();
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);

        org.junit.jupiter.api.Assertions.assertTrue(servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        assertEquals(1, resultadosCierre.size());
        FinalizarPartida resultado = resultadosCierre.get(0);
        assertEquals(1000, resultado.getDineroFinal());
        assertEquals(1000, resultado.getPatrimonioTotal());
        assertEquals(EstadoParticipante.ACTIVO, resultado.getEstadoParticipante());
        assertEquals(true, resultado.getGanador());
        assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.FINALIZADA, partidaCaptura.getEstado());
        org.mockito.InOrder orden = org.mockito.Mockito.inOrder(resultadosCaptura, partidasCierre);
        orden.verify(resultadosCaptura).guardar(resultado);
        orden.verify(resultadosCaptura).buscarPorPartida(10L);
        orden.verify(partidasCierre).guardarPartida(partidaCaptura);
    }

    @Test
    public void elCierreConservaLasFotografiasDeQuebradosYAbandonados() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        FinalizarPartida quebrado = prepararResultadoGanador(2L, -400, EstadoParticipante.QUEBRADO);
        quebrado.setDineroFinal(100);
        quebrado.setValorPropiedades(1500);
        quebrado.setDeudaPendiente(2000);
        FinalizarPartida abandonado = prepararResultadoGanador(3L, 9000, EstadoParticipante.ABANDONADO);
        prepararRepositorioParaCierre();
        resultadosCierre.add(quebrado);
        resultadosCierre.add(abandonado);

        org.junit.jupiter.api.Assertions.assertTrue(servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        assertEquals(3, resultadosCierre.size());
        assertEquals(100, quebrado.getDineroFinal());
        assertEquals(1500, quebrado.getValorPropiedades());
        assertEquals(2000, quebrado.getDeudaPendiente());
        assertEquals(-400, quebrado.getPatrimonioTotal());
        assertEquals(EstadoParticipante.QUEBRADO, quebrado.getEstadoParticipante());
        assertEquals(9000, abandonado.getDineroFinal());
        assertEquals(9000, abandonado.getPatrimonioTotal());
        assertEquals(EstadoParticipante.ABANDONADO, abandonado.getEstadoParticipante());
        assertEquals(false, quebrado.getGanador());
        assertEquals(false, abandonado.getGanador());
        verify(propiedadesCaptura).buscarPropiedadesPorPropietario(jugadorCaptura);
        verify(deudasCaptura).buscarPorPartidaYJugador("CAPTURA", 1L);
        verifyNoMoreInteractions(propiedadesCaptura, deudasCaptura);
        verify(resultadosCaptura, org.mockito.Mockito.times(1)).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void siYaEstaFinalizadaNoRepiteElCierre() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.FINALIZADA);

        org.junit.jupiter.api.Assertions.assertFalse(servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        verifyNoInteractions(propiedadesCaptura, deudasCaptura, resultadosCaptura, partidasCierre);
    }

    @Test
    public void unaPartidaEnEsperaNoSeCierraNiGuardaResultados() {
        org.junit.jupiter.api.Assertions.assertFalse(servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.EN_ESPERA, partidaCaptura.getEstado());
        verifyNoInteractions(propiedadesCaptura, deudasCaptura, resultadosCaptura, partidasCierre);
    }

    @Test
    public void siFaltaLaFotografiaDeUnEliminadoNoReconstruyeSusDatosNiCierra() {
        Jugador eliminado = prepararDosActivosParaCondicion();
        eliminado.setEnBancarrota(true);

        assertThrows(IllegalStateException.class,
                () -> servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO, partidaCaptura.getEstado());
        verifyNoInteractions(propiedadesCaptura, deudasCaptura, partidasCierre);
        verify(resultadosCaptura, org.mockito.Mockito.never()).guardar(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void falloDeCapturaRevierteLosResultadosGuardadosDuranteElCierre() {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext contexto =
                     crearContextoParaCierre()) {
            org.springframework.transaction.support.TransactionTemplate transaccion =
                    new org.springframework.transaction.support.TransactionTemplate(
                            contexto.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            org.hibernate.SessionFactory sesiones = contexto.getBean(org.hibernate.SessionFactory.class);
            Long idPartida = transaccion.execute(estado -> {
                Partida partida = persistirPartidaParaCierre(sesiones);
                Propiedad invalida = new Propiedad();
                invalida.setPropietario(partida.getJugadores().get(0));
                sesiones.getCurrentSession().persist(invalida);
                return partida.getId();
            });
            ServicioFinalizacionPartida cierre = contexto.getBean(ServicioFinalizacionPartida.class);

            assertThrows(NullPointerException.class, () -> transaccion.executeWithoutResult(estado ->
                    cierre.finalizarSiCorresponde(sesiones.getCurrentSession().find(Partida.class, idPartida))));

            transaccion.executeWithoutResult(estado -> {
                assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO,
                        sesiones.getCurrentSession().find(Partida.class, idPartida).getEstado());
                assertEquals(1, contexto.getBean(RepositorioFinalizarPartida.class)
                        .buscarPorPartida(idPartida).size());
            });
        }
    }

    @Test
    public void falloAlDeterminarGanadoresRevierteLaCapturaSinConfirmarUnCierreParcial() {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext contexto =
                     crearContextoParaCierre()) {
            org.springframework.transaction.support.TransactionTemplate transaccion =
                    new org.springframework.transaction.support.TransactionTemplate(
                            contexto.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            org.hibernate.SessionFactory sesiones = contexto.getBean(org.hibernate.SessionFactory.class);
            Long idPartida = transaccion.execute(estado -> {
                Partida partida = persistirPartidaParaCierre(sesiones);
                Jugador tercero = new Jugador();
                sesiones.getCurrentSession().persist(tercero);
                FinalizarPartida incompleto = new FinalizarPartida();
                incompleto.setPartida(partida);
                incompleto.setJugador(tercero);
                incompleto.setEstadoParticipante(EstadoParticipante.ACTIVO);
                sesiones.getCurrentSession().persist(incompleto);
                return partida.getId();
            });
            ServicioFinalizacionPartida cierre = contexto.getBean(ServicioFinalizacionPartida.class);

            assertThrows(NullPointerException.class, () -> transaccion.executeWithoutResult(estado ->
                    cierre.finalizarSiCorresponde(sesiones.getCurrentSession().find(Partida.class, idPartida))));

            transaccion.executeWithoutResult(estado -> {
                assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO,
                        sesiones.getCurrentSession().find(Partida.class, idPartida).getEstado());
                List<FinalizarPartida> resultados = contexto.getBean(RepositorioFinalizarPartida.class)
                        .buscarPorPartida(idPartida);
                assertEquals(2, resultados.size());
                FinalizarPartida incompleto = resultados.stream()
                        .filter(resultado -> resultado.getEstadoParticipante() == EstadoParticipante.ACTIVO)
                        .toList().get(0);
                assertEquals(false, incompleto.getGanador());
                org.junit.jupiter.api.Assertions.assertNull(incompleto.getPatrimonioTotal());
            });
        }
    }

    private void prepararRepositorioParaCierre() {
        org.mockito.Mockito.doAnswer(invocacion -> {
            resultadosCierre.add(invocacion.getArgument(0));
            return null;
        }).when(resultadosCaptura).guardar(org.mockito.ArgumentMatchers.any());
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(resultadosCierre);
        when(resultadosCaptura.buscarPorPartidaYJugador(
                org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.anyLong()))
                .thenAnswer(invocacion -> {
                    Long idJugador = invocacion.getArgument(1);
                    for (FinalizarPartida resultado : resultadosCierre) {
                        if (idJugador.equals(resultado.getJugador().getId())) {
                            return resultado;
                        }
                    }
                    return null;
                });
    }

    private org.springframework.context.annotation.AnnotationConfigApplicationContext crearContextoParaCierre() {
        org.springframework.context.annotation.AnnotationConfigApplicationContext contexto =
                new org.springframework.context.annotation.AnnotationConfigApplicationContext();
        contexto.registerBean(com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida.class,
                () -> temporizadorCierre);
        contexto.register(
                com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig.class,
                ServicioFinalizacionPartidaImpl.class, ServicioParticipacionImpl.class,
                com.tallerwebi.infraestructura.RepositorioPropiedadImpl.class,
                com.tallerwebi.infraestructura.RepositorioDeudaPendienteImpl.class,
                com.tallerwebi.infraestructura.RepositorioFinalizarPartidaImpl.class,
                com.tallerwebi.infraestructura.RepositorioPartidaImpl.class);
        contexto.refresh();
        return contexto;
    }

    private Partida persistirPartidaParaCierre(org.hibernate.SessionFactory sesiones) {
        com.tallerwebi.dominio.entidades.Usuario creador = new com.tallerwebi.dominio.entidades.Usuario();
        creador.setEmail("cierre@test.com");
        creador.setUsername("Cierre");
        sesiones.getCurrentSession().persist(creador);
        Partida partida = new Partida();
        partida.setCreador(creador);
        partida.setCodigoUnico("CIERRE");
        partida.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO);
        Jugador primero = new Jugador();
        primero.setDinero(1000);
        Jugador segundo = new Jugador();
        segundo.setDinero(500);
        segundo.setEnBancarrota(true);
        partida.getJugadores().add(primero);
        partida.getJugadores().add(segundo);
        sesiones.getCurrentSession().persist(partida);
        FinalizarPartida eliminado = new FinalizarPartida();
        eliminado.setPartida(partida);
        eliminado.setJugador(segundo);
        eliminado.setEstadoParticipante(EstadoParticipante.QUEBRADO);
        eliminado.setPatrimonioTotal(500);
        sesiones.getCurrentSession().persist(eliminado);
        return partida;
    }

    @Test
    public void cierrePorUnicoActivoCancelaSoloDespuesDelCommit() {
        verificarCancelacionAlConfirmarCierre(true);
    }

    @Test
    public void rollbackDelCierreNoCancelaElTemporizador() {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext contexto =
                     crearContextoParaCierre()) {
            org.springframework.transaction.support.TransactionTemplate transaccion =
                    new org.springframework.transaction.support.TransactionTemplate(
                            contexto.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            org.hibernate.SessionFactory sesiones = contexto.getBean(org.hibernate.SessionFactory.class);
            Long idPartida = transaccion.execute(estado -> persistirPartidaParaCierre(sesiones).getId());
            ServicioFinalizacionPartida cierre = contexto.getBean(ServicioFinalizacionPartida.class);

            transaccion.executeWithoutResult(estado -> {
                org.junit.jupiter.api.Assertions.assertTrue(cierre.finalizarSiCorresponde(
                        sesiones.getCurrentSession().find(Partida.class, idPartida)));
                verifyNoInteractions(temporizadorCierre);
                estado.setRollbackOnly();
            });

            verifyNoInteractions(temporizadorCierre);
            transaccion.executeWithoutResult(estado -> {
                assertEquals(com.tallerwebi.dominio.enums.EstadoPartida.EN_CURSO,
                        sesiones.getCurrentSession().find(Partida.class, idPartida).getEstado());
                assertEquals(1, contexto.getBean(RepositorioFinalizarPartida.class)
                        .buscarPorPartida(idPartida).size());
            });
        }
    }

    @Test
    public void sinCondicionDeCierreNoCancelaElTemporizador() {
        prepararDosActivosParaCondicion();

        org.junit.jupiter.api.Assertions.assertFalse(servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        verifyNoInteractions(temporizadorCierre);
    }

    @Test
    public void partidaYaFinalizadaNoVuelveACancelarElTemporizador() {
        partidaCaptura.setEstado(com.tallerwebi.dominio.enums.EstadoPartida.FINALIZADA);

        org.junit.jupiter.api.Assertions.assertFalse(servicioCaptura.finalizarSiCorresponde(partidaCaptura));

        verifyNoInteractions(temporizadorCierre);
    }

    private void verificarCancelacionAlConfirmarCierre(boolean unicoActivo) {
        try (org.springframework.context.annotation.AnnotationConfigApplicationContext contexto =
                     crearContextoParaCierre()) {
            org.springframework.transaction.support.TransactionTemplate transaccion =
                    new org.springframework.transaction.support.TransactionTemplate(
                            contexto.getBean(org.springframework.transaction.PlatformTransactionManager.class));
            org.hibernate.SessionFactory sesiones = contexto.getBean(org.hibernate.SessionFactory.class);
            Long idPartida = transaccion.execute(estado -> {
                Partida partida = persistirPartidaParaCierre(sesiones);
                return partida.getId();
            });
            ServicioFinalizacionPartida cierre = contexto.getBean(ServicioFinalizacionPartida.class);

            transaccion.executeWithoutResult(estado -> {
                org.junit.jupiter.api.Assertions.assertTrue(cierre.finalizarSiCorresponde(
                        sesiones.getCurrentSession().find(Partida.class, idPartida)));
                verifyNoInteractions(temporizadorCierre);
            });

            verify(temporizadorCierre).cancelarTemporizador("CIERRE");
            transaccion.executeWithoutResult(estado ->
                    org.junit.jupiter.api.Assertions.assertFalse(cierre.finalizarSiCorresponde(
                            sesiones.getCurrentSession().find(Partida.class, idPartida))));
            verifyNoMoreInteractions(temporizadorCierre);
        }
    }

    private void prepararConsultaResultados() {
        partidaCaptura.setCodigoUnico("FINAL");
        partidaCaptura.setEstado(EstadoPartida.FINALIZADA);
        jugadorCaptura.setApodo("Ana");
        when(partidasCierre.buscarPartidaPorCodigoUnico("FINAL")).thenReturn(partidaCaptura);
    }

    @Test
    void devuelveFotografiasYOrdenaGanadoresSinAlterarEntidades() throws Exception {
        prepararConsultaResultados();
        FinalizarPartida quebrado = resultado(3L, EstadoParticipante.QUEBRADO, false, 9000);
        FinalizarPartida segundoGanador = resultado(2L, EstadoParticipante.ACTIVO, true, 0);
        FinalizarPartida abandonado = resultado(4L, EstadoParticipante.ABANDONADO, false, -100);
        FinalizarPartida primerGanador = resultado(1L, EstadoParticipante.ACTIVO, true, 0);
        List<FinalizarPartida> originales = new ArrayList<>(
                List.of(quebrado, segundoGanador, abandonado, primerGanador));
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(originales);

        var respuesta = servicioCaptura.consultar("FINAL", "ana");

        assertEquals(EstadoPartida.FINALIZADA, respuesta.estado());
        assertEquals("FINAL", respuesta.codigoPartida());
        assertEquals(List.of(1L, 2L, 3L, 4L), respuesta.resultados().stream()
                .map(r -> r.idJugador()).toList());
        var primero = respuesta.resultados().get(0);
        assertEquals("Jugador1", primero.apodo());
        assertEquals(0, primero.dineroFinal());
        assertEquals(2, primero.cantidadPropiedades());
        assertEquals(3, primero.cantidadMejoras());
        assertEquals(500, primero.valorPropiedades());
        assertEquals(600, primero.deudaPendiente());
        assertEquals(0, primero.patrimonioTotal());
        assertTrue(primero.ganador());
        assertTrue(respuesta.resultados().get(1).ganador());
        assertFalse(respuesta.resultados().get(2).ganador());
        assertEquals(EstadoParticipante.QUEBRADO, respuesta.resultados().get(2).estadoParticipante());
        assertEquals(EstadoParticipante.ABANDONADO, respuesta.resultados().get(3).estadoParticipante());
        assertEquals(-100, respuesta.resultados().get(3).patrimonioTotal());
        assertEquals(List.of(quebrado, segundoGanador, abandonado, primerGanador), originales);
        assertEquals(EstadoPartida.FINALIZADA, partidaCaptura.getEstado());
        assertFalse(quebrado.getGanador());
        assertEquals(9000, quebrado.getPatrimonioTotal());
        verify(partidasCierre).buscarPartidaPorCodigoUnico("FINAL");
        verify(resultadosCaptura).buscarPorPartida(10L);
        verifyNoMoreInteractions(partidasCierre, resultadosCaptura);
    }

    @Test
    void conservaCantidadesNulasYCeros() throws Exception {
        prepararConsultaResultados();
        FinalizarPartida historico = resultado(1L, EstadoParticipante.ACTIVO, true, -5);
        historico.setCantidadPropiedades(null);
        historico.setCantidadMejoras(null);
        historico.setDeudaPendiente(0);
        when(resultadosCaptura.buscarPorPartida(10L)).thenReturn(List.of(historico));

        var consultado = servicioCaptura.consultar("FINAL", "Ana").resultados().get(0);

        assertNull(consultado.cantidadPropiedades());
        assertNull(consultado.cantidadMejoras());
        assertEquals(0, consultado.deudaPendiente());
        assertEquals(-5, consultado.patrimonioTotal());
    }

    @Test
    void noPublicaResultadosAntesDelCierre() throws Exception {
        prepararConsultaResultados();
        for (EstadoPartida estado : List.of(EstadoPartida.EN_CURSO, EstadoPartida.EN_ESPERA)) {
            partidaCaptura.setEstado(estado);
            var respuesta = servicioCaptura.consultar("FINAL", "Ana");
            assertEquals(estado, respuesta.estado());
            assertTrue(respuesta.resultados().isEmpty());
        }
        verifyNoInteractions(resultadosCaptura);
    }

    @Test
    void codigoInexistenteNoConsultaResultados() {
        prepararConsultaResultados();
        assertThrows(PartidaNoEncontradaException.class, () -> servicioCaptura.consultar("OTRA", "Ana"));
        verifyNoInteractions(resultadosCaptura);
    }

    @Test
    void rechazaUsuarioAjenoONombreAusente() {
        prepararConsultaResultados();
        assertThrows(SecurityException.class, () -> servicioCaptura.consultar("FINAL", "Ajeno"));
        assertThrows(SecurityException.class, () -> servicioCaptura.consultar("FINAL", null));
        verifyNoInteractions(resultadosCaptura);
    }

    @Test
    void autorizaEspectadoresQuebradosYAbandonados() throws Exception {
        prepararConsultaResultados();
        Jugador jugador = partidaCaptura.getJugadores().get(0);
        jugador.setEnBancarrota(true);
        assertEquals(EstadoPartida.FINALIZADA, servicioCaptura.consultar("FINAL", "Ana").estado());
        jugador.setEnBancarrota(false);
        jugador.setAbandono(true);
        assertEquals(EstadoPartida.FINALIZADA, servicioCaptura.consultar("FINAL", "Ana").estado());
    }

    @Test
    void consultaDeclaradaTransaccionalDeSoloLectura() throws Exception {
        prepararConsultaResultados();
        Transactional transaccion = ServicioFinalizacionPartidaImpl.class
                .getMethod("consultar", String.class, String.class).getAnnotation(Transactional.class);
        assertNotNull(transaccion);
        assertTrue(transaccion.readOnly());
    }

    private FinalizarPartida resultado(Long id, EstadoParticipante estado, boolean ganador, int patrimonio) {
        Jugador jugador = new Jugador();
        jugador.setId(id);
        jugador.setApodo("Jugador" + id);
        FinalizarPartida resultado = new FinalizarPartida();
        resultado.setJugador(jugador);
        resultado.setEstadoParticipante(estado);
        resultado.setGanador(ganador);
        resultado.setDineroFinal(0);
        resultado.setCantidadPropiedades(2);
        resultado.setCantidadMejoras(3);
        resultado.setValorPropiedades(500);
        resultado.setDeudaPendiente(600);
        resultado.setPatrimonioTotal(patrimonio);
        return resultado;
    }
}
