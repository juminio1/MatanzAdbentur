package com.tallerwebi.dominio.publicadores;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tallerwebi.dominio.observadores.Observador;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicadorEventosTest {

    @Test
    void entregaElEventoATodosLosObservadoresSuscritos() {
        List<String> recibidos = new ArrayList<>();
        Observador<String> primero = evento -> recibidos.add("primero:" + evento);
        Observador<String> segundo = evento -> recibidos.add("segundo:" + evento);
        PublicadorEventos<String> publicador = new PublicadorEventos<>(List.of(primero, segundo));

        publicador.notificar("cambio");

        assertEquals(List.of("primero:cambio", "segundo:cambio"), recibidos);
    }

    @Test
    void suscribirDosVecesNoDuplicaLaNotificacion() {
        List<String> recibidos = new ArrayList<>();
        Observador<String> observador = recibidos::add;
        PublicadorEventos<String> publicador = new PublicadorEventos<>(List.of(observador));
        publicador.suscribir(observador);

        publicador.notificar("cambio");

        assertEquals(List.of("cambio"), recibidos);
    }

    @Test
    void elObservadorDesuscritoDejaDeRecibirEventos() {
        List<String> recibidos = new ArrayList<>();
        Observador<String> observador = recibidos::add;
        PublicadorEventos<String> publicador = new PublicadorEventos<>(List.of(observador));
        publicador.desuscribir(observador);

        publicador.notificar("cambio");

        assertEquals(List.of(), recibidos);
    }

    @Test
    void unaSuscripcionDuranteLaNotificacionSeAplicaAlSiguienteEvento() {
        List<String> recibidos = new ArrayList<>();
        Observador<String> tardio = recibidos::add;
        PublicadorEventos<String> publicador = new PublicadorEventos<>(List.of());
        publicador.suscribir(evento -> publicador.suscribir(tardio));

        publicador.notificar("primero");
        assertEquals(List.of(), recibidos);
        publicador.notificar("segundo");
        assertEquals(List.of("segundo"), recibidos);
    }

    @Test
    void admitePublicarSinSuscriptoresYRechazaUnaSuscripcionNula() {
        PublicadorEventos<String> publicador = new PublicadorEventos<>(List.of());

        assertDoesNotThrow(() -> publicador.notificar("cambio"));
        assertThrows(NullPointerException.class, () -> publicador.suscribir(null));
    }
}
