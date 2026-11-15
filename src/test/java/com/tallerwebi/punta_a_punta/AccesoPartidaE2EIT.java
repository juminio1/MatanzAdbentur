package com.tallerwebi.punta_a_punta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.tallerwebi.punta_a_punta.vistas.VistaHome;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AccesoPartidaE2EIT extends EscenarioPartidaE2E {

    final Set<Page> paginasConMensajesDeSala = new HashSet<>();
    final String marcaDocumentoSala = UUID.randomUUID().toString();

    @Test
    void dadoQueTresUsuariosSeRegistranYSeUnenAUnaSalaCuandoInicianUnaPartidaEntoncesTodosIngresanAlTablero() {
        for (Page pagina : paginas()) {
            observarMensajesDeSala(pagina);
        }
        iniciarPartidaConTresJugadores();

        diagnostico("Etapa 7: primer turno y permiso para tirar segun la identidad de cada sesion");
        entoncesTodosIdentificanElMismoPrimerTurno();

        diagnostico("Escenarios completos");
        pausarSiCorresponde("fin");
    }

    private void iniciarPartidaConTresJugadores() {
        diagnostico("Etapa 1: registro e inicio de sesion");
        dadoQueUnUsuarioSeRegistraEIniciaSesion(paginaAnfitrion, ANFITRION, "juli@unlam.edu.ar");
        assertTrue(
            contextoInvitadoUno.cookies().isEmpty(),
            "El login del anfitrion no debe autenticar al invitado"
        );
        assertTrue(
            contextoInvitadoDos.cookies().isEmpty(),
            "El login del anfitrion no debe autenticar al invitado"
        );

        dadoQueUnUsuarioSeRegistraEIniciaSesion(
            paginaInvitadoUno,
            INVITADO_UNO,
            "emi@unlam.edu.ar"
        );
        assertTrue(
            contextoInvitadoDos.cookies().isEmpty(),
            "Los logins previos no deben compartir cookies"
        );

        dadoQueUnUsuarioSeRegistraEIniciaSesion(
            paginaInvitadoDos,
            INVITADO_DOS,
            "may@unlam.edu.ar"
        );
        entoncesLasSesionesSonIndependientes();

        diagnostico("Etapa 2: creacion de sala");
        String codigoSala = cuandoElAnfitrionCreaUnaSala();
        esperarConexionWebSocket(paginaAnfitrion);
        new VistaSalaEspera(paginaAnfitrion).verificarJugadores(ANFITRION);

        diagnostico("Etapas 3 y 4: ingreso y sincronizacion de invitados sin recargar");
        cuandoElInvitadoSeUneALaSala(paginaInvitadoUno, codigoSala);

        esperarConexionWebSocket(paginaInvitadoUno);
        new VistaSalaEspera(paginaAnfitrion).verificarJugadores(ANFITRION, INVITADO_UNO);
        new VistaSalaEspera(paginaInvitadoUno).verificarJugadores(ANFITRION, INVITADO_UNO);

        cuandoElInvitadoSeUneALaSala(paginaInvitadoDos, codigoSala);

        esperarConexionWebSocket(paginaInvitadoDos);

        //Comprobar que los tres se visualizan
        for (Page pagina : paginas()) {
            entoncesAparecenLosTresJugadores(pagina);
            assertEquals(codigoSala, new VistaSalaEspera(pagina).obtenerCodigoSala());
        }
        new VistaSalaEspera(paginaAnfitrion).verificarPermisoParaIniciar(true);
        new VistaSalaEspera(paginaInvitadoUno).verificarPermisoParaIniciar(false);
        new VistaSalaEspera(paginaInvitadoDos).verificarPermisoParaIniciar(false);

        diagnostico("Etapa 5: seleccion y bloqueo de fichas");
        new VistaSalaEspera(paginaAnfitrion).elegirFicha("ROJO");

        for (Page pagina : paginas()) {
            entoncesSeVisualizaLaFicha(pagina, ANFITRION, "ROJO");
        }

        //La ficha ocupada debe quedar bloqueada
        new VistaSalaEspera(paginaInvitadoUno).verificarFichaOcupada("ROJO");
        new VistaSalaEspera(paginaInvitadoDos).verificarFichaOcupada("ROJO");

        // Los invitados seleccionan otras fichas
        new VistaSalaEspera(paginaInvitadoUno).elegirFicha("AZUL");

        new VistaSalaEspera(paginaInvitadoDos).elegirFicha("VERDE");

        //Comprueba la sincronización de fichas
        for (Page pagina : paginas()) {
            entoncesSeVisualizaLaFicha(pagina, INVITADO_UNO, "AZUL");

            entoncesSeVisualizaLaFicha(pagina, INVITADO_DOS, "VERDE");
            entoncesSeVisualizaLaFicha(pagina, ANFITRION, "ROJO");
            assertEquals(
                marcaDocumentoSala,
                pagina.evaluate("() => window.__e2eDocumentoSala"),
                "La sala debe sincronizarse sin reemplazar el documento ni recargar"
            );
            assertTrue(
                paginasConMensajesDeSala.contains(pagina),
                "Cada sesion debe recibir actualizaciones reales de sala por WebSocket"
            );
        }
        new VistaSalaEspera(paginaAnfitrion).verificarFichaOcupada("AZUL");
        new VistaSalaEspera(paginaAnfitrion).verificarFichaOcupada("VERDE");
        new VistaSalaEspera(paginaInvitadoUno).verificarFichaOcupada("VERDE");
        new VistaSalaEspera(paginaInvitadoDos).verificarFichaOcupada("AZUL");
        pausarSiCorresponde("sala");

        diagnostico("Etapa 6: inicio de la misma partida en las tres sesiones");
        iniciarPartidaYEsperarTableros(codigoSala);
        for (Page pagina : paginas()) {
            entoncesSeVisualizaElTableroInicial(pagina);
            assertEquals(codigoSala, pagina.evaluate("() => window.codigoSala"));
        }
    }

    private void esperarConexionWebSocket(Page pagina) {
        new VistaSalaEspera(pagina).esperarConexionWebSocket();
        pagina.evaluate("marca => window.__e2eDocumentoSala = marca", marcaDocumentoSala);
    }

    private void entoncesAparecenLosTresJugadores(Page pagina) {
        new VistaSalaEspera(pagina).verificarJugadores(ANFITRION, INVITADO_UNO, INVITADO_DOS);
    }

    private void entoncesSeVisualizaLaFicha(Page pagina, String nombre, String color) {
        new VistaSalaEspera(pagina).verificarFichaDeJugador(nombre, color);
    }

    private void entoncesSeVisualizaElTableroInicial(Page pagina) {
        PlaywrightAssertions.assertThat(pagina.locator(".tablero")).isVisible();

        PlaywrightAssertions.assertThat(pagina.locator(".jugadores .jugador")).hasCount(3);

        PlaywrightAssertions.assertThat(pagina.locator(".casillero[data-casillero='0'] .ficha")).hasCount(3);
        PlaywrightAssertions.assertThat(pagina.locator(".casillero .ficha")).hasCount(3);

        PlaywrightAssertions.assertThat(pagina.locator(".jugadores .jugador .dinero")).hasCount(3);

        for (Locator dinero : pagina.locator(".jugadores .jugador .dinero").all()) {
            PlaywrightAssertions.assertThat(dinero).hasText("$2.000");
        }

        List<String> colores = List.of("ROJO", "AZUL", "VERDE");
        for (Integer i = 0; i < colores.size(); i++) {
            String color = colores.get(i);
            Locator jugador = pagina.locator(".jugadores .jugador[data-ficha='" + color + "']");
            PlaywrightAssertions.assertThat(jugador).hasCount(1);
            PlaywrightAssertions.assertThat(jugador.locator(".nombre-jugador")).hasText(
                nombres().get(i)
            );
            PlaywrightAssertions.assertThat(
                pagina.locator(
                    ".casillero[data-casillero='0'] .ficha-" + color.toLowerCase(Locale.ROOT)
                )
            ).isVisible();
        }

        PlaywrightAssertions.assertThat(
            pagina.locator(".jugadores .jugador:has(.indicador-turno)")
        ).hasCount(1);
    }

    private void entoncesTodosIdentificanElMismoPrimerTurno() {
        String jugadorEnTurno = obtenerJugadorEnTurno(paginaAnfitrion);

        for (Integer i = 0; i < paginas().size(); i++) {
            Page pagina = paginas().get(i);
            Locator tarjetaPropia = pagina
                .locator(".jugadores .jugador")
                .filter(
                    new Locator.FilterOptions().setHas(
                        pagina.getByText(
                            nombres().get(i),
                            new Page.GetByTextOptions().setExact(true)
                        )
                    )
                );
            assertEquals(
                tarjetaPropia.getAttribute("id"),
                pagina.evaluate("() => String(window.usuarioSesionId)"),
                "El tablero debe conservar la identidad de la sesion"
            );
        }
        verificarPermisoDeTirada(jugadorEnTurno);
    }

    private void entoncesLasSesionesSonIndependientes() {
        Set<String> idsDeSesion = new HashSet<>();
        for (int i = 0; i < paginas().size(); i++) {
            Page pagina = paginas().get(i);
            new VistaHome(pagina).verificarUsuarioAutenticado(nombres().get(i));
            List<String> cookiesDeSesion = pagina
                .context()
                .cookies()
                .stream()
                .filter(cookie -> "JSESSIONID".equals(cookie.name))
                .map(cookie -> cookie.value)
                .toList();
            assertEquals(
                1,
                cookiesDeSesion.size(),
                "Cada jugador debe tener su propia cookie de sesion"
            );
            assertFalse(cookiesDeSesion.get(0).isBlank());
            assertTrue(
                idsDeSesion.add(cookiesDeSesion.get(0)),
                "Las sesiones no deben compartir JSESSIONID"
            );
        }
        assertEquals(3, idsDeSesion.size());
    }

    private void observarMensajesDeSala(Page pagina) {
        // Observa trafico real sin interceptar ni simular mensajes productivos
        pagina.onWebSocket(socket ->
            socket.onFrameReceived(frame -> {
                String texto = frame.text();
                if (
                    texto != null &&
                    texto.contains("MESSAGE") &&
                    texto.contains("destination:/topic/sala/")
                ) {
                    paginasConMensajesDeSala.add(pagina);
                }
            })
        );
    }
}
