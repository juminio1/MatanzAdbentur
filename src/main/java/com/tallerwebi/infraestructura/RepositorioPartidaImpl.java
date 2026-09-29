package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.EstadoPartida;
import com.tallerwebi.dominio.Partida;
import jakarta.persistence.NoResultException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioPartida")
public class RepositorioPartidaImpl implements RepositorioPartida {

  private final SessionFactory sessionFactory;

  @Autowired
  public RepositorioPartidaImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void guardarPartida(Partida partida) {
    this.sessionFactory.getCurrentSession().persist(partida);
  }

  @Override
  public Partida buscarPartidaActiva() {
    String hql = "FROM Partida WHERE estado != :finalizada";
    try {
      return this.sessionFactory.getCurrentSession()
        .createQuery(hql, Partida.class)
        .setParameter("finalizada", EstadoPartida.FINALIZADA)
        .setMaxResults(1)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  @Override
  public Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico) {
    String hql = "FROM Partida WHERE codigoUnico = :codigo AND estado != :finalizada";
    try {
      return this.sessionFactory.getCurrentSession()
        .createQuery(hql, Partida.class)
        .setParameter("codigo", codigoUnico)
        .setParameter("finalizada", EstadoPartida.FINALIZADA)
        .getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }
}
