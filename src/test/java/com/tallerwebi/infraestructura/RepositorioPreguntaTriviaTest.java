package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.PreguntaTrivia;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.DificultadTrivia;
import com.tallerwebi.dominio.excepcion.ClienteIAException;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.ServicioPreguntasTrivia;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.net.http.HttpTimeoutException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
class RepositorioPreguntaTriviaTest {

    private static final List<String> OPCIONES = List.of("A", "B", "C", "D");
    private static final String CODIGO_PARTIDA = "TRIVIA";

    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private PlatformTransactionManager administradorTransacciones;

    private RepositorioPreguntaTrivia banco;
    private RepositorioPartida partidas;

    @BeforeEach
    void preparar() {
        banco = new RepositorioPreguntaTriviaImpl(sessionFactory);
        partidas = new RepositorioPartidaImpl(sessionFactory);
    }

    @Test
    void guardaYRecuperaLasOpcionesYLaRespuestaSegunLaDificultad() {
        guardar("Pregunta simple", DificultadTrivia.SIMPLE);
        guardar("Pregunta media", DificultadTrivia.MEDIA);
        guardar("Pregunta complicada", DificultadTrivia.COMPLICADA);
        limpiarSesion();

        List<PreguntaTrivia> resultado = banco.buscarPorDificultad(DificultadTrivia.MEDIA);
        assertEquals(1, resultado.size());
        PreguntaTrivia recuperada = resultado.getFirst();
        assertNotNull(recuperada.getId());
        assertEquals("Pregunta media", recuperada.getEnunciado());
        assertEquals(OPCIONES, recuperada.getOpciones());
        assertTrue(recuperada.esRespuestaCorrecta(2));
        assertFalse(recuperada.esRespuestaCorrecta(1));
    }

    @Test
    void obtieneHastaCincoDelBancoSiGeminiAgotaElTiempoDeEspera() {
        crearPartida(CODIGO_PARTIDA);
        IntStream.range(0, 6).forEach(i -> guardar("Pregunta simple " + i, DificultadTrivia.SIMPLE));
        guardar("Pregunta media", DificultadTrivia.MEDIA);

        List<PreguntaTrivia> resultado = servicioConTimeout()
            .obtenerParaCasillero(CODIGO_PARTIDA, DificultadRuleta.FACIL);

        assertEquals(5, resultado.size());
        assertTrue(resultado.stream().allMatch(p -> p.getDificultad() == DificultadTrivia.SIMPLE));
        assertEquals(5, new HashSet<>(enunciados(resultado)).size());
    }

    @Test
    void usaUnaPreguntaComplicadaDelBancoCuandoGeminiDevuelveUnaPreguntaInvalida() {
        crearPartida(CODIGO_PARTIDA);
        guardar("Pregunta media", DificultadTrivia.MEDIA);
        guardar("Pregunta complicada", DificultadTrivia.COMPLICADA);
        ServicioPreguntasTrivia servicio = servicioCon((dificultad, cantidad) -> {
            assertEquals("DIFICIL", dificultad);
            assertEquals(1, cantidad);
            return List.of(new PreguntaGenerada(dificultad, "Inválida", OPCIONES, 4));
        });

        assertEquals(List.of("Pregunta complicada"), enunciados(servicio.obtenerParaCarcel(CODIGO_PARTIDA)));
    }

    @Test
    void devuelveLasDisponiblesYUnaListaVaciaCuandoSeAgotaLaDificultad() {
        crearPartida(CODIGO_PARTIDA);
        guardar("Primera", DificultadTrivia.MEDIA);
        guardar("Segunda", DificultadTrivia.MEDIA);
        guardar("De otra dificultad", DificultadTrivia.SIMPLE);
        ServicioPreguntasTrivia servicio = servicioConTimeout();

        assertEquals(2, servicio.obtenerParaCasillero(CODIGO_PARTIDA, DificultadRuleta.MEDIO).size());
        limpiarSesion();
        assertTrue(servicio.obtenerParaCasillero(CODIGO_PARTIDA, DificultadRuleta.MEDIO).isEmpty());
    }

    @Test
    void unaPreguntaGeneradaNoPuedeRepetirseDesdeElBancoEnLaMismaPartida() {
        crearPartida(CODIGO_PARTIDA);
        crearPartida("OTRA-TRIVIA");
        guardar("¿Cuál es el río?", DificultadTrivia.COMPLICADA);
        ServicioPreguntasTrivia conGemini = servicioCon((dificultad, cantidad) ->
            List.of(new PreguntaGenerada(dificultad, "CUAL es el rio", OPCIONES, 1)));

        assertEquals(List.of("CUAL es el rio"), enunciados(conGemini.obtenerParaCarcel(CODIGO_PARTIDA)));
        limpiarSesion();

        assertTrue(conGemini.obtenerParaCarcel(CODIGO_PARTIDA).isEmpty());
        ServicioPreguntasTrivia respaldo = servicioConTimeout();
        assertTrue(respaldo.obtenerParaCarcel(CODIGO_PARTIDA).isEmpty());
        assertEquals(List.of("¿Cuál es el río?"), enunciados(respaldo.obtenerParaCarcel("OTRA-TRIVIA")));
    }

    @Test
    void lasModalidadesCompartenElHistorialDePreguntas() {
        crearPartida(CODIGO_PARTIDA);
        guardar("Primera", DificultadTrivia.COMPLICADA);
        guardar("Segunda", DificultadTrivia.COMPLICADA);
        ServicioPreguntasTrivia servicio = servicioConTimeout();

        String usadaEnCarcel = servicio.obtenerParaCarcel(CODIGO_PARTIDA).getFirst().getEnunciado();
        limpiarSesion();
        List<String> usadasEnCasillero = enunciados(
            servicio.obtenerParaCasillero(CODIGO_PARTIDA, DificultadRuleta.DIFICIL));

        assertEquals(1, usadasEnCasillero.size());
        assertFalse(usadasEnCasillero.contains(usadaEnCarcel));
    }

    @Test
    void serializaLasOpcionesSinLaRespuestaCorrecta() throws Exception {
        PreguntaTrivia pregunta = new PreguntaTrivia("Pregunta", OPCIONES, 2, DificultadTrivia.SIMPLE);
        ObjectMapper mapper = new ObjectMapper();
        var json = mapper.readTree(mapper.writeValueAsString(pregunta));

        assertEquals("Pregunta", json.get("enunciado").asText());
        assertEquals(4, json.get("opciones").size());
        assertFalse(json.has("respuestaCorrecta"));
    }

    private ServicioPreguntasTrivia servicioConTimeout() {
        return servicioCon((dificultad, cantidad) -> {
            throw new ClienteIAException("Gemini no respondió", new HttpTimeoutException("Tiempo de espera agotado"));
        });
    }

    private ServicioPreguntasTrivia servicioCon(ClienteIA cliente) {
        return new ServicioPreguntasTrivia(cliente, banco, partidas, administradorTransacciones);
    }

    private List<String> enunciados(List<PreguntaTrivia> preguntas) {
        return preguntas.stream().map(PreguntaTrivia::getEnunciado).toList();
    }

    private void guardar(String enunciado, DificultadTrivia dificultad) {
        banco.guardar(new PreguntaTrivia(enunciado, OPCIONES, 2, dificultad));
    }

    private void crearPartida(String codigo) {
        Usuario creador = new Usuario();
        creador.setEmail(codigo + "@trivia.test");
        creador.setUsername(codigo);
        creador.setPassword("Clave123!");
        sessionFactory.getCurrentSession().persist(creador);
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setCreador(creador);
        partidas.guardarPartida(partida);
    }

    private void limpiarSesion() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }
}
