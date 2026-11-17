package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import java.util.Locale;
import java.util.regex.Pattern;

public class VistaSalaEspera extends VistaWeb {

    public VistaSalaEspera(Page page) {
        super(page);
    }

    public void elegirFicha(String color) {
        // color debe ser "ROJO", "AZUL", "VERDE" o "AMARILLO"
        botonFicha(color).click();
    }

    public void iniciarPartida() {
        this.darClickEnElElemento(".salaEspera__iniciar");
    }

    public String obtenerCodigoSala() {
        PlaywrightAssertions.assertThat(page.locator("#datos-sala span")).hasText(
            Pattern.compile(".*\\S.*", Pattern.DOTALL)
        );
        return this.obtenerTextoDelElemento("#datos-sala span").trim();
    }

    public void esperarConexionWebSocket() {
        page.waitForFunction(
            "() => typeof stompClient !== 'undefined' && stompClient !== null && stompClient.connected"
        );
    }

    public void verificarJugadores(String... nombres) {
        PlaywrightAssertions.assertThat(page.locator("#contador-jugadores")).hasText(
            nombres.length + "/4"
        );
        Locator jugadores = page.locator("#lista-jugadores .salaEspera__nombre strong");
        PlaywrightAssertions.assertThat(jugadores).hasCount(nombres.length);
        for (String nombre : nombres) {
            PlaywrightAssertions.assertThat(
                page
                    .locator("#lista-jugadores")
                    .getByText(nombre, new Locator.GetByTextOptions().setExact(true))
            ).isVisible();
        }
    }

    public void verificarFichaDeJugador(String nombre, String color) {
        Locator nombreJugador = page.getByText(nombre, new Page.GetByTextOptions().setExact(true));
        Locator jugador = page
            .locator("#lista-jugadores .salaEspera__jugador")
            .filter(new Locator.FilterOptions().setHas(nombreJugador));
        PlaywrightAssertions.assertThat(
            jugador.locator(".salaEspera__jugadorFicha.ficha-" + color.toLowerCase(Locale.ROOT))
        ).isVisible();
    }

    public void verificarFichaOcupada(String color) {
        // La interfaz impide el intento: un clic de Playwright esperaría hasta agotar el timeout.
        PlaywrightAssertions.assertThat(botonFicha(color)).isDisabled();
    }

    public void verificarPermisoParaIniciar(boolean esAnfitrion) {
        if (esAnfitrion) {
            PlaywrightAssertions.assertThat(page.locator(".salaEspera__iniciar")).isEnabled();
        } else {
            PlaywrightAssertions.assertThat(page.locator(".salaEspera__iniciar")).isDisabled();
        }
    }

    private Locator botonFicha(String color) {
        return page.locator(String.format("button[data-ficha='%s']", color));
    }
}
