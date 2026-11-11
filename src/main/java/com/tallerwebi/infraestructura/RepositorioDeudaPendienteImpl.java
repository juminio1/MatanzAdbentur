package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioDeudaPendienteImpl implements RepositorioDeudaPendiente {
    private static final String ALERTA_PRUEBA_RETIRADA = "SALDO_MINIMO_PRUEBA";
    private final SessionFactory sessionFactory;

    @Autowired
    public RepositorioDeudaPendienteImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void guardar(DeudaPendiente deuda) {
        // Retira únicamente alertas sin cobro de la prueba anterior, para liberar
        // la clave única antes de guardar una deuda real del mismo jugador.
        sessionFactory.getCurrentSession().createMutationQuery(
                "DELETE FROM DeudaPendiente d WHERE d.partida.id = :partida "
                    + "AND d.jugadorDeudor.id = :jugador AND d.montoPendiente = 0 AND d.motivo = :motivo")
                .setParameter("partida", deuda.getPartida().getId())
                .setParameter("jugador", deuda.getJugadorDeudor().getId())
                .setParameter("motivo", ALERTA_PRUEBA_RETIRADA).executeUpdate();
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
                                "AND d.jugadorDeudor.id = :idJugador " +
                                "AND (d.montoPendiente <> 0 OR d.motivo <> :alertaRetirada)",
                        DeudaPendiente.class)
                .setParameter("codigo", codigoPartida)
                .setParameter("idJugador", idJugador)
                .setParameter("alertaRetirada", ALERTA_PRUEBA_RETIRADA)
                .uniqueResult(); //devuelve una sola deuda o null si no existe.
    }

    @Override
    public boolean existeEnPartidaActiva(String codigoPartida) {
        // Una sola consulta, sin cargar las entidades ni abrir una transacción por jugador.
        return !sessionFactory.getCurrentSession().createQuery(
                "SELECT d.id FROM DeudaPendiente d JOIN d.partida p JOIN p.jugadores j "
                        + "WHERE p.codigoUnico = :codigo AND p.estado <> :finalizada "
                        + "AND j.id = d.jugadorDeudor.id "
                        + "AND (d.montoPendiente <> 0 OR d.motivo <> :alertaRetirada)", Long.class)
                .setParameter("codigo", codigoPartida)
                .setParameter("finalizada", com.tallerwebi.dominio.enums.EstadoPartida.FINALIZADA)
                .setParameter("alertaRetirada", ALERTA_PRUEBA_RETIRADA)
                .setMaxResults(1)
                .getResultList().isEmpty();
    }

    @Override
    public void eliminar(DeudaPendiente deuda) {
        sessionFactory.getCurrentSession()
                .remove(sessionFactory.getCurrentSession().contains(deuda)
                        ? deuda
                        : sessionFactory.getCurrentSession().merge(deuda));
    }

}
