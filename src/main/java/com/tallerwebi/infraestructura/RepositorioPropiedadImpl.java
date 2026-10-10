package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Propiedad;
import jakarta.persistence.NoResultException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioPropiedad")
public class RepositorioPropiedadImpl implements RepositorioPropiedad {
private final SessionFactory sessionFactory;

@Autowired
    public RepositorioPropiedadImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
}

//Guardar propiedad en la base de datos.
@Override
    public void guardarPropiedad(Propiedad propiedad){
    this.sessionFactory.getCurrentSession().persist(propiedad);//guarda la propiedad que se paso por parametro
}
//Buscar propiedad por id y devolverla.
@Override
    public Propiedad buscarPropiedadPorId(Long id){
    String hql = "FROM Propiedad WHERE id=:id"; //consulta en la base de datos y trae la propiedad cuyo id coincida.
    try{
        return this.sessionFactory.getCurrentSession()
                .createQuery(hql, Propiedad.class)//indica el tipo de rtdo espero
                .setParameter("id", id)
                .getSingleResult();//ejecuta la consulta y devuelve un unico resultado.

    } catch (NoResultException e){
        return null;
    }

}



}
