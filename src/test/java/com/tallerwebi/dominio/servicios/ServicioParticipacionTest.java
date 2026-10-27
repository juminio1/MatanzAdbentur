package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.servicios.ServicioParticipacionImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicioParticipacionTest {
    private final ServicioParticipacionImpl servicio = new ServicioParticipacionImpl();

    @Test
    void soloParticipanJugadoresSinAbandonoNiQuiebra() {
        Jugador jugador = new Jugador();
        assertTrue(servicio.estaActivo(jugador));
        jugador.setAbandono(true);
        assertFalse(servicio.estaActivo(jugador));
        jugador.setAbandono(false);
        jugador.setEnBancarrota(true);
        assertFalse(servicio.estaActivo(jugador));
        assertFalse(servicio.estaActivo(null));
    }

    @Test
    void administrarRequiereQueElJugadorCoincidaConElIdentificadorDeLaPartida() {
        Jugador miembro = new Jugador();
        miembro.setId(1L);
        Jugador ajeno = new Jugador();
        ajeno.setId(2L);
        Partida partida = new Partida();
        partida.setJugadores(List.of(miembro));
        assertTrue(servicio.permiteAdministrarPropiedades(partida, miembro, 1L));
        assertFalse(servicio.permiteAdministrarPropiedades(partida, ajeno, 1L));
        assertFalse(servicio.permiteAdministrarPropiedades(partida, ajeno, 2L));
        miembro.setAbandono(true);
        assertFalse(servicio.permiteAdministrarPropiedades(partida, miembro, 1L));
    }

    @Test
    void pertenenciaNoExigeEstarActivoYRechazaDatosAusentes() {
        Jugador jugador = new Jugador();
        jugador.setId(1L);
        jugador.setEnBancarrota(true);
        Partida partida = new Partida();
        partida.setJugadores(List.of(jugador));
        assertTrue(servicio.pertenece(partida, 1L));
        assertFalse(servicio.pertenece(partida, 2L));
        assertFalse(servicio.pertenece(partida, null));
        assertFalse(servicio.pertenece(null, 1L));
    }
}
