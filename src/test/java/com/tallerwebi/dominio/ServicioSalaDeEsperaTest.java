package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEsperaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioSalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.presentacion.WebSocket.NotificadorSala;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.presentacion.WebSocket.observadores.ObservadorSalaWebSocket;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
            new PublicadorEventos<>(List.of(new ObservadorSalaWebSocket(this.notificadorSalaMock)))
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
    public void notificaLaInstantaneaDeSalaSolamenteDespuesDelCommit() throws Exception {
        SalaDeEspera sala = prepararSalaParaObservadores();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            servicioSalaDeEspera.seleccionarFicha("OBSERVER", 1L, ColorFicha.ROJO);
            verifyNoInteractions(notificadorSalaMock);
            sala.getFichasOcupadas().clear();
            sala.getUsuarios().get(0).setUsername("Nombre cambiado");

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

            verify(notificadorSalaMock).notificarSalaActualizada(argThat(estado ->
                    estado.getUsernames().equals(List.of("Ana"))
                    && estado.getFichasOcupadas().equals(List.of("ROJO"))
                    && estado.getFichasAsignadas().equals(java.util.Map.of("Ana", "ROJO"))));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    public void noNotificaLaSalaCuandoLaTransaccionSeRevierte() throws Exception {
        prepararSalaParaObservadores();
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            servicioSalaDeEspera.seleccionarFicha("OBSERVER", 1L, ColorFicha.ROJO);

            TransactionSynchronizationManager.getSynchronizations().forEach(sincronizacion ->
                    sincronizacion.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verifyNoInteractions(notificadorSalaMock);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private SalaDeEspera prepararSalaParaObservadores() {
        Usuario usuario = crearUsuarioEjemplo(1L, "ana@example.test");
        usuario.setUsername("Ana");
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("OBSERVER");
        sala.setCreador(usuario);
        sala.getUsuarios().add(usuario);
        when(repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(repositorioSalaDeEsperaMock.buscarPorCodigo("OBSERVER")).thenReturn(sala);
        return sala;
    }

    @Test
    public void crearSalaConUsuarioExistente() throws Exception {
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

    @Test
    public void usuarioSeUneASalaDeEsperaExistente() throws Exception {
        Usuario creador = crearUsuarioEjemplo(1L, "creador@test.com");
        Usuario participante = crearUsuarioEjemplo(2L, "participante@test.com");

        creador.setUsername("creador");
        participante.setUsername("participante");

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

        verify(this.notificadorSalaMock).notificarSalaActualizada(
            argThat(
                dto ->
                    "ABC123".equals(dto.getCodigo()) &&
                    dto.getUsernames().size() == 2 &&
                    dto.getUsernames().contains("creador") &&
                    dto.getUsernames().contains("participante")
            )
        );
    }

    @Test
    public void usuarioInexistenteIntentaUnirseASalaDeEspera() {
        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(null);

        assertThrows(UsuarioNoEncontradoException.class, () ->
            this.servicioSalaDeEspera.unirseASalaDeEspera(1L, "ABC123")
        );

        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());

        verifyNoInteractions(this.notificadorSalaMock);
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

        verifyNoInteractions(this.notificadorSalaMock);
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

        verifyNoInteractions(this.notificadorSalaMock);
    }

    @Test
    public void seleccionarFichaExitosamente() throws Exception {
        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        this.servicioSalaDeEspera.seleccionarFicha(
            "ABC123",
            1L,
            com.tallerwebi.dominio.enums.ColorFicha.ROJO);

        assertTrue(sala.getFichasOcupadas().contains(com.tallerwebi.dominio.enums.ColorFicha.ROJO));
        verify(this.repositorioSalaDeEsperaMock).modificar(sala);
    }

    @Test
    public void seleccionarFichaOcupadaLanzaExcepcion() {
        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.getFichasOcupadas().add(com.tallerwebi.dominio.enums.ColorFicha.ROJO);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        assertThrows(com.tallerwebi.dominio.excepcion.FichaOcupadaException.class, () ->
            this.servicioSalaDeEspera.seleccionarFicha(
                "ABC123",
                1L,
                com.tallerwebi.dominio.enums.ColorFicha.ROJO
            )
        );

        verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
    }

    @Test
    public void testGettersYSettersDeSalaDeEspera() {
        SalaDeEspera sala = new SalaDeEspera();
        sala.setId(99L);
        assertEquals(99L, sala.getId());

        List<Usuario> usuarios = new ArrayList<>();
        sala.setUsuarios(usuarios);
        assertEquals(usuarios, sala.getUsuarios());

        List<ColorFicha> fichas = new ArrayList<>();
        fichas.add(ColorFicha.ROJO);
        sala.setFichasOcupadas(fichas);
        assertEquals(fichas, sala.getFichasOcupadas());
    }

    
@Test
public void queSePuedaSeleccionarLaFichaRojaExitosamente() throws Exception {
    Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

    SalaDeEspera sala = new SalaDeEspera();
    sala.setCodigoUnico("ABC123");

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
            .thenReturn(usuario);

    when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
            .thenReturn(sala);

    this.servicioSalaDeEspera.seleccionarFicha("ABC123", 1L, ColorFicha.ROJO);

    assertEquals(ColorFicha.ROJO, usuario.getFicha());
    assertTrue(sala.getFichasOcupadas().contains(ColorFicha.ROJO));

    verify(this.repositorioUsuarioMock).modificar(usuario);
    verify(this.repositorioSalaDeEsperaMock).modificar(sala);
}

@Test
public void queSePuedaSeleccionarLaFichaAmarillaExitosamente() throws Exception {
    Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

    SalaDeEspera sala = new SalaDeEspera();
    sala.setCodigoUnico("ABC123");

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
            .thenReturn(usuario);

    when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
            .thenReturn(sala);

    this.servicioSalaDeEspera.seleccionarFicha("ABC123", 1L, ColorFicha.AMARILLO);

    assertEquals(ColorFicha.AMARILLO, usuario.getFicha());
    assertTrue(sala.getFichasOcupadas().contains(ColorFicha.AMARILLO));

    verify(this.repositorioUsuarioMock).modificar(usuario);
    verify(this.repositorioSalaDeEsperaMock).modificar(sala);
}

@Test
public void queNoSePuedaSeleccionarUnaFichaOcupadaPorOtroUsuario() {
    Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

    SalaDeEspera sala = new SalaDeEspera();
    sala.setCodigoUnico("ABC123");
    sala.getFichasOcupadas().add(ColorFicha.ROJO);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
            .thenReturn(usuario);

    when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
            .thenReturn(sala);

    assertThrows(FichaOcupadaException.class, () ->
            this.servicioSalaDeEspera.seleccionarFicha(
                    "ABC123", 1L, ColorFicha.ROJO
            )
    );

    assertNull(usuario.getFicha());

    verify(this.repositorioUsuarioMock, never()).modificar(any());
    verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
}

@Test
public void queNoSePuedaSeleccionarUnaFichaSiElUsuarioNoExiste() {
    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
            .thenReturn(null);

    assertThrows(UsuarioNoEncontradoException.class, () ->
            this.servicioSalaDeEspera.seleccionarFicha(
                    "ABC123", 1L, ColorFicha.ROJO
            )
    );

    verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
}

@Test
public void queNoSePuedaSeleccionarUnaFichaSiLaSalaNoExiste() {
    Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
            .thenReturn(usuario);

    when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
            .thenReturn(null);

    assertThrows(SalaNoEncontradaException.class, () ->
            this.servicioSalaDeEspera.seleccionarFicha(
                    "ABC123", 1L, ColorFicha.ROJO
            )
    );

    verify(this.repositorioUsuarioMock, never()).modificar(any());
    verify(this.repositorioSalaDeEsperaMock, never()).modificar(any());
}

    
@Test
public void queUnUsuarioPuedaCambiarDeFichaVerdeARoja() throws Exception {
    Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");
    usuario.setFicha(ColorFicha.VERDE);

    SalaDeEspera sala = new SalaDeEspera();
    sala.setCodigoUnico("ABC123");
    sala.getFichasOcupadas().add(ColorFicha.VERDE);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
            .thenReturn(usuario);

    when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
            .thenReturn(sala);

    this.servicioSalaDeEspera.seleccionarFicha("ABC123", 1L, ColorFicha.ROJO);

    assertEquals(ColorFicha.ROJO, usuario.getFicha());
    assertTrue(sala.getFichasOcupadas().contains(ColorFicha.ROJO));
    assertFalse(sala.getFichasOcupadas().contains(ColorFicha.VERDE));
    assertEquals(1, sala.getFichasOcupadas().size());

    verify(this.repositorioUsuarioMock).modificar(usuario);
    verify(this.repositorioSalaDeEsperaMock).modificar(sala);
}

@Test
public void queSeleccionarLaMismaFichaNoGenereUnaExcepcion() {
    Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");
    usuario.setFicha(ColorFicha.AZUL);

    SalaDeEspera sala = new SalaDeEspera();
    sala.setCodigoUnico("ABC123");
    sala.getFichasOcupadas().add(ColorFicha.AZUL);

    when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L))
            .thenReturn(usuario);

    when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123"))
            .thenReturn(sala);

    assertDoesNotThrow(() ->
            this.servicioSalaDeEspera.seleccionarFicha(
                    "ABC123", 1L, ColorFicha.AZUL
            )
    );

    assertEquals(ColorFicha.AZUL, usuario.getFicha());
    assertEquals(1, sala.getFichasOcupadas().size());
}
    @Test
    public void queSePuedaSeleccionarUnaFichaLibreYSeNotifiqueALaSala() throws Exception {
        Usuario usuario = crearUsuarioEjemplo(1L, "jugador@test.com");
        usuario.setUsername("rafael");

        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("ABC123");
        sala.getUsuarios().add(usuario);

        when(this.repositorioUsuarioMock.buscarUsuarioPorId(1L)).thenReturn(usuario);
        when(this.repositorioSalaDeEsperaMock.buscarPorCodigo("ABC123")).thenReturn(sala);

        this.servicioSalaDeEspera.seleccionarFicha("ABC123", 1L, ColorFicha.ROJO);

        assertEquals(ColorFicha.ROJO, usuario.getFicha());
        assertTrue(sala.getFichasOcupadas().contains(ColorFicha.ROJO));

        verify(this.repositorioUsuarioMock).modificar(usuario);
        verify(this.repositorioSalaDeEsperaMock).modificar(sala);

        // Verifica que notificadorSala haya recibido el DTO con la ficha ocupada y asignada
        verify(this.notificadorSalaMock).notificarSalaActualizada(argThat(dto ->
                "ABC123".equals(dto.getCodigo()) &&
                        dto.getFichasOcupadas().contains("ROJO") &&
                        "ROJO".equals(dto.getFichasAsignadas().get("rafael"))
        ));
    }
}

