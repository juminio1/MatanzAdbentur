package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEsperaImpl;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioSalaDeEsperaTest {

    private ServicioSalaDeEspera servicioSalaDeEspera;
    private RepositorioUsuario repositorioUsuarioMock;
    private RepositorioSalaDeEspera repositorioSalaDeEsperaMock;

    @BeforeEach
    public void init() {
        this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
        this.repositorioSalaDeEsperaMock = mock(RepositorioSalaDeEspera.class);
        this.servicioSalaDeEspera = new ServicioSalaDeEsperaImpl(
            this.repositorioSalaDeEsperaMock,
            this.repositorioUsuarioMock
        );
    }

    private Usuario crearUsuarioEjemplo(Long id, String email) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setPassword("123");
        usuario.setRol("USER");
        return usuario;
    }

    // --- Tests de crearSalaDeEspera ---

    @Test
    public void crearSalaConUsuarioExistente() throws UsuarioNoEncontradoException {
        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);

        SalaDeEspera sala = this.servicioSalaDeEspera.crearSalaDeEspera(1L);

        assertNotNull(sala);
        assertNotNull(sala.getCodigoUnico());
        assertEquals(usuario, sala.getCreador());
        assertTrue(sala.getUsuarios().contains(usuario));
        verify(this.repositorioSalaDeEsperaMock).guardar(sala);
    }

    @Test
    public void noCrearSalaSiUsuarioNoExiste() {
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
            this.servicioSalaDeEspera.crearSalaDeEspera(1L)
        );

        verify(this.repositorioSalaDeEsperaMock, never()).guardar(any(SalaDeEspera.class));
    }

    // --- Tests de unirseASalaDeEspera ---

    @Test
    public void usuarioSeUneASalaDeEsperaExistente()
            throws UsuarioNoEncontradoException, SalaNoEncontradaException, SalaDeEsperaLlenaException {

        Usuario creador = crearUsuarioEjemplo(1L, "creador@test.com");
        Usuario participante = crearUsuarioEjemplo(2L, "participante@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(creador);
        sala.getUsuarios().add(creador);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(participante);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        SalaDeEspera resultado = this.servicioSalaDeEspera.unirseASalaDeEspera(2L, "ABC123");

        assertNotNull(resultado);
        assertTrue(resultado.getUsuarios().contains(participante));
        verify(this.repositorioSalaDeEsperaMock).modificar(sala);
    }

    @Test
    public void usuarioInexistenteIntentaUnirseASalaDeEspera() {
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123")
        );

        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    @Test
    public void usuarioSeUneASalaDeEsperaInexistente() {
        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(null);

        assertThrows(SalaNoEncontradaException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123")
        );

        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    @Test
    public void usuarioSeUneASalaDeEsperaLlena() {
        Usuario usuarioExtra = crearUsuarioEjemplo(5L, "extra@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.getUsuarios().add(crearUsuarioEjemplo(1L, "u1@test.com"));
        sala.getUsuarios().add(crearUsuarioEjemplo(2L, "u2@test.com"));
        sala.getUsuarios().add(crearUsuarioEjemplo(3L, "u3@test.com"));
        sala.getUsuarios().add(crearUsuarioEjemplo(4L, "u4@test.com"));

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(5L)).thenReturn(usuarioExtra);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        assertThrows(SalaDeEsperaLlenaException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(5L, "ABC123")
        );

        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    @Test
    public void verificaQueUsuarioNoSeAgregueDuplicadoSiYaEstaEnSala()
            throws UsuarioNoEncontradoException, SalaNoEncontradaException, SalaDeEsperaLlenaException {

        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(usuario);
        sala.getUsuarios().add(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        SalaDeEspera resultado = this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123");

        assertEquals(1, resultado.getUsuarios().size());
        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    // --- Tests de abandonarSala ---

    @Test
    public void usuarioAbandonaSalaExitosamente()
            throws UsuarioNoEncontradoException, SalaNoEncontradaException {

        Usuario usuario = crearUsuarioEjemplo(2L, "participante@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.getUsuarios().add(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        this.servicioSalaDeEspera.abandonarSala(2L, "ABC123");

        assertFalse(sala.getUsuarios().contains(usuario));
        verify(this.repositorioSalaDeEsperaMock).modificar(sala);
    }

    @Test
    public void abandonarSalaLanzaExcepcionSiUsuarioNoExiste() {
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
            this.servicioSalaDeEspera.abandonarSala(1L, "ABC123")
        );

        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    @Test
    public void abandonarSalaLanzaExcepcionSiSalaNoExiste() {
        Usuario usuario = crearUsuarioEjemplo(1L, "test@test.com");
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(null);

        assertThrows(SalaNoEncontradaException.class, () ->
            this.servicioSalaDeEspera.abandonarSala(1L, "ABC123")
        );

        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    @Test
    public void abandonarSalaNoModificaSiUsuarioNoPerteneciaALaSala()
            throws UsuarioNoEncontradoException, SalaNoEncontradaException {

        Usuario usuario = crearUsuarioEjemplo(3L, "ajeno@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.getUsuarios().add(crearUsuarioEjemplo(1L, "u1@test.com"));

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(3L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        this.servicioSalaDeEspera.abandonarSala(3L, "ABC123");

        assertEquals(1, sala.getUsuarios().size());
        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    // --- Tests de obtenerSalaPorCodigo ---

    @Test
    public void obtenerSalaPorCodigoRetornaSalaSiExiste() throws SalaNoEncontradaException {
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        SalaDeEspera resultado = this.servicioSalaDeEspera.obtenerSalaPorCodigo("ABC123");

        assertNotNull(resultado);
        assertEquals("ABC123", resultado.getCodigoUnico());
    }

    @Test
    public void obtenerSalaPorCodigoLanzaExcepcionSiNoExiste() {
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("INEXISTENTE")).thenReturn(null);

        assertThrows(SalaNoEncontradaException.class, () ->
            this.servicioSalaDeEspera.obtenerSalaPorCodigo("INEXISTENTE")
        );
    }
}