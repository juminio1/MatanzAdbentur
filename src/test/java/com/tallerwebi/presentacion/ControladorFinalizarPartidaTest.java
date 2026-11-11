package com.tallerwebi.presentacion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.FinalizarPartida;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.servicios.ServicioFinalizacionPartidaImpl;
import com.tallerwebi.infraestructura.RepositorioFinalizarPartida;
import com.tallerwebi.infraestructura.RepositorioPartida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ControladorFinalizarPartidaTest {
    private final RepositorioPartida partidas = mock(RepositorioPartida.class);
    private final RepositorioFinalizarPartida resultados = mock(RepositorioFinalizarPartida.class);
    private MockMvc mvc;
    private MockHttpSession sesion;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorFinalizarPartida(
                new ServicioFinalizacionPartidaImpl(null, null, resultados, null, partidas))).build();
        sesion = new MockHttpSession();
        sesion.setAttribute("NOMBRE", "Ana");
        Partida partida = new Partida();
        partida.setId(10L);
        partida.setCodigoUnico("FINAL");
        partida.setEstado(EstadoPartida.FINALIZADA);
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setApodo("Ana");
        jugador.setEnBancarrota(true);
        partida.getJugadores().add(jugador);
        when(partidas.buscarPartidaPorCodigoUnico("FINAL")).thenReturn(partida);
        FinalizarPartida resultado = new FinalizarPartida();
        resultado.setJugador(jugador);
        resultado.setDineroFinal(0);
        resultado.setPatrimonioTotal(-50);
        resultado.setGanador(false);
        when(resultados.buscarPorPartida(10L)).thenReturn(java.util.List.of(resultado));
    }

    @Test
    void exponeJsonSinCacheParaEspectadorAutorizado() throws Exception {
        mvc.perform(get("/partida/FINAL/resultados-finales").session(sesion))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(respuesta -> {
                    var json = new ObjectMapper().readTree(respuesta.getResponse().getContentAsByteArray());
                    assertEquals("FINALIZADA", json.get("estado").asText());
                    assertEquals("FINAL", json.get("codigoPartida").asText());
                    assertTrue(json.get("resultados").isArray());
                    var jugador = json.get("resultados").get(0);
                    assertEquals(1L, jugador.get("idJugador").asLong());
                    assertEquals(0, jugador.get("dineroFinal").asInt());
                    assertEquals(-50, jugador.get("patrimonioTotal").asInt());
                    assertTrue(jugador.get("cantidadPropiedades").isNull());
                    assertTrue(jugador.get("cantidadMejoras").isNull());
                    assertFalse(jugador.get("ganador").asBoolean());
                    assertFalse(jugador.has("jugador"));
                    assertFalse(jugador.has("partida"));
                });
    }

    @Test
    void sinSesionResponde401SinConsultarRepositorios() throws Exception {
        mvc.perform(get("/partida/FINAL/resultados-finales"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(partidas, resultados);
    }

    @Test
    void ajenoResponde403AunqueSuIdCoincidaConElJugador() throws Exception {
        sesion.setAttribute("NOMBRE", "Ajeno");
        sesion.setAttribute("id", 1L);
        mvc.perform(get("/partida/FINAL/resultados-finales").session(sesion).param("idJugador", "1"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(resultados);
    }

    @Test
    void idDeUsuarioSinNombreNoAutorizaComoJugador() throws Exception {
        sesion.removeAttribute("NOMBRE");
        sesion.setAttribute("IDUSUARIO", 1L);
        mvc.perform(get("/partida/FINAL/resultados-finales").session(sesion))
                .andExpect(status().isForbidden());
        verifyNoInteractions(resultados);
    }

    @Test
    void codigoInexistenteResponde404() throws Exception {
        mvc.perform(get("/partida/OTRA/resultados-finales").session(sesion))
                .andExpect(status().isNotFound());
        verifyNoInteractions(resultados);
    }
}
