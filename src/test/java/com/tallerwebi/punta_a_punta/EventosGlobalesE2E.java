package com.tallerwebi.punta_a_punta;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.FormData;
import com.microsoft.playwright.options.RequestOptions;
import com.tallerwebi.config.DatabaseInitializationConfig;
import com.tallerwebi.config.HibernateConfig;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Usa únicamente la base y el Jetty aislados indicados mediante variables de entorno. */
@EnabledIfEnvironmentVariable(named = "RUN_EVENTOS_E2E", matches = "true")
class EventosGlobalesE2E {
    @Test
    void dadoQueDosClientesRecarganUnResultadoCuandoConfirmanDaComoResultadoUnaReanudacionQueOmiteSoloElTurnoPenalizado() {
        String base = System.getenv().getOrDefault("EVENTOS_E2E_URL", "http://127.0.0.1:18081/spring");
        List<String> errores = new ArrayList<>();
        try (var contexto = new AnnotationConfigApplicationContext(HibernateConfig.class, DatabaseInitializationConfig.class);
                Playwright playwright = Playwright.create();
                Browser navegador = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("msedge").setHeadless(true));
                BrowserContext primerCliente = navegador.newContext();
                BrowserContext segundoCliente = navegador.newContext()) {
            var sesiones = contexto.getBean(SessionFactory.class);
            var transaccion = new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class));
            String codigo = "E2E_" + UUID.randomUUID().toString().substring(0, 8);
            List<Long> ids = transaccion.execute(estado -> crearResultadoPersistido(sesiones, codigo));
            iniciarSesion(primerCliente, base, codigo + "_0");
            iniciarSesion(segundoCliente, base, codigo + "_1");
            Page primera = primerCliente.newPage();
            Page segunda = segundoCliente.newPage();
            primera.onPageError(errores::add);
            segunda.onPageError(errores::add);
            primera.navigate(base + "/partida?codigo=" + codigo);
            segunda.navigate(base + "/partida?codigo=" + codigo);
            assertThat(primera.locator("#aviso-global")).isVisible();
            assertThat(segunda.locator("#global-consecuencia")).containsText("perderá");
            assertThat(primera.locator("#btn-tirar")).isDisabled();
            primera.reload();
            assertThat(primera.locator("#aviso-global")).isVisible();
            assertThat(primera.locator("#cronometro-nro")).hasText("Pausa");
            assertThat(primera.locator("#global-tiradas")).isHidden();
            primera.screenshot(new Page.ScreenshotOptions().setPath(Path.of("target/eventos-globales-e2e.png")));
            APIResponse prohibida = primerCliente.request().post(base + "/partida/tirar-dados?codigoUnico=" + codigo + "&idJugador=" + ids.get(0));
            assertEquals(409, prohibida.status());
            prohibida.dispose();
            primera.locator("#global-continuar").click();
            segunda.locator("#global-continuar").click();
            assertThat(primera.locator("#aviso-global")).isHidden();
            assertThat(segunda.locator("#aviso-global")).isHidden();
            segunda.waitForFunction("() => window.turnoActualId === " + ids.get(1));
            assertThat(segunda.locator("#btn-tirar")).isEnabled();
            APIResponse avance = segundoCliente.request().post(base + "/partida/" + codigo + "/avanzar-turno");
            assertTrue(avance.ok(), avance.text());
            avance.dispose();
            primera.waitForFunction("() => window.turnoActualId === " + ids.get(0));
            assertThat(primera.locator("#btn-tirar")).isEnabled();
            transaccion.executeWithoutResult(estado -> {
                Partida partida = sesiones.getCurrentSession().createQuery("FROM Partida WHERE codigoUnico = :codigo", Partida.class)
                        .setParameter("codigo", codigo).getSingleResult();
                assertFalse(partida.getEventoGlobal().estaActivo());
                assertEquals(6, partida.getRondaActual());
                assertFalse(sesiones.getCurrentSession().find(Jugador.class, ids.get(0)).getPierdeTurno());
                partida.setEstado(EstadoPartida.FINALIZADA);
            });
            assertEquals(List.of(), errores);
        }
    }

    private List<Long> crearResultadoPersistido(SessionFactory sesiones, String codigo) {
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setEstado(EstadoPartida.EN_CURSO);
        partida.setRondaActual(5);
        for (int indice = 0; indice < 2; indice++) {
            Usuario usuario = new Usuario();
            usuario.setUsername(codigo + "_" + indice);
            usuario.setEmail(usuario.getUsername() + "@example.com");
            usuario.setPassword("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
            sesiones.getCurrentSession().persist(usuario);
            if (indice == 0) partida.setCreador(usuario);
            Jugador jugador = new Jugador();
            jugador.setApodo(usuario.getUsername());
            jugador.setOrden(indice);
            jugador.setFicha(ColorFicha.values()[indice]);
            jugador.setDinero(2000);
            jugador.setUltimaRondaConOportunidad(4);
            partida.getJugadores().add(jugador);
        }
        sesiones.getCurrentSession().persist(partida);
        EventoGlobal evento = sesiones.getCurrentSession().createQuery("FROM EventoGlobal WHERE tipoEventoGlobal = :tipo", EventoGlobal.class)
                .setParameter("tipo", TipoEventoGlobal.ACCIDENTE_SARMIENTO).getSingleResult();
        List<Long> ids = partida.getJugadores().stream().map(Jugador::getId).toList();
        partida.getEventoGlobal().iniciar(evento, 5, new HashSet<>(ids));
        partida.getJugadores().get(0).setPierdeTurno(true);
        partida.getEventoGlobal().resolver(ids.get(0), "El jugador perderá su próxima oportunidad por el accidente del Sarmiento.");
        ReflectionTestUtils.setField(partida.getEventoGlobal(), "vencimientoEvento", Instant.now().plusSeconds(180));
        return ids;
    }

    private void iniciarSesion(BrowserContext cliente, String base, String nombre) {
        APIResponse respuesta = cliente.request().post(base + "/validar-login", RequestOptions.create()
                .setForm(FormData.create().set("credencial", nombre).set("password", "test")));
        assertTrue(respuesta.ok(), respuesta.text());
        respuesta.dispose();
    }
}
