
package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.servicios.GestorRuletaPartida;
import com.tallerwebi.presentacion.DTO.EstadoRuletaDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Profile("ruleta-prueba")
@RequestMapping("/ruleta/prueba")
public class ControladorRuletaPrueba {

    private final GestorRuletaPartida gestorRuletaPartida;

    public ControladorRuletaPrueba(GestorRuletaPartida gestorRuletaPartida) {
        this.gestorRuletaPartida = gestorRuletaPartida;
    }

    @PostMapping("/{codigoPartida}/{idJugador}/activar")
    public ResponseEntity<EstadoRuletaDTO> activarRuleta(@PathVariable("codigoPartida") String codigoPartida, @PathVariable("idJugador") Long idJugador) {

        String activacionId = UUID.randomUUID().toString();

        EstadoRuletaDTO estado = gestorRuletaPartida.activarRuleta(codigoPartida, activacionId, idJugador);

        return ResponseEntity.ok(estado);
    }
}
