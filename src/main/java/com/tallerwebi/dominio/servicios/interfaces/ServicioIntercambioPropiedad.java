package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import java.time.Instant;
import java.util.List;

public interface ServicioIntercambioPropiedad {
    record Propuesta(String id, String codigoPartida, Long idProponente, Long idDestinatario,
        Integer posicionOfrecida, Integer posicionSolicitada, String apodoProponente,
        String nombreOfrecida, String nombreSolicitada, Instant vence, boolean requiereConfirmacion) {
        public Propuesta(String id, String codigoPartida, Long idProponente, Long idDestinatario,
                Integer posicionOfrecida, Integer posicionSolicitada, String apodoProponente,
                String nombreOfrecida, String nombreSolicitada, Instant vence) {
            this(id, codigoPartida, idProponente, idDestinatario, posicionOfrecida, posicionSolicitada,
                apodoProponente, nombreOfrecida, nombreSolicitada, vence, false);
        }

        public boolean esperaConfirmacionFinal() {
            return requiereConfirmacion && posicionSolicitada != null;
        }

        public Long idJugadorQueResponde() {
            return esperaConfirmacionFinal() ? idProponente : idDestinatario;
        }
    }
    record Resultado(Propuesta propuesta, CasilleroPropiedad ofrecida, CasilleroPropiedad solicitada) { }
    Propuesta proponer(String codigo, Long actor, Long destinatario, Integer ofrecida, Integer solicitada);
    List<Propuesta> pendientes(String codigo, Long destinatario);
    List<Propuesta> enviadas(String codigo, Long actor);
    Resultado aceptar(String codigo, String id, Long actor);
    Propuesta elegirContrapropiedad(String codigo, String id, Long actor, Integer posicion);
    Propuesta rechazar(String codigo, String id, Long actor);
}
