
package com.tallerwebi.presentacion.DTO;

public class TurnoDTO {

    private String codigoPartida;
    private Integer idJugadorActual;
    private String apodoJugadorActual;
    private Integer orden;
    private String mensaje;
    private Integer rondaActual;

    public TurnoDTO() {
    }

    public TurnoDTO(String codigoPartida, Integer idJugadorActual,
                    String apodoJugadorActual, Integer orden, String mensaje) {
        this.codigoPartida = codigoPartida;
        this.idJugadorActual = idJugadorActual;
        this.apodoJugadorActual = apodoJugadorActual;
        this.orden = orden;
        this.mensaje = mensaje;
    }

    public String getCodigoPartida() {
        return codigoPartida;
    }

    public void setCodigoPartida(String codigoPartida) {
        this.codigoPartida = codigoPartida;
    }

    public Integer getIdJugadorActual() {
        return idJugadorActual;
    }

    public void setIdJugadorActual(Integer idJugadorActual) {
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
}
