package com.tallerwebi.dominio.eventos;

import java.util.Map;

public record EventoTriviaActualizada(String codigoPartida, Map<String, Object> contenido) {}
