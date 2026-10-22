package com.tallerwebi.presentacion.DTO;

import java.util.List;

@SuppressWarnings("PMD.TooManyFields")
public class RespuestaMovimientoDTO {

    private Integer dado1;
    private Integer dado2;
    private Integer suma;
    private Integer nuevaPosicion;
    private Long idJugador;
    private Integer posicionAnterior;
    private String idMovimiento;
    private Integer posicionCaidaEvento;
    private String nombreEvento;
    private String descripcionEvento;
    private String tipoEfectoEvento;
    private Integer dineroPorInicio;
    private List<SaldoJugadorDTO> saldos;
    private AlquilerMovimientoDTO alquiler;
    private TurnoDTO turnoSiguiente;

    public RespuestaMovimientoDTO() {
    }

    public Integer getDado1() {
        return dado1;
    }

    public void setDado1(Integer dado1) {
        this.dado1 = dado1;
    }

    public Integer getDado2() {
        return dado2;
    }

    public void setDado2(Integer dado2) {
        this.dado2 = dado2;
    }

    public Integer getSuma() {
        return suma;
    }

    public void setSuma(Integer suma) {
        this.suma = suma;
    }

    public Integer getNuevaPosicion() {
        return nuevaPosicion;
    }

    public void setNuevaPosicion(Integer nuevaPosicion) {
        this.nuevaPosicion = nuevaPosicion;
    }

    public Long getIdJugador(){
        return idJugador;
    }

    public void setIdJugador(Long idJugador){
        this.idJugador = idJugador;
    }

    public Integer getPosicionAnterior() { return posicionAnterior; }

    public void setPosicionAnterior(Integer posicionAnterior) { this.posicionAnterior = posicionAnterior; }

    public String getIdMovimiento() { return idMovimiento; }

    public void setIdMovimiento(String idMovimiento) { this.idMovimiento = idMovimiento; }

    public Integer getPosicionCaidaEvento() {
        return posicionCaidaEvento;
    }

    public void setPosicionCaidaEvento(Integer posicionCaidaEvento) {
        this.posicionCaidaEvento = posicionCaidaEvento;
    }

    public String getNombreEvento() {
        return nombreEvento;
    }

    public void setNombreEvento(String nombreEvento) {
        this.nombreEvento = nombreEvento;
    }

    public String getDescripcionEvento() {
        return descripcionEvento;
    }

    public void setDescripcionEvento(String descripcionEvento) {
        this.descripcionEvento = descripcionEvento;
    }

    public String getTipoEfectoEvento() {
        return tipoEfectoEvento;
    }

    public void setTipoEfectoEvento(String tipoEfectoEvento) {
        this.tipoEfectoEvento = tipoEfectoEvento;
    }

    public Integer getDineroPorInicio() {
        return dineroPorInicio;
    }

    public void setDineroPorInicio(Integer dineroPorInicio) {
        this.dineroPorInicio = dineroPorInicio;
    }

    public void informarDineroPorInicio(Boolean pasoPorInicio) {
        Integer dineroPorDados = pasoPorInicio ? 200 : 0;
        Integer dineroPorEvento = nuevaPosicion < posicionCaidaEvento ? 200 : 0;
        this.dineroPorInicio = dineroPorDados + dineroPorEvento;
    }

    public List<SaldoJugadorDTO> getSaldos() {
        return saldos;
    }

    public void setSaldos(List<SaldoJugadorDTO> saldos) {
        this.saldos = saldos;
    }

    public AlquilerMovimientoDTO getAlquiler() {
        return alquiler;
    }

    public void setAlquiler(AlquilerMovimientoDTO alquiler) {
        this.alquiler = alquiler;
    }

    public TurnoDTO getTurnoSiguiente() {
        return turnoSiguiente;
    }

    public void setTurnoSiguiente(TurnoDTO turnoSiguiente) {
        this.turnoSiguiente = turnoSiguiente;
    }

    public Integer calcularSegundosPresentacion() {
        Integer pasosEvento = (nuevaPosicion - posicionCaidaEvento + 30) % 30;
        Integer segundosPopup = nombreEvento == null ? 0 : 7;
        Integer milisegundosAnimacion = (suma + pasosEvento) * 250;
        return segundosPopup + (milisegundosAnimacion + 999) / 1000;
    }

}

