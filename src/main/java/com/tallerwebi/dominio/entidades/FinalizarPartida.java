package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.EstadoParticipante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "finalizar_partida", uniqueConstraints = @UniqueConstraint(
        name = "uk_resultado_partida_jugador", columnNames = {"partida_id", "jugador_id"}))
public class FinalizarPartida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jugador_id", nullable = false)
    private Jugador jugador;

    private Integer dineroFinal;
    private Integer valorPropiedades;
    private Integer deudaPendiente;
    private Integer patrimonioTotal;
    private Boolean ganador = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_participante")
    private EstadoParticipante estadoParticipante;

    public Long getId() {
        return id;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public Jugador getJugador() {
        return jugador;
    }

    public void setJugador(Jugador jugador) {
        this.jugador = jugador;
    }

    public Integer getDineroFinal() {
        return dineroFinal;
    }

    public void setDineroFinal(Integer dineroFinal) {
        this.dineroFinal = dineroFinal;
    }

    public Integer getValorPropiedades() {
        return valorPropiedades;
    }

    public void setValorPropiedades(Integer valorPropiedades) {
        this.valorPropiedades = valorPropiedades;
    }

    public Integer getDeudaPendiente() {
        return deudaPendiente;
    }

    public void setDeudaPendiente(Integer deudaPendiente) {
        this.deudaPendiente = deudaPendiente;
    }

    public Integer getPatrimonioTotal() {
        return patrimonioTotal;
    }

    public void setPatrimonioTotal(Integer patrimonioTotal) {
        this.patrimonioTotal = patrimonioTotal;
    }

    public Boolean getGanador() {
        return ganador;
    }

    public void setGanador(Boolean ganador) {
        this.ganador = ganador;
    }

    public EstadoParticipante getEstadoParticipante() {
        return estadoParticipante;
    }

    public void setEstadoParticipante(EstadoParticipante estadoParticipante) {
        this.estadoParticipante = estadoParticipante;
    }
}
