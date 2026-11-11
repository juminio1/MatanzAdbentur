package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.FinalizarPartida;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoParticipante;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
@Rollback
public class RepositorioFinalizarPartidaTest {
    @Autowired
    private SessionFactory sessionFactory;

    private RepositorioFinalizarPartida repositorio;

    @BeforeEach
    public void preparar() {
        repositorio = new RepositorioFinalizarPartidaImpl(sessionFactory);
    }

    @Test
    public void guardaYRecuperaTodosLosDatosDespuesDeVaciarLaSesion() {
        Partida partida = crearPartida("RESULTADO");
        Jugador jugador = partida.getJugadores().get(0);
        FinalizarPartida resultado = crearResultado(partida, jugador);

        repositorio.guardar(resultado);
        Long id = resultado.getId();
        vaciarSesion();

        FinalizarPartida recuperado = repositorio.buscarPorPartidaYJugador(
                partida.getId(), jugador.getId());

        assertNotNull(id);
        assertNotNull(recuperado);
        assertEquals(id, recuperado.getId());
        assertEquals(partida.getId(), recuperado.getPartida().getId());
        assertEquals(jugador.getId(), recuperado.getJugador().getId());
        assertEquals(100, recuperado.getDineroFinal());
        assertEquals(1500, recuperado.getValorPropiedades());
        assertEquals(2000, recuperado.getDeudaPendiente());
        assertEquals(-400, recuperado.getPatrimonioTotal());
        assertEquals(false, recuperado.getGanador());
        assertEquals(EstadoParticipante.QUEBRADO, recuperado.getEstadoParticipante());
        String estadoPersistido = sessionFactory.getCurrentSession()
                .createNativeQuery("SELECT estado_participante FROM finalizar_partida WHERE id = :id",
                        String.class)
                .setParameter("id", id)
                .getSingleResult();
        assertEquals("QUEBRADO", estadoPersistido);
    }

    @Test
    public void rechazaResultadosDuplicadosParaElMismoJugadorYPartida() {
        Partida partida = crearPartida("DUPLICADO");
        Jugador jugador = partida.getJugadores().get(0);
        repositorio.guardar(crearResultado(partida, jugador));
        sessionFactory.getCurrentSession().flush();

        assertThrows(ConstraintViolationException.class, () -> {
            repositorio.guardar(crearResultado(partida, jugador));
            sessionFactory.getCurrentSession().flush();
        });
    }

    @Test
    public void permiteResultadosDeDistintosJugadoresDeLaMismaPartida() {
        Partida partida = crearPartida("DISTINTOS");
        Jugador primero = partida.getJugadores().get(0);
        Jugador segundo = partida.getJugadores().get(1);
        repositorio.guardar(crearResultado(partida, primero));
        repositorio.guardar(crearResultado(partida, segundo));
        vaciarSesion();

        assertEquals(2, repositorio.buscarPorPartida(partida.getId()).size());
        assertNotNull(repositorio.buscarPorPartidaYJugador(partida.getId(), primero.getId()));
        assertNotNull(repositorio.buscarPorPartidaYJugador(partida.getId(), segundo.getId()));
    }

    @Test
    public void devuelveNullCuandoNoExisteElResultado() {
        Partida partida = crearPartida("SIN_RESULTADO");
        Jugador jugador = partida.getJugadores().get(0);
        repositorio.guardar(crearResultado(partida, jugador));
        vaciarSesion();

        assertNull(repositorio.buscarPorPartidaYJugador(partida.getId(), -1L));
        assertNull(repositorio.buscarPorPartidaYJugador(-1L, jugador.getId()));
    }

    @Test
    public void listaSolamenteResultadosDeLaPartidaConsultadaAunqueEsteFinalizada() {
        Partida primera = crearPartida("PRIMERA");
        primera.setEstado(EstadoPartida.FINALIZADA);
        Partida segunda = crearPartida("SEGUNDA");
        FinalizarPartida esperado = crearResultado(primera, primera.getJugadores().get(0));
        repositorio.guardar(esperado);
        repositorio.guardar(crearResultado(segunda, segunda.getJugadores().get(0)));
        vaciarSesion();

        List<FinalizarPartida> resultados = repositorio.buscarPorPartida(primera.getId());

        assertEquals(1, resultados.size());
        assertEquals(esperado.getId(), resultados.get(0).getId());
        assertEquals(primera.getId(), resultados.get(0).getPartida().getId());
    }

    @Test
    public void devuelveListaVaciaSiLaPartidaNoTieneResultados() {
        Partida conResultados = crearPartida("CON_RESULTADOS");
        Partida sinResultados = crearPartida("SIN_RESULTADOS");
        repositorio.guardar(crearResultado(conResultados, conResultados.getJugadores().get(0)));
        vaciarSesion();

        assertTrue(repositorio.buscarPorPartida(sinResultados.getId()).isEmpty());
    }

    @Test
    public void persisteCantidadesHistoricas() {
        Partida partida = crearPartida("CANTIDADES");
        Jugador jugador = partida.getJugadores().get(0);
        FinalizarPartida resultado = crearResultado(partida, jugador);
        resultado.setCantidadPropiedades(2);
        resultado.setCantidadMejoras(5);
        repositorio.guardar(resultado);
        vaciarSesion();

        FinalizarPartida recuperado = repositorio.buscarPorPartidaYJugador(
                partida.getId(), jugador.getId());

        assertEquals(2, recuperado.getCantidadPropiedades());
        assertEquals(5, recuperado.getCantidadMejoras());
    }

    @Test
    public void resultadoAnteriorConservaCantidadesDesconocidas() {
        Partida partida = crearPartida("HISTORICO");
        Jugador jugador = partida.getJugadores().get(0);
        repositorio.guardar(crearResultado(partida, jugador));
        vaciarSesion();

        FinalizarPartida recuperado = repositorio.buscarPorPartidaYJugador(
                partida.getId(), jugador.getId());

        assertNull(recuperado.getCantidadPropiedades());
        assertNull(recuperado.getCantidadMejoras());
    }

    private Partida crearPartida(String codigo) {
        Usuario creador = new Usuario();
        creador.setEmail(codigo + "@test.com");
        creador.setUsername(codigo);
        sessionFactory.getCurrentSession().persist(creador);
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setCreador(creador);
        partida.getJugadores().add(new Jugador());
        partida.getJugadores().add(new Jugador());
        sessionFactory.getCurrentSession().persist(partida);
        return partida;
    }

    private FinalizarPartida crearResultado(Partida partida, Jugador jugador) {
        FinalizarPartida resultado = new FinalizarPartida();
        resultado.setPartida(partida);
        resultado.setJugador(jugador);
        resultado.setDineroFinal(100);
        resultado.setValorPropiedades(1500);
        resultado.setDeudaPendiente(2000);
        resultado.setPatrimonioTotal(-400);
        resultado.setEstadoParticipante(EstadoParticipante.QUEBRADO);
        return resultado;
    }

    private void vaciarSesion() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }
}
