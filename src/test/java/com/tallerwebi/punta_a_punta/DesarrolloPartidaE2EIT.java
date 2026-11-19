package com.tallerwebi.punta_a_punta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.tallerwebi.dominio.ConfiguracionDadosE2E;
import com.tallerwebi.dominio.catalogo.CatalogoTablero;
import com.tallerwebi.dominio.servicios.ServicioMovimientoImpl;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

public class DesarrolloPartidaE2EIT extends EscenarioPartidaE2E {

    private static final ObjectMapper JSON = new ObjectMapper();
    // El turno dura 60 segundos (ServicioPartidaImpl); se deja margen para la tirada automática.
    private static final double ESPERA_VENCIMIENTO_MS = 90_000;
    // Recorrido de la vuelta: dos paradas en propiedades (se pasa sin comprar) y la llegada a la salida.
    private static final List<Integer> PARADAS_DEL_RECORRIDO = List.of(11, 22);
    private final Set<Page> sesionesSuscritasAlTurno = new HashSet<>();
    private final Map<Page, List<MensajePartida>> mensajesPorPagina = new HashMap<>();

    @Test
    void soloElJugadorDelTurnoPuedeTirarDados() {
        String codigoSala = iniciarPartidaConTresJugadores();
        String nombreInicial = obtenerJugadorEnTurno(paginaAnfitrion);
        Page jugadorInicial = verificarPermisoDeTirada(nombreInicial);

        // Cedemos el primer turno antes de tirar para aislar el permiso de los efectos del tablero.
        String urlAvanzarTurno = jugadorInicial.url().replaceFirst(
            "/partida\\?.*$", "/partida/" + codigoSala + "/avanzar-turno"
        );
        APIResponse cambioTurno = jugadorInicial.request().post(urlAvanzarTurno);
        PlaywrightAssertions.assertThat(cambioTurno).isOK();

        PlaywrightAssertions.assertThat(nombreJugadorEnTurno(jugadorInicial)).not().hasText(nombreInicial);
        String siguiente = obtenerJugadorEnTurno(jugadorInicial);
        Page jugadorSiguiente = verificarPermisoDeTirada(siguiente);

        tirarDados(jugadorSiguiente);
        PlaywrightAssertions.assertThat(jugadorSiguiente.locator("#btn-tirar[aria-busy]")).hasCount(0);
        PlaywrightAssertions.assertThat(jugadorSiguiente.locator("#btn-tirar")).isDisabled();
        pausarSiCorresponde("fin");
    }

    @Test
    void dadoQueExisteUnaPartidaIniciadaCuandoLosJugadoresTiranDadosYCompletanTurnosEntoncesSeActualizanLasPosicionesLosTurnosYLasRondas()
        throws IOException, SQLException {
        String codigo = iniciarPartidaConTresJugadores();
        List<String> orden = obtenerOrdenDeTurnos(codigo);
        assertEquals(paginas().size(), orden.size());
        assertEquals(Set.copyOf(nombres()), Set.copyOf(orden));
        String documento = marcarDocumentos();

        verificarEstadoEnPantallas(1, 0, documento);
        verificarQueElRecorridoEviteLosEfectosDelTablero();

        // En las dos primeras rondas cada jugador avanza once casilleros y pasa sin comprar.
        for (int ronda = 1; ronda <= 2; ronda++) {
            int origen = (ronda - 1) * 11;
            int destino = ronda * 11;
            for (int turno = 0; turno < orden.size(); turno++) {
                Page jugador = verificarPermisoDeTirada(orden.get(turno));
                verificarPosicionEnTodasLasPantallas(idDeSesion(jugador), origen);
                verificarMovimiento(codigo, jugador, origen, destino, 5, 6);
                jugador.locator("#btn-modal-pasar").click();

                int siguienteIndice = (turno + 1) % orden.size();
                int rondaEsperada = siguienteIndice == 0 ? ronda + 1 : ronda;
                esperarMensajeDeTurno(codigo, orden.get(siguienteIndice), rondaEsperada, siguienteIndice);
                verificarEstadoEnPantallas(rondaEsperada, siguienteIndice, documento);
            }
        }

        Page primerJugador = verificarPermisoDeTirada(orden.get(0));
        long idPrimerJugador = idDeSesion(primerJugador);
        verificarPosicionEnTodasLasPantallas(idPrimerJugador, 22);
        int dineroAntesDeLaVuelta = dineroEnPantalla(primerJugador, idPrimerJugador);
        JsonNode vuelta = verificarMovimiento(codigo, primerJugador, 22, 0, 3, 5);

        // Al completar la vuelta cobra el premio de la salida, y todos ven el saldo nuevo.
        int premio = ServicioMovimientoImpl.PREMIO_INICIO;
        assertEquals(premio, vuelta.path("dineroPorInicio").asInt(-1));
        assertEquals(dineroAntesDeLaVuelta + premio, saldoInformado(vuelta, idPrimerJugador));
        for (Page pagina : paginas()) {
            PlaywrightAssertions.assertThat(dineroDe(pagina, idPrimerJugador))
                .hasText(formatearDinero(dineroAntesDeLaVuelta + premio));
        }

        String nombreSiguiente = orden.get(1);
        esperarMensajeDeTurno(codigo, nombreSiguiente, 3, 1);
        verificarPermisoDeTirada(nombreSiguiente);
        verificarEstadoEnPantallas(3, 1, documento);
        pausarSiCorresponde("fin");
    }

    @Test
    void cuandoUnJugadorFueraDeTurnoIntentaTirarDadosEntoncesSeRechazaLaTiradaYLaPartidaNoCambia()
        throws IOException {
        String codigo = iniciarPartidaConTresJugadores();
        String nombreInicial = obtenerJugadorEnTurno(paginaAnfitrion);
        Page jugadorActual = verificarPermisoDeTirada(nombreInicial);
        Page fueraDeTurno = paginas().stream().filter(pagina -> pagina != jugadorActual).findFirst().orElseThrow();
        long idFueraDeTurno = idDeSesion(fueraDeTurno);
        String documento = marcarDocumentos();
        String base = fueraDeTurno.url().replaceFirst("/partida\\?.*$", "/partida");

        APIResponse respuesta = fueraDeTurno.request().post(
            base + "/tirar-dados?codigoUnico=" + codigo + "&idJugador=" + idFueraDeTurno
        );
        assertEquals(403, respuesta.status(), "La sesión autenticada no puede tirar fuera de su turno");
        APIResponse turno = fueraDeTurno.request().get(base + "/" + codigo + "/turno");
        PlaywrightAssertions.assertThat(turno).isOK();
        JsonNode estado = JSON.readTree(turno.text());
        assertEquals(idDeSesion(jugadorActual), estado.path("idJugadorActual").asLong());
        assertEquals(1, estado.path("rondaActual").asInt(-1));
        assertEquals(0, estado.path("turnosFinalizadosEnRonda").asInt(-1));
        verificarPermisoDeTirada(nombreInicial);
        for (Page pagina : paginas()) {
            verificarPosicionEnTodasLasPantallas(idDeSesion(pagina), 0);
        }
        verificarEstadoEnPantallas(1, 0, documento);

        verificarMovimiento(codigo, jugadorActual, 0, 4);
        verificarPosicionEnTodasLasPantallas(idFueraDeTurno, 0);
        for (Page pagina : paginas()) {
            assertFalse(mensajesPorPagina.get(pagina).stream().anyMatch(mensaje ->
                mensaje.destino().equals(topic(codigo)) && mensaje.cuerpo().path("idJugador").asLong() == idFueraDeTurno
            ), "Una tirada rechazada no debe publicar un movimiento");
        }
        pausarSiCorresponde("fin");
    }

    @Test
    void cuandoElJugadorEnTurnoNoTiraAntesDeQueVenzaElTiempoEntoncesElServidorTiraPorElYPasaElTurno()
        throws IOException, SQLException {
        String codigo = iniciarPartidaConTresJugadores();
        List<String> orden = obtenerOrdenDeTurnos(codigo);
        String documento = marcarDocumentos();

        // El primer jugador juega normalmente; así el segundo arranca su turno con el temporizador completo.
        Page primero = verificarPermisoDeTirada(orden.get(0));
        verificarMovimiento(codigo, primero, 0, 11, 5, 6);
        primero.locator("#btn-modal-pasar").click();
        esperarMensajeDeTurno(codigo, orden.get(1), 1, 1);

        Page ausente = verificarPermisoDeTirada(orden.get(1));
        long idAusente = idDeSesion(ausente);
        long idSinJugar = idDeSesion(paginaDe(orden.get(2)));
        // Sin la cabecera de la tirada, el perfil e2e usa los dados por defecto.
        int dadoUno = ConfiguracionDadosE2E.DADO_UNO;
        int dadoDos = ConfiguracionDadosE2E.DADO_DOS;
        int destino = dadoUno + dadoDos;

        // Nadie toca el botón: al vencer el turno, el servidor tira por el jugador y avisa a todos.
        JsonNode automatica = esperarMensajeEnTodasLasSesiones(topic(codigo), cuerpo ->
            cuerpo.path("turnoVencido").asBoolean(false) &&
            cuerpo.path("idJugador").asLong() == idAusente,
            ESPERA_VENCIMIENTO_MS
        );
        assertEquals(dadoUno, automatica.path("dado1").asInt());
        assertEquals(dadoDos, automatica.path("dado2").asInt());
        assertEquals(destino, automatica.path("suma").asInt());
        assertEquals(0, automatica.path("posicionAnterior").asInt(-1));
        assertEquals(destino, automatica.path("nuevaPosicion").asInt(-1));
        verificarPosicionEnTodasLasPantallas(idAusente, destino);

        // Cae en una propiedad libre, pero la tirada automática no compra ni abre el modal de compra.
        assertTrue(CatalogoTablero.POSICIONES_PROPIEDAD.contains(destino));
        PlaywrightAssertions.assertThat(ausente.locator("#modal-compra-propiedad")).not().isVisible();
        assertEquals(saldoInformado(automatica, idSinJugar), saldoInformado(automatica, idAusente),
            "Sin compra, el jugador ausente conserva el mismo dinero que quien todavía no jugó");

        // El turno pasa solo al siguiente jugador, sin cambiar de ronda.
        esperarMensajeDeTurno(codigo, orden.get(2), 1, 2);
        verificarPermisoDeTirada(orden.get(2));
        verificarEstadoEnPantallas(1, 2, documento);
        pausarSiCorresponde("fin");
    }

    private Response tirarDados(Page jugador) {
        Response tirada = jugador.waitForResponse(
            respuesta -> respuesta.url().contains("/partida/tirar-dados?") &&
                "POST".equals(respuesta.request().method()),
            jugador.locator("#btn-tirar")::click
        );
        assertTrue(tirada.ok(), "La tirada del jugador en turno debe ser aceptada");
        return tirada;
    }

    private JsonNode verificarMovimiento(String codigo, Page jugador, int origen, int destino) throws IOException {
        return verificarMovimiento(codigo, jugador, origen, destino, ConfiguracionDadosE2E.DADO_UNO, ConfiguracionDadosE2E.DADO_DOS);
    }

    private JsonNode verificarMovimiento(String codigo, Page jugador, int origen, int destino, int dadoUnoEsperado, int dadoDosEsperado)
        throws IOException {
        JsonNode tirada;
        jugador.setExtraHTTPHeaders(Map.of(ConfiguracionDadosE2E.CABECERA_TIRADA, dadoUnoEsperado + "," + dadoDosEsperado));
        try {
            tirada = JSON.readTree(tirarDados(jugador).text());
        } finally {
            jugador.setExtraHTTPHeaders(Map.of());
        }
        long idJugador = idDeSesion(jugador);
        int dadoUno = tirada.path("dado1").asInt();
        int dadoDos = tirada.path("dado2").asInt();
        assertTrue(dadoUno >= 1 && dadoUno <= 6 && dadoDos >= 1 && dadoDos <= 6);
        assertEquals(dadoUnoEsperado, dadoUno);
        assertEquals(dadoDosEsperado, dadoDos);
        assertEquals(dadoUno + dadoDos, tirada.path("suma").asInt());

        Locator dadosVisibles = jugador.locator("#dados-tablero:visible");
        PlaywrightAssertions.assertThat(dadosVisibles.locator("#dados-tablero-total"))
            .hasText(dadoUno + " + " + dadoDos + " = " + (dadoUno + dadoDos));
        Locator caras = dadosVisibles.locator("[data-cara]");
        PlaywrightAssertions.assertThat(caras.nth(0)).hasAttribute("data-cara", String.valueOf(dadoUno));
        PlaywrightAssertions.assertThat(caras.nth(1)).hasAttribute("data-cara", String.valueOf(dadoDos));

        assertEquals(idJugador, tirada.path("idJugador").asLong());
        assertEquals(origen, tirada.path("posicionAnterior").asInt(-1));
        assertEquals(destino, tirada.path("nuevaPosicion").asInt(-1));
        String idMovimiento = tirada.path("idMovimiento").asText();
        assertFalse(idMovimiento.isBlank());
        esperarMensajeEnTodasLasSesiones(topic(codigo), cuerpo ->
            idMovimiento.equals(cuerpo.path("idMovimiento").asText()) &&
            cuerpo.path("idJugador").asLong() == idJugador &&
            cuerpo.path("nuevaPosicion").asInt(-1) == destino
        );
        verificarPosicionEnTodasLasPantallas(idJugador, destino);
        return tirada;
    }

    private void verificarPosicionEnTodasLasPantallas(long idJugador, int posicion) {
        String ficha = "[data-casillero='" + posicion + "'] .ficha[data-jugador-id='" + idJugador + "']";
        for (Page pagina : paginas()) {
            PlaywrightAssertions.assertThat(pagina.locator(ficha)).hasCount(1);
        }
    }

    private long idDeSesion(Page pagina) {
        return ((Number) pagina.evaluate("() => Number(window.usuarioSesionId)")).longValue();
    }

    private String marcarDocumentos() {
        String marca = UUID.randomUUID().toString();
        for (Page pagina : paginas()) {
            pagina.evaluate("marca => window.__e2eDocumentoPartida = marca", marca);
        }
        return marca;
    }

    private void verificarEstadoEnPantallas(int ronda, int finalizados, String documento) {
        for (Page pagina : paginas()) {
            PlaywrightAssertions.assertThat(pagina.locator("#ronda-actual")).hasText(String.valueOf(ronda));
            pagina.waitForFunction(
                "finalizados => Number(window.claveTurnoActual?.split(':').at(-1)) === finalizados",
                finalizados
            );
            assertEquals(documento, pagina.evaluate("() => window.__e2eDocumentoPartida"),
                "La partida debe actualizarse en el mismo documento");
        }
    }

    private void observarWebSocket(Page pagina) {
        List<MensajePartida> mensajes = new ArrayList<>();
        mensajesPorPagina.put(pagina, mensajes);
        pagina.onWebSocket(socket -> {
            socket.onFrameSent(frame -> {
                String trama = frame.text();
                if (trama != null && trama.contains("SUBSCRIBE") &&
                    trama.contains("destination:/topic/partida/") && trama.contains("/turno")) {
                    sesionesSuscritasAlTurno.add(pagina);
                }
            });
            socket.onFrameReceived(frame -> registrarMensajesSockJs(frame.text(), mensajes));
        });
    }

    private void registrarMensajesSockJs(String trama, List<MensajePartida> mensajes) {
        if (trama == null || !trama.startsWith("a[") || !trama.contains("destination:/topic/partida/")) {
            return;
        }
        try {
            for (JsonNode elemento : JSON.readTree(trama.substring(1))) {
                String mensaje = elemento.asText();
                int separador = mensaje.indexOf("\n\n");
                if (!mensaje.startsWith("MESSAGE\n") || separador < 0) {
                    continue;
                }
                String destino = mensaje.substring(0, separador).lines()
                    .filter(linea -> linea.startsWith("destination:"))
                    .map(linea -> linea.substring("destination:".length()))
                    .findFirst().orElse("");
                if (destino.matches("/topic/partida/[^/]+(?:/turno)?")) {
                    String cuerpo = mensaje.substring(separador + 2).replace("\0", "");
                    mensajes.add(new MensajePartida(destino, JSON.readTree(cuerpo)));
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer un mensaje STOMP de la partida", ex);
        }
    }

    private JsonNode esperarMensajeEnTodasLasSesiones(String destino, Predicate<JsonNode> esperado) {
        return esperarMensajeEnTodasLasSesiones(destino, esperado, 30_000);
    }

    private JsonNode esperarMensajeEnTodasLasSesiones(String destino, Predicate<JsonNode> esperado, double timeoutMs) {
        Predicate<MensajePartida> coincide = mensaje -> destino.equals(mensaje.destino()) && esperado.test(mensaje.cuerpo());
        for (Page pagina : paginas()) {
            pagina.waitForCondition(
                () -> mensajesPorPagina.get(pagina).stream().anyMatch(coincide),
                new Page.WaitForConditionOptions().setTimeout(timeoutMs)
            );
        }
        return mensajesPorPagina.get(paginaAnfitrion).stream().filter(coincide).findFirst().orElseThrow().cuerpo();
    }

    private void verificarQueElRecorridoEviteLosEfectosDelTablero() {
        // Las paradas deben ser propiedades (se pasa sin comprar) y nunca casilleros de trivia o de eventos,
        // que abren la ruleta o sortean una tarjeta y cambiarían posiciones, turnos o rondas.
        for (Integer parada : PARADAS_DEL_RECORRIDO) {
            assertTrue(CatalogoTablero.POSICIONES_PROPIEDAD.contains(parada), "La parada " + parada + " debe ser una propiedad");
            assertFalse(CatalogoTablero.POSICIONES_TRIVIA.contains(parada), "La parada " + parada + " no puede ser trivia");
        }
    }

    private Page paginaDe(String apodo) {
        return paginas().get(nombres().indexOf(apodo));
    }

    private Locator dineroDe(Page pagina, long idJugador) {
        // Cada tarjeta de jugador usa su id como id del elemento (partida.html).
        return pagina.locator(".jugadores .jugador[id='" + idJugador + "'] .dinero");
    }

    private int dineroEnPantalla(Page pagina, long idJugador) {
        return Integer.parseInt(dineroDe(pagina, idJugador).innerText().replaceAll("[^0-9]", ""));
    }

    private int saldoInformado(JsonNode movimiento, long idJugador) {
        for (JsonNode saldo : movimiento.path("saldos")) {
            if (saldo.path("idJugador").asLong() == idJugador) {
                return saldo.path("dinero").asInt();
            }
        }
        throw new AssertionError("El movimiento no informa el saldo del jugador " + idJugador);
    }

    private String formatearDinero(int monto) {
        // Igual que partida.js (toLocaleString "es-AR"): separador de miles con punto.
        return "$" + String.format(Locale.ROOT, "%,d", monto).replace(',', '.');
    }

    private void esperarMensajeDeTurno(String codigo, String jugador, int ronda, int finalizados) {
        esperarMensajeEnTodasLasSesiones(topic(codigo) + "/turno", cuerpo ->
            jugador.equals(cuerpo.path("apodoJugadorActual").asText()) &&
            cuerpo.path("rondaActual").asInt(-1) == ronda &&
            cuerpo.path("turnosFinalizadosEnRonda").asInt(-1) == finalizados
        );
    }

    private void esperarSuscripciones() {
        paginaAnfitrion.waitForCondition(() -> sesionesSuscritasAlTurno.containsAll(paginas()));
    }

    private String topic(String codigo) {
        return "/topic/partida/" + codigo;
    }

    private List<String> obtenerOrdenDeTurnos(String codigo) throws SQLException {
        try (Connection conexion = ReiniciarDB.abrirConexionE2E();
             PreparedStatement consulta = conexion.prepareStatement("""
                 SELECT jugador.apodo FROM Jugador jugador JOIN Partida partida ON partida.id = jugador.partida_id
                 WHERE partida.codigoUnico = ?
                 ORDER BY CASE WHEN jugador.orden < partida.indiceTurnoActual THEN 1 ELSE 0 END, jugador.orden
                 """)) {
            consulta.setString(1, codigo);
            try (ResultSet filas = consulta.executeQuery()) {
                List<String> orden = new ArrayList<>();
                while (filas.next()) {
                    orden.add(filas.getString("apodo"));
                }
                return orden;
            }
        }
    }

    private record MensajePartida(String destino, JsonNode cuerpo) {}

    private String iniciarPartidaConTresJugadores() {
        paginas().forEach(this::observarWebSocket);
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaAnfitrion, ANFITRION, "juli@unlam.edu.ar");
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaInvitadoUno, INVITADO_UNO, "emi@unlam.edu.ar");
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaInvitadoDos, INVITADO_DOS, "may@unlam.edu.ar");

        String codigoSala = cuandoElAnfitrionCreaUnaSala();
        cuandoElInvitadoSeUneALaSala(paginaInvitadoUno, codigoSala);
        cuandoElInvitadoSeUneALaSala(paginaInvitadoDos, codigoSala);
        elegirFicha(paginaAnfitrion, ANFITRION, "ROJO");
        elegirFicha(paginaInvitadoUno, INVITADO_UNO, "AZUL");
        elegirFicha(paginaInvitadoDos, INVITADO_DOS, "VERDE");
        pausarSiCorresponde("sala");

        iniciarPartidaYEsperarTableros(codigoSala);
        esperarSuscripciones();
        return codigoSala;
    }

    private void elegirFicha(Page pagina, String nombre, String color) {
        VistaSalaEspera sala = new VistaSalaEspera(pagina);
        sala.esperarConexionWebSocket();
        sala.elegirFicha(color);
        sala.verificarFichaDeJugador(nombre, color);
    }
}
