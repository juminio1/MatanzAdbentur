package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.ColorFicha;
import jakarta.persistence.*;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String apodo;
    private Integer dinero;

    @Enumerated(EnumType.STRING)
    private ColorFicha ficha;

    private Integer posicionActual;
    private Integer orden;

    @Embedded
    private EstadoCarcel estadoCarcel;

    @Embedded
    private EstadoJugador estadoJugador;

    public Jugador() {
        this.dinero = 0;
        this.posicionActual = 0;
        this.estadoCarcel = new EstadoCarcel();
        this.estadoJugador = new EstadoJugador();
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

    public void setPosicionActual(Integer posicionActual) {
        this.posicionActual = posicionActual;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public EstadoCarcel getEstadoCarcel() {
        return this.estadoCarcel;
    }

    // --- MÉTODOS DELEGADOS AL ESTADO JUGADOR ---

    public Boolean getTiroEnTurno() {
        return this.estadoJugador.getTiroEnTurno();
    }

    public void setTiroEnTurno(Boolean tiroEnTurno) {
        this.estadoJugador.setTiroEnTurno(tiroEnTurno);
    }

    public Boolean getPierdeTurno() {
        return this.estadoJugador.getPierdeTurno();
    }

    public void setPierdeTurno(Boolean pierdeTurno) {
        this.estadoJugador.setPierdeTurno(pierdeTurno);
    }

    public Boolean getEnBancarrota() {
        return Boolean.TRUE.equals(this.estadoJugador.getEnBancarrota());
    }

    public void setEnBancarrota(Boolean enBancarrota) {
        this.estadoJugador.setEnBancarrota(enBancarrota);
    }

    public Boolean getAbandono() {
        return Boolean.TRUE.equals(this.estadoJugador.getAbandono());
    }

    public void setAbandono(Boolean abandono) {
        this.estadoJugador.setAbandono(abandono);
    }
}