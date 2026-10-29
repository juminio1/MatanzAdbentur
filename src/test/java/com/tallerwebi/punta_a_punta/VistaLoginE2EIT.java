package com.tallerwebi.punta_a_punta;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.tallerwebi.punta_a_punta.vistas.VistaLogin;
import com.tallerwebi.punta_a_punta.vistas.VistaNuevoUsuario;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VistaLoginE2EIT {

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    VistaLogin vistaLogin;

    @BeforeAll
    static void abrirNavegador() {
        playwright = Playwright.create();

        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(1500));
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
    void crearContextoYPagina() {
        ReiniciarDB.limpiarBaseDeDatos();
        context = browser.newContext();
        Page page = context.newPage();
        vistaLogin = new VistaLogin(page);
    }

    @AfterEach
    void cerrarContexto() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void deberiaMostrarElTituloDeLogin() throws MalformedURLException {
        dadoQueElUsuarioEstaEnLaVistaDeLogin();
        entoncesDeberiaVerElTituloDeLogin();
    }

    @Test
    void deberiaDarUnErrorAlIntentarIniciarSesionConUnUsuarioQueNoExiste() {
        dadoQueElUsuarioCargaSusDatosDeLoginCon("damian@unlam.edu.ar", "unlam");
        cuandoElUsuarioTocaElBotonDeLogin();
        entoncesDeberiaVerUnMensajeDeError();
    }

    @Test
    void deberiaNavegarAlHomeSiElUsuarioExiste() throws MalformedURLException {
        dadoQueElUsuarioCargaSusDatosDeLoginCon("test@unlam.edu.ar", "test");
        cuandoElUsuarioTocaElBotonDeLogin();
        entoncesDeberiaSerRedirigidoALaVistaDeHome();
    }

    @Test
    void deberiaRegistrarUnUsuarioEIniciarSesionExitosamente() throws MalformedURLException {
        dadoQueElUsuarioNavegaALaVistaDeRegistro();
        dadoQueElUsuarioSeRegistraCon("juan123", "juan@unlam.edu.ar", "Clave123!");
        dadoQueElUsuarioEstaEnLaVistaDeLogin();
        dadoQueElUsuarioCargaSusDatosDeLoginCon("juan@unlam.edu.ar", "Clave123!");
        cuandoElUsuarioTocaElBotonDeLogin();
        entoncesDeberiaSerRedirigidoALaVistaDeHome();
    }

    private void entoncesDeberiaVerElTituloDeLogin() {
        assertThat(vistaLogin.obtenerTituloLogin(), equalToIgnoringCase("Iniciar sesión"));
    }

    private void dadoQueElUsuarioEstaEnLaVistaDeLogin() throws MalformedURLException {
        URL urlLogin = vistaLogin.obtenerURLActual();
        assertThat(urlLogin.getPath(), matchesPattern("^/spring/login(?:;jsessionid=[^/\\s]+)?$"));
    }

    private void cuandoElUsuarioTocaElBotonDeLogin() {
        vistaLogin.darClickEnIniciarSesion();
    }

    private void entoncesDeberiaSerRedirigidoALaVistaDeHome() throws MalformedURLException {
        Page page = context.pages().get(0);

        URL urlActual = vistaLogin.obtenerURLActual();

        String mensajeError = "No se muestra un mensaje de error";

        if (page.locator(".login-error span:last-child").count() > 0) {
            mensajeError = page.locator(".login-error span:last-child").innerText();
        }

        assertThat(
            "El usuario no fue redirigido al Home. URL actual: " +
                urlActual +
                ". Mensaje: " +
                mensajeError,
            urlActual.getPath(),
            matchesPattern("^/spring/home(?:;jsessionid=[^/\\s]+)?$")
        );
    }

    private void entoncesDeberiaVerUnMensajeDeError() {
        String texto = vistaLogin.obtenerMensajeDeError();
        assertThat(texto, equalToIgnoringCase("Usuario o clave incorrecta"));
    }

    private void dadoQueElUsuarioCargaSusDatosDeLoginCon(String email, String clave) {
        vistaLogin.escribirEMAIL(email);
        vistaLogin.escribirClave(clave);
    }

    private void dadoQueElUsuarioNavegaALaVistaDeRegistro() {
        vistaLogin.darClickEnRegistrarse();
        context.pages().get(0).waitForURL("**/nuevo-usuario");
    }

    private void dadoQueElUsuarioSeRegistraCon(String username, String email, String clave) {
        VistaNuevoUsuario vistaNuevoUsuario = new VistaNuevoUsuario(context.pages().get(0));
        vistaNuevoUsuario.escribirUsername(username);
        vistaNuevoUsuario.escribirEMAIL(email);
        vistaNuevoUsuario.escribirClave(clave);
        vistaNuevoUsuario.escribirRepetirClave(clave);
        vistaNuevoUsuario.darClickEnRegistrarme();
        context.pages().get(0).waitForURL("**/login");
    }
}
