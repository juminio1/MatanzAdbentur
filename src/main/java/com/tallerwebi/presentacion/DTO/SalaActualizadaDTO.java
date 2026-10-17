package com.tallerwebi.presentacion.DTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalaActualizadaDTO {

    private String codigo;
    private List<String> usernames;
    private List<String> fichasOcupadas;
    private Map<String, String> fichasAsignadas = new java.util.HashMap<>();
    private String creador;

    public SalaActualizadaDTO() {
    }

    public SalaActualizadaDTO(String codigo, List<String> usernames) {
        this.codigo = codigo;
        this.usernames = usernames;
    }

    public SalaActualizadaDTO(String codigo, List<String> usernames, List<String> fichasOcupadas) {
        this.codigo = codigo;
        this.usernames = usernames;
        this.fichasOcupadas = fichasOcupadas;
    }

    public String getCodigo() {
        return codigo;
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public List<String> getFichasOcupadas() {
        return fichasOcupadas;
    }

    public void setFichasOcupadas(List<String> fichasOcupadas) {
        this.fichasOcupadas = fichasOcupadas;
    }

    public void setUsernames(List<String> usernames) {
        this.usernames = usernames;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Map<String, String> getFichasAsignadas() {
        return fichasAsignadas;
    }

    public void setFichasAsignadas(Map<String, String> fichasAsignadas) {
        this.fichasAsignadas = fichasAsignadas;
    }

    public String getCreador() {
        return creador;
    }

    public void setCreador(String creador) {
        this.creador = creador;
    }
}
