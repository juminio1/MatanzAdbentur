package com.tallerwebi.dominio.eventos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record EventoSalaActualizada(
    String codigo,
    List<String> usernames,
    List<String> fichasOcupadas,
    Map<String, String> fichasAsignadas,
    String creador,
    Map<String, String> avatares
) {
    public EventoSalaActualizada {
        usernames = Collections.unmodifiableList(new ArrayList<>(usernames));
        fichasOcupadas = Collections.unmodifiableList(new ArrayList<>(fichasOcupadas));
        fichasAsignadas = Collections.unmodifiableMap(new HashMap<>(fichasAsignadas));
        avatares = Collections.unmodifiableMap(new HashMap<>(avatares));
    }

    public EventoSalaActualizada(
        String codigo,
        List<String> usernames,
        List<String> fichasOcupadas,
        Map<String, String> fichasAsignadas,
        String creador
    ) {
        this(codigo, usernames, fichasOcupadas, fichasAsignadas, creador, Map.of());
    }
}
