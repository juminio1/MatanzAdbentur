public interface RepositorioUsuario {
  Usuario buscarUsuarioPorEmail(String email);
  void guardar(Usuario usuario);
  void modificar(Usuario usuario);
  Usuario buscarUsuarioPorUsername(String nick);
  Boolean verificarEmailExistente(String email);
  Boolean verificarUsernameExistente(String username);
  Usuario buscarUsuarioPorId(Long id);
}

