package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.EventoGlobal;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;

/** Recupera ambas tablas de la jerarquía mediante HQL. */
@Repository
public class RepositorioEventoGlobalImpl implements RepositorioEventoGlobal {
    private final SessionFactory sessionFactory;

    public RepositorioEventoGlobalImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public List<EventoGlobal> obtenerTodos() {
        return sessionFactory.getCurrentSession()
                .createQuery("FROM EventoGlobal ORDER BY id", EventoGlobal.class).list();
    }
}
