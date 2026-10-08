package com.tallerwebi.dominio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioPartida;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServicioPartidaTest {

    private RepositorioPartida repositorioPartidaMock;
    private ServicioPartida servicioPartida;

    @BeforeEach
    public void init() {
        repositorioPartidaMock = mock(RepositorioPartida.class);
        servicioPartida = new ServicioPartidaImpl(repositorioPartidaMock);
    }

    @Test
    public void queSeObtenganLosUsuariosPorCodigoDePartida() {
        // partida falsa
        String codigoPrueba = "MATANZA123";
        Partida partidaSimulada = new Partida();

        Usuario usuario1 = new Usuario();
        usuario1.setId(1L);
        usuario1.setEmail("jugador1@test.com");

        Usuario usuario2 = new Usuario();
        usuario2.setId(2L);
        usuario2.setEmail("jugador2@test.com");

        // Usamos la lista de usuarios real de la entidad Partida
        partidaSimulada.setUsuarios(Arrays.asList(usuario1, usuario2));

        // devuelve la partida falsa
        when(repositorioPartidaMock.buscarPartidaActivaPorCodigoUnico(codigoPrueba)).thenReturn(
            partidaSimulada
        );

        Partida partidaObtenida = servicioPartida.obtenerPartida(codigoPrueba);

        // verifica que trae a los usuarios correctamente
        assertNotNull(partidaObtenida);
        assertEquals(2, partidaObtenida.getUsuarios().size());
        assertEquals(1L, partidaObtenida.getUsuarios().get(0).getId());
        assertEquals(2L, partidaObtenida.getUsuarios().get(1).getId());
    }
}
