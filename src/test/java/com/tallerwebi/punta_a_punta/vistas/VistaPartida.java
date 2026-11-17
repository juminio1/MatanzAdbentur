package com.tallerwebi.punta_a_punta.vistas;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import java.text.NumberFormat;
import java.util.Locale;

public class VistaPartida extends VistaWeb {

    public VistaPartida(Page page) {
        super(page);
    }

    public void verificarInicio() {
        assertThat(page.locator(".tablero")).isVisible();
        assertThat(page.locator(".jugadores .jugador")).hasCount(3);
        assertThat(page.locator("[data-casillero='0'] .ficha")).hasCount(3);
    }

    public long obtenerIdDeSesion() {
        return ((Number) page.evaluate("() => Number(window.usuarioSesionId)")).longValue();
    }

    public void verificarSaldo(String nombre, int saldo) {
        Locator tarjeta = page.locator(".jugadores .jugador").filter(
            new Locator.FilterOptions().setHas(page.getByText(
                nombre, new Page.GetByTextOptions().setExact(true))));
        assertThat(tarjeta).hasCount(1);
        assertThat(tarjeta.locator(".dinero")).hasText(
            "$" + NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-AR")).format(saldo));
    }

    public void verificarPosicion(long idJugador, int posicion) {
        assertThat(page.locator("[data-casillero='" + posicion
            + "'] .ficha[data-jugador-id='" + idJugador + "']")).hasCount(1);
    }

    public void verificarDisponible(int posicion, int precio) {
        Locator etiqueta = etiqueta(posicion);
        assertThat(etiqueta).hasAttribute("aria-label", "Precio de compra: $" + precio);
        assertThat(page.locator("[data-casillero='" + posicion
            + "'] .casillero__precio[data-color]")).hasCount(0);
    }

    public Response tirarDados() {
        return esperarPost("/partida/tirar-dados?", "#btn-tirar");
    }

    public void verificarOportunidadDeCompra(String nombre, int precio) {
        assertThat(page.locator("#modal-compra-propiedad")).isVisible();
        assertThat(page.locator("#modal-propiedad-titulo")).hasText(nombre);
        assertThat(page.locator("#compra-precio")).hasText("$" + precio);
        assertThat(page.locator("#btn-modal-comprar")).isEnabled();
    }

    public Response comprar(int posicion) {
        Response respuesta = esperarPost("/propiedades/" + posicion + "/comprar", "#btn-modal-comprar");
        assertThat(page.locator("#modal-compra-propiedad")).not().isVisible();
        return respuesta;
    }

    public void verificarPropietario(int posicion, String nombre, String color) {
        verificarPropietario(posicion, nombre, color, 30);
    }

    public void pasarSinComprar() {
        assertThat(page.locator("#modal-compra-propiedad")).isVisible();
        page.locator("#btn-modal-pasar").click();
        assertThat(page.locator("#modal-compra-propiedad")).not().isVisible();
    }

    public void abrirInformacionPropiedad(int posicion, String nombre) {
        // El tablero admite Enter; evita que una ficha tape el punto de clic del texto.
        page.locator("[data-casillero='" + posicion + "']").press("Enter");
        assertThat(page.locator("#modal-info-propiedad")).isVisible();
        assertThat(page.locator("#popup-info-titulo")).hasText(nombre);
    }

    public void verificarPermisoDeMejora(boolean permitido, int costo) {
        Locator boton = page.locator("#btn-info-mejorar");
        assertThat(boton).hasText("Mejorar por $" + costo);
        if (permitido) assertThat(boton).isEnabled();
        else assertThat(boton).isDisabled();
    }

    public Response mejorar(int posicion) {
        return esperarPost("/propiedades/" + posicion + "/subir-alquiler", "#btn-info-mejorar");
    }

    public void verificarAlquilerEnInformacion(int alquiler) {
        assertThat(page.locator("#popup-info-alquiler")).hasText("$" + alquiler);
    }

    public void cerrarInformacionPropiedad() {
        page.locator("#btn-cerrar-info-propiedad").click();
        assertThat(page.locator("#modal-info-propiedad")).not().isVisible();
    }

    public void prepararVentaAlBanco(String nombre, int importe) {
        page.locator("#btn-info-vender").click();
        assertThat(page.locator("#modal-vender-propiedad")).isVisible();
        assertThat(page.locator("#vender-propiedad-nombre")).hasText(nombre);
        page.locator("[data-destino-venta='banco']").click();
        assertThat(page.locator("#venta-panel-banco")).isVisible();
        assertThat(page.locator("#venta-banco-importe")).hasText("$" + importe);
        assertThat(page.locator("#btn-vender-al-banco")).isEnabled();
    }

    public Response confirmarVentaAlBanco(int posicion) {
        Response respuesta = esperarPost("/propiedades/" + posicion + "/vender", "#btn-vender-al-banco");
        assertThat(page.locator("#modal-vender-propiedad")).not().isVisible();
        return respuesta;
    }

    public void verificarInformacionSinPropietario(int precio) {
        assertThat(page.locator("#popup-info-propietario")).hasText("Sin propietario");
        assertThat(page.locator("#popup-info-precio")).isVisible();
        assertThat(page.locator("#popup-info-precio")).hasText("$" + precio);
        assertThat(page.locator("#popup-acciones-propias")).not().isVisible();
    }

    public void verificarAusenteEnMisPropiedades(String nombre) {
        page.locator("#btn-bono-propiedades").click();
        assertThat(page.locator("#modal-bono-propiedades")).isVisible();
        // Espera a que termine la carga antes de comprobar una ausencia.
        assertThat(page.locator("#bono-propiedades-mensaje")).hasText("Todavía no tenés propiedades.");
        assertThat(page.locator("#lista-propiedades-propias .propiedad-propia")
            .filter(new Locator.FilterOptions().setHasText(nombre))).hasCount(0);
        page.locator("#btn-cerrar-bono-propiedades").click();
        assertThat(page.locator("#modal-bono-propiedades")).not().isVisible();
    }

    public void verificarPropietario(int posicion, String nombre, String color, int alquiler) {
        Locator etiqueta = etiqueta(posicion);
        assertThat(etiqueta).isVisible();
        assertThat(etiqueta).hasAttribute("aria-label", "Alquiler: $" + alquiler + " · Propietario: " + nombre);
        assertThat(etiqueta).hasAttribute("data-color", color);
        String fondo = switch (color) {
            case "rojo" -> "rgb(118, 45, 48)";
            case "azul" -> "rgb(23, 76, 104)";
            case "verde" -> "rgb(18, 89, 65)";
            default -> throw new IllegalArgumentException("Color inesperado: " + color);
        };
        assertThat(etiqueta).hasCSS("background-color", fondo);
    }

    private Locator etiqueta(int posicion) {
        return page.locator("[data-casillero='" + posicion + "'] .casillero__precio");
    }

    private Response esperarPost(String ruta, String boton) {
        Response respuesta = page.waitForResponse(
            recibida -> recibida.url().contains(ruta)
                && "POST".equals(recibida.request().method()),
            () -> page.locator(boton).click());
        assertTrue(respuesta.ok(), "La accion de interfaz debe ser aceptada: HTTP " + respuesta.status());
        return respuesta;
    }
}
