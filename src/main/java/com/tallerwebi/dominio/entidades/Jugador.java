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

    // Hibernate deja el estado en null si todas sus columnas están vacías en la base
    // (por ejemplo, jugadores guardados antes de que existieran estas columnas).
    // En ese caso se crea un estado nuevo con los valores por defecto.
    public EstadoCarcel getEstadoCarcel() {
        if (this.estadoCarcel == null) {
            this.estadoCarcel = new EstadoCarcel();
        }
        return this.estadoCarcel;
    }

    private EstadoJugador getEstadoJugador() {
        if (this.estadoJugador == null) {
            this.estadoJugador = new EstadoJugador();
        }
        return this.estadoJugador;
    }

    // --- MÉTODOS DELEGADOS AL ESTADO JUGADOR ---

    public Boolean getTiroEnTurno() {
        return getEstadoJugador().getTiroEnTurno();
    }

    public void setTiroEnTurno(Boolean tiroEnTurno) {
        getEstadoJugador().setTiroEnTurno(tiroEnTurno);
    }

    public Boolean getPierdeTurno() {
        return getEstadoJugador().getPierdeTurno();
    }

    public void setPierdeTurno(Boolean pierdeTurno) {
        getEstadoJugador().setPierdeTurno(pierdeTurno);
    }

    public Boolean getEnBancarrota() {
        return Boolean.TRUE.equals(getEstadoJugador().getEnBancarrota());
    }

    public void setEnBancarrota(Boolean enBancarrota) {
        getEstadoJugador().setEnBancarrota(enBancarrota);
    }

    public Boolean getAbandono() {
        return Boolean.TRUE.equals(getEstadoJugador().getAbandono());
    }

    public void setAbandono(Boolean abandono) {
        getEstadoJugador().setAbandono(abandono);
    }
}