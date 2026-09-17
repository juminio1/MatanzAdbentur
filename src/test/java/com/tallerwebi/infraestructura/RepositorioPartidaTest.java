package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.Partida;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RepositorioPartidaImplTest {

  private SessionFactory sessionFactory;
  private Session session;
  private RepositorioPartidaImpl repositorio;

  @BeforeEach
  void setUp() {
    sessionFactory = mock(SessionFactory.class);
    session = mock(Session.class);

    when(sessionFactory.getCurrentSession()).thenReturn(session);

    repositorio = new RepositorioPartidaImpl(sessionFactory);
  }

  @Test
  void deberiaGuardarPartida() {
    Partida partida = new Partida();

    repositorio.guardarPartida(partida);

    verify(session).persist(partida);
  }

  @Test
  void deberiaBuscarPartidaActivaPorCodigoUnico() {
    String codigoUnico = "ABC123";
    Partida partida = new Partida();

    @SuppressWarnings("unchecked")
    Query<Partida> query = mock(Query.class);

    when(
      session.createQuery(
        "from Partida where codigoUnico = :codigoUnico and activa = true",
        Partida.class
      )
    )
      .thenReturn(query);

    when(query.setParameter("codigoUnico", codigoUnico)).thenReturn(query);

    when(query.uniqueResult()).thenReturn(partida);

    Partida resultado = repositorio.buscarPartidaActivaPorCodigoUnico(codigoUnico);

    assertSame(partida, resultado);

    verify(session)
      .createQuery(
        "from Partida where codigoUnico = :codigoUnico and activa = true",
        Partida.class
      );

    verify(query).setParameter("codigoUnico", codigoUnico);
    verify(query).uniqueResult();
  }

  @Test
  void deberiaRetornarNullSiNoExistePartidaActiva() {
    String codigoUnico = "ABC123";

    @SuppressWarnings("unchecked")
    Query<Partida> query = mock(Query.class);

    when(
      session.createQuery(
        "from Partida where codigoUnico = :codigoUnico and activa = true",
        Partida.class
      )
    )
      .thenReturn(query);

    when(query.setParameter("codigoUnico", codigoUnico)).thenReturn(query);

    when(query.uniqueResult()).thenReturn(null);

    Partida resultado = repositorio.buscarPartidaActivaPorCodigoUnico(codigoUnico);

    assertEquals(null, resultado);

    verify(query).uniqueResult();
  }
}
