package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.time.Instant;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HibernateInfraestructuraTestConfig.class })
public class RepositorioPartidaTest {

    @Autowired
    private SessionFactory sessionFactory;

    private RepositorioPartida repositorioPartida;

    @BeforeEach
    public void init() {
        this.repositorioPartida = new RepositorioPartidaImpl(sessionFactory);
    }

    @Test
    @Transactional
    @Rollback
    public void devuelveNullCuandoNoHayPartidasActivasNiJugadorConElIdBuscado() {
        assertThat(repositorioPartida.buscarPartidaActiva(), is(nullValue()));
        assertThat(repositorioPartida.buscarJugadorPorId(-1L), is(nullValue()));
    }

    @Test
    @Transactional
    @Rollback
    public void encuentraUnJugadorPorIdSinAlterarSusDatos() {
        Jugador jugador = new Jugador();
        jugador.setApodo("Jugador de prueba");
        jugador.setDinero(700);
        sessionFactory.getCurrentSession().persist(jugador);

        Jugador encontrado = repositorioPartida.buscarJugadorPorId(jugador.getId());

        assertThat(encontrado.getId(), is(jugador.getId()));
        assertThat(encontrado.getDinero(), is(700));
    }

    @Test
    @Transactional
    @Rollback
    public void ambasBusquedasDePartidaActivaExcluyenLasFinalizadas() {
        Usuario creador = new Usuario();
        creador.setEmail("busqueda@test.com");
        creador.setPassword("Clave123!");
        creador.setUsername("HostBusqueda");
        sessionFactory.getCurrentSession().persist(creador);
        Partida finalizada = new Partida();
        finalizada.setCodigoUnico("BUSQUEDA");
        finalizada.setCreador(creador);
        finalizada.setEstado(EstadoPartida.FINALIZADA);
        repositorioPartida.guardarPartida(finalizada);
        Partida activa = new Partida();
        activa.setCodigoUnico("BUSQUEDA");
        activa.setCreador(creador);
        activa.setEstado(EstadoPartida.EN_CURSO);
        repositorioPartida.guardarPartida(activa);

        assertThat(repositorioPartida.buscarPartidaActiva().getId(), is(activa.getId()));
        assertThat(repositorioPartida.buscarPartidaActivaPorCodigoUnico("BUSQUEDA").getId(), is(activa.getId()));
    }

    @Test
    @Transactional
    @Rollback
    public void deberiaGuardarUnaPartidaVinculadaAlUsuarioCreador() {
        Usuario creador = new Usuario();
        creador.setEmail("creador@matancero.com");
        creador.setPassword("Clave123!");
        creador.setUsername("ElHostMatancero");
        creador.setRol("JUGADOR");
        this.sessionFactory.getCurrentSession().persist(creador);

        Partida partida = new Partida();
        partida.setCodigoUnico("MAT123");
        partida.setEstado(EstadoPartida.EN_ESPERA);
        partida.setTiempoInicio(Instant.now());
        partida.setCreador(creador);

        this.repositorioPartida.guardarPartida(partida);

        Partida partidaObtenida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(
            "MAT123"
        );

        assertThat(partidaObtenida, is(notNullValue()));
        assertThat(partidaObtenida.getCreador(), is(notNullValue()));
        assertThat(partidaObtenida.getCreador().getId(), is(equalTo(creador.getId())));
        assertThat(partidaObtenida.getCreador().getUsername(), is(equalTo("ElHostMatancero")));
    }

    @Test
    @Transactional
    public void deberiaRetornarNullSiLaPartidaPorCodigoNoExiste() {
        Partida partidaObtenida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(
            "NO_EXISTE"
        );

        assertThat(partidaObtenida, is(nullValue()));
    }

    @Test
    @Transactional
    @Rollback
    public void noDeberiaEncontrarUnaPartidaSiEstaFinalizada() {
        Usuario creador = new Usuario();
        creador.setEmail("finalizada@test.com");
        creador.setPassword("Clave123!");
        creador.setUsername("HostFinalizado");
        creador.setRol("JUGADOR");
        this.sessionFactory.getCurrentSession().persist(creador);

        Partida partida = new Partida();
        partida.setCodigoUnico("FIN123");
        partida.setEstado(EstadoPartida.FINALIZADA);
        partida.setTiempoInicio(Instant.now());
        partida.setCreador(creador);

        this.repositorioPartida.guardarPartida(partida);

        Partida partidaObtenida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico(
            "FIN123"
        );

        assertThat(partidaObtenida, is(nullValue()));
    }

    @Test
    @Transactional
    public void noDeberiaGuardarUnaPartidaSinCreador() {
        // Arrange: Creamos la partida pero omitimos intencionalmente setearle el creador
        Partida partidaSinCreador = new Partida();
        partidaSinCreador.setCodigoUnico("FANTASMA");
        partidaSinCreador.setEstado(EstadoPartida.EN_ESPERA);

        assertThrows(Exception.class, () -> {
            this.repositorioPartida.guardarPartida(partidaSinCreador);
            this.sessionFactory.getCurrentSession().flush();
        });
    }

    @Test
    @Transactional
    @Rollback
    public void encuentraLaPartidaEnCursoDeUnJugador() {
        Partida partida = crearPartidaConJugador("JUGADOR-CURSO", EstadoPartida.EN_CURSO);
        verificarPartidaDelJugador(partida);
    }

    @Test
    @Transactional
    @Rollback
    public void encuentraLaPartidaFinalizadaDeUnJugador() {
        Partida partida = crearPartidaConJugador("JUGADOR-FINALIZADA", EstadoPartida.FINALIZADA);
        verificarPartidaDelJugador(partida);
    }

    @Test
    @Transactional
    @Rollback
    public void encuentraLaPartidaEnEsperaDeUnJugador() {
        Partida partida = crearPartidaConJugador("JUGADOR-ESPERA", EstadoPartida.EN_ESPERA);
        verificarPartidaDelJugador(partida);
    }

    @Test
    @Transactional
    @Rollback
    public void devuelveNullSiElJugadorNoPerteneceANingunaPartida() {
        crearPartidaConJugador("OTRA-PARTIDA", EstadoPartida.EN_CURSO);
        Jugador sinPartida = new Jugador();
        sessionFactory.getCurrentSession().persist(sinPartida);
        Long idJugador = sinPartida.getId();
        vaciarSesionParaBuscarPartida();

        assertThat(repositorioPartida.buscarPartidaPorJugador(idJugador), is(nullValue()));
    }

    @Test
    @Transactional
    @Rollback
    public void noConfundeLaPartidaDelJugadorConOtraPartidaActiva() {
        Partida otra = crearPartidaConJugador("OTRA-ACTIVA", EstadoPartida.EN_CURSO);
        Partida buscada = crearPartidaConJugador("BUSCADA-FINALIZADA", EstadoPartida.FINALIZADA);
        Long idOtroJugador = otra.getJugadores().get(0).getId();
        Long idJugador = buscada.getJugadores().get(0).getId();
        vaciarSesionParaBuscarPartida();

        Partida encontrada = repositorioPartida.buscarPartidaPorJugador(idJugador);

        assertThat(encontrada, is(notNullValue()));
        assertThat(encontrada.getId(), is(buscada.getId()));
        assertThat(encontrada.getEstado(), is(EstadoPartida.FINALIZADA));
        assertThat(repositorioPartida.buscarPartidaPorJugador(idOtroJugador).getId(), is(otra.getId()));
    }

    @Test
    @Transactional
    @Rollback
    public void encuentraPorCodigoUnaPartidaEnCurso() {
        verificarBusquedaPorCodigo(EstadoPartida.EN_CURSO);
    }

    @Test
    @Transactional
    @Rollback
    public void encuentraPorCodigoUnaPartidaFinalizada() {
        verificarBusquedaPorCodigo(EstadoPartida.FINALIZADA);
    }

    @Test
    @Transactional
    @Rollback
    public void encuentraPorCodigoUnaPartidaEnEspera() {
        verificarBusquedaPorCodigo(EstadoPartida.EN_ESPERA);
    }

    private void verificarBusquedaPorCodigo(EstadoPartida estado) {
        crearPartidaConJugador("OTRA", EstadoPartida.EN_CURSO);
        Partida esperada = crearPartidaConJugador("POR-CODIGO", estado);
        vaciarSesionParaBuscarPartida();

        Partida encontrada = repositorioPartida.buscarPartidaPorCodigoUnico("POR-CODIGO");

        assertThat(encontrada, is(notNullValue()));
        assertThat(encontrada.getId(), is(esperada.getId()));
        assertThat(encontrada.getEstado(), is(estado));
    }

    @Test
    @Transactional
    @Rollback
    public void busquedaSinFiltroDevuelveNullParaCodigoInexistente() {
        crearPartidaConJugador("EXISTENTE", EstadoPartida.EN_CURSO);
        vaciarSesionParaBuscarPartida();

        assertThat(repositorioPartida.buscarPartidaPorCodigoUnico("INEXISTENTE"), is(nullValue()));
    }

    @Test
    @Transactional
    @Rollback
    public void busquedaActivaSigueExcluyendoLaFinalizadaConsultadaSinFiltro() {
        Partida finalizada = crearPartidaConJugador("FINALIZADA", EstadoPartida.FINALIZADA);
        vaciarSesionParaBuscarPartida();

        assertThat(repositorioPartida.buscarPartidaPorCodigoUnico("FINALIZADA").getId(),
                is(finalizada.getId()));
        assertThat(repositorioPartida.buscarPartidaActivaPorCodigoUnico("FINALIZADA"), is(nullValue()));
        assertThat(repositorioPartida.buscarPartidaActiva(), is(nullValue()));
    }

    @Test
    @Transactional
    @Rollback
    public void consultarPorCodigoNoModificaLosDatosPersistidos() {
        Partida original = crearPartidaConJugador("INMUTABLE", EstadoPartida.FINALIZADA);
        Instant inicio = Instant.parse("2026-10-31T12:00:00Z");
        original.setTiempoInicio(inicio);
        original.setRondaActual(31);
        original.setTurnosFinalizadosEnRonda(2);
        original.setIndiceTurnoActual(3);
        Jugador jugador = original.getJugadores().get(0);
        jugador.setApodo("Historico");
        jugador.setDinero(700);
        Long idCreador = original.getCreador().getId();
        Long idJugador = jugador.getId();
        vaciarSesionParaBuscarPartida();

        Partida encontrada = repositorioPartida.buscarPartidaPorCodigoUnico("INMUTABLE");

        assertThat(encontrada.getId(), is(original.getId()));
        vaciarSesionParaBuscarPartida();
        Partida persistida = sessionFactory.getCurrentSession().find(Partida.class, original.getId());
        assertThat(persistida.getEstado(), is(EstadoPartida.FINALIZADA));
        assertThat(persistida.getCodigoUnico(), is("INMUTABLE"));
        assertThat(persistida.getTiempoInicio(), is(inicio));
        assertThat(persistida.getRondaActual(), is(31));
        assertThat(persistida.getTurnosFinalizadosEnRonda(), is(2));
        assertThat(persistida.getIndiceTurnoActual(), is(3));
        assertThat(persistida.getCreador().getId(), is(idCreador));
        assertThat(persistida.getJugadores().size(), is(1));
        assertThat(persistida.getJugadores().get(0).getId(), is(idJugador));
        assertThat(persistida.getJugadores().get(0).getApodo(), is("Historico"));
        assertThat(persistida.getJugadores().get(0).getDinero(), is(700));
    }

    private Partida crearPartidaConJugador(String codigo, EstadoPartida estado) {
        Usuario creador = new Usuario();
        creador.setEmail(codigo + "@test.com");
        creador.setUsername(codigo);
        sessionFactory.getCurrentSession().persist(creador);
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setEstado(estado);
        partida.setCreador(creador);
        partida.getJugadores().add(new Jugador());
        repositorioPartida.guardarPartida(partida);
        return partida;
    }

    private void verificarPartidaDelJugador(Partida partida) {
        Long idJugador = partida.getJugadores().get(0).getId();
        Long idPartida = partida.getId();
        EstadoPartida estado = partida.getEstado();
        vaciarSesionParaBuscarPartida();

        Partida encontrada = repositorioPartida.buscarPartidaPorJugador(idJugador);

        assertThat(encontrada, is(notNullValue()));
        assertThat(encontrada.getId(), is(idPartida));
        assertThat(encontrada.getEstado(), is(estado));
    }

    private void vaciarSesionParaBuscarPartida() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }
}
