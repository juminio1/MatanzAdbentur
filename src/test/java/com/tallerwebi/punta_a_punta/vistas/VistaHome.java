package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import java.util.regex.Pattern;

public class VistaHome extends VistaWeb {

    public VistaHome(Page page) {
        super(page);
    }

    public void verificarUsuarioAutenticado(String nombre) {
        PlaywrightAssertions.assertThat(page).hasURL(Pattern.compile(".*/home(?:[;?#].*)?$"));
        PlaywrightAssertions.assertThat(page.locator(".hero-kicker span")).hasText(nombre);
        PlaywrightAssertions.assertThat(
            page.locator("button[data-bs-target='#modalJugar']")
        ).isVisible();
    }

    public void darClickEnJugarAhora() {
        // Apuntamos específicamente al botón que levanta el modal
        this.darClickEnElElemento("button[data-bs-target='#modalJugar']");
    }

    public void darClickEnUnirseAPartida(String codigoSala) {
        this.darClickEnElElemento("#btnIrAUnirse");
        this.escribirEnElElemento("input[name='codigoUnico']", codigoSala);
        this.darClickEnElElemento("#ventanaCodigo button[type='submit']");
    }

    public void darClickEnCrearPartida() {
        // Apuntamos al botón dentro del formulario de crear partida
        this.darClickEnElElemento("button:has-text('Crear partida')");
    }
}
