package com.tallerwebi.infraestructura;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.Usuario;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { HibernateInfraestructuraTestConfig.class })
public class RepositorioUsuarioTest {

  @Autowired
  private SessionFactory sessionFactory;

  private RepositorioUsuario repositorioUsuario;

  @BeforeEach
  public void init() {
    repositorioUsuario = new RepositorioUsuarioImpl(sessionFactory);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaGuardarUnNuevoUsuario() {
    String emailNuevoUsuario = "nuevo.usuario@test.com";
    // preparacion
    Usuario usuario = this.dadoQueTengoUnUsuario(emailNuevoUsuario, "juli123", "1234", "USER");

    // ejecucion
    this.cuandoGuardoUnUsuario(usuario);

    // validacion
    this.entoncesSeGuardoElUsuario(emailNuevoUsuario, usuario);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaEncontrarUnUsuarioExistenteCuandoBuscoPorEmailYPassword() {
    String email = "test@test.com";
    String password = "123";
    Usuario usuario = this.dadoQueTengoUnUsuario(email, "juli123", password, "USER");
    this.dadoQueExisteElUsuario(usuario);

    Usuario obtenido = this.cuandoBuscoUnUsuario(email);

    this.entoncesElUsuarioObtenidoEsCorrecto(obtenido, usuario);
  }

  @Test
  @Transactional
  public void noDeberiaEncontrarUnUsuarioInexistenteCuandoBuscoPorEmailYPassword() {
    Usuario obtenido = this.cuandoBuscoUnUsuario("test@test.com");
    this.entoncesElUsuarioObtenidoEsNull(obtenido);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaEncontrarUnUsuarioExistenteCuandoBuscoPorEmail() {
    String email = "test@test.com";
    Usuario usuario = this.dadoQueTengoUnUsuario(email, "juli123", "123", "USER");
    this.dadoQueExisteElUsuario(usuario);
    Usuario obtenido = this.repositorioUsuario.buscarUsuarioPorEmail(email);

    this.entoncesElUsuarioObtenidoEsCorrecto(obtenido, usuario);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaModificarUnUsuarioExistente() {
    String email = "test@test.com";
    Usuario usuario = this.dadoQueTengoUnUsuario(email, "juli123", "123", "USER");
    this.dadoQueExisteElUsuario(usuario);

    usuario.setPassword("4567");
    usuario.setActivo(true);
    usuario.setRol("ADMIN");

    this.cuandoModificoUnUsuario(usuario);

    Usuario obtenido = this.cuandoBuscoUnUsuario(email);
    this.entoncesElUsuarioObtenidoEsCorrecto(obtenido, usuario);
  }

  @Test
  @Transactional
  @Rollback
  public void deberiaLanzarUnaExcepcionAlIntentarModificarUnUsuarioInexistente() {
    Usuario usuario = this.dadoQueTengoUnUsuario("noexiste@test.com", "juli123", "123", "USER");

    // Al no tener ID (no estar persistido), buscar por id devuelve null y
    // modificar debe lanzar UsuarioNoEncontrado.
    this.entoncesSeLanzaUnaUsuarioNoEncontrado(usuario);
  }

  private Usuario dadoQueTengoUnUsuario(
    String email,
    String username,
    String password,
    String rol
  ) {
    Usuario usuario = new Usuario();
    usuario.setEmail(email);
    usuario.setPassword(password);
    usuario.setRol(rol);
    usuario.setUsername(username);
    return usuario;
  }

  @Test
  @Transactional
  @Rollback
  public void quieroBuscarUnUsuarioPorUsernameExistente() {
    // Preparación
    String email = "juli@gmail.com";
    String username = "juli123";
    String password = "estaOk123$";
    String rol = "user";
    Usuario user = this.dadoQueTengoUnUsuario(email, username, password, rol);
    this.dadoQueExisteElUsuario(user);
    // this.cuandoGuardoUnUsuario(user); // El usuario ya esta persistido loquita

    // Ejecución
    String nickBuscado = "juli123";
    Usuario userBuscado = this.cuandoBuscoUnUsuarioPorUsername(nickBuscado);

    // Validación
    assertThat(userBuscado.getUsername(), equalTo(username));
  }

  @Test
  @Transactional
  @Rollback
  public void quieroVerificarUnUsuarioPorUsernameExistente() {
    // Preparación
    String email = "juli@gmail.com";
    String username = "juli123";
    String password = "estaOk123$";
    String rol = "user";
    Usuario user = this.dadoQueTengoUnUsuario(email, username, password, rol);
    this.dadoQueExisteElUsuario(user);

    // Ejecución
    Boolean userVerificado = this.repositorioUsuario.verificarUsernameExistente(username);

    // Validación
    assertThat(userVerificado, is(true));
  }

  @Test
  @Transactional
  @Rollback
  public void quieroVerificarUnUsuarioPorEmailInexistente() {
    // Preparación
    String email = "juli123@gmail.com";

    // Ejecución
    Boolean userVerificado = this.repositorioUsuario.verificarEmailExistente(email);

    // Validación
    assertThat(userVerificado, is(false));
  }

  @Test
  @Transactional
  @Rollback
  public void quieroVerificarUnUsuarioPorUsernameInexistente() {
    // Preparación
    String username = "juli123";

    // Ejecución
    Boolean userVerificado = this.repositorioUsuario.verificarUsernameExistente(username);

    // Validación
    assertThat(userVerificado, is(false));
  }

  @Test
  @Transactional
  @Rollback
  public void quieroVerificarUnUsuarioPorEmailExistente() {
    // Preparación
    String email = "juli@gmail.com";
    String username = "juli123";
    String password = "estaOk123$";
    String rol = "user";
    Usuario user = this.dadoQueTengoUnUsuario(email, username, password, rol);

    this.dadoQueExisteElUsuario(user);

    // Ejecución
    Boolean userVerificado = this.repositorioUsuario.verificarEmailExistente(email);

    // Validación
    assertThat(userVerificado, is(true));
  }

  @Test
  @Transactional
  @Rollback
  public void quieroBuscarUnUsuarioPorId() {
    // Preparación
    String email = "juli@gmail.com";
    String username = "juli123";
    String password = "estaOk123$";
    String rol = "user";
    Usuario user = this.dadoQueTengoUnUsuario(email, username, password, rol);
    this.dadoQueExisteElUsuario(user);

    Long idGenerado = user.getId();

    // Ejecución
    Usuario userBuscado = this.repositorioUsuario.buscarUsuarioPorId(idGenerado);

    // Validación
    //Long idEsperado = 1L;
    assertThat(user.getId(), equalTo(userBuscado.getId()));
  }

  private void dadoQueExisteElUsuario(Usuario usuario) {
    this.sessionFactory.getCurrentSession().persist(usuario);
  }

  private void cuandoGuardoUnUsuario(Usuario usuario) {
    repositorioUsuario.guardar(usuario);
  }

  private Usuario cuandoBuscoUnUsuario(String email) {
    return repositorioUsuario.buscarUsuarioPorEmail(email);
  }

  private Usuario cuandoBuscoUnUsuarioPorUsername(String username) {
    return repositorioUsuario.buscarUsuarioPorUsername(username);
  }

  private void cuandoModificoUnUsuario(Usuario usuario) {
    repositorioUsuario.modificar(usuario);
  }

  private void entoncesSeGuardoElUsuario(String email, Usuario usuarioEsperado) {
    String hql = "FROM Usuario WHERE email = :email";
    Query query = this.sessionFactory.getCurrentSession().createQuery(hql, Usuario.class);
    query.setParameter("email", email);
    Usuario usuarioObtenido = (Usuario) query.getSingleResult();
    this.entoncesElUsuarioObtenidoEsCorrecto(usuarioEsperado, usuarioObtenido);
  }

  private void entoncesElUsuarioObtenidoEsCorrecto(
    Usuario usuarioObtenido,
    Usuario usuarioEsperado
  ) {
    assertThat(usuarioObtenido.getEmail(), is(equalTo(usuarioEsperado.getEmail())));
    assertThat(usuarioObtenido.getUsername(), is(equalTo(usuarioEsperado.getUsername())));
    assertThat(usuarioObtenido.getPassword(), is(equalTo(usuarioEsperado.getPassword())));
    assertThat(usuarioObtenido.getActivo(), is(equalTo(usuarioEsperado.getActivo())));
    assertThat(usuarioObtenido.getRol(), is(equalTo(usuarioEsperado.getRol())));
  }

  private void entoncesElUsuarioObtenidoEsNull(Usuario obtenido) {
    assertThat(obtenido, is(nullValue()));
  }

  private void entoncesSeLanzaUnaUsuarioNoEncontrado(Usuario usuario) {
    assertThrows(
      UsuarioNoEncontradoException.class,
      () -> {
        this.cuandoModificoUnUsuario(usuario);
      }
    );
  }
}
