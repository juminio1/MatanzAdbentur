package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import java.util.List;
import java.util.Random;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioEvento")
public class RepositorioEventoImpl implements RepositorioEvento {

    private final SessionFactory sessionFactory;
    private final Random random;

    @Autowired
    public RepositorioEventoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.random = new Random();
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public Evento obtenerEventoAleatorio() {
        final Session session = sessionFactory.getCurrentSession();

        // Traemos únicamente los eventos comunes, excluyendo los globales.
        List<Evento> eventos = session.createQuery("FROM Evento e WHERE TYPE(e) = Evento", Evento.class).list();

        if (eventos.isEmpty()) {
            return null;
        }

        // Seleccionamos aleatoriamente uno de los eventos comunes.
        Integer indiceAleatorio = random.nextInt(eventos.size());

        return eventos.get(indiceAleatorio);
    }

    @Override
    @SuppressWarnings("PMD.CloseResource")
    public Evento obtenerEventoGlobalAleatorio() {
        final Session session = sessionFactory.getCurrentSession();

        // Traemos únicamente los eventos globales guardados en la base de datos.
        List<EventoGlobal> eventosGlobales = session.createQuery("FROM EventoGlobal", EventoGlobal.class).list();

        if (eventosGlobales.isEmpty()) {
            return null;
        }

        // Seleccionamos aleatoriamente uno de los eventos globales.
        Integer indiceAleatorio = random.nextInt(eventosGlobales.size());

        return eventosGlobales.get(indiceAleatorio);
    }
}
