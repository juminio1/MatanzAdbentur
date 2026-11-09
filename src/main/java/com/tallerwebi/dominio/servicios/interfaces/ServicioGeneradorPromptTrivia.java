package com.tallerwebi.dominio.servicios.interfaces;

@FunctionalInterface
public interface ServicioGeneradorPromptTrivia {
    

    public  String generarPromptTrivia(Integer cantidad, String dificultad);

}
