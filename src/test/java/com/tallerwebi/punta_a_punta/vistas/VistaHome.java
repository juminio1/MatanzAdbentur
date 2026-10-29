package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;

public class VistaHome extends VistaWeb {

    public VistaHome(Page page) {
        super(page);
    }

    public void darClickEnJugarAhora() {
        // Apuntamos específicamente al botón que levanta el modal
        this.darClickEnElElemento("button[data-bs-target='#modalJugar']");
    }

    public void darClickEnCrearPartida() {
        // Apuntamos al botón dentro del formulario de crear partida
        this.darClickEnElElemento("button:has-text('Crear partida')");
    }
}