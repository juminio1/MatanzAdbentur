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

private Integer posicionActual;
private Integer orden;
private Boolean pierdeTurno;

    public Jugador() {
        this.posicionActual = 0;
        this.pierdeTurno = false;
    }


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
    public void setApodo(String apodo) {
        this.apodo = apodo;
    }
    public void setId(int id) {
        this.id = id;
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
