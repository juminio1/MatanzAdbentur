package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.ColorFicha;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String apodo;

    @Enumerated(EnumType.STRING)
    private ColorFicha ficha;

    private Integer dinero;
    private Integer posicionActual;

    private Integer orden;
    private Boolean pierdeTurno;

    public Jugador() {
        this.dinero = 0;
        this.posicionActual = 0;
        this.pierdeTurno = false;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getApodo() {
        return apodo;
    }

    public void setApodo(String apodo) {
        this.apodo = apodo;
    }

    public ColorFicha getFicha() {
        return ficha;
    }

    public void setFicha(ColorFicha ficha) {
        this.ficha = ficha;
    }

    public Integer getDinero() {
        return dinero;
    }

    public void setDinero(Integer dinero) {
        this.dinero = dinero;
    }

    public Integer getPosicionActual() {
        return posicionActual;
    }

    public void setPosicionActual(Integer posicionActual) {
        this.posicionActual = posicionActual;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Boolean getPierdeTurno() {
        return pierdeTurno;
    }

    public void setPierdeTurno(Boolean pierdeTurno) {
        this.pierdeTurno = pierdeTurno;
    }
}