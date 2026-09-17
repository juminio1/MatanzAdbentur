package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.tallerwebi.dominio.Partida;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
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
        repositorioPartida = new RepositorioPartidaImpl(sessionFactory);
    }

    @Test
    @Transactional
    public void encontrarUnaPartidaActivaDisponible() {
        Partida partida = new Partida();

        Usuario usuario = new Usuario();
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        sessionFactory.getCurrentSession().persist(usuario);

        partida.agregarUsuario(usuario);

        sessionFactory.getCurrentSession().persist(partida);

        Partida partidaObtenida = repositorioPartida.buscarPartidaActiva();

        assertThat(partidaObtenida, is(equalTo(partida)));
    }

    @Test
    @Transactional
    public void noEncontrarUnaPartidaActivaConCuatroJugadores() {

        Partida partida = new Partida();

        for (int i = 1; i <= 4; i++) {
            Usuario usuario = new Usuario();
            usuario.setEmail("jugador" + i + "@test.com");
            usuario.setPassword("123");
            usuario.setRol("USER");

            sessionFactory.getCurrentSession().persist(usuario);
            partida.agregarUsuario(usuario);
        }

        sessionFactory.getCurrentSession().persist(partida);

        Partida partidaObtenida = repositorioPartida.buscarPartidaActiva();

        assertThat(partidaObtenida, is(nullValue()));
    }

    @Test
    @Transactional
    public void noEncontrarUnaPartidaFinalizada() {

        Partida partida = new Partida();

        Usuario usuario = new Usuario();
        usuario.setEmail("jugador@test.com");
        usuario.setPassword("123");
        usuario.setRol("USER");

        sessionFactory.getCurrentSession().persist(usuario);

        partida.agregarUsuario(usuario);

        partida.finalizar();

        sessionFactory.getCurrentSession().persist(partida);

        Partida partidaObtenida = repositorioPartida.buscarPartidaActiva();

        assertThat(partidaObtenida, is(nullValue()));
    }
}

