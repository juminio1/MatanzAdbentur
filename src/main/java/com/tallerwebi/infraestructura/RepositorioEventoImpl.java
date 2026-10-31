package com.tallerwebi.infraestructura;

import java.util.List;
import java.util.Random;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;

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
    public EventoGlobal obtenerEventoGlobalAleatorio() {
        final Session session = sessionFactory.getCurrentSession();

        List<EventoGlobal> eventosGlobales = session.createQuery("FROM EventoGlobal", EventoGlobal.class).list();

        if (eventosGlobales.isEmpty()) {
            return null;
        }

        Integer indiceAleatorio = random.nextInt(eventosGlobales.size());

        return eventosGlobales.get(indiceAleatorio);
    }


}
