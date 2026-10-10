package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("repositorioCasillero")
public class RepositorioCasilleroImpl implements RepositorioCasillero {

    private SessionFactory sessionFactory;

    @Autowired
    public RepositorioCasilleroImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<Casillero> obtenerTodos() {
        return sessionFactory.getCurrentSession()//trae todos los casilleros
                .createQuery("FROM Casillero", Casillero.class)
                .list();
    }
}
