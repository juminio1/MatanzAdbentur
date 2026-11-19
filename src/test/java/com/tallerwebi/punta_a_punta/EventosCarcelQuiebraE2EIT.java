package com.tallerwebi.punta_a_punta;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.tallerwebi.dominio.ConfiguracionDadosE2E;
import com.tallerwebi.dominio.ConfiguracionEventosE2E;
import com.tallerwebi.dominio.ConfiguracionTriviaE2E;
import com.tallerwebi.dominio.entidades.EstadoCarcel;
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
 *   <li>Ronda 2: J0 cae en el 14 y «Cine en el Shopping» lo lleva al casillero 7, donde puede comprar;
 *       J1 pierde su oportunidad; J2, preso, tira sin dobles y no se mueve (intento 1 de 3).</li>
 *   <li>Ronda 3: J0 juega; J1 cae en la comisaría; J2 vuelve a fallar (intento 2 de 3).</li>
 *   <li>Ronda 4: J0 juega; J1 paga la fianza, sale, saca «Anda con cuidado» sin dinero suficiente y
 *       queda en quiebra; J2 falla el tercer intento: le cobran la fianza y queda libre.</li>
 *   <li>Ronda 5: sale el evento global PIQUETE y J0, con la menor suma, vuelve a Inicio. Después J0 cae
 *       en la comisaría y J2 juega normalmente.</li>
 *   <li>Ronda 6: J0 sale de la comisaría sacando dobles y el turno vuelve a saltear a J1.</li>
 * </ol>
 */
public class EventosCarcelQuiebraE2EIT extends EscenarioPartidaE2E {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final double ESPERA_ASERCIONES_MS = 20_000;
    private static final double ESPERA_ASERCIONES_ORIGINAL_MS = 5_000;

    private static final int CASILLERO_INICIO = 0;
    private static final int CASILLERO_EVENTO = 3;
    private static final int CASILLERO_SHOPPING = 7;
    private static final int CASILLERO_COMISARIA = 8;
    private static final int CASILLERO_EVENTO_14 = 14;
    private static final int DINERO_INICIAL = 2000;
    private static final int COSTO_FIANZA = EstadoCarcel.COSTO_FIANZA;
    // Alcanza para la fianza ($300) pero lo que queda ($100) no alcanza para la multa ($300).
    private static final int DINERO_ANTES_DE_QUIEBRA = 400;

    private static final String EVENTO_BENEFICIO = "Changa de finde";
    private static final int MONTO_BENEFICIO = 300;
    private static final String EVENTO_PIERDE_TURNO = "Choripán traicionero";
    private static final String EVENTO_IR_A_CASILLERO = "Cine en el Shopping";
    private static final String EVENTO_MULTA = "Anda con cuidado";
    private static final String EVENTO_GLOBAL = "PIQUETE";
    private static final String EVENTO_GLOBAL_SIN_DADOS = "ROBO";
    private static final int MONTO_ROBO = 300;
    private static final String EVENTO_MULTA_LLAVES = "Copia de llaves";
    private static final int MONTO_MULTA = 300;
    private static final int CASILLERO_PLAZA_SAN_JUSTO = 4;
    private static final int CASILLERO_UNLAM = 5;
    private static final String NOMBRE_SHOPPING = "San Justo Shopping";
    private static final int PRECIO_SAN_JUSTO = 200;
    // No alcanza para la multa ($300), pero vendiendo el Shopping al banco ($100) sí.
    private static final int DINERO_ANTES_DE_VENDER = 250;
    private static final int DINERO_ANTES_DE_QUEBRAR = 50;
    private static final int CASILLERO_TRIVIA = 10;
    private static final String DIFICULTAD_COMPLICADA = "DIFICIL";
    private static final int PREMIO_COMPLICADA = 500;
    // Texto de la tarjeta de un jugador en quiebra.
    private static final String ETIQUETA_QUIEBRA = "SE FUNDIÓ";

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

        // Preparación: el saldo alcanza para la fianza pero no para la multa, y no tiene propiedades.
        actualizarDinero(idDeSesion(paginaDe(j1)), DINERO_ANTES_DE_QUIEBRA);

        // --- Ronda 1 ---------------------------------------------------------------------------
        cuandoUnJugadorCaeEnUnEventoIndividualEntoncesVeLaDescripcionYCobraElBeneficio(j0);
        cuandoUnJugadorSacaUnEventoDePenalizacionEntoncesQuedaConElTurnoPerdido(j1);
        cuandoUnJugadorCaeEnLaComisariaEntoncesQuedaEncarceladoEnTodasLasPantallas(
            j2,
            CASILLERO_INICIO,
            3,
            5
        );

        // --- Ronda 2 ---------------------------------------------------------------------------
        cuandoUnEventoLlevaAlJugadorAUnCasilleroEntoncesSeMueveYPuedeComprarlo(j0);
        entoncesElJugadorPenalizadoPierdeSuOportunidadYElTurnoPasaAlSiguiente(j1, j2);
        cuandoElJugadorPresoTiraSinDoblesEntoncesNoPuedeAvanzar(j2, 1);

        // --- Ronda 3 ---------------------------------------------------------------------------
        moverYPasar(j0, CASILLERO_SHOPPING, 11, 1, 3);
        cuandoUnJugadorCaeEnLaComisariaEntoncesQuedaEncarceladoEnTodasLasPantallas(
            j1,
            CASILLERO_EVENTO,
            2,
            3
        );
        cuandoElJugadorPresoTiraSinDoblesEntoncesNoPuedeAvanzar(j2, 2);

        // --- Ronda 4 ---------------------------------------------------------------------------
        moverYPasar(j0, 11, 16, 2, 3);
        cuandoElJugadorPresoPagaLaFianzaEntoncesSaleDeLaComisaria(j1, DINERO_ANTES_DE_QUIEBRA);
        cuandoUnJugadorSinDineroNiPropiedadesNoPuedePagarUnaMultaEntoncesQuedaEnQuiebra(j1, j2);
        // El último turno de la ronda 4 abre la ronda 5, que tiene evento global.
        fijarCabeceraEnTodasLasSesiones(
            ConfiguracionEventosE2E.CABECERA_EVENTO_GLOBAL,
            EVENTO_GLOBAL
        );
        cuandoElJugadorPresoFallaElTercerIntentoEntoncesLeCobranLaFianzaYQuedaLibre(j2);

        // --- Ronda 5 ---------------------------------------------------------------------------
        cuandoSaleElPiqueteEntoncesSoloElJugadorConMenorSumaVuelveAInicio(j0, j2, j1);
        cuandoUnJugadorCaeEnLaComisariaEntoncesQuedaEncarceladoEnTodasLasPantallas(
            j0,
            CASILLERO_INICIO,
            3,
            5
        );
        // Después de la fianza automática, J2 vuelve a jugar normalmente (y se saltea a J1, en quiebra).
        moverYPasar(j2, CASILLERO_COMISARIA, 12, 1, 3);

        // --- Ronda 6 ---------------------------------------------------------------------------
        cuandoElJugadorPresoSacaDoblesEntoncesSaleDeLaComisariaYAvanza(j0);
        entoncesLaGestionDeTurnosSalteaAlJugadorEnQuiebra(j2);

        entoncesElHistorialRegistraLosEventosYLasPenalizaciones(j0, j1, j2);
        assertEquals(
            List.of(),
            erroresDePagina,
            "Ninguna pantalla debe registrar errores de JavaScript"
        );
        pausarSiCorresponde("fin");
    }

    /**
     * Segunda partida: la ronda 5 arranca enseguida con un ROBO (evento global sin dados) y después dos
     * jugadores con propiedades no pueden pagar una multa: uno vende al banco y salda la deuda, el otro se
     * declara en quiebra desde la ventana de deuda.
     */
    @Test
    void dadoQueUnaPartidaEstaEnCursoCuandoHayUnRoboYJugadoresConPropiedadesNoPuedenPagarEntoncesGestionanSuDeuda()
        throws IOException, SQLException {
        String codigo = iniciarPartidaConTresJugadores();
        List<String> orden = obtenerOrdenDeTurnos(codigo);
        String j0 = orden.get(0);
        String j1 = orden.get(1);
        String j2 = orden.get(2);
        // Preparación: la partida arranca en la ronda 4, así la siguiente (5) trae el evento global.
        actualizarRonda(codigo, 4);
        fijarCabeceraEnTodasLasSesiones(ConfiguracionEventosE2E.CABECERA_EVENTO_GLOBAL, EVENTO_GLOBAL_SIN_DADOS);

        // --- Ronda 4: J0 y J1 compran una propiedad cada uno -----------------------------------
        moverYComprar(j0, CASILLERO_INICIO, CASILLERO_SHOPPING, 3, 4);
        moverYComprar(j1, CASILLERO_INICIO, CASILLERO_PLAZA_SAN_JUSTO, 1, 3);
        moverYPasar(j2, CASILLERO_INICIO, CASILLERO_UNLAM, 2, 3);

        // --- Ronda 5 ---------------------------------------------------------------------------
        cuandoHayUnRoboEntoncesSoloUnJugadorPierdeElDinero(j0, j1, j2);
        cuandoUnJugadorConPropiedadesNoPuedePagarEntoncesVendeAlBancoYSaldaLaDeuda(j0, j1);
        cuandoUnJugadorConPropiedadesNoPuedePagarEntoncesSeDeclaraEnQuiebra(j1, j2, codigo);
        moverYPasar(j2, CASILLERO_UNLAM, 9, 1, 3);
        // El turno vuelve a J0 y saltea a J1, que está en quiebra.
        verificarPermisoDeTirada(j0);

        verificarHistorialEnTodasLasPantallas("Resultado de «" + EVENTO_GLOBAL_SIN_DADOS + "»");
        verificarHistorialEnTodasLasPantallas(j0 + " sacó el evento «" + EVENTO_MULTA + "»");
        verificarHistorialEnTodasLasPantallas(j1 + " sacó el evento «" + EVENTO_MULTA_LLAVES + "»");
        verificarHistorialEnTodasLasPantallas(j1 + " se declaró en quiebra.");
        assertEquals(List.of(), erroresDePagina, "Ninguna pantalla debe registrar errores de JavaScript");
        pausarSiCorresponde("fin");
    }

    /**
     * Tercera partida: J1 compra el Shopping y en la ronda 2 J0 cae en el casillero de trivia (10). La ruleta da
     * Complicada, J0 es el único que acierta y como premio le roba el Shopping a J1.
     */
    @Test
    void dadoQueUnaPartidaEstaEnCursoCuandoUnJugadorGanaUnaTriviaComplicadaEntoncesLeRobaUnaPropiedadAOtro()
        throws IOException, SQLException {
        String codigo = iniciarPartidaConTresJugadores();
        List<String> orden = obtenerOrdenDeTurnos(codigo);
        String j0 = orden.get(0);
        String j1 = orden.get(1);
        String j2 = orden.get(2);

        // --- Ronda 1: J1 compra el Shopping, que después le van a robar -------------------------
        moverYPasar(j0, CASILLERO_INICIO, CASILLERO_UNLAM, 2, 3);
        moverYComprar(j1, CASILLERO_INICIO, CASILLERO_SHOPPING, 3, 4);
        moverYPasar(j2, CASILLERO_INICIO, CASILLERO_PLAZA_SAN_JUSTO, 1, 3);

        // --- Ronda 2: J0 cae en la trivia ----------------------------------------------------------
        cuandoUnJugadorCaeEnLaTriviaEntoncesGiraLaRuletaYSaleComplicada(j0, codigo);
        String enunciado = entoncesTodosVenLaMismaPreguntaComplicada();
        cuandoSoloUnJugadorAciertaEntoncesGanaLaTrivia(j0, enunciado);
        cuandoElGanadorEligeRobarEntoncesSeQuedaConLaPropiedadDelOtro(j0, j1);

        // Con el premio entregado, la partida sigue con el turno de J1.
        verificarPermisoDeTirada(j1);
        assertEquals(List.of(), erroresDePagina, "Ninguna pantalla debe registrar errores de JavaScript");
        pausarSiCorresponde("fin");
    }

    private void cuandoUnJugadorCaeEnLaTriviaEntoncesGiraLaRuletaYSaleComplicada(String nombre, String codigo)
        throws IOException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        verificarFichaEnTodasLasPantallas(id, CASILLERO_UNLAM);

        JsonNode tirada = tirar(jugador, 2, 3, null);
        assertEquals(CASILLERO_TRIVIA, tirada.path("nuevaPosicion").asInt(-1));
        verificarFichaEnTodasLasPantallas(id, CASILLERO_TRIVIA);

        // Todos ven la ruleta, pero solo el que cayó puede girarla.
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#panel-ruleta")).isVisible();
            Locator girar = pagina.locator("#btn-girar-ruleta");
            if (pagina == jugador) {
                assertThat(girar).isEnabled();
            } else {
                assertThat(girar).isDisabled();
            }
        }

        Map<String, String> cabeceras = new HashMap<>(cabecerasFijas.getOrDefault(jugador, Map.of()));
        cabeceras.put(ConfiguracionTriviaE2E.CABECERA_DIFICULTAD, DIFICULTAD_COMPLICADA);
        jugador.setExtraHTTPHeaders(cabeceras);
        try {
            Response giro = jugador.waitForResponse(
                r -> r.url().contains("/ruleta/" + codigo + "/") && r.url().endsWith("/sortear") &&
                    "POST".equals(r.request().method()),
                jugador.locator("#btn-girar-ruleta")::click
            );
            assertTrue(giro.ok(), "El jugador que cayó en la trivia puede girar la ruleta");
            assertEquals(DIFICULTAD_COMPLICADA, JSON.readTree(giro.text()).path("dificultad").asText());
        } finally {
            restaurarCabeceras(jugador);
        }
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#ruleta-resultado")).containsText("DIFÍCIL");
        }
    }

    private String entoncesTodosVenLaMismaPreguntaComplicada() {
        String enunciado = null;
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#trivia")).isVisible();
            assertThat(pagina.locator("#trivia-ronda")).hasText("Ronda 1 de 5");
            assertThat(pagina.locator("#trivia-dificultad")).hasText("Complicada");
            assertThat(pagina.locator("#trivia-enunciado")).not().isEmpty();
            assertThat(opcionesDeTrivia(pagina)).hasCount(4);
            String texto = pagina.locator("#trivia-enunciado").innerText().trim();
            if (enunciado == null) {
                enunciado = texto;
            } else {
                assertEquals(enunciado, texto, "Todos los jugadores responden la misma pregunta");
            }
        }
        return enunciado;
    }

    private void cuandoSoloUnJugadorAciertaEntoncesGanaLaTrivia(String ganador, String enunciado)
        throws SQLException {
        // La respuesta correcta nunca llega al navegador: se busca la pregunta en el banco de la base.
        int correcta = respuestaCorrectaDelBanco(enunciado);
        int incorrecta = (correcta + 1) % 4;
        for (Page pagina : paginas()) {
            boolean esGanador = pagina == paginaDe(ganador);
            Response respuesta = pagina.waitForResponse(
                r -> r.url().contains("/trivia/respuesta") && "POST".equals(r.request().method()),
                opcionesDeTrivia(pagina).nth(esGanador ? correcta : incorrecta)::click
            );
            assertTrue(respuesta.ok(), "Cada jugador puede responder una vez");
        }

        for (Page pagina : paginas()) {
            String titulo = pagina == paginaDe(ganador) ? "¡Ganaste la trivia!" : "Ganó " + ganador;
            assertThat(pagina.locator("#trivia-final-titulo")).hasText(titulo);
            assertThat(pagina.locator("#trivia-final-detalle")).hasText("Fue el último en pie");
        }
    }

    private void cuandoElGanadorEligeRobarEntoncesSeQuedaConLaPropiedadDelOtro(String ganador, String robado)
        throws SQLException {
        Page paginaGanador = paginaDe(ganador);
        long idGanador = idDeSesion(paginaGanador);
        long idRobado = idDeSesion(paginaDe(robado));

        // Solo el ganador elige el premio: la plata o robar una propiedad.
        assertThat(paginaGanador.locator("#trivia-elegir-premio")).isVisible();
        assertThat(paginaGanador.locator("[data-premio='PLATA']")).hasText("Cobrar $" + PREMIO_COMPLICADA);
        for (Page pagina : paginas()) {
            if (pagina != paginaGanador) {
                assertThat(pagina.locator("#trivia-elegir-premio")).isHidden();
            }
        }

        paginaGanador.locator("[data-premio='ROBAR']").click();
        assertThat(paginaGanador.locator("#trivia-robar")).isVisible();
        assertThat(paginaGanador.locator("#trivia-robar-lista")).containsText("De " + robado);
        Locator shopping = paginaGanador.locator(".trivia__robar-opcion")
            .filter(new Locator.FilterOptions().setHasText(NOMBRE_SHOPPING));
        Response premio = paginaGanador.waitForResponse(
            r -> r.url().contains("/trivia/premio") && "POST".equals(r.request().method()),
            shopping::click
        );
        assertTrue(premio.ok(), "El ganador de una trivia complicada puede robar una propiedad");

        String textoPremio = "Premio: " + NOMBRE_SHOPPING + ", robada a " + robado;
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#trivia-premio")).hasText(textoPremio);
            pagina.locator("#trivia-cerrar").click();
            assertThat(pagina.locator("#trivia")).isHidden();
        }

        assertEquals(1, contarPropiedades(idGanador), "El ganador se queda con la propiedad robada");
        assertEquals(0, contarPropiedades(idRobado), "Al robado le sacan la propiedad");
        // Robar no cuesta nada ni devuelve plata: los saldos no cambian.
        assertEquals(DINERO_INICIAL, consultarJugador(idGanador).dinero());
        assertEquals(DINERO_INICIAL - PRECIO_SAN_JUSTO, consultarJugador(idRobado).dinero());
        verificarHistorialEnTodasLasPantallas(
            ganador + " ganó la trivia y recibió " + NOMBRE_SHOPPING + ", robada a " + robado + "."
        );
    }

    private Locator opcionesDeTrivia(Page pagina) {
        return pagina.locator("#trivia-opciones .trivia__opcion");
    }

    private void cuandoHayUnRoboEntoncesSoloUnJugadorPierdeElDinero(String j0, String j1, String j2)
        throws SQLException {
        Map<String, Integer> dineroAntes = new HashMap<>();
        dineroAntes.put(j0, DINERO_INICIAL - PRECIO_SAN_JUSTO);
        dineroAntes.put(j1, DINERO_INICIAL - PRECIO_SAN_JUSTO);
        dineroAntes.put(j2, DINERO_INICIAL);

        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#aviso-global")).isVisible();
            // Es un evento sin dados: nadie tira.
            assertThat(pagina.locator("#global-tirar")).isHidden();
        }
        verificarHistorialEnTodasLasPantallas("Evento global «" + EVENTO_GLOBAL_SIN_DADOS + "» (ronda 5).");
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#global-consecuencia")).containsText("perdió $" + MONTO_ROBO + " en el robo.");
        }

        // La víctima se sortea: se verifica que sea exactamente una y que solo a ella le falte el monto del robo.
        List<String> victimas = new ArrayList<>();
        for (String nombre : List.of(j0, j1, j2)) {
            long id = idDeSesion(paginaDe(nombre));
            int dinero = consultarJugador(id).dinero();
            if (dinero == dineroAntes.get(nombre) - MONTO_ROBO) {
                victimas.add(nombre);
                verificarDineroEnTodasLasPantallas(id, dinero);
            } else {
                assertEquals(dineroAntes.get(nombre), dinero, nombre + " no debe perder dinero en el robo");
            }
        }
        assertEquals(1, victimas.size(), "El robo afecta a un solo jugador");
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#global-consecuencia")).containsText(victimas.get(0));
        }

        for (Page pagina : paginas()) {
            pagina.locator("#global-continuar").click();
        }
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#aviso-global")).isHidden();
        }
    }

    private void cuandoUnJugadorConPropiedadesNoPuedePagarEntoncesVendeAlBancoYSaldaLaDeuda(
        String nombre,
        String siguiente
    ) throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        // Preparación: no alcanza para la multa, pero sí vendiendo la propiedad al banco.
        actualizarDinero(id, DINERO_ANTES_DE_VENDER);

        JsonNode tirada = tirar(jugador, 3, 4, EVENTO_MULTA);
        assertEquals(CASILLERO_EVENTO_14, tirada.path("posicionCaidaEvento").asInt(-1));

        // Solo el deudor ve la ventana de deuda.
        assertThat(jugador.locator("#modal-deuda-pendiente")).isVisible();
        assertThat(jugador.locator("#aviso-evento")).isHidden();
        assertThat(jugador.locator("#deuda-importe")).hasText("$" + MONTO_MULTA);
        for (Page pagina : paginas()) {
            if (pagina != jugador) {
                assertThat(pagina.locator("#modal-deuda-pendiente")).isHidden();
            }
        }

        jugador.locator("#btn-vender-desde-deuda").click();
        Locator propiedad = jugador.locator("#lista-propiedades-propias .propiedad-propia")
            .filter(new Locator.FilterOptions().setHasText(NOMBRE_SHOPPING));
        propiedad.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Vender").setExact(true)).click();
        jugador.locator("[data-destino-venta='banco']").click();
        Response venta = jugador.waitForResponse(
            r -> r.url().contains("/propiedades/" + CASILLERO_SHOPPING + "/vender"),
            jugador.locator("#btn-vender-al-banco")::click
        );
        assertTrue(venta.ok(), "La venta al banco debe ser aceptada");

        int dineroEsperado = DINERO_ANTES_DE_VENDER + PRECIO_SAN_JUSTO / 2 - MONTO_MULTA;
        verificarDineroEnTodasLasPantallas(id, dineroEsperado);
        assertEquals(dineroEsperado, consultarJugador(id).dinero());
        assertEquals(0, contarPropiedades(id), "La propiedad vendida vuelve al banco");
        assertFalse(consultarJugador(id).enBancarrota());
        assertThat(jugador.locator("#btn-deuda-pendiente")).isHidden();
        cerrarVentanaSiEstaAbierta(jugador, "#modal-bono-propiedades", "#btn-cerrar-bono-propiedades");

        // Con la deuda saldada, el turno sigue normalmente.
        verificarPermisoDeTirada(siguiente);
    }

    private void cuandoUnJugadorConPropiedadesNoPuedePagarEntoncesSeDeclaraEnQuiebra(
        String nombre,
        String siguiente,
        String codigo
    ) throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        actualizarDinero(id, DINERO_ANTES_DE_QUEBRAR);

        JsonNode tirada = tirar(jugador, 4, 6, EVENTO_MULTA_LLAVES);
        assertEquals(CASILLERO_EVENTO_14, tirada.path("posicionCaidaEvento").asInt(-1));

        // Tiene una propiedad para vender, así que la quiebra no es automática: la declara él.
        assertThat(jugador.locator("#modal-deuda-pendiente")).isVisible();
        assertThat(jugador.locator("#aviso-evento")).isHidden();
        assertFalse(consultarJugador(id).enBancarrota());
        jugador.locator("#btn-declarar-quiebra").click();
        assertThat(jugador.locator("#deuda-confirmacion")).isVisible();
        Response declaracion = jugador.waitForResponse(
            r -> r.url().contains("/declarar-quiebra") && "POST".equals(r.request().method()),
            jugador.locator("#btn-confirmar-quiebra")::click
        );
        assertTrue(declaracion.ok(), "La declaración de quiebra debe ser aceptada");

        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, id).locator(".estado-bancarrota")).hasText(ETIQUETA_QUIEBRA);
        }
        assertTrue(consultarJugador(id).enBancarrota());
        assertEquals(0, contarPropiedades(id), "Las propiedades del jugador en quiebra se liberan");
        // Se liberan al banco: la Plaza San Justo queda sin dueño, no pasa a otro jugador.
        assertNull(
            propietarioDeLaPropiedad(codigo, CASILLERO_PLAZA_SAN_JUSTO),
            "La propiedad del jugador en quiebra vuelve al banco"
        );
        assertThat(jugador.locator("#modal-deuda-pendiente")).isHidden();
        verificarPermisoDeTirada(siguiente);
        assertThat(jugador.locator("#btn-tirar")).isDisabled();
    }

    /** Mueve al jugador a una propiedad libre y la compra. */
    private void moverYComprar(String nombre, int origen, int destino, int dadoUno, int dadoDos)
        throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        verificarFichaEnTodasLasPantallas(id, origen);
        JsonNode tirada = tirar(jugador, dadoUno, dadoDos, null);
        assertEquals(destino, tirada.path("nuevaPosicion").asInt(-1));
        jugador.locator("#btn-modal-comprar").click();
        verificarFichaEnTodasLasPantallas(id, destino);
        verificarDineroEnTodasLasPantallas(id, DINERO_INICIAL - PRECIO_SAN_JUSTO);
        assertEquals(1, contarPropiedades(id));
    }

    private void cerrarVentanaSiEstaAbierta(Page pagina, String ventana, String botonCerrar) {
        if (pagina.locator(ventana).isVisible()) {
            pagina.locator(botonCerrar).click();
        }
        assertThat(pagina.locator(ventana)).isHidden();
    }

    // ----------------------------------------------------------------------------------------------
    // Pasos del checklist
    // ----------------------------------------------------------------------------------------------

    private void cuandoUnJugadorCaeEnUnEventoIndividualEntoncesVeLaDescripcionYCobraElBeneficio(
        String nombre
    ) throws IOException, SQLException {
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
        for (Page pagina : paginas()) {
            if (pagina != jugador) {
                assertThat(pagina.locator("#aviso-evento")).isHidden();
            }
        }
        jugador.locator("#btn-continuar-evento").click();

        verificarFichaEnTodasLasPantallas(id, CASILLERO_EVENTO);
        verificarDineroEnTodasLasPantallas(id, DINERO_INICIAL + MONTO_BENEFICIO);
        assertEquals(DINERO_INICIAL + MONTO_BENEFICIO, consultarJugador(id).dinero());
        verificarHistorialEnTodasLasPantallas(
            nombre + " sacó el evento «" + EVENTO_BENEFICIO + "»"
        );
    }

    private void cuandoUnJugadorSacaUnEventoDePenalizacionEntoncesQuedaConElTurnoPerdido(
        String nombre
    ) throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);

        JsonNode tirada = tirar(jugador, 1, 2, EVENTO_PIERDE_TURNO);
        assertEquals(EVENTO_PIERDE_TURNO, tirada.path("nombreEvento").asText());
        assertThat(jugador.locator("#aviso-evento-nombre")).hasText(EVENTO_PIERDE_TURNO);
        jugador.locator("#btn-continuar-evento").click();

        verificarFichaEnTodasLasPantallas(id, CASILLERO_EVENTO);
        assertTrue(
            consultarJugador(id).pierdeTurno(),
            "El evento debe dejar una penalización pendiente"
        );
        verificarHistorialEnTodasLasPantallas(
            nombre + " sacó el evento «" + EVENTO_PIERDE_TURNO + "»"
        );
    }

    private void cuandoUnEventoLlevaAlJugadorAUnCasilleroEntoncesSeMueveYPuedeComprarlo(
        String nombre
    ) throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        verificarFichaEnTodasLasPantallas(id, CASILLERO_EVENTO);

        JsonNode tirada = tirar(jugador, 5, 6, EVENTO_IR_A_CASILLERO);
        assertEquals(CASILLERO_EVENTO_14, tirada.path("posicionCaidaEvento").asInt(-1));
        assertEquals(CASILLERO_SHOPPING, tirada.path("nuevaPosicion").asInt(-1));
        assertEquals(EVENTO_IR_A_CASILLERO, tirada.path("nombreEvento").asText());

        assertThat(jugador.locator("#aviso-evento-nombre")).hasText(EVENTO_IR_A_CASILLERO);
        jugador.locator("#btn-continuar-evento").click();

        // La ficha sigue sola hasta el destino del evento y se ofrece la compra de esa propiedad.
        verificarFichaEnTodasLasPantallas(id, CASILLERO_SHOPPING);
        assertThat(jugador.locator("#btn-modal-comprar")).isVisible();
        assertThat(jugador.locator("#btn-modal-comprar")).isEnabled();
        // Se pasa para que el resto del recorrido (saldos y rondas siguientes) no cambie.
        jugador.locator("#btn-modal-pasar").click();
        assertEquals(CASILLERO_SHOPPING, consultarJugador(id).posicion());
        verificarHistorialEnTodasLasPantallas(
            nombre + " sacó el evento «" + EVENTO_IR_A_CASILLERO + "»"
        );
    }

    private void cuandoUnJugadorCaeEnLaComisariaEntoncesQuedaEncarceladoEnTodasLasPantallas(
        String nombre,
        int origen,
        int dadoUno,
        int dadoDos
    ) throws IOException, SQLException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        verificarFichaEnTodasLasPantallas(id, origen);

        JsonNode tirada = tirar(jugador, dadoUno, dadoDos, null);
        assertEquals(CASILLERO_COMISARIA, tirada.path("nuevaPosicion").asInt(-1));

        verificarFichaEnTodasLasPantallas(id, CASILLERO_COMISARIA);
        verificarPresoEnTodasLasPantallas(id, "Preso");
        EstadoJugador estado = consultarJugador(id);
        assertTrue(estado.encarcelado());
        assertEquals(0, estado.intentosCarcel());
        verificarHistorialEnTodasLasPantallas(nombre + " quedó detenido en la comisaría.");
    }

    private void entoncesElJugadorPenalizadoPierdeSuOportunidadYElTurnoPasaAlSiguiente(
        String penalizado,
        String siguiente
    ) throws SQLException {
        long idPenalizado = idDeSesion(paginaDe(penalizado));
        // Después de J0 el turno salta directamente a J2.
        verificarPermisoDeTirada(siguiente);
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#ronda-actual")).hasText("2");
        }
        assertFalse(
            consultarJugador(idPenalizado).pierdeTurno(),
            "La penalización se consume al saltear el turno"
        );
        verificarFichaEnTodasLasPantallas(idPenalizado, CASILLERO_EVENTO);
    }

    private void cuandoElJugadorPresoTiraSinDoblesEntoncesNoPuedeAvanzar(String nombre, int intento)
        throws IOException, SQLException {
        Page preso = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(preso);

        // Solo el preso ve el panel de la comisaría en su turno.
        assertThat(preso.locator("#carcel-turno")).isVisible();
        assertThat(preso.locator("#btn-pagar-fianza")).isVisible();
        for (Page pagina : paginas()) {
            if (pagina != preso) {
                assertThat(pagina.locator("#btn-pagar-fianza")).isHidden();
            }
        }

        // Intenta avanzar sin cumplir la condición de salida: la ficha no se mueve.
        JsonNode tirada = tirar(preso, 1, 2, null);
        assertEquals(CASILLERO_COMISARIA, tirada.path("posicionAnterior").asInt(-1));
        assertEquals(CASILLERO_COMISARIA, tirada.path("nuevaPosicion").asInt(-1));

        verificarFichaEnTodasLasPantallas(id, CASILLERO_COMISARIA);
        verificarPresoEnTodasLasPantallas(id, "Preso (" + intento + "/3)");
        EstadoJugador estado = consultarJugador(id);
        assertTrue(estado.encarcelado());
        assertEquals(intento, estado.intentosCarcel());
        verificarHistorialEnTodasLasPantallas(
            nombre + " no sacó dobles y sigue preso (intento " + intento + " de 3)."
        );
    }

    private void cuandoElJugadorPresoFallaElTercerIntentoEntoncesLeCobranLaFianzaYQuedaLibre(
        String nombre
    ) throws IOException, SQLException {
        Page preso = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(preso);
        int dineroAntes = consultarJugador(id).dinero();

        JsonNode tirada = tirar(preso, 1, 2, null);
        // Al último intento le cobran la fianza, pero se queda en el lugar hasta su próximo turno.
        assertEquals(CASILLERO_COMISARIA, tirada.path("nuevaPosicion").asInt(-1));
        verificarFichaEnTodasLasPantallas(id, CASILLERO_COMISARIA);
        verificarLibreEnTodasLasPantallas(id);
        verificarDineroEnTodasLasPantallas(id, dineroAntes - COSTO_FIANZA);
        EstadoJugador estado = consultarJugador(id);
        assertFalse(estado.encarcelado());
        assertEquals(dineroAntes - COSTO_FIANZA, estado.dinero());
        // El aviso puede llegar por la tirada o por la actualización de saldo: ambos textos son válidos.
        verificarHistorialEnTodasLasPantallas(
            Pattern.compile(
                // Sin Pattern.quote: Playwright pasa la expresión al navegador y JavaScript no entiende \\Q...\\E.
                nombre +
                    " (no sacó dobles en 3 intentos: le cobran \\$" + COSTO_FIANZA + " de fianza" +
                    "|pagó \\$" + COSTO_FIANZA + " de fianza y salió de la comisaría)"
            )
        );
    }

    private void cuandoElJugadorPresoPagaLaFianzaEntoncesSaleDeLaComisaria(
        String nombre,
        int dineroAntes
    ) throws SQLException {
        Page preso = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(preso);

        Response pago = preso.waitForResponse(
            respuesta ->
                respuesta.url().contains("/pagar-fianza") &&
                "POST".equals(respuesta.request().method()),
            preso.locator("#btn-pagar-fianza")::click
        );
        assertTrue(pago.ok(), "El preso con dinero suficiente puede pagar la fianza");

        int dineroEsperado = dineroAntes - COSTO_FIANZA;
        EstadoJugador estado = consultarJugador(id);
        assertFalse(estado.encarcelado());
        assertEquals(dineroEsperado, estado.dinero());
        verificarLibreEnTodasLasPantallas(id);
        verificarDineroEnTodasLasPantallas(id, dineroEsperado);
        verificarHistorialEnTodasLasPantallas(
            nombre + " pagó $" + COSTO_FIANZA + " de fianza y salió de la comisaría."
        );
        // Ya libre, puede tirar en este mismo turno.
        assertThat(preso.locator("#btn-tirar")).isEnabled();
    }

    private void cuandoElJugadorPresoSacaDoblesEntoncesSaleDeLaComisariaYAvanza(String nombre)
        throws IOException, SQLException {
        Page preso = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(preso);

        JsonNode tirada = tirar(preso, 2, 2, null);
        assertEquals(CASILLERO_COMISARIA, tirada.path("posicionAnterior").asInt(-1));
        assertEquals(12, tirada.path("nuevaPosicion").asInt(-1));

        preso.locator("#btn-modal-pasar").click();
        verificarFichaEnTodasLasPantallas(id, 12);
        verificarLibreEnTodasLasPantallas(id);
        assertFalse(consultarJugador(id).encarcelado());
        verificarHistorialEnTodasLasPantallas(nombre + " sacó dobles y salió de la comisaría.");
    }

    private void cuandoUnJugadorSinDineroNiPropiedadesNoPuedePagarUnaMultaEntoncesQuedaEnQuiebra(
        String nombre,
        String siguiente
    ) throws IOException, SQLException {
        Page jugador = paginaDe(nombre);
        long id = idDeSesion(jugador);

        JsonNode tirada = tirar(jugador, 2, 4, EVENTO_MULTA);
        assertEquals(CASILLERO_EVENTO_14, tirada.path("posicionCaidaEvento").asInt(-1));
        assertEquals(EVENTO_MULTA, tirada.path("nombreEvento").asText());

        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, id)).hasClass(
                Pattern.compile(".*\\bjugador--bancarrota\\b.*")
            );
            assertThat(tarjeta(pagina, id).locator(".estado-bancarrota")).hasText(ETIQUETA_QUIEBRA);
        }
        assertTrue(consultarJugador(id).enBancarrota());
        verificarHistorialEnTodasLasPantallas(nombre + " se declaró en quiebra.");

        // Restricciones: el turno pasa al siguiente y el jugador en quiebra ya no puede tirar.
        verificarPermisoDeTirada(siguiente);
        assertThat(jugador.locator("#btn-tirar")).isDisabled();
    }

    private void cuandoSaleElPiqueteEntoncesSoloElJugadorConMenorSumaVuelveAInicio(
        String perdedor,
        String otro,
        String enQuiebra
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
        assertEquals(
            posicionOtro,
            consultarJugador(idOtro).posicion(),
            "El evento no debe mover al ganador"
        );

        paginaPerdedor.locator("#global-continuar").click();
        paginaOtro.locator("#global-continuar").click();
        for (Page pagina : paginas()) {
            assertThat(pagina.locator("#aviso-global")).isHidden();
        }
        verificarFichaEnTodasLasPantallas(idPerdedor, CASILLERO_INICIO);
        verificarFichaEnTodasLasPantallas(idOtro, posicionOtro);
        verificarHistorialEnTodasLasPantallas("Finalizó el evento global «" + EVENTO_GLOBAL + "».");
    }

    private void entoncesLaGestionDeTurnosSalteaAlJugadorEnQuiebra(String siguiente) {
        // Después de J0 el turno pasa directo a J2: J1 está en quiebra.
        verificarPermisoDeTirada(siguiente);
    }

    private void entoncesElHistorialRegistraLosEventosYLasPenalizaciones(
        String j0,
        String j1,
        String j2
    ) {
        List<String> esperados = List.of(
            j0 + " sacó el evento «" + EVENTO_BENEFICIO + "»",
            j1 + " sacó el evento «" + EVENTO_PIERDE_TURNO + "»",
            j2 + " quedó detenido en la comisaría.",
            j0 + " sacó el evento «" + EVENTO_IR_A_CASILLERO + "»",
            j2 + " no sacó dobles y sigue preso (intento 1 de 3).",
            j1 + " quedó detenido en la comisaría.",
            j2 + " no sacó dobles y sigue preso (intento 2 de 3).",
            j1 + " pagó $" + COSTO_FIANZA + " de fianza y salió de la comisaría.",
            j1 + " sacó el evento «" + EVENTO_MULTA + "»",
            j1 + " se declaró en quiebra.",
            "Resultado de «" + EVENTO_GLOBAL + "»",
            j0 + " quedó detenido en la comisaría.",
            j0 + " sacó dobles y salió de la comisaría."
        );
        for (String texto : esperados) {
            verificarHistorialEnTodasLasPantallas(texto);
        }
    }

    // ----------------------------------------------------------------------------------------------
    // Acciones
    // ----------------------------------------------------------------------------------------------

    /** Tira con dados fijos (y opcionalmente un evento fijo) desde la pantalla del jugador en turno. */
    private JsonNode tirar(Page jugador, int dadoUno, int dadoDos, String evento)
        throws IOException {
        Map<String, String> cabeceras = new HashMap<>(
            cabecerasFijas.getOrDefault(jugador, Map.of())
        );
        cabeceras.put(ConfiguracionDadosE2E.CABECERA_TIRADA, dadoUno + "," + dadoDos);
        if (evento != null) {
            cabeceras.put(ConfiguracionEventosE2E.CABECERA_EVENTO_CASILLERO, codificar(evento));
        }
        jugador.setExtraHTTPHeaders(cabeceras);
        try {
            Response respuesta = jugador.waitForResponse(
                r ->
                    r.url().contains("/partida/tirar-dados?") &&
                    "POST".equals(r.request().method()),
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
    private void moverYPasar(String nombre, int origen, int destino, int dadoUno, int dadoDos)
        throws IOException {
        Page jugador = verificarPermisoDeTirada(nombre);
        long id = idDeSesion(jugador);
        verificarFichaEnTodasLasPantallas(id, origen);
        JsonNode tirada = tirar(jugador, dadoUno, dadoDos, null);
        assertEquals(destino, tirada.path("nuevaPosicion").asInt(-1));
        jugador.locator("#btn-modal-pasar").click();
        verificarFichaEnTodasLasPantallas(id, destino);
    }

    private void tirarEnEventoGlobal(Page jugador, int dadoUno, int dadoDos) {
        Map<String, String> cabeceras = new HashMap<>(
            cabecerasFijas.getOrDefault(jugador, Map.of())
        );
        cabeceras.put(ConfiguracionDadosE2E.CABECERA_TIRADA, dadoUno + "," + dadoDos);
        jugador.setExtraHTTPHeaders(cabeceras);
        try {
            Response respuesta = jugador.waitForResponse(
                r ->
                    r.url().contains("/evento-global/tirar") && "POST".equals(r.request().method()),
                jugador.locator("#global-tirar")::click
            );
            assertTrue(respuesta.ok(), "El jugador activo puede tirar en el evento global");
        } finally {
            restaurarCabeceras(jugador);
        }
    }

    private void fijarCabeceraEnTodasLasSesiones(String cabecera, String valor) {
        for (Page pagina : paginas()) {
            cabecerasFijas
                .computeIfAbsent(pagina, p -> new HashMap<>())
                .put(cabecera, codificar(valor));
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
        String ficha =
            "[data-casillero='" + posicion + "'] .ficha[data-jugador-id='" + idJugador + "']";
        for (Page pagina : paginas()) {
            assertThat(pagina.locator(ficha)).hasCount(1);
        }
    }

    private void verificarDineroEnTodasLasPantallas(long idJugador, int dinero) {
        // El tablero formatea con es-AR: según el navegador se ve $2.500 o $2500.
        String numero =
            dinero < 1000
                ? String.valueOf(dinero)
                : dinero / 1000 + "\\.?" + String.format("%03d", dinero % 1000);
        Pattern texto = Pattern.compile("^\\$" + numero + "$");
        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, idJugador).locator(".dinero")).hasText(texto);
        }
    }

    private void verificarHistorialEnTodasLasPantallas(String texto) {
        for (Page pagina : paginas()) {
            assertThat(pagina.locator(".historial__lista")).containsText(texto);
        }
    }

    private void verificarHistorialEnTodasLasPantallas(Pattern texto) {
        for (Page pagina : paginas()) {
            assertThat(pagina.locator(".historial__lista")).containsText(texto);
        }
    }

    private void verificarPresoEnTodasLasPantallas(long idJugador, String etiqueta) {
        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, idJugador)).hasAttribute("data-encarcelado", "true");
            assertThat(tarjeta(pagina, idJugador).locator(".estado-carcel")).hasText(etiqueta);
        }
    }

    private void verificarLibreEnTodasLasPantallas(long idJugador) {
        for (Page pagina : paginas()) {
            assertThat(tarjeta(pagina, idJugador)).hasAttribute("data-encarcelado", "false");
            assertThat(tarjeta(pagina, idJugador).locator(".estado-carcel")).isHidden();
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
        int dinero,
        int posicion,
        boolean pierdeTurno,
        boolean encarcelado,
        int intentosCarcel,
        boolean enBancarrota
    ) {}

    private EstadoJugador consultarJugador(long idJugador) throws SQLException {
        try (
            Connection conexion = ReiniciarDB.abrirConexionE2E();
            PreparedStatement consulta = conexion.prepareStatement(
                """
                SELECT dinero, posicionActual, pierdeTurno, turnosPenalizadosPendientes,
                       encarcelado, intentosSalirCarcel, enBancarrota
                FROM Jugador WHERE id = ?
                """
            )
        ) {
            consulta.setLong(1, idJugador);
            try (ResultSet fila = consulta.executeQuery()) {
                assertTrue(fila.next(), "El jugador debe existir en la base de pruebas");
                return new EstadoJugador(
                    fila.getInt("dinero"),
                    fila.getInt("posicionActual"),
                    fila.getBoolean("pierdeTurno") ||
                        fila.getInt("turnosPenalizadosPendientes") > 0,
                    fila.getBoolean("encarcelado"),
                    fila.getInt("intentosSalirCarcel"),
                    fila.getBoolean("enBancarrota")
                );
            }
        }
    }

    private int contarPropiedades(long idJugador) throws SQLException {
        try (
            Connection conexion = ReiniciarDB.abrirConexionE2E();
            PreparedStatement consulta = conexion.prepareStatement(
                "SELECT COUNT(*) FROM Propiedad WHERE propietario_id = ?"
            )
        ) {
            consulta.setLong(1, idJugador);
            try (ResultSet fila = consulta.executeQuery()) {
                fila.next();
                return fila.getInt(1);
            }
        }
    }

    /** Id del dueño de la propiedad en esa posición de la partida, o null si es del banco. */
    private Long propietarioDeLaPropiedad(String codigo, int posicion) throws SQLException {
        try (
            Connection conexion = ReiniciarDB.abrirConexionE2E();
            PreparedStatement consulta = conexion.prepareStatement(
                """
                SELECT propiedad.propietario_id
                FROM CasilleroPropiedad casilleroPropiedad
                JOIN Casillero casillero ON casillero.id = casilleroPropiedad.id
                JOIN Propiedad propiedad ON propiedad.id = casilleroPropiedad.propiedad_id
                JOIN Partida partida ON partida.id = casilleroPropiedad.partida_id
                WHERE partida.codigoUnico = ? AND casillero.posicion = ?
                """
            )
        ) {
            consulta.setString(1, codigo);
            consulta.setInt(2, posicion);
            try (ResultSet fila = consulta.executeQuery()) {
                assertTrue(
                    fila.next(),
                    "La propiedad del casillero " + posicion + " debe existir en la partida"
                );
                long propietario = fila.getLong(1);
                return fila.wasNull() ? null : propietario;
            }
        }
    }

    private int respuestaCorrectaDelBanco(String enunciado) throws SQLException {
        try (
            Connection conexion = ReiniciarDB.abrirConexionE2E();
            PreparedStatement consulta = conexion.prepareStatement(
                "SELECT respuesta_correcta FROM pregunta_trivia WHERE enunciado = ?"
            )
        ) {
            consulta.setString(1, enunciado);
            try (ResultSet fila = consulta.executeQuery()) {
                assertTrue(fila.next(), "En el E2E la pregunta tiene que salir del banco: " + enunciado);
                return fila.getInt(1);
            }
        }
    }

    private void actualizarRonda(String codigo, int ronda) throws SQLException {
        try (
            Connection conexion = ReiniciarDB.abrirConexionE2E();
            PreparedStatement actualizacion = conexion.prepareStatement(
                "UPDATE Partida SET rondaActual = ? WHERE codigoUnico = ?"
            )
        ) {
            actualizacion.setInt(1, ronda);
            actualizacion.setString(2, codigo);
            assertEquals(1, actualizacion.executeUpdate());
        }
    }

    private void actualizarDinero(long idJugador, int dinero) throws SQLException {
        try (
            Connection conexion = ReiniciarDB.abrirConexionE2E();
            PreparedStatement actualizacion = conexion.prepareStatement(
                "UPDATE Jugador SET dinero = ? WHERE id = ?"
            )
        ) {
            actualizacion.setInt(1, dinero);
            actualizacion.setLong(2, idJugador);
            assertEquals(1, actualizacion.executeUpdate());
        }
    }

    private List<String> obtenerOrdenDeTurnos(String codigo) throws SQLException {
        try (
            Connection conexion = ReiniciarDB.abrirConexionE2E();
            PreparedStatement consulta = conexion.prepareStatement(
                """
                SELECT jugador.apodo FROM Jugador jugador JOIN Partida partida ON partida.id = jugador.partida_id
                WHERE partida.codigoUnico = ?
                ORDER BY CASE WHEN jugador.orden < partida.indiceTurnoActual THEN 1 ELSE 0 END, jugador.orden
                """
            )
        ) {
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
            pagina.onWebSocket(socket ->
                socket.onFrameSent(frame -> {
                    String trama = frame.text();
                    if (
                        trama != null &&
                        trama.contains("SUBSCRIBE") &&
                        trama.contains("destination:/topic/partida/") &&
                        trama.contains("/turno")
                    ) {
                        sesionesSuscritasAlTurno.add(pagina);
                    }
                })
            );
        }
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaAnfitrion, ANFITRION, "juli@unlam.edu.ar");
        dadoQueUnUsuarioSeRegistraEIniciaSesion(
            paginaInvitadoUno,
            INVITADO_UNO,
            "emi@unlam.edu.ar"
        );
        dadoQueUnUsuarioSeRegistraEIniciaSesion(
            paginaInvitadoDos,
            INVITADO_DOS,
            "may@unlam.edu.ar"
        );

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
