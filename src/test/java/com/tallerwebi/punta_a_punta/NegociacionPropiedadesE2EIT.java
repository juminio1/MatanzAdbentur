package com.tallerwebi.punta_a_punta;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.tallerwebi.dominio.ConfiguracionDadosE2E;
import com.tallerwebi.punta_a_punta.vistas.VistaPartida;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Oferta y aceptacion reales entre dos sesiones fuera de turno. */
public class NegociacionPropiedadesE2EIT extends EscenarioPartidaE2E {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<Page, Long> identidades = new HashMap<>();
    private List<ObservadorPartidaE2E> observadores;
    private String topic;
    private String documento;
    private Page vendedor;
    private Page comprador;

    @Test
    void cuandoDosJugadoresFueraDeTurnoNegocianPlazaSanJustoEntoncesSeTransfierePorTrescientosCincuenta()
        throws IOException {
        preparar();
        vendedor = verificarPermisoDeTirada(obtenerJugadorEnTurno(paginaAnfitrion));
        comprarPlaza();
        for (Page pagina : paginas()) assertThat(nombreJugadorEnTurno(pagina)).not().hasText(nombre(vendedor));
        Page jugadorB = verificarPermisoDeTirada(obtenerJugadorEnTurno(paginaAnfitrion));
        // En una partida de tres, el participante restante es el tercero del orden real.
        comprador = paginas().stream().filter(pagina -> pagina != vendedor && pagina != jugadorB)
            .findFirst().orElseThrow();
        verificarEstado(vendedor, 1800, 2000);

        VistaPartida vistaVendedor = new VistaPartida(vendedor);
        vistaVendedor.abrirInformacionPropiedad(4, "Plaza San Justo");
        vistaVendedor.prepararOferta("Plaza San Justo", identidades.get(comprador), 350);
        descartarMensajes();
        JsonNode envio = JSON.readTree(vistaVendedor.enviarOferta(4).text());
        String idOferta = envio.path("idOferta").asText();
        assertFalse(idOferta.isBlank());
        assertEquals("pendiente", envio.path("estado").asText());
        verificarOfertaEnSesiones(idOferta);
        VistaPartida vistaComprador = new VistaPartida(comprador);
        vistaComprador.verificarOfertaRecibida(nombre(vendedor), "Plaza San Justo", 350);
        for (Page pagina : List.of(vendedor, jugadorB)) new VistaPartida(pagina).verificarSinOfertaVisible();
        verificarEstado(vendedor, 1800, 2000);
        assertEquals(jugadorB, verificarPermisoDeTirada(nombre(jugadorB)));

        descartarMensajes();
        Response aceptacion = vistaComprador.aceptarOferta(idOferta);
        assertTrue(aceptacion.url().endsWith("/ofertas/" + idOferta + "/responder"));
        assertTrue(JSON.readTree(aceptacion.request().postData()).path("aceptar").asBoolean());
        verificarTransferencia(JSON.readTree(aceptacion.text()));
        verificarTransferenciaEnSesiones();
        verificarEstado(comprador, 2150, 1650);
        assertEquals(jugadorB, verificarPermisoDeTirada(nombre(jugadorB)));
        vistaVendedor.verificarAusenteEnMisPropiedades("Plaza San Justo");
        vistaComprador.verificarPresenteEnMisPropiedades("Plaza San Justo", 30, 0);
        verificarEstado(comprador, 2150, 1650);
    }

    private void preparar() {
        observadores = paginas().stream().map(ObservadorPartidaE2E::new).toList();
        topic = "/topic/partida/" + PreparacionPartidaPropiedadesE2E.iniciarConTresJugadores(this);
        documento = UUID.randomUUID().toString();
        for (ObservadorPartidaE2E observador : observadores) {
            for (String canal : List.of("", "/propiedades", "/dinero", "/ofertas", "/turno")) {
                observador.esperarSuscripcion(topic + canal);
            }
        }
        for (Page pagina : paginas()) {
            VistaPartida vista = new VistaPartida(pagina);
            vista.verificarInicio();
            vista.verificarDisponible(4, 200);
            identidades.put(pagina, vista.obtenerIdDeSesion());
            for (String nombre : nombres()) vista.verificarSaldo(nombre, 2000);
            pagina.evaluate("marca => window.__e2eDocumentoNegociacion = marca", documento);
        }
    }

    private void comprarPlaza() throws IOException {
        vendedor.setExtraHTTPHeaders(Map.of(ConfiguracionDadosE2E.CABECERA_TIRADA, "1,3"));
        JsonNode movimiento;
        try {
            movimiento = JSON.readTree(new VistaPartida(vendedor).tirarDados().text());
        } finally {
            vendedor.setExtraHTTPHeaders(Map.of());
        }
        assertEquals(1, movimiento.path("dado1").asInt(-1));
        assertEquals(3, movimiento.path("dado2").asInt(-1));
        assertEquals(0, movimiento.path("posicionAnterior").asInt(-1));
        assertEquals(4, movimiento.path("nuevaPosicion").asInt(-1));
        assertEquals(identidades.get(vendedor).longValue(), movimiento.path("idJugador").asLong(-1));
        String idMovimiento = movimiento.path("idMovimiento").asText();
        assertFalse(idMovimiento.isBlank());
        for (ObservadorPartidaE2E observador : observadores) {
            assertEquals(movimiento, observador.esperarMensaje(topic,
                cuerpo -> idMovimiento.equals(cuerpo.path("idMovimiento").asText())));
        }
        for (Page pagina : paginas()) {
            Locator ficha = pagina.locator("[data-casillero='4'] .ficha[data-jugador-id='" + identidades.get(vendedor) + "']");
            pagina.waitForCondition(() -> ficha.count() == 1);
        }
        VistaPartida vista = new VistaPartida(vendedor);
        vista.verificarOportunidadDeCompra("Plaza San Justo", 200);
        descartarMensajes();
        JsonNode compra = JSON.readTree(vista.comprar(4).text());
        assertEquals(200, compra.path("precioCompra").asInt(-1));
        assertEquals(1800, compra.path("dineroJugador").asInt(-1));
        long idVendedor = identidades.get(vendedor);
        for (ObservadorPartidaE2E observador : observadores) {
            observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == 4 && cuerpo.path("propietarioId").asLong(-1) == idVendedor);
        }
    }

    private void verificarOfertaEnSesiones(String idOferta) {
        for (ObservadorPartidaE2E observador : observadores) {
            JsonNode oferta = observador.esperarMensaje(topic + "/ofertas",
                cuerpo -> idOferta.equals(cuerpo.path("idOferta").asText()));
            assertEquals(identidades.get(comprador).longValue(), oferta.path("idComprador").asLong(-1));
            assertEquals(nombre(vendedor), oferta.path("apodoVendedor").asText());
            assertEquals(4, oferta.path("posicion").asInt(-1));
            assertEquals("Plaza San Justo", oferta.path("nombrePropiedad").asText());
            assertEquals(350, oferta.path("precio").asInt(-1));
        }
    }

    private void verificarTransferenciaEnSesiones() {
        long idComprador = identidades.get(comprador);
        for (ObservadorPartidaE2E observador : observadores) {
            verificarTransferencia(observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == 4 && cuerpo.path("propietarioId").asLong(-1) == idComprador));
            for (Page jugador : paginas()) {
                long idJugador = identidades.get(jugador);
                int saldoEsperado = saldo(jugador, 2150, 1650);
                observador.esperarMensaje(topic + "/dinero", cuerpo ->
                    cuerpo.path("idJugador").asLong(-1) == idJugador && cuerpo.path("dinero").asInt(-1) == saldoEsperado);
            }
        }
    }

    private void verificarTransferencia(JsonNode transferencia) {
        assertEquals(4, transferencia.path("posicion").asInt(-1));
        assertEquals(identidades.get(comprador).longValue(), transferencia.path("propietarioId").asLong(-1));
        assertEquals(nombre(comprador), transferencia.path("propietario").asText());
        assertTrue(transferencia.path("disponible").isBoolean());
        assertFalse(transferencia.path("disponible").asBoolean());
        assertEquals(0, transferencia.path("nivelMejora").asInt(-1));
        assertEquals(30, transferencia.path("alquiler").asInt(-1));
        assertEquals(200, transferencia.path("precioCompra").asInt(-1));
        assertEquals(350, transferencia.path("precioVenta").asInt(-1));
        assertEquals(identidades.get(vendedor).longValue(), transferencia.path("idVendedor").asLong(-1));
        assertEquals(2150, transferencia.path("dineroVendedor").asInt(-1));
        assertEquals(identidades.get(comprador).longValue(), transferencia.path("idJugador").asLong(-1));
        assertEquals(1650, transferencia.path("dineroJugador").asInt(-1));
    }

    private void verificarEstado(Page propietario, int saldoVendedor, int saldoComprador) {
        String color = List.of("rojo", "azul", "verde").get(paginas().indexOf(propietario));
        for (Page pagina : paginas()) {
            VistaPartida vista = new VistaPartida(pagina);
            vista.verificarPropietario(4, nombre(propietario), color);
            for (Page jugador : paginas()) vista.verificarSaldo(nombre(jugador), saldo(jugador, saldoVendedor, saldoComprador));
            // La suma se calcula desde los importes efectivamente visibles en cada navegador.
            int totalVisible = pagina.locator(".jugadores .jugador .dinero").allTextContents().stream()
                .mapToInt(texto -> Integer.parseInt(texto.replaceAll("[^0-9]", ""))).sum();
            assertEquals(5800, totalVisible);
            assertEquals(documento, pagina.evaluate("() => window.__e2eDocumentoNegociacion"), "No debe recargarse la pagina");
        }
    }

    private int saldo(Page jugador, int saldoVendedor, int saldoComprador) {
        if (jugador == vendedor) return saldoVendedor;
        return jugador == comprador ? saldoComprador : 2000;
    }

    private String nombre(Page jugador) {
        return nombres().get(paginas().indexOf(jugador));
    }

    private void descartarMensajes() {
        observadores.forEach(ObservadorPartidaE2E::descartarMensajesAnteriores);
    }
}
