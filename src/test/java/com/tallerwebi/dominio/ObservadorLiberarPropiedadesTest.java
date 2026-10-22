package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.eventos.EventoJugadorAbandonado;
import com.tallerwebi.dominio.observadores.ObservadorLiberarPropiedades;
// Ajustá estos imports según dónde pongas la clase y tu repositorio
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class ObservadorLiberarPropiedadesTest {

    private RepositorioPropiedad repositorioPropiedadMock;
    private ObservadorLiberarPropiedades observador;

    @BeforeEach
    public void init() {
        // Simulamos el repositorio que maneja la base de datos de las propiedades
        repositorioPropiedadMock = mock(RepositorioPropiedad.class);
        
        // Instanciamos el observador inyectándole el repositorio
        observador = new ObservadorLiberarPropiedades(repositorioPropiedadMock);
    }

    @Test
    public void dadoQueUnJugadorAbandonaCuandoElObservadorRecibeElEventoDaComoResultadoSusPropiedadesLiberadas() {
        // 1. Preparación (Arrange)
        Jugador jugadorQueAbandona = new Jugador();
        jugadorQueAbandona.setId(1L);
        Partida partida = new Partida();

        Propiedad casaMatanza = new Propiedad();
        casaMatanza.setPropietario(jugadorQueAbandona);
        casaMatanza.setNivelMejora(2); // Tenía casas construidas

        Propiedad deptoCasanova = new Propiedad();
        deptoCasanova.setPropietario(jugadorQueAbandona);
        deptoCasanova.setNivelMejora(0);

        List<Propiedad> propiedadesDelJugador = Arrays.asList(casaMatanza, deptoCasanova);

        // Cuando el observador busque las propiedades del jugador, le devolvemos estas dos
        when(repositorioPropiedadMock.buscarPropiedadesPorPropietario(jugadorQueAbandona))
                .thenReturn(propiedadesDelJugador);

        EventoJugadorAbandonado evento = new EventoJugadorAbandonado(partida, jugadorQueAbandona);

        // 2. Ejecución (Act)
        // Asumo que tu interfaz de observadores usa el método "actualizar" o "notificar"
        observador.actualizar(evento); 

        // 3. Validación (Assert)
        assertNull(casaMatanza.getPropietario(), "La primera propiedad debería no tener dueño");
        assertEquals(0, casaMatanza.getNivelMejora(), "Las mejoras de la primera propiedad deberían resetearse a 0");
        
        assertNull(deptoCasanova.getPropietario(), "La segunda propiedad debería no tener dueño");
        
        // Verificamos que se haya mandado a guardar la actualización en la BD para ambas
        verify(repositorioPropiedadMock).guardarPropiedad(casaMatanza);
        verify(repositorioPropiedadMock).guardarPropiedad(deptoCasanova);
    }
}