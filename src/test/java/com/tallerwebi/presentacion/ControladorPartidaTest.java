package com.tallerwebi.presentacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ControladorPartidaTest {

    private ControladorPartida controladorPartida;

    @BeforeEach
    public void setUp() {
        controladorPartida = new ControladorPartida();
    }

    @Test
    public void queSePuedaVerElTableroMock() {
        ModelAndView modelAndView = controladorPartida.verTableroMock();

        assertNotNull(modelAndView);
        assertEquals("partida", modelAndView.getViewName());
        assertNotNull(modelAndView.getModel().get("partida"));
        assertNotNull(modelAndView.getModel().get("usuarioSesionId"));
        assertNotNull(modelAndView.getModel().get("turnoActualId"));
    }
}
