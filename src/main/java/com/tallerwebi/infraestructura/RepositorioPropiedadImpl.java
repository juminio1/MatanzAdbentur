package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import jakarta.persistence.NoResultException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

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
    this.sessionFactory.getCurrentSession().merge(propiedad);
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

    //Obtiene todas las propiedades de una localidad
    @Override
    public List<Propiedad> buscarPropiedadesPorGrupo(GrupoPropiedades grupo) {

        //Consulta las entidades propiedad.
        String hql = "FROM Propiedad WHERE grupo = :grupo";


        return sessionFactory.getCurrentSession()
                .createQuery(hql, Propiedad.class)
                .setParameter("grupo", grupo) //asigna el grupo recibido a la consulta
                .getResultList();//devuelve la lista con las propiedades encontradas.
    }

    @Override
    public List<Propiedad> buscarPropiedadesPorPropietario(Jugador propietario) {
        String hql = "FROM Propiedad WHERE propietario = :propietario";

        return sessionFactory.getCurrentSession()
                .createQuery(hql, Propiedad.class)
                .setParameter("propietario", propietario)
                .getResultList();
    }

}
