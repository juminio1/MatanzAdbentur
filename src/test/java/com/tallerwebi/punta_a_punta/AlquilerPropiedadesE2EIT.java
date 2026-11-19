package com.tallerwebi.punta_a_punta;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.tallerwebi.dominio.ConfiguracionDadosE2E;
import com.tallerwebi.punta_a_punta.vistas.VistaPartida;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Alquileres reales, con importes conocidos y tres sesiones aisladas. */
public class AlquilerPropiedadesE2EIT extends EscenarioPartidaE2E {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final Map<Page, Integer> saldos = new HashMap<>();
    private List<ObservadorPartidaE2E> observadores;
    private String topic;
    private String marcaDocumento;
    private Page propietario;

    @Test
    void cuandoOtroJugadorCaeEnPlazaSanJustoEntoncesPagaTreintaAlPropietarioEnLasTresSesiones()
        throws IOException {
        iniciarYComprar(4, "Plaza San Justo", 200, 1, 3);
        Page pagador = siguienteJugador(propietario);
        mover(pagador, 0, 4, 1, 3, 30, 0);
        siguienteJugador(pagador);

        assertEquals(1830, saldos.get(propietario));
        assertEquals(1970, saldos.get(pagador));
        Page tercero = paginas().stream().filter(pagina -> pagina != propietario && pagina != pagador)
            .findFirst().orElseThrow();
        assertEquals(2000, saldos.get(tercero));
        assertEquals(5800, saldos.values().stream().mapToInt(Integer::intValue).sum());
        verificarPropietario(4, 30);
        verificarPantallas();
    }

    @Test
    void cuandoElPropietarioVuelveAJesseJamesEntoncesNoPagaAlquilerNiCambianLosSaldos()
        throws IOException {
        iniciarYComprar(11, "Jesse James", 250, 5, 6);
        Page invitadoUno = siguienteJugador(propietario);
        mover(invitadoUno, 0, 11, 5, 6, 40, 0);
        Page invitadoDos = siguienteJugador(invitadoUno);
        assertTrue(invitadoDos != propietario && invitadoDos != invitadoUno);
        mover(invitadoDos, 0, 11, 5, 6, 40, 0);
        assertEquals(propietario, siguienteJugador(invitadoDos));

        assertEquals(1830, saldos.get(propietario));
        assertEquals(1960, saldos.get(invitadoUno));
        assertEquals(1960, saldos.get(invitadoDos));
        List<Page> orden = List.of(propietario, invitadoUno, invitadoDos);
        for (Page jugador : orden) {
            verificarPermisoDeTirada(nombre(jugador));
            mover(jugador, 11, 22, 5, 6, 0, 0);
            new VistaPartida(jugador).verificarOportunidadDeCompra("Superchino Luna", 400);
            new VistaPartida(jugador).pasarSinComprar();
            siguienteJugador(jugador);
        }
        for (Page jugador : orden) {
            verificarPermisoDeTirada(nombre(jugador));
            mover(jugador, 22, 0, 3, 5, 0, 150);
            siguienteJugador(jugador);
        }

        assertEquals(1980, saldos.get(propietario));
        assertEquals(2110, saldos.get(invitadoUno));
        assertEquals(2110, saldos.get(invitadoDos));
        Map<Page, Integer> antesDeCaer = Map.copyOf(saldos);
        verificarPantallas();
        JsonNode caidaPropia = mover(propietario, 0, 11, 5, 6, 0, 0);
        siguienteJugador(propietario);
        assertTrue(caidaPropia.path("alquiler").isNull(), "La caida propia no debe generar alquiler");
        assertEquals(antesDeCaer, saldos);
        verificarPropietario(11, 40);
        verificarPantallas();
    }

    private void iniciarYComprar(int posicion, String propiedad, int precio, int dadoUno, int dadoDos)
        throws IOException {
        observadores = paginas().stream().map(ObservadorPartidaE2E::new).toList();
        for (int indice = 0; indice < paginas().size(); indice++) {
            dadoQueUnUsuarioSeRegistraEIniciaSesion(paginas().get(indice), nombres().get(indice),
                "alquiler" + indice + "@unlam.edu.ar");
            saldos.put(paginas().get(indice), 2000);
        }
        String codigo = cuandoElAnfitrionCreaUnaSala();
        cuandoElInvitadoSeUneALaSala(paginaInvitadoUno, codigo);
        cuandoElInvitadoSeUneALaSala(paginaInvitadoDos, codigo);
        elegirFichas();
        iniciarPartidaYEsperarTableros(codigo);
        topic = "/topic/partida/" + codigo;
        esperarSuscripciones();
        marcaDocumento = UUID.randomUUID().toString();
        for (Page pagina : paginas()) {
            new VistaPartida(pagina).verificarInicio();
            new VistaPartida(pagina).verificarDisponible(posicion, precio);
            pagina.evaluate("marca => window.__e2eDocumentoAlquiler = marca", marcaDocumento);
        }
        verificarPantallas();
        propietario = verificarPermisoDeTirada(obtenerJugadorEnTurno(paginaAnfitrion));
        mover(propietario, 0, posicion, dadoUno, dadoDos, 0, 0);
        VistaPartida vista = new VistaPartida(propietario);
        vista.verificarOportunidadDeCompra(propiedad, precio);
        observadores.forEach(ObservadorPartidaE2E::descartarMensajesAnteriores);
        JsonNode compra = JSON.readTree(vista.comprar(posicion).text());
        saldos.put(propietario, 2000 - precio);
        assertEquals(id(propietario), compra.path("propietarioId").asLong(-1));
        assertEquals(precio, compra.path("precioCompra").asInt(-1));
        assertEquals(2000 - precio, compra.path("dineroJugador").asInt(-1));
        assertTrue(compra.path("disponible").isBoolean());
        assertFalse(compra.path("disponible").asBoolean());
        for (ObservadorPartidaE2E observador : observadores) {
            observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == posicion
                    && cuerpo.path("propietarioId").asLong(-1) == id(propietario));
        }
        verificarPantallas();
    }

    private void elegirFichas() {
        List<String> colores = List.of("ROJO", "AZUL", "VERDE");
        for (int indice = 0; indice < paginas().size(); indice++) {
            VistaSalaEspera sala = new VistaSalaEspera(paginas().get(indice));
            sala.esperarConexionWebSocket();
            sala.elegirFicha(colores.get(indice));
            sala.verificarFichaDeJugador(nombres().get(indice), colores.get(indice));
        }
    }

    private void esperarSuscripciones() {
        for (ObservadorPartidaE2E observador : observadores) {
            observador.esperarSuscripcion(topic);
            observador.esperarSuscripcion(topic + "/propiedades");
            observador.esperarSuscripcion(topic + "/turno");
        }
    }

    private JsonNode mover(Page jugador, int origen, int destino, int dadoUno, int dadoDos,
        int alquiler, int premioInicio) throws IOException {
        verificarPermisoDeTirada(nombre(jugador));
        observadores.forEach(ObservadorPartidaE2E::descartarMensajesAnteriores);
        jugador.setExtraHTTPHeaders(Map.of(ConfiguracionDadosE2E.CABECERA_TIRADA, dadoUno + "," + dadoDos));
        JsonNode movimiento;
        try {
            movimiento = JSON.readTree(new VistaPartida(jugador).tirarDados().text());
        } finally {
            jugador.setExtraHTTPHeaders(Map.of());
        }
        assertEquals(id(jugador), movimiento.path("idJugador").asLong(-1));
        assertEquals(origen, movimiento.path("posicionAnterior").asInt(-1));
        assertEquals(destino, movimiento.path("nuevaPosicion").asInt(-1));
        assertEquals(dadoUno, movimiento.path("dado1").asInt(-1));
        assertEquals(dadoDos, movimiento.path("dado2").asInt(-1));
        assertEquals(dadoUno + dadoDos, movimiento.path("suma").asInt(-1));
        assertEquals(premioInicio, movimiento.path("dineroPorInicio").asInt(-1));
        verificarAlquiler(movimiento, jugador, alquiler);
        saldos.compute(jugador, (pagina, saldo) -> saldo + premioInicio - alquiler);
        if (alquiler > 0) saldos.compute(propietario, (pagina, saldo) -> saldo + alquiler);
        verificarSaldosMensaje(movimiento);
        String movimientoId = movimiento.path("idMovimiento").asText();
        assertFalse(movimientoId.isBlank());
        for (ObservadorPartidaE2E observador : observadores) {
            JsonNode recibido = observador.esperarMensaje(topic, cuerpo ->
                movimientoId.equals(cuerpo.path("idMovimiento").asText()));
            assertEquals(movimiento, recibido, "Cada sesion debe recibir el movimiento real completo");
        }
        long idJugador = id(jugador);
        for (Page pagina : paginas()) {
            var ficha = pagina.locator("[data-casillero='" + destino
                + "'] .ficha[data-jugador-id='" + idJugador + "']");
            // Espera la llegada real de la ficha, tambien en documentos en segundo plano.
            pagina.waitForCondition(() -> ficha.count() == 1);
            new VistaPartida(pagina).verificarPosicion(idJugador, destino);
        }
        verificarPantallas();
        return movimiento;
    }

    private void verificarAlquiler(JsonNode movimiento, Page jugador, int monto) {
        JsonNode alquiler = movimiento.path("alquiler");
        if (monto == 0) {
            assertTrue(alquiler.isNull(), "Este movimiento no debe generar alquiler");
            return;
        }
        assertEquals(monto, alquiler.path("monto").asInt(-1));
        assertEquals(id(jugador), alquiler.path("idJugadorPagador").asLong(-1));
        assertEquals(nombre(jugador), alquiler.path("pagador").asText());
        assertEquals(nombre(propietario), alquiler.path("propietario").asText());
        assertEquals(movimiento.path("nuevaPosicion").asInt() == 4 ? "Plaza San Justo" : "Jesse James",
            alquiler.path("propiedad").asText());
    }

    private void verificarSaldosMensaje(JsonNode movimiento) {
        JsonNode recibidos = movimiento.path("saldos");
        assertEquals(3, recibidos.size());
        for (Page jugador : paginas()) {
            long idJugador = id(jugador);
            List<JsonNode> coincidencias = new java.util.ArrayList<>();
            recibidos.forEach(saldo -> {
                if (saldo.path("idJugador").asLong(-1) == idJugador) coincidencias.add(saldo);
            });
            assertEquals(1, coincidencias.size());
            assertEquals(saldos.get(jugador).intValue(), coincidencias.get(0).path("dinero").asInt(-1));
        }
    }

    private Page siguienteJugador(Page anterior) {
        for (Page pagina : paginas()) assertThat(nombreJugadorEnTurno(pagina)).not().hasText(nombre(anterior));
        return verificarPermisoDeTirada(obtenerJugadorEnTurno(paginaAnfitrion));
    }

    private void verificarPantallas() {
        for (Page pagina : paginas()) {
            for (Page jugador : paginas()) {
                new VistaPartida(pagina).verificarSaldo(nombre(jugador), saldos.get(jugador));
            }
            assertEquals(marcaDocumento, pagina.evaluate("() => window.__e2eDocumentoAlquiler"),
                "El documento debe mantenerse sin recargas");
        }
    }

    private void verificarPropietario(int posicion, int alquiler) {
        String color = List.of("rojo", "azul", "verde").get(paginas().indexOf(propietario));
        for (Page pagina : paginas()) {
            new VistaPartida(pagina).verificarPropietario(posicion, nombre(propietario), color, alquiler);
        }
    }

    private String nombre(Page jugador) {
        return nombres().get(paginas().indexOf(jugador));
    }

    private long id(Page jugador) {
        return new VistaPartida(jugador).obtenerIdDeSesion();
    }
}
