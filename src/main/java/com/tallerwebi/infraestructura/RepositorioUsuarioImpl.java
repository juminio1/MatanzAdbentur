package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.RepositorioUsuario;
import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository("repositorioUsuario")
public class RepositorioUsuarioImpl implements RepositorioUsuario {

  private SessionFactory sessionFactory;

  @Autowired
  public RepositorioUsuarioImpl(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public Usuario buscarUsuarioPorEmail(String email) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where email = :email", Usuario.class)
      .setParameter("email", email)
      .uniqueResult();
  }

  @Override
  public void guardar(Usuario usuario) {
    sessionFactory.getCurrentSession().persist(usuario);
  }

  @Override
  public void modificar(Usuario usuario) {
    Usuario existente = sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where id = :id", Usuario.class)
      .setParameter("id", usuario.getId())
      .uniqueResult();
    if (existente == null) {
      throw new UsuarioNoEncontradoException();
    }
    sessionFactory.getCurrentSession().merge(usuario);
  }

  @Override
  public Usuario buscarUsuarioPorUsername(String nick) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where username = :username", Usuario.class)
      .setParameter("username", nick)
      .uniqueResult();
  }

  @Override
  public Boolean verificarEmailExistente(String email) {
    Usuario existente = sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where email = :email", Usuario.class)
      .setParameter("email", email)
      .uniqueResult();

    return existente != null;
  }

  @Override
  public Boolean verificarUsernameExistente(String username) {
    Usuario existente = sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where username = :username", Usuario.class)
      .setParameter("username", username)
      .uniqueResult();

    return existente != null;
  }

  public Usuario buscarUsuarioPorId(Integer id) {
    return sessionFactory
      .getCurrentSession()
      .createQuery("from Usuario where id = :id", Usuario.class)
      .setParameter("id", id)
      .uniqueResult();
  }
}
