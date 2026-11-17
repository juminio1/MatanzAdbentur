package com.tallerwebi.punta_a_punta;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.tallerwebi.dominio.ConfiguracionDadosE2E;
import com.tallerwebi.punta_a_punta.vistas.VistaPartida;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Compra, venta al banco y nueva oportunidad de compra mediante la interfaz real. */
public class VentaPropiedadBancoE2EIT extends EscenarioPartidaE2E {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<Page, Long> identidades = new HashMap<>();
    private List<ObservadorPartidaE2E> observadores;
    private String topic;
    private String documento;
    private Page vendedor;

    @Test
    void cuandoVendePlazaSanJustoAlBancoEntoncesRecibeCienYQuedaDisponibleEnLasTresSesiones()
        throws IOException {
        preparar();
        vendedor = verificarPermisoDeTirada(obtenerJugadorEnTurno(paginaAnfitrion));
        verificarPantallas(2000);
        verificarMovimientoSinAlquiler(tirarHastaPlaza(vendedor), 2000);
        VistaPartida vistaVendedor = new VistaPartida(vendedor);
        vistaVendedor.verificarOportunidadDeCompra("Plaza San Justo", 200);
        descartarMensajes();
        JsonNode compra = JSON.readTree(vistaVendedor.comprar(4).text());
        assertEquals(identidades.get(vendedor).longValue(), compra.path("propietarioId").asLong(-1));
        assertEquals(200, compra.path("precioCompra").asInt(-1));
        assertEquals(1800, compra.path("dineroJugador").asInt(-1));
        verificarCompraEnSesiones();
        Page jugadorB = esperarSiguienteJugador(vendedor);
        verificarPantallas(1800);
        String color = List.of("rojo", "azul", "verde").get(paginas().indexOf(vendedor));
        for (Page pagina : paginas()) {
            new VistaPartida(pagina).verificarPropietario(4, nombre(vendedor), color);
        }

        vistaVendedor.abrirInformacionPropiedad(4, "Plaza San Justo");
        vistaVendedor.prepararVentaAlBanco("Plaza San Justo", 100);
        descartarMensajes();
        JsonNode venta = JSON.readTree(vistaVendedor.confirmarVentaAlBanco(4).text());
        verificarVenta(venta);
        verificarVentaEnSesiones();
        verificarPantallas(1900);
        for (Page pagina : paginas()) {
            VistaPartida vista = new VistaPartida(pagina);
            vista.verificarDisponible(4, 200);
            vista.abrirInformacionPropiedad(4, "Plaza San Justo");
            vista.verificarInformacionSinPropietario(200);
            vista.cerrarInformacionPropiedad();
        }
        vistaVendedor.verificarAusenteEnMisPropiedades("Plaza San Justo");

        verificarPermisoDeTirada(nombre(jugadorB));
        JsonNode nuevaCaida = tirarHastaPlaza(jugadorB);
        verificarMovimientoSinAlquiler(nuevaCaida, 1900);
        new VistaPartida(jugadorB).verificarOportunidadDeCompra("Plaza San Justo", 200);
        verificarPantallas(1900);
        for (Page pagina : paginas()) new VistaPartida(pagina).verificarDisponible(4, 200);
    }

    private void preparar() {
        observadores = paginas().stream().map(ObservadorPartidaE2E::new).toList();
        topic = "/topic/partida/" + PreparacionPartidaPropiedadesE2E.iniciarConTresJugadores(this);
        documento = UUID.randomUUID().toString();
        for (ObservadorPartidaE2E observador : observadores) {
            observador.esperarSuscripcion(topic);
            observador.esperarSuscripcion(topic + "/propiedades");
            observador.esperarSuscripcion(topic + "/dinero");
            observador.esperarSuscripcion(topic + "/turno");
        }
        for (Page pagina : paginas()) {
            VistaPartida vista = new VistaPartida(pagina);
            vista.verificarInicio();
            vista.verificarDisponible(4, 200);
            identidades.put(pagina, vista.obtenerIdDeSesion());
            pagina.evaluate("marca => window.__e2eDocumentoVenta = marca", documento);
        }
    }

    private JsonNode tirarHastaPlaza(Page jugador) throws IOException {
        verificarPermisoDeTirada(nombre(jugador));
        descartarMensajes();
        jugador.setExtraHTTPHeaders(Map.of(ConfiguracionDadosE2E.CABECERA_TIRADA, "1,3"));
        JsonNode movimiento;
        try {
            movimiento = JSON.readTree(new VistaPartida(jugador).tirarDados().text());
        } finally {
            jugador.setExtraHTTPHeaders(Map.of());
        }
        assertEquals(identidades.get(jugador).longValue(), movimiento.path("idJugador").asLong(-1));
        assertEquals(0, movimiento.path("posicionAnterior").asInt(-1));
        assertEquals(4, movimiento.path("nuevaPosicion").asInt(-1));
        assertEquals(1, movimiento.path("dado1").asInt(-1));
        assertEquals(3, movimiento.path("dado2").asInt(-1));
        assertEquals(4, movimiento.path("suma").asInt(-1));
        String movimientoId = movimiento.path("idMovimiento").asText();
        assertFalse(movimientoId.isBlank());
        for (ObservadorPartidaE2E observador : observadores) {
            assertEquals(movimiento, observador.esperarMensaje(topic,
                cuerpo -> movimientoId.equals(cuerpo.path("idMovimiento").asText())));
        }
        for (Page pagina : paginas()) {
            Locator ficha = pagina.locator("[data-casillero='4'] .ficha[data-jugador-id='"
                + identidades.get(jugador) + "']");
            pagina.waitForCondition(() -> ficha.count() == 1);
            new VistaPartida(pagina).verificarPosicion(identidades.get(jugador), 4);
        }
        return movimiento;
    }

    private void verificarCompraEnSesiones() {
        long idVendedor = identidades.get(vendedor);
        for (ObservadorPartidaE2E observador : observadores) {
            JsonNode compra = observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == 4
                    && cuerpo.path("propietarioId").asLong(-1) == idVendedor);
            assertEquals(1800, compra.path("dineroJugador").asInt(-1));
        }
    }

    private void verificarVentaEnSesiones() {
        for (ObservadorPartidaE2E observador : observadores) {
            JsonNode venta = observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == 4 && cuerpo.path("disponible").asBoolean());
            verificarVenta(venta);
            for (Page jugador : paginas()) {
                long idJugador = identidades.get(jugador);
                int esperado = jugador == vendedor ? 1900 : 2000;
                JsonNode saldo = observador.esperarMensaje(topic + "/dinero", cuerpo ->
                    cuerpo.path("idJugador").asLong(-1) == idJugador
                        && cuerpo.path("dinero").asInt(-1) == esperado);
                assertEquals(esperado, saldo.path("dinero").asInt(-1));
            }
        }
    }

    private void verificarVenta(JsonNode venta) {
        assertEquals(4, venta.path("posicion").asInt(-1));
        assertEquals("Plaza San Justo", venta.path("nombre").asText());
        assertTrue(venta.has("propietarioId") && venta.path("propietarioId").isNull());
        assertTrue(venta.has("propietario") && venta.path("propietario").isNull());
        assertTrue(venta.path("disponible").isBoolean() && venta.path("disponible").asBoolean());
        assertEquals(0, venta.path("nivelMejora").asInt(-1));
        assertEquals(200, venta.path("precioCompra").asInt(-1));
        assertEquals(100, venta.path("precioVenta").asInt(-1));
        assertEquals(30, venta.path("alquiler").asInt(-1));
        assertEquals(1900, venta.path("dineroJugador").asInt(-1));
        assertEquals(identidades.get(vendedor).longValue(), venta.path("idJugador").asLong(-1));
    }

    private void verificarMovimientoSinAlquiler(JsonNode movimiento, int saldoVendedor) {
        assertTrue(movimiento.path("alquiler").isNull());
        assertEquals(0, movimiento.path("dineroPorInicio").asInt(-1));
        assertEquals(3, movimiento.path("saldos").size());
        for (Page jugador : paginas()) {
            List<JsonNode> encontrados = new java.util.ArrayList<>();
            movimiento.path("saldos").forEach(saldo -> {
                if (saldo.path("idJugador").asLong(-1) == identidades.get(jugador)) encontrados.add(saldo);
            });
            assertEquals(1, encontrados.size());
            assertEquals(jugador == vendedor ? saldoVendedor : 2000,
                encontrados.get(0).path("dinero").asInt(-1));
        }
    }

    private void verificarPantallas(int saldoVendedor) {
        for (Page pagina : paginas()) {
            for (Page jugador : paginas()) {
                new VistaPartida(pagina).verificarSaldo(nombre(jugador), jugador == vendedor ? saldoVendedor : 2000);
            }
            assertEquals(documento, pagina.evaluate("() => window.__e2eDocumentoVenta"),
                "La actualizacion debe conservar el documento sin recargas");
        }
    }

    private Page esperarSiguienteJugador(Page anterior) {
        for (Page pagina : paginas()) assertThat(nombreJugadorEnTurno(pagina)).not().hasText(nombre(anterior));
        return verificarPermisoDeTirada(obtenerJugadorEnTurno(paginaAnfitrion));
    }

    private String nombre(Page jugador) {
        return nombres().get(paginas().indexOf(jugador));
    }

    private void descartarMensajes() {
        observadores.forEach(ObservadorPartidaE2E::descartarMensajesAnteriores);
    }
}
