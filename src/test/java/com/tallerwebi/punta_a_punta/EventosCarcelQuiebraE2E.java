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
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.tallerwebi.dominio.ConfiguracionDadosE2E;
import com.tallerwebi.dominio.ConfiguracionEventosE2E;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * E2E 4: eventos, cárcel y quiebra.
 *
 * <p>Los dados se fijan con la cabecera de ConfiguracionDadosE2E y los eventos con la de
 * ConfiguracionEventosE2E, así cada jugada es siempre la misma. Recorrido (J0, J1 y J2 en orden de turno):
 *
 * <ol>
 *   <li>Ronda 1: J0 cae en el casillero 3 y cobra «Changa de finde»; J1 cae en el 3 y saca
 *       «Choripán traicionero» (pierde el turno); J2 cae en la comisaría (8).</li>
 *   <li>Ronda 2: J0 juega, J1 pierde su oportunidad y J2, preso, tira sin dobles y no se mueve.</li>
 *   <li>Ronda 3: J0 y J1 juegan; J2 paga la fianza, sale de la comisaría y tira.</li>
 *   <li>Ronda 4: J1, sin dinero ni propiedades, saca «Anda con cuidado» y queda en quiebra.</li>
 *   <li>Ronda 5: sale el evento global PIQUETE; J0 saca la menor suma y vuelve a Inicio.</li>
 * </ol>
 */
public class EventosCarcelQuiebraE2E extends EscenarioPartidaE2E {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final double ESPERA_ASERCIONES_MS = 20_000;
    private static final double ESPERA_ASERCIONES_ORIGINAL_MS = 5_000;

    private static final int CASILLERO_EVENTO = 3;
    private static final int CASILLERO_COMISARIA = 8;
    private static final int CASILLERO_EVENTO_QUIEBRA = 14;
    private static final int CASILLERO_INICIO = 0;
    private static final int DINERO_INICIAL = 2000;
    private static final int COSTO_FIANZA = 100;
    private static final int DINERO_ANTES_DE_QUIEBRA = 100;

    private static final String EVENTO_BENEFICIO = "Changa de finde";
    private static final int MONTO_BENEFICIO = 500;
    private static final String EVENTO_PIERDE_TURNO = "Choripán traicionero";
    private static final String EVENTO_MULTA = "Anda con cuidado";
    private static final String EVENTO_GLOBAL = "PIQUETE";

    private final Set<Page> sesionesSuscritasAlTurno = new HashSet<>();
    private final Map<Page, Map<String, String>> cabecerasFijas = new HashMap<>();
    private final List<String> erroresDePagina = new ArrayList<>();

    @BeforeAll
    static void ampliarEsperaDeAserciones() {
        // Las animaciones de dados, fichas y avisos superan la espera de 5 s de Playwright.
        PlaywrightAssertions.setDefaultAssertionTimeout(ESPERA_ASERCIONES_MS);
    }

    @AfterAll
    static void restaurarEsperaDeAserciones() {
        PlaywrightAssertions.setDefaultAssertionTimeout(ESPERA_ASERCIONES_ORIGINAL_MS);
    }

    @Test
    void dadoQueUnaPartidaEstaEnCursoCuandoLosJugadoresActivanEventosYCumplenPenalizacionesEntoncesSeAplicanLasReglasCorrespondientes()
        throws IOException, SQLException {
        String codigo = iniciarPartidaConTresJugadores();
        List<String> orden = obtenerOrdenDeTurnos(codigo);
        String j0 = orden.get(0);
        String j1 = orden.get(1);
        String j2 = orden.get(2);

        // --- Ronda 1 ---------------------------------------------------------------------------
        cuandoUnJugadorCaeEnUnEventoIndividualEntoncesVeLaDescripcionYCobraElBeneficio(j0);
        cuandoUnJugadorSacaUnEventoDePenalizacionEntoncesQuedaConElTurnoPerdido(j1);
        cuandoUnJugadorCaeEnLaComisariaEntoncesQuedaEncarceladoEnTodasLasPantallas(j2);
        verificarPermisoDeTirada(j0);

        // --- Ronda 2 ---------------------------------------------------------------------------
        moverYPasar(j0, CASILLERO_EVENTO, 7, 1, 3);
        entoncesElJugadorPenalizadoPierdeSuOportunidadYElTurnoPasaAlSiguiente(j1, j2);
        cuandoElJugadorPresoTiraSinDoblesEntoncesNoPuedeAvanzar(j2);
        verificarPermisoDeTirada(j0);

        // --- Ronda 3 ---------------------------------------------------------------------------
        moverYPasar(j0, 7, 11, 1, 3);
        moverYPasar(j1, CASILLERO_EVENTO, 7, 2, 2);
        cuandoElJugadorPresoPagaLaFianzaEntoncesSaleDeLaComisariaYVuelveAJugar(j2);

        // --- Ronda 4 ---------------------------------------------------------------------------
        moverYPasar(j0, 11, 16, 2, 3);
        cuandoUnJugadorSinDineroNiPropiedadesNoPuedePagarUnaMultaEntoncesQuedaEnQuiebra(j1, j2);

        // El último turno de la ronda 4 abre la ronda 5, que tiene evento global.
        fijarCabeceraEnTodasLasSesiones(ConfiguracionEventosE2E.CABECERA_EVENTO_GLOBAL, EVENTO_GLOBAL);
        moverYPasar(j2, 12, 15, 1, 2);

        // --- Ronda 5 ---------------------------------------------------------------------------
        cuandoSaleElPiqueteEntoncesSoloElJugadorConMenorSumaVuelveAInicio(j0, j2, j1);
        entoncesLaGestionDeTurnosSalteaAlJugadorEnQuiebra(j0, j2);

        entoncesElHistorialRegistraLosEventosYLasPenalizaciones(j0, j1, j2);
        assertEquals(List.of(), erroresDePagina, "Ninguna pantalla debe registrar errores de JavaScript");
        pausarSiCorresponde("fin");
    }

    // ----------------------------------------------------------------------------------------------
    // Pasos del checklist
    // ----------------------------------------------------------------------------------------------

    private void cuandoUnJugadorCaeEnUnEventoIndividualEntoncesVeLaDescripcionYCobraElBeneficio(String nombre)
        throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);

        JsonNode tirada = tirar(jugador, 1, 2, EVENTO_BENEFICIO);
        assertEquals(CASILLERO_EVENTO, tirada.path("posicionCaidaEvento").asInt(-1));
        assertEquals(EVENTO_BENEFICIO, tirada.path("nombreEvento").asText());
        String descripcion = tirada.path("descripcionEvento").asText();
        assertFalse(descripcion.isBlank());

        // Solo el jugador que cayó ve el aviso del evento.
        assertThat(jugador.locator("#aviso-evento-nombre")).hasText(EVENTO_BENEFICIO);
        assertThat(jugador.locator("#aviso-evento-descripcion")).hasText(descripcion);
        jugador.locator("#btn-continuar-evento").click();

        verificarFichaEnTodasLasPantallas(id, CASILLERO_EVENTO);
        verificarDineroEnTodasLasPantallas(id, DINERO_INICIAL + MONTO_BENEFICIO);
        assertEquals(DINERO_INICIAL + MONTO_BENEFICIO, consultarJugador(id).dinero());
        verificarHistorialEnTodasLasPantallas(nombre + " sacó el evento «" + EVENTO_BENEFICIO + "»");
    }

    private void cuandoUnJugadorSacaUnEventoDePenalizacionEntoncesQuedaConElTurnoPerdido(String nombre)
        throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);

        JsonNode tirada = tirar(jugador, 1, 2, EVENTO_PIERDE_TURNO);
        assertEquals(EVENTO_PIERDE_TURNO, tirada.path("nombreEvento").asText());
        assertThat(jugador.locator("#aviso-evento-nombre")).hasText(EVENTO_PIERDE_TURNO);
        jugador.locator("#btn-continuar-evento").click();

        verificarFichaEnTodasLasPantallas(id, CASILLERO_EVENTO);
        assertTrue(consultarJugador(id).pierdeTurno(), "El evento debe dejar una penalización pendiente");
        verificarHistorialEnTodasLasPantallas(nombre + " sacó el evento «" + EVENTO_PIERDE_TURNO + "»");
    }

    private void cuandoUnJugadorCaeEnLaComisariaEntoncesQuedaEncarceladoEnTodasLasPantallas(String nombre)
        throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);

        JsonNode tirada = tirar(jugador, 3, 5, null);
        assertEquals(CASILLERO_COMISARIA, tirada.path("nuevaPosicion").asInt(-1));

        verificarFichaEnTodasLasPantallas(id, CASILLERO_COMISARIA);
        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, id).locator(".estado-carcel")).isVisible();
            assertThat(tarjeta(pagina, id)).hasAttribute("data-encarcelado", "true");
        }
        EstadoJugador estado = consultarJugador(id);
        assertTrue(estado.encarcelado());
        assertEquals(0, estado.intentosCarcel());
        verificarHistorialEnTodasLasPantallas(nombre + " quedó detenido en la comisaría.");
    }

    private void entoncesElJugadorPenalizadoPierdeSuOportunidadYElTurnoPasaAlSiguiente(
        String penalizado, String siguiente
    ) throws SQLException {
        long idPenalizado = idDeSesion(paginaDe(penalizado));
        // Después de J0 el turno salta directamente a J2.
        verificarPermisoDeTirada(siguiente);
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#ronda-actual")).hasText("2");
        }
        assertFalse(consultarJugador(idPenalizado).pierdeTurno(), "La penalización se consume al saltear el turno");
        verificarFichaEnTodasLasPantallas(idPenalizado, CASILLERO_EVENTO);
    }

    private void cuandoElJugadorPresoTiraSinDoblesEntoncesNoPuedeAvanzar(String nombre)
        throws IOException, SQLException {
        Page preso = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(preso);

        // Solo el preso ve el panel de la comisaría en su turno.
        assertThat(preso.locator("#carcel-turno")).isVisible();
        assertThat(preso.locator("#btn-pagar-fianza")).isVisible();
        for (Page pagina : paginas()) {
            if (pagina != preso) {
                assertThat(pagina.locator("#carcel-turno")).isHidden();
            }
        }

        // Intenta avanzar sin cumplir la condición de salida: la ficha no se mueve.
        JsonNode tirada = tirar(preso, 1, 2, null);
        assertEquals(CASILLERO_COMISARIA, tirada.path("posicionAnterior").asInt(-1));
        assertEquals(CASILLERO_COMISARIA, tirada.path("nuevaPosicion").asInt(-1));

        verificarFichaEnTodasLasPantallas(id, CASILLERO_COMISARIA);
        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, id).locator(".estado-carcel")).hasText("Preso (1/3)");
        }
        EstadoJugador estado = consultarJugador(id);
        assertTrue(estado.encarcelado());
        assertEquals(1, estado.intentosCarcel());
        verificarHistorialEnTodasLasPantallas(nombre + " no sacó dobles y sigue preso (intento 1 de 3).");
    }

    private void cuandoElJugadorPresoPagaLaFianzaEntoncesSaleDeLaComisariaYVuelveAJugar(String nombre)
        throws IOException, SQLException {
        Page preso = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(preso);

        Response pago = preso.waitForResponse(
            respuesta -> respuesta.url().contains("/pagar-fianza") && "POST".equals(respuesta.request().method()),
            preso.locator("#btn-pagar-fianza")::click
        );
        assertTrue(pago.ok(), "El preso con dinero suficiente puede pagar la fianza");

        int dineroEsperado = DINERO_INICIAL - COSTO_FIANZA;
        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, id).locator(".estado-carcel")).isHidden();
            assertThat(tarjeta(pagina, id)).hasAttribute("data-encarcelado", "false");
        }
        verificarDineroEnTodasLasPantallas(id, dineroEsperado);
        EstadoJugador estado = consultarJugador(id);
        assertFalse(estado.encarcelado());
        assertEquals(dineroEsperado, estado.dinero());
        verificarHistorialEnTodasLasPantallas(nombre + " pagó $100 de fianza y salió de la comisaría.");

        // Ya libre, vuelve a tirar y avanza normalmente.
        moverYPasar(nombre, CASILLERO_COMISARIA, 12, 1, 3);
    }

    private void cuandoUnJugadorSinDineroNiPropiedadesNoPuedePagarUnaMultaEntoncesQuedaEnQuiebra(
        String nombre, String siguiente
    ) throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        // Preparación: saldo menor que la multa y sin propiedades para vender.
        actualizarDinero(id, DINERO_ANTES_DE_QUIEBRA);

        JsonNode tirada = tirar(jugador, 3, 4, EVENTO_MULTA);
        assertEquals(CASILLERO_EVENTO_QUIEBRA, tirada.path("posicionCaidaEvento").asInt(-1));
        assertEquals(EVENTO_MULTA, tirada.path("nombreEvento").asText());

        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, id)).hasClass(Pattern.compile(".*\\bjugador--bancarrota\\b.*"));
            assertThat(tarjeta(pagina, id).locator(".estado-bancarrota")).hasText("En quiebra");
        }
        assertTrue(consultarJugador(id).enBancarrota());
        verificarHistorialEnTodasLasPantallas(nombre + " se declaró en quiebra.");

        // Restricciones: el turno pasa al siguiente y el jugador en quiebra ya no puede tirar.
        verificarPermisoDeTirada(siguiente);
        assertThat(jugador.locator("#btn-tirar")).isDisabled();
    }

    private void cuandoSaleElPiqueteEntoncesSoloElJugadorConMenorSumaVuelveAInicio(
        String perdedor, String otro, String enQuiebra
    ) throws SQLException {
        Page paginaPerdedor = paginaDe(perdedor);
        Page paginaOtro = paginaDe(otro);
        long idPerdedor = idDeSesion(paginaPerdedor);
        long idOtro = idDeSesion(paginaOtro);
        int posicionOtro = consultarJugador(idOtro).posicion();

        for (Page pagina : List.of(paginaPerdedor, paginaOtro)) {
            assertThat(pagina.locator("#aviso-global")).isVisible();
        }
        verificarHistorialEnTodasLasPantallas("Evento global «" + EVENTO_GLOBAL + "» (ronda 5).");

        // El jugador en quiebra no participa del evento.
        assertThat(paginaDe(enQuiebra).locator("#global-tirar")).isHidden();

        tirarEnEventoGlobal(paginaPerdedor, 1, 2);
        tirarEnEventoGlobal(paginaOtro, 6, 6);

        for (Page pagina : List.of(paginaPerdedor, paginaOtro)) {
            assertThat(pagina.locator("#global-consecuencia")).containsText("volvió a Inicio");
        }
        verificarHistorialEnTodasLasPantallas("Resultado de «" + EVENTO_GLOBAL + "»");
        assertEquals(CASILLERO_INICIO, consultarJugador(idPerdedor).posicion());
        assertEquals(posicionOtro, consultarJugador(idOtro).posicion(), "El evento no debe mover al ganador");

        paginaPerdedor.locator("#global-continuar").click();
        paginaOtro.locator("#global-continuar").click();
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#aviso-global")).isHidden();
        }
        verificarFichaEnTodasLasPantallas(idPerdedor, CASILLERO_INICIO);
        verificarFichaEnTodasLasPantallas(idOtro, posicionOtro);
        verificarHistorialEnTodasLasPantallas("Finalizó el evento global «" + EVENTO_GLOBAL + "».");
    }

    private void entoncesLaGestionDeTurnosSalteaAlJugadorEnQuiebra(String primero, String segundo)
        throws IOException {
        // La ronda 5 sigue con J0 y después pasa directo a J2: J1 está en quiebra.
        verificarPermisoDeTirada(primero);
        moverYPasar(primero, CASILLERO_INICIO, 4, 1, 3);
        verificarPermisoDeTirada(segundo);
    }

    private void entoncesElHistorialRegistraLosEventosYLasPenalizaciones(String j0, String j1, String j2) {
        List<String> esperados = List.of(
            j0 + " sacó el evento «" + EVENTO_BENEFICIO + "»",
            j1 + " sacó el evento «" + EVENTO_PIERDE_TURNO + "»",
            j2 + " quedó detenido en la comisaría.",
            j2 + " no sacó dobles y sigue preso (intento 1 de 3).",
            j2 + " pagó $100 de fianza y salió de la comisaría.",
            j1 + " sacó el evento «" + EVENTO_MULTA + "»",
            j1 + " se declaró en quiebra.",
            "Resultado de «" + EVENTO_GLOBAL + "»"
        );
        for (String texto : esperados) {
            verificarHistorialEnTodasLasPantallas(texto);
        }
    }

    // ----------------------------------------------------------------------------------------------
    // Acciones
    // ----------------------------------------------------------------------------------------------

    /** Tira con dados fijos (y opcionalmente un evento fijo) desde la pantalla del jugador en turno. */
    private JsonNode tirar(Page jugador, int dadoUno, int dadoDos, String evento) throws IOException {
        Map<String, String> cabeceras = new HashMap<>(cabecerasFijas.getOrDefault(jugador, Map.of()));
        cabeceras.put(ConfiguracionDadosE2E.CABECERA_TIRADA, dadoUno + "," + dadoDos);
        if (evento != null) {
            cabeceras.put(ConfiguracionEventosE2E.CABECERA_EVENTO_CASILLERO, codificar(evento));
        }
        jugador.setExtraHTTPHeaders(cabeceras);
        try {
            Response respuesta = jugador.waitForResponse(
                r -> r.url().contains("/partida/tirar-dados?") && "POST".equals(r.request().method()),
                jugador.locator("#btn-tirar")::click
            );
            assertTrue(respuesta.ok(), "La tirada del jugador en turno debe ser aceptada");
            JsonNode tirada = JSON.readTree(respuesta.text());
            assertEquals(dadoUno, tirada.path("dado1").asInt());
            assertEquals(dadoDos, tirada.path("dado2").asInt());
            return tirada;
        } finally {
            restaurarCabeceras(jugador);
        }
    }

    /** Mueve al jugador a una propiedad libre y rechaza la compra para cerrar el turno. */
    private void moverYPasar(String nombre, int origen, int destino, int dadoUno, int dadoDos) throws IOException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        verificarFichaEnTodasLasPantallas(id, origen);
        JsonNode tirada = tirar(jugador, dadoUno, dadoDos, null);
        assertEquals(destino, tirada.path("nuevaPosicion").asInt(-1));
        jugador.locator("#btn-modal-pasar").click();
        verificarFichaEnTodasLasPantallas(id, destino);
    }

    private void tirarEnEventoGlobal(Page jugador, int dadoUno, int dadoDos) {
        Map<String, String> cabeceras = new HashMap<>(cabecerasFijas.getOrDefault(jugador, Map.of()));
        cabeceras.put(ConfiguracionDadosE2E.CABECERA_TIRADA, dadoUno + "," + dadoDos);
        jugador.setExtraHTTPHeaders(cabeceras);
        try {
            Response respuesta = jugador.waitForResponse(
                r -> r.url().contains("/evento-global/tirar") && "POST".equals(r.request().method()),
                jugador.locator("#global-tirar")::click
            );
            assertTrue(respuesta.ok(), "El jugador activo puede tirar en el evento global");
        } finally {
            restaurarCabeceras(jugador);
        }
    }

    private void fijarCabeceraEnTodasLasSesiones(String cabecera, String valor) {
        for (Page pagina : paginas()) {
            cabecerasFijas.computeIfAbsent(pagina, p -> new HashMap<>()).put(cabecera, codificar(valor));
            restaurarCabeceras(pagina);
        }
    }

    private void restaurarCabeceras(Page pagina) {
        pagina.setExtraHTTPHeaders(cabecerasFijas.getOrDefault(pagina, Map.of()));
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    // ----------------------------------------------------------------------------------------------
    // Verificaciones en pantalla
    // ----------------------------------------------------------------------------------------------

    private void verificarFichaEnTodasLasPantallas(long idJugador, int posicion) {
        String ficha = "[data-casillero='" + posicion + "'] .ficha[data-jugador-id='" + idJugador + "']";
        for (Page pagina : paginas()) {
            assertThat(pagina.locator(ficha)).hasCount(1);
        }
    }

    private void verificarDineroEnTodasLasPantallas(long idJugador, int dinero) {
        // El tablero formatea con es-AR: según el navegador se ve $2.500 o $2500.
        String miles = String.valueOf(dinero / 1000);
        String resto = String.format("%03d", dinero % 1000);
        Pattern texto = Pattern.compile("^\\$" + miles + "\\.?" + resto + "$");
        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, idJugador).locator(".dinero")).hasText(texto);
        }
    }

    private void verificarHistorialEnTodasLasPantallas(String texto) {
        for (Page pagina : paginas()) {
            assertThat(pagina.locator(".historial__lista")).containsText(texto);
        }
    }

    private Locator tarjeta(Page pagina, long idJugador) {
        return pagina.locator(".jugadores .jugador[id='" + idJugador + "']");
    }

    private Page paginaDe(String nombre) {
        return paginas().get(nombres().indexOf(nombre));
    }

    private long idDeSesion(Page pagina) {
        return ((Number) pagina.evaluate("() => Number(window.usuarioSesionId)")).longValue();
    }

    // ----------------------------------------------------------------------------------------------
    // Base de datos de pruebas
    // ----------------------------------------------------------------------------------------------

    private record EstadoJugador(
        int dinero, int posicion, boolean pierdeTurno, boolean encarcelado, int intentosCarcel, boolean enBancarrota
    ) {}

    private EstadoJugador consultarJugador(long idJugador) throws SQLException {
        try (Connection conexion = ReiniciarDB.abrirConexionE2E();
             PreparedStatement consulta = conexion.prepareStatement("""
                 SELECT dinero, posicionActual, pierdeTurno, turnosPenalizadosPendientes,
                        encarcelado, intentosSalirCarcel, enBancarrota
                 FROM Jugador WHERE id = ?
                 """)) {
            consulta.setLong(1, idJugador);
            try (ResultSet fila = consulta.executeQuery()) {
                assertTrue(fila.next(), "El jugador debe existir en la base de pruebas");
                return new EstadoJugador(
                    fila.getInt("dinero"),
                    fila.getInt("posicionActual"),
                    fila.getBoolean("pierdeTurno") || fila.getInt("turnosPenalizadosPendientes") > 0,
                    fila.getBoolean("encarcelado"),
                    fila.getInt("intentosSalirCarcel"),
                    fila.getBoolean("enBancarrota")
                );
            }
        }
    }

    private void actualizarDinero(long idJugador, int dinero) throws SQLException {
        try (Connection conexion = ReiniciarDB.abrirConexionE2E();
             PreparedStatement actualizacion = conexion.prepareStatement(
                 "UPDATE Jugador SET dinero = ? WHERE id = ?")) {
            actualizacion.setInt(1, dinero);
            actualizacion.setLong(2, idJugador);
            assertEquals(1, actualizacion.executeUpdate());
        }
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
                assertEquals(3, orden.size());
                return orden;
            }
        }
    }

    // ----------------------------------------------------------------------------------------------
    // Preparación de la partida
    // ----------------------------------------------------------------------------------------------

    private String iniciarPartidaConTresJugadores() {
        for (Page pagina : paginas()) {
            pagina.onPageError(error -> erroresDePagina.add(error));
            pagina.onWebSocket(socket -> socket.onFrameSent(frame -> {
                String trama = frame.text();
                if (trama != null && trama.contains("SUBSCRIBE") &&
                    trama.contains("destination:/topic/partida/") && trama.contains("/turno")) {
                    sesionesSuscritasAlTurno.add(pagina);
                }
            }));
        }
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
        paginaAnfitrion.waitForCondition(() -> sesionesSuscritasAlTurno.containsAll(paginas()));
        return codigoSala;
    }

    private void elegirFicha(Page pagina, String nombre, String color) {
        VistaSalaEspera sala = new VistaSalaEspera(pagina);
        sala.esperarConexionWebSocket();
        sala.elegirFicha(color);
        sala.verificarFichaDeJugador(nombre, color);
    }
}
