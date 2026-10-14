package com.tallerwebi.infraestructura;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.tallerwebi.dominio.entidades.eventos.Evento;

import java.util.List;
import java.util.Random;

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

        //traemos todos los eventos que guardamos con el CommandLineRunner
        List<Evento> eventos = session.createQuery("FROM Evento", Evento.class).list();

        if (eventos == null || eventos.isEmpty()) {
            return null;
        }

        // Agarra un evento aleatoriamente ya guardado en la base de datos.
        Integer indiceAleatorio = random.nextInt(eventos.size());
        return eventos.get(indiceAleatorio);
    }
}
