package com.tallerwebi.dominio.observadores;

/** Contrato de los objetos que reaccionan a un evento del dominio. */
@FunctionalInterface
public interface Observador<T> {
    void actualizar(T evento);
}
