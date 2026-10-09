package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.enums.ColorFicha;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ControladorPartida {

    private static final String VISTA_PARTIDA = "partida";

   @GetMapping("/partida")
    public ModelAndView verTableroMock() {
        Map<String, Object> model = new HashMap<>();

        Partida partidaMock = new Partida();
        
        Jugador j1 = new Jugador();
        j1.setId(1);
        j1.setApodo("El Turro");
        j1.setDinero(1500);
        j1.setPosicionActual(0); 
        j1.setFicha(ColorFicha.ROJO); 

        Jugador j2 = new Jugador();
        j2.setId(2);
        j2.setApodo("La Mishi");
        j2.setDinero(1200);
        j2.setPosicionActual(5); 
        j2.setFicha(ColorFicha.AZUL); 

        partidaMock.setJugadores(List.of(j1, j2));

        model.put("partida", partidaMock);
        model.put("usuarioSesionId", 1L);
        model.put("turnoActualId", 1L);

        return new ModelAndView(VISTA_PARTIDA, model);
    }
}
