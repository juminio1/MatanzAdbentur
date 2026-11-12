package com.tallerwebi.dominio.servicios.interfaces;

import java.util.List;

import com.tallerwebi.dominio.resultados.PreguntaGenerada;

@FunctionalInterface 
public interface ClienteIA { //Define como solicita preguntas hacia una IA, sin depender de la cheminai

    public List<PreguntaGenerada> generarPreguntas(String dificultad, Integer cantidad);
    
}
