package com.tallerwebi.punta_a_punta.vistas;

import com.microsoft.playwright.Page;

public class VistaSalaEspera extends VistaWeb {

    public VistaSalaEspera(Page page) {
        super(page);
    }

    public void elegirFicha(String color) {
        // color debe ser "ROJO", "AZUL", "VERDE" o "AMARILLO"
        String selector = String.format("button[data-ficha='%s']", color);
        this.darClickEnElElemento(selector);
    }

    public String obtenerCodigoSala() {
        return this.obtenerTextoDelElemento("#datos-sala span").trim();
    }
}