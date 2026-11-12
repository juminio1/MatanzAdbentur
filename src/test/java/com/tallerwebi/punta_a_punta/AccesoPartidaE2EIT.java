package com.tallerwebi.punta_a_punta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.tallerwebi.punta_a_punta.vistas.VistaHome;
import com.tallerwebi.punta_a_punta.vistas.VistaLogin;
import com.tallerwebi.punta_a_punta.vistas.VistaNuevoUsuario;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class AccesoPartidaE2EIT {

    private static final String CLAVE = "Clave123!";
    private static final String ANFITRION = "anfitrionE2E";
    private static final String INVITADO_UNO = "invitadoUnoE2E";
    private static final String INVITADO_DOS = "invitadoDosE2E";
    private static final String PAUSA = System.getProperty("e2e.pause", "ninguna");
    private static final boolean DEBUG =
        Boolean.getBoolean("e2e.debug") ||
        "1".equals(System.getenv("PWDEBUG")) ||
        "console".equals(System.getenv("PWDEBUG"));
    private static final boolean DIAGNOSTICO = DEBUG || Boolean.getBoolean("e2e.diagnostico");

    static Playwright playwright;
    static Browser browser;

    BrowserContext contextoAnfitrion;
    BrowserContext contextoInvitadoUno;
    BrowserContext contextoInvitadoDos;

    Page paginaAnfitrion;
    Page paginaInvitadoUno;
    Page paginaInvitadoDos;
    final Set<Page> paginasConMensajesDeSala = new HashSet<>();
    final String marcaDocumentoSala = UUID.randomUUID().toString();

    @BeforeAll
    static void abrirNavegador() {
        if (!List.of("ninguna", "inicio", "sala", "fin").contains(PAUSA)) {
            throw new IllegalArgumentException("e2e.pause debe ser ninguna, inicio, sala o fin");
        }
        Boolean headless = !DEBUG && "ninguna".equals(PAUSA);
        Double slowMo = Double.parseDouble(System.getProperty("e2e.slowMo", DEBUG ? "250" : "0"));
        if (!Double.isFinite(slowMo) || slowMo < 0) {
            throw new IllegalArgumentException(
                "e2e.slowMo debe ser un numero finito mayor o igual a cero"
            );
        }
        diagnostico(
            "Inicializando Playwright; clase cargada desde " +
                AccesoPartidaE2EIT.class.getResource("AccesoPartidaE2EIT.class")
        );
        playwright = Playwright.create();

        browser = playwright
            .chromium()
            .launch(new BrowserType.LaunchOptions().setHeadless(headless).setSlowMo(slowMo));
        diagnostico(
            "Chromium " +
                browser.version() +
                " conectado=" +
                browser.isConnected() +
                ", headless=" +
                headless +
                ", slowMo=" +
                slowMo +
                ", pausa=" +
                PAUSA
        );
    }

    @AfterAll
    static void cerrarNavegador() {
        diagnostico("Cerrando Chromium y Playwright");
        try {
            if (browser != null) {
                browser.close();
            }
        } finally {
            if (playwright != null) {
                playwright.close();
            }
        }
    }

    @BeforeEach
    public void prepararEscenario(TestInfo testInfo) {
        diagnostico(
            "Preparando " +
                testInfo.getTestMethod().orElseThrow().getName() +
                "; limpieza protegida de DB_NAME=" +
                System.getProperty("DB_NAME")
        );
        ReiniciarDB.limpiarBaseDeDatos();

        contextoAnfitrion = browser.newContext();
        contextoInvitadoUno = browser.newContext();
        contextoInvitadoDos = browser.newContext();

        paginaAnfitrion = contextoAnfitrion.newPage();
        paginaInvitadoUno = contextoInvitadoUno.newPage();
        paginaInvitadoDos = contextoInvitadoDos.newPage();

        assertEquals(3, browser.contexts().size());
        for (Page pagina : paginas()) {
            assertEquals(List.of(pagina), pagina.context().pages());
            assertTrue(
                pagina.context().cookies().isEmpty(),
                "Cada contexto debe comenzar sin cookies"
            );
            assertFalse(pagina.isClosed());
            observarMensajesDeSala(pagina);
        }
        assertSame(contextoAnfitrion, paginaAnfitrion.context());
        assertSame(contextoInvitadoUno, paginaInvitadoUno.context());
        assertSame(contextoInvitadoDos, paginaInvitadoDos.context());
        diagnostico("Creados tres BrowserContext independientes, cada uno con una pagina abierta");
        pausarSiCorresponde("inicio");
    }

    @AfterEach
    void limpiarEscenario() {
        diagnostico("Cerrando los tres contextos");
        // Intentar cerrar todos los contextos incluso si uno ya se cerro durante la depuracion.
        try {
            if (contextoAnfitrion != null) {
                contextoAnfitrion.close();
            }
        } finally {
            try {
                if (contextoInvitadoUno != null) {
                    contextoInvitadoUno.close();
                }
            } finally {
                if (contextoInvitadoDos != null) {
                    contextoInvitadoDos.close();
                }
            }
        }
    }

    @Test
    void dadoQueTresUsuariosSeRegistranYSeUnenAUnaSalaCuandoInicianUnaPartidaEntoncesTodosIngresanAlTablero() {
        diagnostico("Etapa 1: registro e inicio de sesion");
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaAnfitrion, ANFITRION, "juli@unlam.edu.ar");
        assertTrue(
            contextoInvitadoUno.cookies().isEmpty(),
            "El login del anfitrion no debe autenticar al invitado"
        );
        assertTrue(
            contextoInvitadoDos.cookies().isEmpty(),
            "El login del anfitrion no debe autenticar al invitado"
        );

        dadoQueUnUsuarioSeRegistraEIniciaSesion(
            paginaInvitadoUno,
            INVITADO_UNO,
            "emi@unlam.edu.ar"
        );
        assertTrue(
            contextoInvitadoDos.cookies().isEmpty(),
            "Los logins previos no deben compartir cookies"
        );

        dadoQueUnUsuarioSeRegistraEIniciaSesion(
            paginaInvitadoDos,
            INVITADO_DOS,
            "may@unlam.edu.ar"
        );
        entoncesLasSesionesSonIndependientes();

        diagnostico("Etapa 2: creacion de sala");
        String codigoSala = cuandoElAnfitrionCreaUnaSala();
        esperarConexionWebSocket(paginaAnfitrion);
        new VistaSalaEspera(paginaAnfitrion).verificarJugadores(ANFITRION);

        diagnostico("Etapas 3 y 4: ingreso y sincronizacion de invitados sin recargar");
        cuandoElInvitadoSeUneALaSala(paginaInvitadoUno, codigoSala);

        esperarConexionWebSocket(paginaInvitadoUno);
        new VistaSalaEspera(paginaAnfitrion).verificarJugadores(ANFITRION, INVITADO_UNO);
        new VistaSalaEspera(paginaInvitadoUno).verificarJugadores(ANFITRION, INVITADO_UNO);

        cuandoElInvitadoSeUneALaSala(paginaInvitadoDos, codigoSala);

        esperarConexionWebSocket(paginaInvitadoDos);

        //Comprobar que los tres se visualizan
        for (Page pagina : paginas()) {
            entoncesAparecenLosTresJugadores(pagina);
            assertEquals(codigoSala, new VistaSalaEspera(pagina).obtenerCodigoSala());
        }
        new VistaSalaEspera(paginaAnfitrion).verificarPermisoParaIniciar(true);
        new VistaSalaEspera(paginaInvitadoUno).verificarPermisoParaIniciar(false);
        new VistaSalaEspera(paginaInvitadoDos).verificarPermisoParaIniciar(false);

        diagnostico("Etapa 5: seleccion y bloqueo de fichas");
        new VistaSalaEspera(paginaAnfitrion).elegirFicha("ROJO");

        for (Page pagina : paginas()) {
            entoncesSeVisualizaLaFicha(pagina, ANFITRION, "ROJO");
        }

        //La ficha ocupada debe quedar bloqueada
        new VistaSalaEspera(paginaInvitadoUno).verificarFichaOcupada("ROJO");
        new VistaSalaEspera(paginaInvitadoDos).verificarFichaOcupada("ROJO");

        // Los invitados seleccionan otras fichas
        new VistaSalaEspera(paginaInvitadoUno).elegirFicha("AZUL");

        new VistaSalaEspera(paginaInvitadoDos).elegirFicha("VERDE");

        //Comprueba la sincronización de fichas
        for (Page pagina : paginas()) {
            entoncesSeVisualizaLaFicha(pagina, INVITADO_UNO, "AZUL");

            entoncesSeVisualizaLaFicha(pagina, INVITADO_DOS, "VERDE");
            entoncesSeVisualizaLaFicha(pagina, ANFITRION, "ROJO");
            assertEquals(
                marcaDocumentoSala,
                pagina.evaluate("() => window.__e2eDocumentoSala"),
                "La sala debe sincronizarse sin reemplazar el documento ni recargar"
            );
            assertTrue(
                paginasConMensajesDeSala.contains(pagina),
                "Cada sesion debe recibir actualizaciones reales de sala por WebSocket"
            );
        }
        new VistaSalaEspera(paginaAnfitrion).verificarFichaOcupada("AZUL");
        new VistaSalaEspera(paginaAnfitrion).verificarFichaOcupada("VERDE");
        new VistaSalaEspera(paginaInvitadoUno).verificarFichaOcupada("VERDE");
        new VistaSalaEspera(paginaInvitadoDos).verificarFichaOcupada("AZUL");
        pausarSiCorresponde("sala");

        diagnostico("Etapa 6: inicio de la misma partida en las tres sesiones");
        new VistaSalaEspera(paginaAnfitrion).iniciarPartida();

        //Todos ingresan al tablero
        for (Page pagina : paginas()) {
            pagina.waitForURL(Pattern.compile(".*/partida\\?codigo=" + codigoSala + "(?:&.*)?$"));

            entoncesSeVisualizaElTableroInicial(pagina);
            assertEquals(codigoSala, pagina.evaluate("() => window.codigoSala"));
        }

        diagnostico("Etapa 7: primer turno y permiso para tirar segun la identidad de cada sesion");
        entoncesTodosIdentificanElMismoPrimerTurno();

        diagnostico("Escenarios completos");
        pausarSiCorresponde("fin");
    }

    private void dadoQueUnUsuarioSeRegistraEIniciaSesion(Page pagina, String nombre, String email) {
        VistaLogin vistaLogin = new VistaLogin(pagina);

        vistaLogin.darClickEnRegistrarse();
        pagina.waitForURL("**/nuevo-usuario*");

        VistaNuevoUsuario vistaRegistro = new VistaNuevoUsuario(pagina);

        vistaRegistro.escribirUsername(nombre);
        vistaRegistro.escribirEMAIL(email);
        vistaRegistro.escribirClave(CLAVE);
        vistaRegistro.escribirRepetirClave(CLAVE);
        vistaRegistro.darClickEnRegistrarme();

        pagina.waitForURL("**/login*");

        vistaLogin = new VistaLogin(pagina);

        vistaLogin.escribirEMAIL(email);
        vistaLogin.escribirClave(CLAVE);
        vistaLogin.darClickEnIniciarSesion();

        pagina.waitForURL("**/home*");
        new VistaHome(pagina).verificarUsuarioAutenticado(nombre);
    }

    private String cuandoElAnfitrionCreaUnaSala() {
        VistaHome vistaHome = new VistaHome(paginaAnfitrion);

        vistaHome.darClickEnJugarAhora();
        vistaHome.darClickEnCrearPartida();

        paginaAnfitrion.waitForURL("**/sala-de-espera*");

        String codigo = new VistaSalaEspera(paginaAnfitrion).obtenerCodigoSala();

        assertFalse(codigo.isBlank(), "El anfitrion deberia obtener un codigo de sala");

        return codigo;
    }

    private void cuandoElInvitadoSeUneALaSala(Page pagina, String codigo) {
        VistaHome vistaHome = new VistaHome(pagina);

        vistaHome.darClickEnJugarAhora();
        vistaHome.darClickEnUnirseAPartida(codigo);

        pagina.waitForURL("**/sala-de-espera*");

        assertEquals(codigo, new VistaSalaEspera(pagina).obtenerCodigoSala());
    }

    private void esperarConexionWebSocket(Page pagina) {
        new VistaSalaEspera(pagina).esperarConexionWebSocket();
        pagina.evaluate("marca => window.__e2eDocumentoSala = marca", marcaDocumentoSala);
    }

    private void entoncesAparecenLosTresJugadores(Page pagina) {
        new VistaSalaEspera(pagina).verificarJugadores(ANFITRION, INVITADO_UNO, INVITADO_DOS);
    }

    private void entoncesSeVisualizaLaFicha(Page pagina, String nombre, String color) {
        new VistaSalaEspera(pagina).verificarFichaDeJugador(nombre, color);
    }

    private void entoncesSeVisualizaElTableroInicial(Page pagina) {
        PlaywrightAssertions.assertThat(pagina.locator(".tablero")).isVisible();

        PlaywrightAssertions.assertThat(pagina.locator(".jugadores .jugador")).hasCount(3);

        PlaywrightAssertions.assertThat(pagina.locator(".casillero[data-casillero='0'] .ficha")).hasCount(3);
        PlaywrightAssertions.assertThat(pagina.locator(".casillero .ficha")).hasCount(3);

        PlaywrightAssertions.assertThat(pagina.locator(".jugadores .jugador .dinero")).hasCount(3);

        for (Locator dinero : pagina.locator(".jugadores .jugador .dinero").all()) {
            PlaywrightAssertions.assertThat(dinero).hasText("$2.000");
        }

        List<String> colores = List.of("ROJO", "AZUL", "VERDE");
        for (Integer i = 0; i < colores.size(); i++) {
            String color = colores.get(i);
            Locator jugador = pagina.locator(".jugadores .jugador[data-ficha='" + color + "']");
            PlaywrightAssertions.assertThat(jugador).hasCount(1);
            PlaywrightAssertions.assertThat(jugador.locator(".nombre-jugador")).hasText(
                nombres().get(i)
            );
            PlaywrightAssertions.assertThat(
                pagina.locator(
                    ".casillero[data-casillero='0'] .ficha-" + color.toLowerCase(Locale.ROOT)
                )
            ).isVisible();
        }

        PlaywrightAssertions.assertThat(
            pagina.locator(".jugadores .jugador:has(.indicador-turno)")
        ).hasCount(1);
    }

    private void entoncesTodosIdentificanElMismoPrimerTurno() {
        String jugadorEnTurno = obtenerJugadorEnTurno(paginaAnfitrion);

        assertTrue(
            nombres().contains(jugadorEnTurno),
            "El primer turno debe pertenecer a uno de los tres jugadores"
        );
        Integer botonesHabilitados = 0;
        for (Integer i = 0; i < paginas().size(); i++) {
            Page pagina = paginas().get(i);
            PlaywrightAssertions.assertThat(nombreJugadorEnTurno(pagina)).hasText(jugadorEnTurno);
            Locator tarjetaPropia = pagina
                .locator(".jugadores .jugador")
                .filter(
                    new Locator.FilterOptions().setHas(
                        pagina.getByText(
                            nombres().get(i),
                            new Page.GetByTextOptions().setExact(true)
                        )
                    )
                );
            assertEquals(
                tarjetaPropia.getAttribute("id"),
                pagina.evaluate("() => String(window.usuarioSesionId)"),
                "El tablero debe conservar la identidad de la sesion"
            );
            if (nombres().get(i).equals(jugadorEnTurno)) {
                PlaywrightAssertions.assertThat(pagina.locator("#btn-tirar")).isEnabled();
                botonesHabilitados++;
            } else {
                PlaywrightAssertions.assertThat(pagina.locator("#btn-tirar")).isDisabled();
            }
        }
        assertEquals(1, botonesHabilitados, "Solo la sesion del jugador en turno puede tirar");
    }

    private String obtenerJugadorEnTurno(Page pagina) {
        PlaywrightAssertions.assertThat(nombreJugadorEnTurno(pagina)).hasCount(1);
        return nombreJugadorEnTurno(pagina).innerText().trim();
    }

    private Locator nombreJugadorEnTurno(Page pagina) {
        return pagina.locator(".jugadores .jugador:has(.indicador-turno) " + ".nombre-jugador");
    }

    private void entoncesLasSesionesSonIndependientes() {
        Set<String> idsDeSesion = new HashSet<>();
        for (int i = 0; i < paginas().size(); i++) {
            Page pagina = paginas().get(i);
            new VistaHome(pagina).verificarUsuarioAutenticado(nombres().get(i));
            List<String> cookiesDeSesion = pagina
                .context()
                .cookies()
                .stream()
                .filter(cookie -> "JSESSIONID".equals(cookie.name))
                .map(cookie -> cookie.value)
                .toList();
            assertEquals(
                1,
                cookiesDeSesion.size(),
                "Cada jugador debe tener su propia cookie de sesion"
            );
            assertFalse(cookiesDeSesion.get(0).isBlank());
            assertTrue(
                idsDeSesion.add(cookiesDeSesion.get(0)),
                "Las sesiones no deben compartir JSESSIONID"
            );
        }
        assertEquals(3, idsDeSesion.size());
    }

    private void observarMensajesDeSala(Page pagina) {
        // Observa trafico real sin interceptar ni simular mensajes productivos
        pagina.onWebSocket(socket ->
            socket.onFrameReceived(frame -> {
                String texto = frame.text();
                if (
                    texto != null &&
                    texto.contains("MESSAGE") &&
                    texto.contains("destination:/topic/sala/")
                ) {
                    paginasConMensajesDeSala.add(pagina);
                }
            })
        );
    }

    private void pausarSiCorresponde(String etapa) {
        if (etapa.equals(PAUSA)) {
            System.out.println(
                "[AccesoPartidaE2EIT] Pausa en " +
                    etapa +
                    "; inspeccionar las tres ventanas y pulsar Resume en Playwright Inspector."
            );
            paginaAnfitrion.bringToFront();
            paginaAnfitrion.pause();
        }
    }

    private static void diagnostico(String mensaje) {
        if (DIAGNOSTICO) {
            System.out.println("[AccesoPartidaE2EIT] " + mensaje);
        }
    }

    private List<String> nombres() {
        return List.of(ANFITRION, INVITADO_UNO, INVITADO_DOS);
    }

    private List<Page> paginas() {
        return List.of(paginaAnfitrion, paginaInvitadoUno, paginaInvitadoDos);
    }
}
