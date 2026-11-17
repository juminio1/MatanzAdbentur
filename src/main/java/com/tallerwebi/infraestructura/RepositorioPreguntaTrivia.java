package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.PreguntaTrivia;
import com.tallerwebi.dominio.enums.DificultadTrivia;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RepositorioPreguntaTrivia {

    void guardar(PreguntaTrivia pregunta);

    List<PreguntaTrivia> buscarPorDificultad(DificultadTrivia dificultad);

    Set<UUID> buscarHuellasUsadas(Partida partida);

    void registrarUso(Partida partida, UUID huella);
}
