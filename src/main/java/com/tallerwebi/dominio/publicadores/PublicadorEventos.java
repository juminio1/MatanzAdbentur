package com.tallerwebi.dominio.publicadores;

import com.tallerwebi.dominio.excepcion.ActivacionEventoInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalActivoException;
import com.tallerwebi.dominio.excepcion.EventoGlobalInvalidoException;
import com.tallerwebi.dominio.excepcion.EventoInexistenteException;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.observadores.Observador;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Aviso a los observadores cuando recibo un evento. */
public class PublicadorEventos<T> {

    private final Set<Observador<T>> observadores = new CopyOnWriteArraySet<>();

    public PublicadorEventos(List<Observador<T>> observadoresIniciales) {
        observadoresIniciales.forEach(this::suscribir);
    }

    public void suscribir(Observador<T> observador) {
        observadores.add(Objects.requireNonNull(observador));
    }

    public void desuscribir(Observador<T> observador) {
        observadores.remove(observador);
    }

    public void notificar(T evento) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {
        for (Observador<T> observador : observadores) {
            observador.actualizar(evento);
        }
    }
}
