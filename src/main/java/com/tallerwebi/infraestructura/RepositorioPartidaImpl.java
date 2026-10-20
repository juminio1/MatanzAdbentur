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

/** Persiste jugadores y consulta partidas que todavía no finalizaron. */
@Repository("repositorioPartida")
public class RepositorioPartidaImpl implements RepositorioPartida {

    private static final String CONSULTA_PARTIDAS_ACTIVAS =
        "FROM Partida WHERE estado != :finalizada";

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
    public Partida buscarPartidaActivaPorCodigoUnico(String codigoUnico) {
        List<Partida> partidas = crearConsultaPartidasActivas(
            CONSULTA_PARTIDAS_ACTIVAS + " AND codigoUnico = :codigo"
        )
            .setParameter("codigo", codigoUnico)
            .getResultList();

        return partidas.isEmpty() ? null : partidas.get(0);
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
