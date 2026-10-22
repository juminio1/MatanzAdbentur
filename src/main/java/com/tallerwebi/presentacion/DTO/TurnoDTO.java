
package com.tallerwebi.presentacion.DTO;

public class TurnoDTO {

    private String codigoPartida;
    private Long idJugadorActual;
    private String apodoJugadorActual;
    private Integer orden;
    private String mensaje;
    private Integer rondaActual;
    private Integer duracionSegundos;
    private Integer turnosFinalizadosEnRonda;

    // Constructor vacío (requerido por Jackson/Spring)
    public TurnoDTO() {}

    // Único constructor con parámetros (incluye la duración)
    public TurnoDTO(
        String codigoPartida,
        Long idJugadorActual,
        String apodoJugadorActual,
        Integer orden,
        String mensaje,
        Integer duracionSegundos
    ) {
        this.codigoPartida = codigoPartida;
        this.idJugadorActual = idJugadorActual;
        this.apodoJugadorActual = apodoJugadorActual;
        this.orden = orden;
        this.mensaje = mensaje;
        this.duracionSegundos = duracionSegundos;
    }

    public String getCodigoPartida() {
        return codigoPartida;
    }

    public void setCodigoPartida(String codigoPartida) {
        this.codigoPartida = codigoPartida;
    }

    public Long getIdJugadorActual() {
        return idJugadorActual;
    }

    public void setIdJugadorActual(Long idJugadorActual) {
        this.idJugadorActual = idJugadorActual;
    }

    public String getApodoJugadorActual() {
        return apodoJugadorActual;
    }

    public void setApodoJugadorActual(String apodoJugadorActual) {
        this.apodoJugadorActual = apodoJugadorActual;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Integer getRondaActual() {
        return rondaActual;
    }

    public void setRondaActual(Integer rondaActual) {
        this.rondaActual = rondaActual;
    }

    public Integer getDuracionSegundos() {
        return duracionSegundos;
    }

    public void setDuracionSegundos(Integer duracionSegundos) {
        this.duracionSegundos = duracionSegundos;
    }

    public Integer getTurnosFinalizadosEnRonda() {
        return turnosFinalizadosEnRonda;
    }

    public void setTurnosFinalizadosEnRonda(Integer turnosFinalizadosEnRonda) {
        this.turnosFinalizadosEnRonda = turnosFinalizadosEnRonda;
    }
}
