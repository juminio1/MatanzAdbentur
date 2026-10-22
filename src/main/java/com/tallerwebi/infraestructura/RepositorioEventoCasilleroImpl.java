package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.EventoCasillero;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

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
