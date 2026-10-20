package com.tallerwebi.dominio.observadores;

@FunctionalInterface
public interface Observador<T> {
    void actualizar(T evento);
}
