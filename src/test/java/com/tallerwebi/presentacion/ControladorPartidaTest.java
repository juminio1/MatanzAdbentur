package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.Partida;
import com.tallerwebi.dominio.ServicioPartida;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontrado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorPartidaTest {

  private ServicioPartida servicioPartida;
  private ControladorPartida controlador;
  private Partida partida;

  @BeforeEach
  void setUp() {
    servicioPartida = mock(ServicioPartida.class);
    controlador = new ControladorPartida(servicioPartida);
    partida = new Partida();
  }

  @Test
  void queSePuedaCrearUnaPartida() {
    when(servicioPartida.crearPartida(1)).thenReturn(partida);

    ModelAndView resultado = controlador.crearPartida();

    assertEquals("sala-de-espera", resultado.getViewName());
    assertEquals(partida, resultado.getModel().get("partidaCreada"));
  }

  @Test
  void deberiaNoCrearPartidaSiNoSeEncontroUsuario() {
    when(servicioPartida.crearPartida(1)).thenThrow(new UsuarioNoEncontrado());

    ModelAndView resultado = controlador.crearPartida();

    assertEquals("home", resultado.getViewName());
    assertEquals("usuario no encontrado", resultado.getModel().get("error"));
  }
}
