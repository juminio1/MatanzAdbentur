package com.tallerwebi.dominio.eventos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Copia del estado de una sala, independiente de entidades y DTO de presentación. */
public record EventoSalaActualizada(
    String codigo,
    List<String> usernames,
    List<String> fichasOcupadas,
    Map<String, String> fichasAsignadas,
    String creador
) {
    public EventoSalaActualizada {
        usernames = Collections.unmodifiableList(new ArrayList<>(usernames));
        fichasOcupadas = Collections.unmodifiableList(new ArrayList<>(fichasOcupadas));
        fichasAsignadas = Collections.unmodifiableMap(new HashMap<>(fichasAsignadas));
    }
}
