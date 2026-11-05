package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import java.time.Instant;
import java.util.List;

public interface ServicioIntercambioPropiedad {
    record Propuesta(String id, String codigoPartida, Long idProponente, Long idDestinatario,
        Integer posicionOfrecida, Integer posicionSolicitada, String apodoProponente,
        String nombreOfrecida, String nombreSolicitada, Instant vence) { }
    record Resultado(Propuesta propuesta, CasilleroPropiedad ofrecida, CasilleroPropiedad solicitada) { }
    Propuesta proponer(String codigo, Long actor, Long destinatario, Integer ofrecida, Integer solicitada);
    List<Propuesta> pendientes(String codigo, Long destinatario);
    List<Propuesta> enviadas(String codigo, Long actor);
    Resultado aceptar(String codigo, String id, Long actor);
    Propuesta rechazar(String codigo, String id, Long actor);
}
