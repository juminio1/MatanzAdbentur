package com.tallerwebi.dominio.publicadores;

import com.tallerwebi.dominio.observadores.Observador;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/** Publicador Observer: registra suscriptores y les entrega eventos del mismo tipo. */
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

    public void notificar(T evento) {
        for (Observador<T> observador : observadores) {
            observador.actualizar(evento);
        }
    }
}
