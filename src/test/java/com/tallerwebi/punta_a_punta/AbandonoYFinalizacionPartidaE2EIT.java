package com.tallerwebi.punta_a_punta;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.tallerwebi.punta_a_punta.vistas.VistaHome;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class AbandonoYFinalizacionPartidaE2EIT extends EscenarioPartidaE2E {

    private final Set<Page> sesionesSuscritasAlAbandono = new HashSet<>();

    @Test
    void cuandoLosOponentesAbandonanEntoncesLaPartidaFinalizaYSeMuestraElResumen() {
        String codigo = iniciarPartidaConTresJugadores();
        String primerNombre = obtenerJugadorEnTurno(paginaAnfitrion);
        Page primerAbandono = verificarPermisoDeTirada(primerNombre);
        List<Page> restantes = paginas().stream()
            .filter(pagina -> pagina != primerAbandono).toList();
        Page segundoAbandono = restantes.get(0);
        Page ganador = restantes.get(1);
        String urlPartida = ganador.url();

        diagnostico("Cancelar el abandono conserva al jugador y su turno");
        abrirConfirmacionDeAbandono(primerAbandono);
        primerAbandono.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("No, seguir jugando").setExact(true)).click();
        PlaywrightAssertions.assertThat(primerAbandono.locator("#modal-abandonar")).isHidden();
        PlaywrightAssertions.assertThat(primerAbandono).hasURL(urlPartida);
        verificarPermisoDeTirada(primerNombre);
        for (Page pagina : paginas()) {
            verificarJugadoresActivos(pagina, 3);
            PlaywrightAssertions.assertThat(tarjetaJugador(pagina, primerNombre))
                .hasAttribute("data-abandono", "false");
            PlaywrightAssertions.assertThat(pagina.locator("#modal-victoria")).isHidden();
        }

        diagnostico("El primer abandono se refleja en ambos tableros y la partida continúa");
        confirmarAbandono(primerAbandono, codigo);
        new VistaHome(primerAbandono).verificarUsuarioAutenticado(primerNombre);
        for (Page pagina : restantes) {
            verificarAbandonoEnTablero(pagina, primerNombre);
            verificarJugadoresActivos(pagina, 2);
            PlaywrightAssertions.assertThat(nombreJugadorEnTurno(pagina)).not().hasText(primerNombre);
        }
        verificarContinuidad(restantes, urlPartida);

        diagnostico("El segundo abandono finaliza automáticamente la partida del ganador");
        confirmarAbandono(segundoAbandono, codigo);
        verificarResumenFinal(ganador, primerNombre, nombreDe(segundoAbandono));
        verificarAbandonoEnTablero(ganador, nombreDe(segundoAbandono));
        verificarJugadoresActivos(ganador, 1);
        PlaywrightAssertions.assertThat(tarjetaJugador(ganador, nombreDe(ganador)))
            .hasAttribute("data-abandono", "false");
        PlaywrightAssertions.assertThat(ganador).hasURL(urlPartida);
        verificarAccionesBloqueadas(ganador);
        new VistaHome(segundoAbandono).verificarUsuarioAutenticado(nombreDe(segundoAbandono));

        pausarSiCorresponde("fin");
        ganador.locator("#modal-victoria").getByRole(AriaRole.LINK,
            new Locator.GetByRoleOptions().setName("Volver al inicio").setExact(true)).click();
        new VistaHome(ganador).verificarUsuarioAutenticado(nombreDe(ganador));
    }

    private String iniciarPartidaConTresJugadores() {
        paginas().forEach(this::observarSuscripcionAlAbandono);
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaAnfitrion, ANFITRION, "juli@unlam.edu.ar");
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaInvitadoUno, INVITADO_UNO, "emi@unlam.edu.ar");
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaInvitadoDos, INVITADO_DOS, "may@unlam.edu.ar");

        String codigo = cuandoElAnfitrionCreaUnaSala();
        new VistaSalaEspera(paginaAnfitrion).esperarConexionWebSocket();
        cuandoElInvitadoSeUneALaSala(paginaInvitadoUno, codigo);
        new VistaSalaEspera(paginaInvitadoUno).esperarConexionWebSocket();
        cuandoElInvitadoSeUneALaSala(paginaInvitadoDos, codigo);
        new VistaSalaEspera(paginaInvitadoDos).esperarConexionWebSocket();

        List<String> colores = List.of("ROJO", "AZUL", "VERDE");
        for (int indice = 0; indice < paginas().size(); indice++) {
            VistaSalaEspera sala = new VistaSalaEspera(paginas().get(indice));
            sala.verificarJugadores(ANFITRION, INVITADO_UNO, INVITADO_DOS);
            sala.elegirFicha(colores.get(indice));
            sala.verificarFichaDeJugador(nombres().get(indice), colores.get(indice));
        }
        pausarSiCorresponde("sala");
        iniciarPartidaYEsperarTableros(codigo);
        paginaAnfitrion.waitForCondition(() -> sesionesSuscritasAlAbandono.containsAll(paginas()));
        for (Page pagina : paginas()) {
            PlaywrightAssertions.assertThat(pagina.locator(".tablero")).isVisible();
            PlaywrightAssertions.assertThat(pagina.locator(".jugadores .jugador")).hasCount(3);
            verificarJugadoresActivos(pagina, 3);
            for (String nombre : nombres()) {
                PlaywrightAssertions.assertThat(tarjetaJugador(pagina, nombre)).isVisible();
            }
        }
        return codigo;
    }

    private void observarSuscripcionAlAbandono(Page pagina) {
        pagina.onWebSocket(socket -> socket.onFrameSent(frame -> {
            String trama = frame.text();
            if (trama != null && trama.contains("SUBSCRIBE") &&
                trama.contains("destination:/topic/partida/") && trama.contains("/abandono")) {
                sesionesSuscritasAlAbandono.add(pagina);
            }
        }));
    }

    private void abrirConfirmacionDeAbandono(Page pagina) {
        pagina.getByRole(AriaRole.BUTTON,
            new Page.GetByRoleOptions().setName("Abandonar").setExact(true)).click();
        PlaywrightAssertions.assertThat(pagina.locator("#modal-abandonar")).isVisible();
        PlaywrightAssertions.assertThat(pagina.locator("#titulo-abandonar"))
            .hasText("¿Seguro que querés abandonar?");
    }

    private void confirmarAbandono(Page pagina, String codigo) {
        abrirConfirmacionDeAbandono(pagina);
        Response respuesta = pagina.waitForResponse(
            recibida -> recibida.url().endsWith("/partida/" + codigo + "/abandonar") &&
                "POST".equals(recibida.request().method()),
            () -> pagina.getByRole(AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Sí, abandonar").setExact(true)).click()
        );
        assertTrue(respuesta.ok(), "El abandono desde la interfaz debe ser aceptado");
    }

    private Locator tarjetaJugador(Page pagina, String nombre) {
        return pagina.locator(".jugadores .jugador").filter(new Locator.FilterOptions().setHas(
            pagina.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName(nombre).setExact(true))
        )); 
    }

    private void verificarJugadoresActivos(Page pagina, Integer cantidad) {
        PlaywrightAssertions.assertThat(pagina.locator(
            ".jugadores .jugador[data-abandono='false'][data-bancarrota='false']"
        )).hasCount(cantidad);
        PlaywrightAssertions.assertThat(pagina.locator(".casillero .ficha")).hasCount(cantidad);
    }

    private void verificarAbandonoEnTablero(Page pagina, String nombre) {
        Locator tarjeta = tarjetaJugador(pagina, nombre);
        PlaywrightAssertions.assertThat(tarjeta).hasAttribute("data-abandono", "true");
        PlaywrightAssertions.assertThat(tarjeta.locator(".estado-abandono")).hasText("ABANDONÓ");
        PlaywrightAssertions.assertThat(pagina.locator(
            ".casillero .ficha[data-jugador-id='" + tarjeta.getAttribute("id") + "']"
        )).hasCount(0);
    }

    private void verificarContinuidad(List<Page> restantes, String urlPartida) {
        String siguiente = obtenerJugadorEnTurno(restantes.get(0));
        assertTrue(restantes.stream().anyMatch(pagina -> nombreDe(pagina).equals(siguiente)),
            "El turno debe pasar a uno de los dos jugadores activos");
        for (Page pagina : restantes) {
            PlaywrightAssertions.assertThat(nombreJugadorEnTurno(pagina)).hasText(siguiente);
            if (nombreDe(pagina).equals(siguiente)) {
                PlaywrightAssertions.assertThat(pagina.locator("#btn-tirar")).isEnabled();
            } else {
                PlaywrightAssertions.assertThat(pagina.locator("#btn-tirar")).isDisabled();
            }
            // Una interacción real disponible para ambos, sin compras ni movimientos adicionales.
            pagina.locator("#btn-bono-propiedades").click();
            PlaywrightAssertions.assertThat(pagina.locator("#modal-bono-propiedades")).isVisible();
            pagina.locator("#btn-cerrar-bono-propiedades").click();
            PlaywrightAssertions.assertThat(pagina.locator("#modal-bono-propiedades")).isHidden();
            PlaywrightAssertions.assertThat(pagina.locator("#btn-abandonar")).isEnabled();
            PlaywrightAssertions.assertThat(pagina.locator("#modal-victoria")).isHidden();
            PlaywrightAssertions.assertThat(pagina).hasURL(urlPartida);
        }
    }

    private void verificarResumenFinal(Page ganador, String primerAbandono, String segundoAbandono) {

        PlaywrightAssertions.assertThat(ganador.locator("#modal-victoria"))
            .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15000));
        PlaywrightAssertions.assertThat(ganador.locator("#titulo-finalizacion"))
            .hasText("¡PARTIDA FINALIZADA!");
        PlaywrightAssertions.assertThat(ganador.locator("#ganadores-partida"))
            .hasText("Ganador: " + nombreDe(ganador));
        Locator filas = ganador.locator("#ranking-final > li");
        PlaywrightAssertions.assertThat(filas).hasCount(3);
        PlaywrightAssertions.assertThat(ganador.locator("#ranking-final .resultado-ganador"))
            .hasCount(1);
        PlaywrightAssertions.assertThat(ganador.locator("#ranking-final .resultado-ganador summary"))
            .hasText("1. " + nombreDe(ganador) + " · GANADOR");
        PlaywrightAssertions.assertThat(filas.locator("summary").filter(
            new Locator.FilterOptions().setHasText("GANADOR"))).hasCount(1);
        for (String nombre : List.of(primerAbandono, segundoAbandono)) {
            Locator resumen = filas.locator("summary")
                .filter(new Locator.FilterOptions().setHasText(nombre));
            PlaywrightAssertions.assertThat(resumen).hasCount(1);
            PlaywrightAssertions.assertThat(resumen)
                .hasText(Pattern.compile("^[23]\\. " + nombre + " · Abandonó$"));
            PlaywrightAssertions.assertThat(resumen).isVisible();
        }
        for (Integer indice = 0; indice < 3; indice++) {
            PlaywrightAssertions.assertThat(filas.nth(indice).locator("summary"))
                .hasText(Pattern.compile("^" + (indice + 1) + "\\. .+$"));
        }
    }

    private void verificarAccionesBloqueadas(Page pagina) {
        for (String selector : List.of("#btn-tirar", "#btn-abandonar", "#btn-bono-propiedades")) {
            PlaywrightAssertions.assertThat(pagina.locator(selector)).isDisabled();
        }
        PlaywrightAssertions.assertThat(pagina.locator(".jugadores .indicador-turno")).hasCount(0);
    }

    private String nombreDe(Page pagina) {
        return nombres().get(paginas().indexOf(pagina));
    }
}
