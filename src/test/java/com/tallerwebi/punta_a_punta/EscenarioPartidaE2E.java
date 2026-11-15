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
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

abstract class EscenarioPartidaE2E {

    private static final String CLAVE = "Clave123!";
    static final String ANFITRION = "anfitrionE2E";
    static final String INVITADO_UNO = "invitadoUnoE2E";
    static final String INVITADO_DOS = "invitadoDosE2E";
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
                EscenarioPartidaE2E.class.getResource("EscenarioPartidaE2E.class")
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

    void dadoQueUnUsuarioSeRegistraEIniciaSesion(Page pagina, String nombre, String email) {
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

    String cuandoElAnfitrionCreaUnaSala() {
        VistaHome vistaHome = new VistaHome(paginaAnfitrion);

        vistaHome.darClickEnJugarAhora();
        vistaHome.darClickEnCrearPartida();

        paginaAnfitrion.waitForURL("**/sala-de-espera*");

        String codigo = new VistaSalaEspera(paginaAnfitrion).obtenerCodigoSala();

        assertFalse(codigo.isBlank(), "El anfitrion deberia obtener un codigo de sala");

        return codigo;
    }

    void cuandoElInvitadoSeUneALaSala(Page pagina, String codigo) {
        VistaHome vistaHome = new VistaHome(pagina);

        vistaHome.darClickEnJugarAhora();
        vistaHome.darClickEnUnirseAPartida(codigo);

        pagina.waitForURL("**/sala-de-espera*");

        assertEquals(codigo, new VistaSalaEspera(pagina).obtenerCodigoSala());
    }

    void iniciarPartidaYEsperarTableros(String codigoSala) {
        new VistaSalaEspera(paginaAnfitrion).iniciarPartida();

        //Todos ingresan al tablero
        for (Page pagina : paginas()) {
            pagina.waitForURL(Pattern.compile(".*/partida\\?codigo=" + codigoSala + "(?:&.*)?$"));
        }
    }

    Page verificarPermisoDeTirada(String jugadorEnTurno) {
        int indiceEnTurno = nombres().indexOf(jugadorEnTurno);
        assertTrue(indiceEnTurno >= 0, "El turno debe pertenecer a uno de los tres jugadores");
        List<Page> sesiones = paginas();
        Page paginaEnTurno = sesiones.get(indiceEnTurno);
        for (Page pagina : sesiones) {
            PlaywrightAssertions.assertThat(nombreJugadorEnTurno(pagina)).hasText(jugadorEnTurno);
            Locator botonTirar = pagina.locator("#btn-tirar");
            if (pagina == paginaEnTurno) {
                PlaywrightAssertions.assertThat(botonTirar).isEnabled();
            } else {
                PlaywrightAssertions.assertThat(botonTirar).isDisabled();
            }
        }
        return paginaEnTurno;
    }

    String obtenerJugadorEnTurno(Page pagina) {
        PlaywrightAssertions.assertThat(nombreJugadorEnTurno(pagina)).hasCount(1);
        return nombreJugadorEnTurno(pagina).innerText().trim();
    }

    Locator nombreJugadorEnTurno(Page pagina) {
        return pagina.locator(".jugadores .jugador:has(.indicador-turno) " + ".nombre-jugador");
    }

    void pausarSiCorresponde(String etapa) {
        if (etapa.equals(PAUSA)) {
            System.out.println(
                "[E2E partida] Pausa en " +
                    etapa +
                    "; inspeccionar las tres ventanas y pulsar Resume en Playwright Inspector."
            );
            paginaAnfitrion.bringToFront();
            paginaAnfitrion.pause();
        }
    }

    static void diagnostico(String mensaje) {
        if (DIAGNOSTICO) {
            System.out.println("[E2E partida] " + mensaje);
        }
    }

    List<String> nombres() {
        return List.of(ANFITRION, INVITADO_UNO, INVITADO_DOS);
    }

    List<Page> paginas() {
        return List.of(paginaAnfitrion, paginaInvitadoUno, paginaInvitadoDos);
    }
}
