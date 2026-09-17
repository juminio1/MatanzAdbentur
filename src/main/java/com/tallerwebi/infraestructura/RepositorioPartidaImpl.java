package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Partida;
import com.tallerwebi.dominio.Usuario;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioPartida")
public class RepositorioPartidaImpl implements RepositorioPartida {

  private SessionFactory sessionFactory;

  @Autowired
  public RepositorioPartidaImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardarPartida(Partida partida) {
    sessionFactory.getCurrentSession().persist(partida);
  }

  @Override
  public Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Partida where codigoUnico = :codigoUnico and activa = true", Partida.class)
      .setParameter("codigoUnico", codigoUnico)
      .uniqueResult();
  }
}
