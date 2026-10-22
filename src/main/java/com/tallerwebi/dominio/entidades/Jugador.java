package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.ColorFicha;
import jakarta.persistence.*;

@Entity
public class Jugador {

@Id
@GeneratedValue(strategy= GenerationType.IDENTITY)
private Long id;
private String apodo;
private Integer dinero;

@Enumerated(EnumType.STRING)
private ColorFicha ficha;

    private Integer posicionActual;

    private Integer orden;
    private Boolean pierdeTurno;
    private Boolean enBancarrota;

    public Jugador() {
        this.dinero = 0;
        this.posicionActual = 0;
        this.pierdeTurno = false;
        this.enBancarrota = false;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public Boolean estaEnPosicion(Integer posicion) {
        return posicion.equals(posicionActual);
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

    public Boolean getEnBancarrota() {
        return Boolean.TRUE.equals(enBancarrota);
    }

    public void setEnBancarrota(Boolean enBancarrota) {
        this.enBancarrota = Boolean.TRUE.equals(enBancarrota);
    }
}
