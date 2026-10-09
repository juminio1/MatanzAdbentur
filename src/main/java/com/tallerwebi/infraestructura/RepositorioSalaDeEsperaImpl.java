package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioSalaDeEspera")
public class RepositorioSalaDeEsperaImpl implements RepositorioSalaDeEspera {

    private final SessionFactory sessionFactory;

    @Autowired
    public RepositorioSalaDeEsperaImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void guardar(SalaDeEspera sala) {
        this.sessionFactory.getCurrentSession().persist(sala);
    }

    @Override
    public void modificar(SalaDeEspera sala) {
        this.sessionFactory.getCurrentSession().merge(sala);
    }

    @Override
    public SalaDeEspera buscarPorCodigo(String codigoUnico) {
        return this.sessionFactory.getCurrentSession().createQuery("from SalaDeEspera where codigoUnico = :codigoUnico", SalaDeEspera.class).setParameter("codigoUnico", codigoUnico).uniqueResult();
    }

    @Override
    public SalaDeEspera buscarPorId(Long id) {
        return this.sessionFactory.getCurrentSession().createQuery("from SalaDeEspera where id = :id", SalaDeEspera.class).setParameter("id", id).uniqueResult();
    }
}
