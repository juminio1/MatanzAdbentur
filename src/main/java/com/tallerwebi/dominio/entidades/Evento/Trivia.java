
package com.tallerwebi.dominio.entidades.Evento;

import com.tallerwebi.dominio.enums.DificultadRuleta;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.tallerwebi.dominio.entidades.Jugador;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import jakarta.persistence.Transient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;

@Entity
public class Trivia extends Evento {

    @ManyToMany
    private List<Jugador> participantes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private DificultadRuleta dificultad;

    private int rondaActual;
    private boolean finalizada;
    private boolean premioEntregado;

    @ManyToOne
    private Jugador ganador;

    @Transient
    private PreguntaGenerada preguntaActual;

    @Transient
    private Set<Jugador> respondieronRonda = new HashSet<>();

    @Transient
    private Instant inicioRonda;

    @Transient
    private Map<Jugador, Long> tiemposAcumulados = new HashMap<>();

    public List<Jugador> getParticipantes() {
        return participantes;
    }

public DificultadRuleta getDificultad() {
    return dificultad;
}

public void setDificultad(DificultadRuleta dificultad) {
    this.dificultad = dificultad;
}

public int getRondaActual() {
    return rondaActual;
}

public void setRondaActual(int rondaActual) {
    this.rondaActual = rondaActual;
}

public Jugador getGanador() {
    return ganador;
}

public PreguntaGenerada getPreguntaActual() {
    return preguntaActual;
}

public void setPreguntaActual(PreguntaGenerada preguntaActual) {
    this.preguntaActual = preguntaActual;
}

public void declararGanador(Jugador jugador) {
    this.ganador = jugador;
}

public boolean estaFinalizada() {
    return finalizada;
}

public void finalizar() {
    this.finalizada = true;
}

public void eliminarParticipante(Jugador jugador) {
    participantes.remove(jugador);
}

public boolean yaRespondioEnRonda(Jugador jugador) {
    return respondieronRonda.contains(jugador);
}

public void registrarRespuesta(Jugador jugador) {
    respondieronRonda.add(jugador);
}


public void avanzarRonda() {
    rondaActual++;
    respondieronRonda.clear();
}


public Instant getInicioRonda() {
    return inicioRonda;
}

public void setInicioRonda(Instant inicioRonda) {
    this.inicioRonda = inicioRonda;
}

public boolean estaPremioEntregado() {
    return premioEntregado;
}

public void registrarPremioEntregado() {
    this.premioEntregado = true;
}


public long getTiempoAcumuladoMilisegundos(Jugador jugador) {
    return tiemposAcumulados.getOrDefault(jugador, 0L);
}

public void registrarTiempoRespuesta(Jugador jugador, long milisegundos) {
    tiemposAcumulados.merge(jugador, milisegundos, Long::sum);
}

@SuppressWarnings("PMD.NullAssignment")
public void cancelarSinPremio() {
    this.ganador = null;
    this.finalizada = true;
}
}

