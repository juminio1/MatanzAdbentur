package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.ColorFicha;
import jakarta.persistence.*;

@Entity
public class Jugador {

@Id
@GeneratedValue(strategy= GenerationType.IDENTITY)
private int id;
private String apodo;
private Integer dinero;

@Enumerated(EnumType.STRING)
private ColorFicha ficha;

    private Integer posicionActual = 0;

    public Integer getPosicionActual() {
        return posicionActual;
    }

    public void setPosicionActual(Integer posicionActual) {
        this.posicionActual = posicionActual;
    }

    public Jugador() {}

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

   /* public void agregarPropiedad(Propiedad propiedad) {
    }

    public void agregarDinero(Integer alquiler) {
    }

    public void quitarDinero(Integer monto) {

    }*/
}
