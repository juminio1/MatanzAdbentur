package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.PreguntaTrivia;
import com.tallerwebi.dominio.entidades.PreguntaTriviaUsada;
import com.tallerwebi.dominio.enums.DificultadTrivia;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioPreguntaTriviaImpl implements RepositorioPreguntaTrivia {

    private final SessionFactory sessionFactory;

    public RepositorioPreguntaTriviaImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void guardar(PreguntaTrivia pregunta) {
        sessionFactory.getCurrentSession().persist(pregunta);
    }

    @Override
    public List<PreguntaTrivia> buscarPorDificultad(DificultadTrivia dificultad) {
        return sessionFactory.getCurrentSession()
            .createQuery("FROM PreguntaTrivia WHERE dificultad = :dificultad", PreguntaTrivia.class)
            .setParameter("dificultad", dificultad)
            .getResultList();
    }

    @Override
    public Set<UUID> buscarHuellasUsadas(Partida partida) {
        return new HashSet<>(sessionFactory.getCurrentSession().createQuery(
            "SELECT uso.huellaEnunciado FROM PreguntaTriviaUsada uso WHERE uso.partida = :partida", UUID.class)
            .setParameter("partida", partida)
            .getResultList());
    }

    @Override
    public void registrarUso(Partida partida, UUID huella) {
        sessionFactory.getCurrentSession().persist(new PreguntaTriviaUsada(partida, huella));
    }
}
