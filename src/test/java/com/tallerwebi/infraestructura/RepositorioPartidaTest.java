package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.tallerwebi.dominio.EstadoPartida;
import com.tallerwebi.dominio.Partida;
import com.tallerwebi.dominio.Usuario;
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

    Partida partidaObtenida = this.repositorioPartida.buscarPartidaActivaPorCodigoUnico("MAT123");

    assertThat(partidaObtenida, is(notNullValue()));
    assertThat(partidaObtenida.getCreador(), is(notNullValue()));
    assertThat(partidaObtenida.getCreador().getId(), is(equalTo(creador.getId())));
    assertThat(partidaObtenida.getCreador().getUsername(), is(equalTo("ElHostMatancero")));
  }
}
