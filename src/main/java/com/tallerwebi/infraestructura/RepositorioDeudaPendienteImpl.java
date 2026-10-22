package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeudaPendienteImpl implements RepositorioDeudaPendiente {
    private final SessionFactory sessionFactory;

    @Autowired
    public RepositorioDeudaPendienteImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void guardar(DeudaPendiente deuda) {
        sessionFactory.getCurrentSession().persist(deuda);
        //guarda una deuda nueva
    }

    @Override
    public DeudaPendiente buscarPorPartidaYJugador(
            String codigoPartida,
            Long idJugador) {

        return sessionFactory.getCurrentSession()
                //busca la deuda correspondiente a un jugador y una partida.
                .createQuery(
                        "FROM DeudaPendiente d " +
                                "WHERE d.partida.codigoUnico = :codigo " +
                                "AND d.jugadorDeudor.id = :idJugador",
                        DeudaPendiente.class)
                .setParameter("codigo", codigoPartida)
                .setParameter("idJugador", idJugador)
                .uniqueResult(); //devuelve una sola deuda o null si no existe.
    }

    @Override
    public void eliminar(DeudaPendiente deuda) {
        sessionFactory.getCurrentSession()
                .remove(sessionFactory.getCurrentSession().contains(deuda)
                        ? deuda
                        : sessionFactory.getCurrentSession().merge(deuda));
    }

}