package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.ColorFicha;
import jakarta.persistence.*;

@Entity
public class Jugador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //relaciones
    @ManyToOne
    private Usuario usuario;

    @ManyToOne
    private Partida partida;

    @Enumerated(EnumType.STRING)
    private ColorFicha ficha;

    private Integer posicionActual = 0;

    public Jugador() {
    }

    public Jugador(Usuario usuario, Partida partida, ColorFicha ficha) {
        this.usuario = usuario;
        this.partida = partida;
        this.ficha = ficha;
        this.posicionActual = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public ColorFicha getFicha() {
        return ficha;
    }

    public void setFicha(ColorFicha ficha) {
        this.ficha = ficha;
    }

    public Integer getPosicionActual() {
        return posicionActual;
    }

    public void setPosicionActual(Integer posicionActual) {
        this.posicionActual = posicionActual;
    }
}
