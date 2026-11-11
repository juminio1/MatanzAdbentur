package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.FinalizarPartida;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioFinalizarPartidaImpl implements RepositorioFinalizarPartida {
    private final SessionFactory sessionFactory;

    @Autowired
    public RepositorioFinalizarPartidaImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void guardar(FinalizarPartida resultado) {
        sessionFactory.getCurrentSession().persist(resultado);
    }

    @Override
    public FinalizarPartida buscarPorPartidaYJugador(Long idPartida, Long idJugador) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM FinalizarPartida r WHERE r.partida.id = :idPartida "
                        + "AND r.jugador.id = :idJugador", FinalizarPartida.class)
                .setParameter("idPartida", idPartida)
                .setParameter("idJugador", idJugador)
                .uniqueResult();
    }

    @Override
    public List<FinalizarPartida> buscarPorPartida(Long idPartida) {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM FinalizarPartida r WHERE r.partida.id = :idPartida",
                        FinalizarPartida.class)
                .setParameter("idPartida", idPartida)
                .getResultList();
    }
}
