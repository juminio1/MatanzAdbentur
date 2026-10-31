package com.tallerwebi.infraestructura;

import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;

@Repository("repositorioEventoCasillero")
public class RepositorioEventoCasilleroImpl implements RepositorioEventoCasillero {
    private final SessionFactory sessionFactory;

    @Autowired
    public RepositorioEventoCasilleroImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<EventoCasillero> obtenerTodos() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM EventoCasillero", EventoCasillero.class)
                .list();
    }
}
