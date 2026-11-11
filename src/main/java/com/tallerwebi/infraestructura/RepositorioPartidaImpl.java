package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import jakarta.persistence.NoResultException;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioPartida")
public class RepositorioPartidaImpl implements RepositorioPartida {

    private static final String CONSULTA_PARTIDAS_ACTIVAS =
        "FROM Partida WHERE estado != :finalizada";
    private static final String CONSULTA_PARTIDA_COMPLETA =
        "SELECT DISTINCT p FROM Partida p LEFT JOIN FETCH p.jugadores "
            + "LEFT JOIN FETCH p.creador LEFT JOIN FETCH p.jugadorTurnoAleatorio "
            + "WHERE p.codigoUnico = :codigo";

    private final SessionFactory sessionFactory;

    @Autowired
    public RepositorioPartidaImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void bloquearPartida(Partida partida) {
        if (partida == null) return;
        if (!sessionFactory.getCurrentSession().contains(partida)
                || sessionFactory.getCurrentSession().getCurrentLockMode(partida) != org.hibernate.LockMode.PESSIMISTIC_WRITE) {
            // Primero bloqueo sólo la fila padre. La segunda consulta recarga el grafo DESPUÉS
            // de esperar el bloqueo, para no conservar colecciones leídas antes de otra petición.
            sessionFactory.getCurrentSession().createNativeQuery(
                    "SELECT id FROM Partida WHERE id = :id FOR UPDATE", Long.class)
                    .setParameter("id", partida.getId()).getSingleResult();
            sessionFactory.getCurrentSession().refresh(partida, org.hibernate.LockMode.PESSIMISTIC_WRITE);
            // refresh no conserva necesariamente el modo en EntityEntry. Lo registro para
            // que los servicios anidados reutilicen el bloqueo sin descartar cambios pendientes.
            sessionFactory.getCurrentSession().lock(partida, org.hibernate.LockMode.PESSIMISTIC_WRITE);
        }
    }

    @Override
    public void guardarPartida(Partida partida) {
        this.sessionFactory.getCurrentSession().persist(partida);
    }

    @Override
    public void actualizarJugador(Jugador jugador) {
        this.sessionFactory.getCurrentSession().merge(jugador);
    }

    @Override
    public Jugador buscarJugadorPorId(Long idJugador) {
        Query<Jugador> consulta = this.sessionFactory.getCurrentSession()
            .createQuery("FROM Jugador WHERE id = :idJugador", Jugador.class)
            .setParameter("idJugador", idJugador);
        return obtenerResultadoUnicoONull(consulta);
    }

    @Override
    public Partida buscarPartidaActiva() {
        return obtenerResultadoUnicoONull(
            crearConsultaPartidasActivas(CONSULTA_PARTIDAS_ACTIVAS).setMaxResults(1)
        );
    }

    @Override
    public Partida buscarPartidaPorJugador(Long idJugador) {
        Query<Partida> consulta = this.sessionFactory.getCurrentSession()
                .createQuery("SELECT partida FROM Partida partida JOIN partida.jugadores jugador "
                        + "WHERE jugador.id = :idJugador", Partida.class)
                .setParameter("idJugador", idJugador);
        return obtenerResultadoUnicoONull(consulta);
    }

    @Override
    public Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico) {
        List<Partida> partidas = sessionFactory.getCurrentSession()
            .createQuery(CONSULTA_PARTIDA_COMPLETA + " AND p.estado <> :finalizada", Partida.class)
            .setParameter("finalizada", EstadoPartida.FINALIZADA)
            .setParameter("codigo", codigoUnico)
            .getResultList();

        return partidas.isEmpty() ? null : partidas.get(0);
    }

    @Override
    public Partida buscarPartidaPorCodigoUnico(String codigoUnico) {
        Query<Partida> consulta = this.sessionFactory.getCurrentSession()
                .createQuery(CONSULTA_PARTIDA_COMPLETA, Partida.class)
                .setParameter("codigo", codigoUnico);
        return obtenerResultadoUnicoONull(consulta);
    }

    private Query<Partida> crearConsultaPartidasActivas(String hql) {
        return this.sessionFactory.getCurrentSession()
            .createQuery(hql, Partida.class)
            .setParameter("finalizada", EstadoPartida.FINALIZADA);
    }

    private <T> T obtenerResultadoUnicoONull(Query<T> consulta) {
        try {
            return consulta.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
}
