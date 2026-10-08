package com.tallerwebi.presentacion.DTO;

import java.util.List;

public class SalaActualizadaDTO {
    
   private String codigo;
    private List<String> usernames;

    public SalaActualizadaDTO(String codigo, List<String> usernames) {
        this.codigo = codigo;
        this.usernames = usernames;
    }

    public String getCodigo() {
        return codigo;
    }

    public List<String> getUsernames() {
        return usernames;
    }
}

