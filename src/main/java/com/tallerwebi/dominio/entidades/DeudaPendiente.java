package com.tallerwebi.dominio.entidades;

import jakarta.persistence.*;

@Entity
@Table(
        name = "deuda_pendiente",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deuda_partida_jugador",
                columnNames = {"partida_id", "jugador_deudor_id"}
        )
)
public class DeudaPendiente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jugador_deudor_id", nullable = false)
    private Jugador jugadorDeudor;

    @ManyToOne
    @JoinColumn(name = "jugador_acreedor_id")
    private Jugador jugadorAcreedor;

    @Column(nullable = false)
    private Integer montoPendiente;

    @Column(nullable = false)
    private String motivo;

    public DeudaPendiente() {
    }

    public Long getId() {
        return id;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public Jugador getJugadorDeudor() {
        return jugadorDeudor;
    }

    public void setJugadorDeudor(Jugador jugadorDeudor) {
        this.jugadorDeudor = jugadorDeudor;
    }

    public Jugador getJugadorAcreedor() {
        return jugadorAcreedor;
    }

    public void setJugadorAcreedor(Jugador jugadorAcreedor) {
        this.jugadorAcreedor = jugadorAcreedor;
    }

    public Integer getMontoPendiente() {
        return montoPendiente;
    }

    public void setMontoPendiente(Integer montoPendiente) {
        this.montoPendiente = montoPendiente;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
