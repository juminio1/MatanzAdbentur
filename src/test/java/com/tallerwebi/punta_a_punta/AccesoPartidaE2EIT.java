
package com.tallerwebi.punta_a_punta;

import com.microsoft.playwright.*;
import com.tallerwebi.punta_a_punta.vistas.VistaHome;
import com.tallerwebi.punta_a_punta.vistas.VistaLogin;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import org.junit.jupiter.api.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class AccesoPartidaE2EIT {

    static Playwright playwright;
    static Browser browser;

    // Variables exclusivas para el primer usuario (el que crea la partida)
    BrowserContext contextoAnfitrion;
    Page paginaAnfitrion;

    @BeforeAll
    static void abrirNavegador() {
        playwright = Playwright.create();
        // Mantenemos el slowMo para que puedas ver cómo hace los clicks paso a paso
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false).setSlowMo(1000));
    }

    @AfterAll
    static void cerrarNavegador() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void prepararEscenario() {
        // Limpiamos la BD. Recordá que esto ya inserta un usuario: test@unlam.edu.ar / test
        ReiniciarDB.limpiarBaseDeDatos(); 
        
        // Creamos el contexto (sesión aislada) y la página solo para el anfitrión
        contextoAnfitrion = browser.newContext();
        paginaAnfitrion = contextoAnfitrion.newPage();
    }

    @AfterEach
    void limpiarEscenario() {
        if (contextoAnfitrion != null) contextoAnfitrion.close();
    }

    @Test
    void unUsuarioDebePoderCrearUnaPartidaYElegirFicha() {
        // 1. El anfitrión inicia sesión
        dadoQueElAnfitrionIniciaSesion("test@unlam.edu.ar", "test");

        // 2. El anfitrión crea la partida desde el Home
        cuandoElAnfitrionCreaLaPartida();

        // 3. El anfitrión elige su ficha en la sala de espera
        entoncesElAnfitrionEligeLaFichaRoja();
    }

    // --- MÉTODOS DE APOYO (Steps) ---

    private void dadoQueElAnfitrionIniciaSesion(String email, String clave) {
        VistaLogin vistaLogin = new VistaLogin(paginaAnfitrion);
        vistaLogin.escribirEMAIL(email);
        vistaLogin.escribirClave(clave);
        vistaLogin.darClickEnIniciarSesion();
        
        // Esperamos a que la URL cambie al home para confirmar el login
        paginaAnfitrion.waitForURL("**/home*"); 
    }

    private void cuandoElAnfitrionCreaLaPartida() {
        VistaHome vistaHome = new VistaHome(paginaAnfitrion);
        vistaHome.darClickEnJugarAhora();
        vistaHome.darClickEnCrearPartida();
        
        // Esperamos a que el backend nos redirija a la sala de espera
        paginaAnfitrion.waitForURL("**/sala-de-espera*"); 
    }

    private void entoncesElAnfitrionEligeLaFichaRoja() {
        VistaSalaEspera vistaSalaEspera = new VistaSalaEspera(paginaAnfitrion);
        vistaSalaEspera.elegirFicha("ROJO");

        // Verificamos que la URL actual efectivamente contenga "sala-de-espera"
        String urlActual = paginaAnfitrion.url();
        assertThat(urlActual, containsString("sala-de-espera"));
    }
}