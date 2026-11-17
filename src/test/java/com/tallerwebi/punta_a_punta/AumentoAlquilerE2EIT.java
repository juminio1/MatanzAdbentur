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

/** Completa una localidad y cobra el alquiler mejorado mediante navegadores reales. */
public class AumentoAlquilerE2EIT extends EscenarioPartidaE2E {
    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<Page, Integer> saldos = new HashMap<>();
    private final Map<Page, Long> identidades = new HashMap<>();
    private List<ObservadorPartidaE2E> observadores;
    private String topic;
    private String documento;
    private Page propietario;

    @Test
    void cuandoCompletaIsidroCasanovaYMejoraCanchaDeBrownEntoncesOtroJugadorPagaOchenta()
        throws IOException {
        preparar();
        propietario = verificarPermisoDeTirada(obtenerJugadorEnTurno(paginaAnfitrion));
        mover(propietario, 0, 9, 4, 5);
        comprar(9, "Cancha de Brown");
        Page jugadorB = siguiente(propietario);
        pasar(jugadorB, 0, 2, 1, 1, "Parroquia Stella Maris", 150);
        Page jugadorC = siguiente(jugadorB);
        assertTrue(jugadorC != propietario && jugadorC != jugadorB);
        pasar(jugadorC, 0, 2, 1, 1, "Parroquia Stella Maris", 150);
        assertEquals(propietario, siguiente(jugadorC));

        VistaPartida vista = new VistaPartida(propietario);
        vista.abrirInformacionPropiedad(9, "Cancha de Brown");
        vista.verificarAlquilerEnInformacion(40);
        vista.verificarPermisoDeMejora(false, 125);
        vista.cerrarInformacionPropiedad();
        mover(propietario, 9, 11, 1, 1);
        comprar(11, "Jesse James");
        siguiente(propietario);
        pasar(jugadorB, 2, 5, 1, 2, "UNLaM", 200);
        siguiente(jugadorB);
        pasar(jugadorC, 2, 5, 1, 2, "UNLaM", 200);
        assertEquals(propietario, siguiente(jugadorC));

        mover(propietario, 11, 13, 1, 1);
        comprar(13, "Plaza Hipólito Yrigoyen");
        siguiente(propietario);
        pasar(jugadorB, 5, 7, 1, 1, "San Justo Shopping", 200);
        siguiente(jugadorB);
        pasar(jugadorC, 5, 7, 1, 1, "San Justo Shopping", 200);
        assertEquals(propietario, siguiente(jugadorC));
        for (Page pagina : paginas()) assertThat(pagina.locator("#ronda-actual")).hasText("4");
        assertEquals(1250, saldos.get(propietario));

        mejorarCancha();
        verificarNivelesPropios();
        pasar(propietario, 13, 15, 1, 1, "Feria Carlos Casares", 300);
        assertEquals(jugadorB, siguiente(propietario));
        JsonNode caida = mover(jugadorB, 7, 9, 1, 1);
        JsonNode alquiler = caida.path("alquiler");
        assertEquals(80, alquiler.path("monto").asInt(-1));
        assertEquals(identidades.get(jugadorB).longValue(), alquiler.path("idJugadorPagador").asLong(-1));
        assertEquals(nombre(jugadorB), alquiler.path("pagador").asText());
        assertEquals(nombre(propietario), alquiler.path("propietario").asText());
        assertEquals("Cancha de Brown", alquiler.path("propiedad").asText());
        saldos.put(propietario, 1125 + 80);
        saldos.put(jugadorB, 2000 - 80);
        verificarSaldosMovimiento(caida);
        verificarPantallas();
        assertEquals(jugadorC, siguiente(jugadorB));
        verificarPropiedades();
        assertEquals(1205, saldos.get(propietario));
        assertEquals(1920, saldos.get(jugadorB));
        assertEquals(2000, saldos.get(jugadorC));
    }

    private void preparar() {
        observadores = paginas().stream().map(ObservadorPartidaE2E::new).toList();
        topic = "/topic/partida/" + PreparacionPartidaPropiedadesE2E.iniciarConTresJugadores(this);
        documento = UUID.randomUUID().toString();
        for (ObservadorPartidaE2E observador : observadores) {
            observador.esperarSuscripcion(topic);
            observador.esperarSuscripcion(topic + "/propiedades");
            observador.esperarSuscripcion(topic + "/turno");
        }
        for (Page pagina : paginas()) {
            VistaPartida vista = new VistaPartida(pagina);
            vista.verificarInicio();
            identidades.put(pagina, vista.obtenerIdDeSesion());
            saldos.put(pagina, 2000);
            for (int posicion : List.of(9, 11, 13)) vista.verificarDisponible(posicion, 250);
            pagina.evaluate("marca => window.__e2eDocumentoMejora = marca", documento);
        }
        verificarPantallas();
    }

    private void comprar(int posicion, String nombrePropiedad) throws IOException {
        VistaPartida vista = new VistaPartida(propietario);
        vista.verificarOportunidadDeCompra(nombrePropiedad, 250);
        descartarMensajes();
        JsonNode compra = JSON.readTree(vista.comprar(posicion).text());
        saldos.compute(propietario, (pagina, saldo) -> saldo - 250);
        verificarDatosPropiedad(compra, posicion, 0, 40);
        assertEquals(250, compra.path("precioCompra").asInt(-1));
        assertEquals(saldos.get(propietario).intValue(), compra.path("dineroJugador").asInt(-1));
        for (ObservadorPartidaE2E observador : observadores) {
            JsonNode recibido = observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == posicion);
            verificarDatosPropiedad(recibido, posicion, 0, 40);
        }
        verificarPantallas();
    }

    private void mejorarCancha() throws IOException {
        VistaPartida vista = new VistaPartida(propietario);
        vista.abrirInformacionPropiedad(9, "Cancha de Brown");
        vista.verificarPermisoDeMejora(true, 125);
        vista.verificarAlquilerEnInformacion(40);
        descartarMensajes();
        JsonNode mejora = JSON.readTree(vista.mejorar(9).text());
        verificarDatosPropiedad(mejora, 9, 1, 80);
        saldos.put(propietario, 1250 - 125);
        for (ObservadorPartidaE2E observador : observadores) {
            JsonNode recibido = observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == 9 && cuerpo.path("nivelMejora").asInt(-1) == 1);
            verificarDatosPropiedad(recibido, 9, 1, 80);
            assertEquals(1125, recibido.path("dineroJugador").asInt(-1));
            assertEquals(identidades.get(propietario).longValue(), recibido.path("idJugador").asLong(-1));
        }
        assertEquals(1125, mejora.path("dineroJugador").asInt(-1));
        vista.verificarAlquilerEnInformacion(80);
        vista.cerrarInformacionPropiedad();
        verificarPropiedades();
        verificarPantallas();
        verificarPermisoDeTirada(nombre(propietario));
    }

    private void verificarNivelesPropios() {
        propietario.locator("#btn-bono-propiedades").click();
        assertThat(propietario.locator("#modal-bono-propiedades")).isVisible();
        for (String propiedad : List.of("Cancha de Brown", "Jesse James", "Plaza Hipólito Yrigoyen")) {
            Locator tarjeta = propietario.locator("#lista-propiedades-propias .propiedad-propia")
                .filter(new Locator.FilterOptions().setHasText(propiedad));
            assertThat(tarjeta).hasCount(1);
            assertThat(tarjeta.locator("dd").nth(0)).hasText(propiedad.equals("Cancha de Brown") ? "$80" : "$40");
            assertThat(tarjeta.locator("dd").nth(1)).hasText(propiedad.equals("Cancha de Brown") ? "1 / 5" : "0 / 5");
        }
        propietario.locator("#btn-cerrar-bono-propiedades").click();
        assertThat(propietario.locator("#modal-bono-propiedades")).not().isVisible();
    }

    private void pasar(Page jugador, int origen, int destino, int dadoUno, int dadoDos,
        String propiedad, int precio) throws IOException {
        JsonNode movimiento = mover(jugador, origen, destino, dadoUno, dadoDos);
        assertTrue(movimiento.path("alquiler").isNull());
        verificarSaldosMovimiento(movimiento);
        VistaPartida vista = new VistaPartida(jugador);
        vista.verificarOportunidadDeCompra(propiedad, precio);
        vista.pasarSinComprar();
        verificarPantallas();
    }

    private JsonNode mover(Page jugador, int origen, int destino, int dadoUno, int dadoDos) throws IOException {
        verificarPermisoDeTirada(nombre(jugador));
        descartarMensajes();
        jugador.setExtraHTTPHeaders(Map.of(ConfiguracionDadosE2E.CABECERA_TIRADA, dadoUno + "," + dadoDos));
        JsonNode movimiento;
        try {
            movimiento = JSON.readTree(new VistaPartida(jugador).tirarDados().text());
        } finally {
            jugador.setExtraHTTPHeaders(Map.of());
        }
        assertEquals(origen, movimiento.path("posicionAnterior").asInt(-1));
        assertEquals(destino, movimiento.path("nuevaPosicion").asInt(-1));
        assertEquals(dadoUno, movimiento.path("dado1").asInt(-1));
        assertEquals(dadoDos, movimiento.path("dado2").asInt(-1));
        assertEquals(0, movimiento.path("dineroPorInicio").asInt(-1));
        assertEquals(identidades.get(jugador).longValue(), movimiento.path("idJugador").asLong(-1));
        String movimientoId = movimiento.path("idMovimiento").asText();
        assertFalse(movimientoId.isBlank());
        for (ObservadorPartidaE2E observador : observadores) {
            assertEquals(movimiento, observador.esperarMensaje(topic,
                cuerpo -> movimientoId.equals(cuerpo.path("idMovimiento").asText())));
        }
        for (Page pagina : paginas()) {
            Locator ficha = pagina.locator("[data-casillero='" + destino
                + "'] .ficha[data-jugador-id='" + identidades.get(jugador) + "']");
            // Los tres documentos presentan la animacion real, incluso en segundo plano.
            pagina.waitForCondition(() -> ficha.count() == 1);
            new VistaPartida(pagina).verificarPosicion(identidades.get(jugador), destino);
        }
        return movimiento;
    }

    private void verificarDatosPropiedad(JsonNode propiedad, int posicion, int nivel, int alquiler) {
        assertEquals(posicion, propiedad.path("posicion").asInt(-1));
        assertEquals(nivel, propiedad.path("nivelMejora").asInt(-1));
        assertEquals(alquiler, propiedad.path("alquiler").asInt(-1));
        assertEquals(identidades.get(propietario).longValue(), propiedad.path("propietarioId").asLong(-1));
        assertEquals(nombre(propietario), propiedad.path("propietario").asText());
    }

    private void verificarSaldosMovimiento(JsonNode movimiento) {
        assertEquals(3, movimiento.path("saldos").size());
        for (Page jugador : paginas()) {
            List<JsonNode> encontrados = new java.util.ArrayList<>();
            movimiento.path("saldos").forEach(saldo -> {
                if (saldo.path("idJugador").asLong(-1) == identidades.get(jugador)) encontrados.add(saldo);
            });
            assertEquals(1, encontrados.size());
            assertEquals(saldos.get(jugador).intValue(), encontrados.get(0).path("dinero").asInt(-1));
        }
    }

    private void verificarPropiedades() {
        String color = List.of("rojo", "azul", "verde").get(paginas().indexOf(propietario));
        for (Page pagina : paginas()) {
            for (int posicion : List.of(9, 11, 13)) {
                new VistaPartida(pagina).verificarPropietario(posicion, nombre(propietario), color, posicion == 9 ? 80 : 40);
            }
        }
    }

    private void verificarPantallas() {
        for (Page pagina : paginas()) {
            for (Page jugador : paginas()) new VistaPartida(pagina).verificarSaldo(nombre(jugador), saldos.get(jugador));
            assertEquals(documento, pagina.evaluate("() => window.__e2eDocumentoMejora"), "No debe recargarse la pagina");
        }
    }

    private Page siguiente(Page anterior) {
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
