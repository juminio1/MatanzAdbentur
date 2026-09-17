package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.Partida;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioPartida")
public class RepositorioPartidaImpl implements RepositorioPartida{

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
    public Partida buscarPartidaActiva() {
        return sessionFactory
                .getCurrentSession()
                .createQuery(
                        "select p from Partida p " +
                                "where p.activa = true " +
                                "and size(p.usuarios) < :maxUsuarios",
                        Partida.class
                )
                .setParameter("maxUsuarios", Partida.MAX_USUARIOS)
                .setMaxResults(1)
                .uniqueResult();
    }
}
