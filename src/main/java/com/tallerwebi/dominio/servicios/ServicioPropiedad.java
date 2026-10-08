package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
@Service("servicioPropiedad")
@Transactional//si o si completa todas las operaciones
public interface ServicioPropiedad {
    void comprarPropiedad(Long idPropiedad, Jugador jugador);
}
