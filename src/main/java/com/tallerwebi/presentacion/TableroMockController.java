package com.tallerwebi.presentacion; 

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.enums.EstadoPartida;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TableroMockController { // <-- Cambiamos el nombre acá

    @GetMapping("/test-tablero") // La URL puede quedar igual
    public String probarTableroFrontend(Model model) {
        
        Partida partidaMock = new Partida();
        partidaMock.setEstado(EstadoPartida.EN_CURSO);

        Usuario u1 = new Usuario();
        u1.setId(1L);
        u1.setUsername("Juli");

        Usuario u2 = new Usuario();
        u2.setId(2L);
        u2.setUsername("Rafa");

        Jugador j1 = new Jugador();
        j1.setId(1);
        j1.setApodo(u1.getUsername() + " (Mock)");
        j1.setDinero(150000);
        j1.setFicha(ColorFicha.ROJO);
        j1.setPosicionActual(0); 

        Jugador j2 = new Jugador();
        j2.setId(2);
        j2.setApodo(u2.getUsername() + " (Mock)");
        j2.setDinero(80000);
        j2.setFicha(ColorFicha.AZUL);
        j2.setPosicionActual(3); 

        partidaMock.getJugadores().add(j1);
        partidaMock.getJugadores().add(j2);

        model.addAttribute("partida", partidaMock);
        model.addAttribute("turnoActualId", 1L); 
        model.addAttribute("usuarioSesionId", 1L); 

        return "partida"; 
    }
}