package com.tallerwebi.dominio.entidades;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;

/** Estado persistente de una competencia, separado para cada partida. */
@Embeddable
public class EstadoEventoGlobal {

    @ManyToOne
    @JoinColumn(name = "evento_global_id")
    private EventoGlobal evento;

    private Integer ultimaRondaEvento = 0;
    private Long revisionEvento = 0L;
    private Integer etapaEvento = 0;
    private Instant vencimientoEvento;
    private Long seleccionadoEvento = 0L;

    @Embedded
    private ConsecuenciaEventoGlobal consecuencia = new ConsecuenciaEventoGlobal();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "Partida_tiradasGlobales",
        joinColumns = @JoinColumn(name = "partida_id")
    )
    @MapKeyColumn(name = "etapa_jugador")
    @Column(name = "dados")
    private Map<String, Integer> tiradas = new HashMap<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "Partida_habilitadosGlobales",
        joinColumns = @JoinColumn(name = "partida_id")
    )
    @Column(name = "jugador_id")
    private Set<Long> habilitados = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "Partida_confirmacionesGlobales",
        joinColumns = @JoinColumn(name = "partida_id")
    )
    @Column(name = "jugador_id")
    private Set<Long> confirmados = new HashSet<>();

    public boolean estaActivo() {
        return evento != null && getEtapa() > 0;
    }

    public boolean estaResuelto() {
        return getSeleccionado() != null;
    }

    public EventoGlobal getEvento() {
        return evento;
    }

    public int getUltimaRonda() {
        return ultimaRondaEvento == null ? 0 : ultimaRondaEvento;
    }

    public long getRevision() {
        return revisionEvento == null ? 0 : revisionEvento;
    }

    public int getEtapa() {
        return etapaEvento == null ? 0 : etapaEvento;
    }

    public Instant getVencimiento() {
        return vencimientoEvento;
    }

    public Long getSeleccionado() {
        return seleccionadoEvento == null || seleccionadoEvento == 0 ? null : seleccionadoEvento;
    }

    public String getConsecuencia() {
        return consecuencia == null ? null : consecuencia.getDetalle();
    }

    public EventoCasillero getTarjeta() {
        return consecuencia == null ? null : consecuencia.getTarjeta();
    }

    public void guardarTarjeta(EventoCasillero tarjeta) {
        if (consecuencia == null) consecuencia = new ConsecuenciaEventoGlobal();
        consecuencia.setTarjeta(tarjeta);
    }

    public Integer getPosicionCompra() {
        return consecuencia == null ? null : consecuencia.getPosicionCompraEvento();
    }

    public void ofrecerCompra(Integer posicion) {
        if (consecuencia == null) consecuencia = new ConsecuenciaEventoGlobal();
        consecuencia.setPosicionCompraEvento(posicion);
    }

    public Set<Long> getHabilitados() {
        return Set.copyOf(habilitados);
    }

    public Set<Long> getConfirmados() {
        return Set.copyOf(confirmados);
    }

    public Map<String, Integer> getTiradas() {
        return Map.copyOf(tiradas);
    }

    public Integer tirada(Long jugador) {
        return tiradas.get(etapaEvento + ":" + jugador);
    }

    public void iniciar(EventoGlobal elegido, int ronda, Set<Long> participantes) {
        evento = elegido;
        ultimaRondaEvento = ronda;
        etapaEvento = 1;
        seleccionadoEvento = 0L;
        consecuencia = new ConsecuenciaEventoGlobal();
        consecuencia.setDetalle("");
        tiradas.clear();
        confirmados.clear();
        habilitados.clear();
        habilitados.addAll(participantes);
        renovarPlazo();
    }

    public void registrar(Long jugador, int dado1, int dado2) {
        tiradas.put(etapaEvento + ":" + jugador, dado1 * 10 + dado2);
        revisionEvento = getRevision() + 1;
    }

    public void desempatar(Set<Long> empatados) {
        habilitados.retainAll(empatados);
        etapaEvento++;
        renovarPlazo();
    }

    public void resolver(Long jugador, String consecuencia) {
        seleccionadoEvento = jugador;
        actualizarConsecuencia(consecuencia);
        renovarPlazo(60);
    }

    public void actualizarConsecuencia(String consecuencia) {
        if (this.consecuencia == null) this.consecuencia = new ConsecuenciaEventoGlobal();
        this.consecuencia.setDetalle(consecuencia);
    }

    public void excluirInactivos(Set<Long> activos) {
        boolean cambioHabilitados = habilitados.retainAll(activos);
        boolean cambioConfirmados = confirmados.retainAll(activos);
        if (cambioHabilitados || cambioConfirmados) revisionEvento = getRevision() + 1;
    }

    public void confirmar(Long jugador) {
        if (confirmados.add(jugador)) revisionEvento = getRevision() + 1;
    }

    public void cerrar() {
        etapaEvento = 0;
        vencimientoEvento = Instant.EPOCH;
        revisionEvento = getRevision() + 1;
    }

    private void renovarPlazo() {
        renovarPlazo(30);
    }

    private void renovarPlazo(int segundos) {
        vencimientoEvento = Instant.now()
            .plusSeconds(segundos)
            .truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        revisionEvento = getRevision() + 1;
    }
}
