package com.tallerwebi.dominio.entidades;

import java.util.ArrayList;
//import java.util.HashMap;
import java.util.List;

import jakarta.persistence.Entity;

@Entity 
public class SalaDeEspera {

    private List<Usuario> usuarios;
    //private HashMap<Usuario, Ficha> fichasElegidas;
    private String codigoGenerado;

    public SalaDeEspera(){
        this.usuarios = new ArrayList();
        
    }
    
    public List<Usuario> getUsuarios() {
        return usuarios;
    }


    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }


    public String getCodigoGenerado() {
        return codigoGenerado;
    }


    public void setCodigoGenerado(String codigoGenerado) {
        this.codigoGenerado = codigoGenerado;
    }


    



    




}
