package com.tallerwebi.dominio.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Jugador {
@Id
@GeneratedValue(strategy= GenerationType.IDENTITY)
private int id;
private String apodo;
private Integer dinero;

public Jugador() {}






   //GETTERS Y SETTERS
    public int getId() {
        return id;
    }

    public String getApodo() {
        return apodo;
    }

    public Integer getDinero() {
        return dinero;
    }

    public void setDinero(Integer dinero) {
        this.dinero = dinero;
    }

    public void setApodo(String apodo) {
        this.apodo = apodo;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void restarDinero(Integer precioCompra) {

    }

    public void agregarPropiedad(Propiedad propiedad) {
    }

    public void agregarDinero(Integer alquiler) {
    }

    public void quitarDinero(Integer monto) {

    }
}
