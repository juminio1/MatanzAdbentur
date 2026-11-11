package com.tallerwebi.punta_a_punta;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.tallerwebi.config.DatabaseInitializationConfig;
import com.tallerwebi.config.HibernateConfig;
import com.tallerwebi.dominio.catalogo.CatalogoTablero;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.enums.*;
import java.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** E2E 4: eventos, cárcel y quiebra. Usa la base y el Jetty aislados de las variables de entorno. */
@EnabledIfEnvironmentVariable(named = "RUN_EVENTOS_E2E", matches = "true")
class EventosCarcelQuiebraE2E {
    private static final int CANTIDAD_JUGADORES = 3;
    private static final int DINERO_INICIAL = 2000;
    private static final int POSICION_ANTES_DE_EVENTO = 4;
    private static final int MAXIMO_INTENTOS_TIRADA = 25;
    private static final double ESPERA_ANIMACIONES_MS = 60_000;
    private static final String HASH_TEST = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";

    @Test
    void dadoQueUnaPartidaEstaEnCursoCuandoLosJugadoresActivanEventosYCumplenPenalizacionesEntoncesSeAplicanLasReglasCorrespondientes() {
        String base = System.getenv().getOrDefault("EVENTOS_E2E_URL", "http://127.0.0.1:18081/spring");
        List<String> errores = new ArrayList<>();
        try (var contexto = new AnnotationConfigApplicationContext(HibernateConfig.class, DatabaseInitializationConfig.class);
                Playwright playwright = Playwright.create();
                Browser navegador = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("msedge").setHeadless(true))) {
            var sesiones = contexto.getBean(SessionFactory.class);
            var transaccion = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
            String codigo = "E2E4_" + UUID.randomUUID().toString().substring(0, 8);
            List<Long> ids = transaccion.execute(estado -> crearPartidaEnCurso(sesiones, codigo));

            // Cada jugador entra a la partida desde su propio navegador.
            List<Page> paginas = new ArrayList<>();
            for (int indice = 0; indice < CANTIDAD_JUGADORES; indice++) {
                BrowserContext cliente = navegador.newContext();
                iniciarSesion(cliente, base, codigo + "_" + indice);
                Page pagina = cliente.newPage();
                pagina.onPageError(errores::add);
                pagina.navigate(base + "/partida?codigo=" + codigo);
                paginas.add(pagina);
            }

            // Todos ven el tablero y el turno es del primer jugador.
            for (Page pagina : paginas) {
                pagina.waitForFunction("() => window.turnoActualId === " + ids.get(0));
            }
            assertThat(paginas.get(0).locator("#btn-tirar")).isEnabled();
            assertThat(paginas.get(1).locator("#btn-tirar")).isDisabled();
            assertThat(paginas.get(2).locator("#btn-tirar")).isDisabled();

            // El primer jugador cae en un casillero de evento individual y ve su descripción.
            JsonNode tirada = tirarHastaCaerEnEvento(transaccion, sesiones, paginas.get(0), base, codigo, ids.get(0));
            String nombreEvento = tirada.get("nombreEvento").asText();
            assertThat(paginas.get(0).locator("#aviso-evento-nombre")).hasText(nombreEvento);
            assertThat(paginas.get(0).locator("#aviso-evento-descripcion"))
                    .hasText(tirada.get("descripcionEvento").asText());
            for (Page pagina : paginas) {
                assertThat(pagina.locator(".historial__lista")).containsText("sacó el evento «" + nombreEvento + "»",
                        new LocatorAssertions.ContainsTextOptions().setTimeout(ESPERA_ANIMACIONES_MS));
            }
            transaccion.executeWithoutResult(estado -> verificarEfectoDelEvento(sesiones, ids.get(0), tirada));

            // Próximos pasos: eventos globales, cárcel y quiebra.

            transaccion.executeWithoutResult(estado -> finalizarPartida(sesiones, codigo));
            assertEquals(List.of(), errores);
        }
    }

    private List<Long> crearPartidaEnCurso(SessionFactory sesiones, String codigo) {
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setRondaActual(1);
        for (int indice = 0; indice < CANTIDAD_JUGADORES; indice++) {
            Usuario usuario = new Usuario();
            usuario.setUsername(codigo + "_" + indice);
            usuario.setEmail(usuario.getUsername() + "@example.com");
            usuario.setPassword(HASH_TEST);
            sesiones.getCurrentSession().persist(usuario);
            if (indice == 0) partida.setCreador(usuario);
            Jugador jugador = new Jugador();
            jugador.setApodo(usuario.getUsername());
            jugador.setOrden(indice);
            jugador.setFicha(ColorFicha.values()[indice]);
            jugador.setDinero(DINERO_INICIAL);
            partida.getJugadores().add(jugador);
        }
        sesiones.getCurrentSession().persist(partida);
        return partida.getJugadores().stream().map(Jugador::getId).toList();
    }

    /**
     * Los dados son aleatorios: se tira desde la posición 4 (cae en evento con 6 o 10) y, si no cae,
     * se devuelve al jugador a esa posición y se vuelve a tirar. Todo debe entrar en el turno de 60 s.
     */
    private JsonNode tirarHastaCaerEnEvento(TransactionTemplate transaccion, SessionFactory sesiones, Page pagina,
            String base, String codigo, Long idJugador) {
        ObjectMapper json = new ObjectMapper();
        for (int intento = 0; intento < MAXIMO_INTENTOS_TIRADA; intento++) {
            transaccion.executeWithoutResult(estado -> ubicarParaTirar(sesiones, idJugador, POSICION_ANTES_DE_EVENTO));
            // Se espera a que la página vuelva a escuchar la partida para recibir el aviso del evento.
            pagina.waitForConsoleMessage(new Page.WaitForConsoleMessageOptions()
                    .setPredicate(mensaje -> mensaje.text().contains("WebSocket conectado")), pagina::reload);
            pagina.waitForFunction("() => window.turnoActualId === " + idJugador);
            APIResponse respuesta = pagina.context().request()
                    .post(base + "/partida/tirar-dados?codigoUnico=" + codigo + "&idJugador=" + idJugador);
            assertTrue(respuesta.ok(), respuesta.text());
            String cuerpo = respuesta.text();
            respuesta.dispose();
            try {
                JsonNode tirada = json.readTree(cuerpo);
                if (tirada.hasNonNull("nombreEvento")) return tirada;
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Respuesta de tirada inválida: " + cuerpo, e);
            }
        }
        return fail("El jugador no cayó en un casillero de evento en " + MAXIMO_INTENTOS_TIRADA + " intentos");
    }

    private void ubicarParaTirar(SessionFactory sesiones, Long idJugador, int posicion) {
        Jugador jugador = sesiones.getCurrentSession().find(Jugador.class, idJugador);
        jugador.setPosicionActual(posicion);
        jugador.setDinero(DINERO_INICIAL);
        jugador.setTiroEnTurno(false);
        jugador.getEstadoCarcel().liberar();
    }

    private void verificarEfectoDelEvento(SessionFactory sesiones, Long idJugador, JsonNode tirada) {
        EventoCasillero evento = sesiones.getCurrentSession()
                .createQuery("FROM EventoCasillero WHERE nombre = :nombre", EventoCasillero.class)
                .setParameter("nombre", tirada.get("nombreEvento").asText()).getSingleResult();
        Jugador jugador = sesiones.getCurrentSession().find(Jugador.class, idJugador);
        int caida = tirada.get("posicionCaidaEvento").asInt();
        switch (evento.getTipoEfecto()) {
            case SUMA -> assertEquals(DINERO_INICIAL + evento.getMonto(), jugador.getDinero());
            case RESTA -> assertEquals(DINERO_INICIAL - evento.getMonto(), jugador.getDinero());
            case PIERDE_TURNO -> assertTrue(jugador.getPierdeTurno());
            case AVANZA_CASILLEROS -> assertEquals((caida + evento.getCantidadCasilleros()) % CatalogoTablero.TOTAL_CASILLEROS,
                    jugador.getPosicionActual());
            case AVANZA_A_POSICION -> assertEquals(evento.getPosicionDestino(), jugador.getPosicionActual());
        }
    }

    private void finalizarPartida(SessionFactory sesiones, String codigo) {
        sesiones.getCurrentSession().createQuery("FROM Partida WHERE codigoUnico = :codigo", Partida.class)
                .setParameter("codigo", codigo).getSingleResult().setEstado(EstadoPartida.FINALIZADA);
    }

    private void iniciarSesion(BrowserContext cliente, String base, String nombre) {
        APIResponse respuesta = cliente.request().post(base + "/validar-login", RequestOptions.create()
                .setForm(FormData.create().set("credencial", nombre).set("password", "test")));
        assertTrue(respuesta.ok(), respuesta.text());
        respuesta.dispose();
    }
}
