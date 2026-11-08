package com.tallerwebi.dominio.resultados;

import java.util.List;

public class PreguntaGenerada {
    
    private String dificultad;
    private String enunciado;
    private List<String> opciones;
    private Integer respuestaCorrecta;
    
    public PreguntaGenerada(String dificultad, String enunciado, List<String> opcion, Integer respuestaCorrecta){
    
        this.dificultad = dificultad;
        this.enunciado = enunciado;
        this.opciones = opcion;
        this.respuestaCorrecta = respuestaCorrecta;
    
    }

    public PreguntaGenerada(){ //Se crea un objeto vacio para que el amigo JSON pueda leer y asignarle los valores mediante los setters.

    }

    public String getDificultad() {
        return dificultad;
    }

    public void setDificultad(String dificultad) {
        this.dificultad = dificultad;
    }

    public String getEnunciado() {
        return enunciado;
    }

    public void setEnunciado(String enunciado) {
        this.enunciado = enunciado;
    }

    public List<String> getOpciones() {
        return opciones;
    }

    public void setOpciones(List<String> opciones) {
        this.opciones = opciones;
    }

    public Integer getRespuestaCorrecta() {
        return respuestaCorrecta;
    }

    public void setRespuestaCorrecta(Integer respuestaCorrecta) {
        this.respuestaCorrecta = respuestaCorrecta;
    }

    



}
