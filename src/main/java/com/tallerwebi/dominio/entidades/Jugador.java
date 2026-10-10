package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.ColorFicha;
import jakarta.persistence.*;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String apodo;
    private Integer dinero;

    @Enumerated(EnumType.STRING)
    private ColorFicha ficha;

    private Integer posicionActual;

    public Integer getPosicionActual() {
        return posicionActual;
    }
    public void setPosicionActual(Integer posicionActual) {
        this.posicionActual = posicionActual;
    }
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

    public ColorFicha getFicha() {
        return ficha;
    }

    public void setFicha(ColorFicha ficha) {
        this.ficha = ficha;
    }

    public void setApodo(String apodo) {
        this.apodo = apodo;
    }
    public void setId(int id) {
        this.id = id;
    }

}
