
package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEsperaImpl;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.presentacion.WebSocket.NotificadorSala;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioSalaDeEsperaTest {

    private ServicioSalaDeEspera servicioSalaDeEspera;
    private RepositorioUsuario repositorioUsuarioMock;
    private RepositorioSalaDeEspera repositorioSalaDeEsperaMock;
    private NotificadorSala notificadorSalaMock;

    @BeforeEach
    public void init() {

        this.repositorioUsuarioMock = mock(RepositorioUsuario.class);
        this.repositorioSalaDeEsperaMock = mock(RepositorioSalaDeEspera.class);
        this.notificadorSalaMock = mock(NotificadorSala.class);

        this.servicioSalaDeEspera = new ServicioSalaDeEsperaImpl(
                this.repositorioSalaDeEsperaMock,
                this.repositorioUsuarioMock,
                this.notificadorSalaMock
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

    @Test
    public void crearSalaConUsuarioExistente() throws Exception {

        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        SalaDeEspera sala =
                this.servicioSalaDeEspera.crearSalaDeEspera(1L);

        assertNotNull(sala);
        assertNotNull(sala.getCodigoUnico());
        assertEquals(usuario, sala.getCreador());
        assertTrue(sala.getUsuarios().contains(usuario));

        verify(this.repositorioSalaDeEsperaMock).guardar(sala);
    }

    @Test
    public void noCrearSalaSiUsuarioNoExiste() {

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
                this.servicioSalaDeEspera.crearSalaDeEspera(1L)
        );

        verify(this.repositorioSalaDeEsperaMock, never())
                .guardar(any(SalaDeEspera.class));
    }

    @Test
    public void usuarioSeUneASalaDeEsperaExistente() throws Exception {

        Usuario creador = crearUsuarioEjemplo(1L, "creador@test.com");
        Usuario participante =
                crearUsuarioEjemplo(2L, "participante@test.com");

        creador.setUsername("creador");
        participante.setUsername("participante");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(creador);
        sala.getUsuarios().add(creador);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L))
                .thenReturn(participante);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        SalaDeEspera resultado =
                this.servicioSalaDeEspera.unirseASalaDeEspera(
                        2L, "ABC123"
                );

        assertNotNull(resultado);
        assertTrue(resultado.getUsuarios().contains(participante));

        verify(this.repositorioSalaDeEsperaMock).modificar(sala);

        verify(this.notificadorSalaMock)
                .notificarSalaActualizada(
                        argThat(dto ->
                                "ABC123".equals(dto.getCodigo())
                                && dto.getUsernames().size() == 2
                                && dto.getUsernames().contains("creador")
                                && dto.getUsernames().contains("participante")
                        )
                );
    }

    @Test
    public void usuarioInexistenteIntentaUnirseASalaDeEspera() {

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
                this.servicioSalaDeEspera.unirseASalaDeEspera(
                        1L, "ABC123"
                )
        );

        verify(this.repositorioSalaDeEsperaMock, never())
                .modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void usuarioSeUneASalaDeEsperaInexistente() {

        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(null);

        assertThrows(SalaNoEncontradaException.class, () ->
                this.servicioSalaDeEspera.unirseASalaDeEspera(
                        1L, "ABC123"
                )
        );

        verify(this.repositorioSalaDeEsperaMock, never())
                .modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void usuarioSeUneASalaDeEsperaLlena() {

        Usuario usuarioExtra =
                crearUsuarioEjemplo(5L, "extra@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        sala.getUsuarios().add(
                crearUsuarioEjemplo(1L, "u1@test.com")
        );
        sala.getUsuarios().add(
                crearUsuarioEjemplo(2L, "u2@test.com")
        );
        sala.getUsuarios().add(
                crearUsuarioEjemplo(3L, "u3@test.com")
        );
        sala.getUsuarios().add(
                crearUsuarioEjemplo(4L, "u4@test.com")
        );

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(5L))
                .thenReturn(usuarioExtra);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        assertThrows(SalaDeEsperaLlenaException.class, () ->
                this.servicioSalaDeEspera.unirseASalaDeEspera(
                        5L, "ABC123"
                )
        );

        verify(this.repositorioSalaDeEsperaMock, never())
                .modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void verificaQueUsuarioNoSeAgregueDuplicadoSiYaEstaEnSala()
            throws Exception {

        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(usuario);
        sala.getUsuarios().add(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        SalaDeEspera resultado =
                this.servicioSalaDeEspera.unirseASalaDeEspera(
                        1L, "ABC123"
                );

        assertEquals(1, resultado.getUsuarios().size());

        verify(this.repositorioSalaDeEsperaMock, never())
                .modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void usuarioAbandonaSalaExitosamente() throws Exception {

        Usuario usuario =
                crearUsuarioEjemplo(2L, "participante@test.com");

        usuario.setUsername("participante");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.getUsuarios().add(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L))
                .thenReturn(usuario);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        this.servicioSalaDeEspera.abandonarSala(2L, "ABC123");

        assertFalse(sala.getUsuarios().contains(usuario));

        verify(this.repositorioSalaDeEsperaMock).modificar(sala);

        verify(this.notificadorSalaMock)
                .notificarSalaActualizada(
                        argThat(dto ->
                                "ABC123".equals(dto.getCodigo())
                                && dto.getUsernames().isEmpty()
                        )
                );
    }

    @Test
    public void abandonarSalaLanzaExcepcionSiUsuarioNoExiste() {

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
                this.servicioSalaDeEspera.abandonarSala(
                        1L, "ABC123"
                )
        );

        verify(this.repositorioSalaDeEsperaMock, never())
                .modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void abandonarSalaLanzaExcepcionSiSalaNoExiste() {

        Usuario usuario = crearUsuarioEjemplo(1L, "test@test.com");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(null);

        assertThrows(SalaNoEncontradaException.class, () ->
                this.servicioSalaDeEspera.abandonarSala(
                        1L, "ABC123"
                )
        );

        verify(this.repositorioSalaDeEsperaMock, never())
                .modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void abandonarSalaNoModificaSiUsuarioNoPerteneciaALaSala()
            throws Exception {

        Usuario usuario = crearUsuarioEjemplo(3L, "ajeno@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        sala.getUsuarios().add(
                crearUsuarioEjemplo(1L, "u1@test.com")
        );

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(3L))
                .thenReturn(usuario);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        this.servicioSalaDeEspera.abandonarSala(3L, "ABC123");

        assertEquals(1, sala.getUsuarios().size());

        verify(this.repositorioSalaDeEsperaMock, never())
                .modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void obtenerSalaPorCodigoRetornaSalaSiExiste() throws Exception {

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        SalaDeEspera resultado =
                this.servicioSalaDeEspera.obtenerSalaPorCodigo("ABC123");

        assertNotNull(resultado);
        assertEquals("ABC123", resultado.getCodigoUnico());
    }

    @Test
    public void obtenerSalaPorCodigoLanzaExcepcionSiNoExiste() {

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("INEXISTENTE"))
                .thenReturn(null);

        assertThrows(SalaNoEncontradaException.class, () ->
                this.servicioSalaDeEspera.obtenerSalaPorCodigo(
                        "INEXISTENTE"
                )
        );
    }

    @Test
    public void dadoQueUnUsuarioSeUneAUnaSalaSeDebeNotificarLaListaActualizada()
            throws Exception {

        Usuario creador = crearUsuarioEjemplo(1L, "creador@test.com");
        Usuario invitado = crearUsuarioEjemplo(2L, "invitado@test.com");

        creador.setUsername("creador");
        invitado.setUsername("invitado");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(creador);
        sala.getUsuarios().add(creador);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(2L))
                .thenReturn(invitado);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        this.servicioSalaDeEspera.unirseASalaDeEspera(
                2L, "ABC123"
        );

        verify(this.notificadorSalaMock)
                .notificarSalaActualizada(
                        argThat(dto ->
                                "ABC123".equals(dto.getCodigo())
                                && dto.getUsernames().equals(
                                        java.util.List.of("creador", "invitado")
                                )
                        )
                );
    }

    @Test
    public void dadoQueUnUsuarioYaEstaEnUnaSalaNoSeDebeNotificarNuevamente()
            throws Exception {

        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.setCreador(usuario);
        sala.getUsuarios().add(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
                .thenReturn(usuario);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        this.servicioSalaDeEspera.unirseASalaDeEspera(
                1L, "ABC123"
        );

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void dadoQueLaSalaEstaLlenaNoSeDebeNotificarElIngreso() {

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        for (long id = 1; id <= 4; id++) {
            sala.getUsuarios().add(
                    crearUsuarioEjemplo(id, "usuario" + id + "@test.com")
            );
        }

        Usuario invitado = crearUsuarioEjemplo(5L, "invitado@test.com");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(5L))
                .thenReturn(invitado);

        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
                .thenReturn(sala);

        assertThrows(SalaDeEsperaLlenaException.class, () ->
                this.servicioSalaDeEspera.unirseASalaDeEspera(
                        5L, "ABC123"
                )
        );

        verifyNoInteractions(this.notificadorSalaMock);
    }
}
