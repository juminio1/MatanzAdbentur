
package com.tallerwebi.punta_a_punta;

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
import java.util.Locale;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AccesoPartidaE2EIT {

    private static final String CLAVE = "Clave123!";
    private static final String ANFITRION = "anfitrionE2E";
    private static final String INVITADO_UNO = "invitadoUnoE2E";
    private static final String INVITADO_DOS = "invitadoDosE2E";

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
        playwright = Playwright.create();

        browser = playwright.chromium().launch(
            new BrowserType.LaunchOptions()
                .setHeadless(false)
                .setSlowMo(1000)
        );
    }

    @AfterAll
    static void cerrarNavegador() {
        if (browser != null) {
            browser.close();
        }

        if (playwright != null) {
            playwright.close();
        }
    }

    
    @BeforeEach
    public void prepararEscenario() {
        ReiniciarDB.limpiarBaseDeDatos();

        contextoAnfitrion = browser.newContext();
        contextoInvitadoUno = browser.newContext();
        contextoInvitadoDos = browser.newContext();

        paginaAnfitrion = contextoAnfitrion.newPage();
        paginaInvitadoUno = contextoInvitadoUno.newPage();
        paginaInvitadoDos = contextoInvitadoDos.newPage();

        // Pausa temporal para ver las ventanas
        paginaAnfitrion.pause();
    }


    @AfterEach
    void limpiarEscenario() {
        if (contextoAnfitrion != null) {
            contextoAnfitrion.close();
        }

        if (contextoInvitadoUno != null) {
            contextoInvitadoUno.close();
        }

        if (contextoInvitadoDos != null) {
            contextoInvitadoDos.close();
        }
    }

    @Test
    void dadoQueTresUsuariosSeRegistranYSeUnenAUnaSalaCuandoInicianUnaPartidaEntoncesTodosIngresanAlTablero() {

        //Registrar e iniciar sesión con tres usuarios.
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaAnfitrion, ANFITRION, "juli@unlam.edu.ar");

        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaInvitadoUno, INVITADO_UNO, "emi@unlam.edu.ar");

        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaInvitadoDos, INVITADO_DOS, "may@unlam.edu.ar");

        //El anfitrión crea una sala
        String codigoSala = cuandoElAnfitrionCreaUnaSala();
        esperarConexionWebSocket(paginaAnfitrion);

        //Los invitados ingresan con el código
        cuandoElInvitadoSeUneALaSala(
            paginaInvitadoUno, codigoSala);

        esperarConexionWebSocket(paginaInvitadoUno);

        cuandoElInvitadoSeUneALaSala(paginaInvitadoDos, codigoSala);

        esperarConexionWebSocket(paginaInvitadoDos);

        //Comprobar que los tres se visualizan
        for (Page pagina : paginas()) {
            entoncesAparecenLosTresJugadores(pagina);
        }

        //El anfitrión selecciona la ficha roja
        new VistaSalaEspera(paginaAnfitrion)
            .elegirFicha("ROJO");

        for (Page pagina : paginas()) {
            entoncesSeVisualizaLaFicha(
                pagina, ANFITRION, "ROJO");
        }

        //La ficha ocupada debe quedar bloqueada
        PlaywrightAssertions.assertThat(
            paginaInvitadoUno.locator(
                "button[data-ficha='ROJO']")
        ).isDisabled();

        // Los invitados seleccionan otras fichas
        new VistaSalaEspera(paginaInvitadoUno)
            .elegirFicha("AZUL");

        new VistaSalaEspera(paginaInvitadoDos)
            .elegirFicha("VERDE");

        // 8. Comprobar la sincronización de fichas.
        for (Page pagina : paginas()) {
            entoncesSeVisualizaLaFicha(
                pagina, INVITADO_UNO, "AZUL");

            entoncesSeVisualizaLaFicha(
                pagina, INVITADO_DOS, "VERDE");
        }

        // 9. El anfitrión inicia la partida.
        new VistaSalaEspera(paginaAnfitrion)
            .iniciarPartida();

        // 10. Todos ingresan al tablero.
        for (Page pagina : paginas()) {
            pagina.waitForURL(
                Pattern.compile(
                    ".*/partida\\?codigo="
                        + Pattern.quote(codigoSala)
                        + "(?:&.*)?$"
                )
            );

            entoncesSeVisualizaElTableroInicial(pagina);
        }

        // 11. Comprobar el primer turno.
        entoncesTodosIdentificanElMismoPrimerTurno();

        // 12. Pausar para inspeccionar los navegadores.
        // Ejecutar con PWDEBUG=1.
        paginaAnfitrion.pause();
    }

    private void dadoQueUnUsuarioSeRegistraEIniciaSesion(
        Page pagina, String nombre, String email) {

        VistaLogin vistaLogin = new VistaLogin(pagina);

        vistaLogin.darClickEnRegistrarse();
        pagina.waitForURL("**/nuevo-usuario*");

        VistaNuevoUsuario vistaRegistro =
            new VistaNuevoUsuario(pagina);

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
    }

    private String cuandoElAnfitrionCreaUnaSala() {
        VistaHome vistaHome =
            new VistaHome(paginaAnfitrion);

        vistaHome.darClickEnJugarAhora();
        vistaHome.darClickEnCrearPartida();

        paginaAnfitrion.waitForURL(
            "**/sala-de-espera*");

        String codigo = new VistaSalaEspera(
            paginaAnfitrion).obtenerCodigoSala();

        assertFalse(
            codigo.isBlank(),
            "El anfitrion deberia obtener un codigo de sala"
        );

        return codigo;
    }

    private void cuandoElInvitadoSeUneALaSala(
        Page pagina, String codigo) {

        VistaHome vistaHome = new VistaHome(pagina);

        vistaHome.darClickEnJugarAhora();
        vistaHome.darClickEnUnirseAPartida(codigo);

        pagina.waitForURL("**/sala-de-espera*");

        assertEquals(codigo,new VistaSalaEspera(pagina).obtenerCodigoSala());
    }

    private void esperarConexionWebSocket(Page pagina) {
        pagina.waitForFunction(
            "() => typeof stompClient !== 'undefined' && "
                + "stompClient !== null && "
                + "stompClient.connected === true"
        );
    }

    private void entoncesAparecenLosTresJugadores(
        Page pagina) {

        PlaywrightAssertions.assertThat(
            pagina.locator("#contador-jugadores")
        ).hasText("3/4");

        for (String nombre : List.of(
            ANFITRION, INVITADO_UNO, INVITADO_DOS)) {

            Locator usuario = pagina.locator(
                "#lista-jugadores .salaEspera__nombre strong"
            ).filter(
                new Locator.FilterOptions().setHasText(nombre));

            PlaywrightAssertions.assertThat(usuario).isVisible();
        }
    }

    private void entoncesSeVisualizaLaFicha(
        Page pagina, String nombre, String color) {

        Locator jugador = pagina.locator(
            "#lista-jugadores .salaEspera__jugador"
        ).filter(
            new Locator.FilterOptions()
                .setHasText(nombre)
        );

        Locator ficha = jugador.locator(
            ".salaEspera__jugadorFicha.ficha-"
                + color.toLowerCase(Locale.ROOT)
        );

        PlaywrightAssertions.assertThat(ficha)
            .isVisible();
    }

    private void entoncesSeVisualizaElTableroInicial(
        Page pagina) {

        PlaywrightAssertions.assertThat(
            pagina.locator(".tablero")
        ).isVisible();

        PlaywrightAssertions.assertThat(
            pagina.locator(".jugadores .jugador")
        ).hasCount(3);

        PlaywrightAssertions.assertThat(
            pagina.locator(
                ".casillero[data-casillero='0'] .ficha"
            )
        ).hasCount(3);

        PlaywrightAssertions.assertThat(
            pagina.locator(".jugadores .jugador .dinero")
        ).hasCount(3);

        for (Locator dinero : pagina.locator(
            ".jugadores .jugador .dinero").all()) {

            PlaywrightAssertions.assertThat(dinero)
                .hasText("$2.000");
        }

        for (String color : List.of(
            "ROJO", "AZUL", "VERDE")) {

            PlaywrightAssertions.assertThat(
                pagina.locator(
                    ".jugadores .jugador[data-ficha='"
                        + color + "']"
                )
            ).hasCount(1);
        }

        PlaywrightAssertions.assertThat(
            pagina.locator(
                ".jugadores .jugador:has(.indicador-turno)"
            )
        ).hasCount(1);
    }

    private void entoncesTodosIdentificanElMismoPrimerTurno() {

        String jugadorEnTurno =
            obtenerJugadorEnTurno(paginaAnfitrion);

        assertEquals(jugadorEnTurno, obtenerJugadorEnTurno(paginaInvitadoUno));

        assertEquals(jugadorEnTurno, obtenerJugadorEnTurno(paginaInvitadoDos));

        Integer botonesHabilitados = 0;

        for (Page pagina : paginas()) {
            if (pagina.locator("#btn-tirar").isEnabled()) {
                botonesHabilitados++;
            }
        }

        assertEquals(
            1,
            botonesHabilitados,
            "Solo la sesion del jugador actual debe poder tirar dados"
        );

        assertTrue(
            List.of(
                ANFITRION, INVITADO_UNO, INVITADO_DOS
            ).contains(jugadorEnTurno)
        );
    }

    private String obtenerJugadorEnTurno(Page pagina) {
        return pagina.locator(
            ".jugadores .jugador:has(.indicador-turno) "
                + ".nombre-jugador"
        ).innerText().trim();
    }

    private List<Page> paginas() {
        return List.of(
            paginaAnfitrion,
            paginaInvitadoUno,
            paginaInvitadoDos
        );
    }
}
