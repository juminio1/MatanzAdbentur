package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.*;

import com.tallerwebi.dominio.entidades.Evento.Evento;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.enums.TipoEfectoEventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
@Rollback
class RepositorioEventoGlobalTest {
    @Autowired private SessionFactory sesiones;

    @Test
    void dadoQueSePersistenLosSeisTiposCuandoSeRecuperanDaComoResultadoLaHerenciaJoinedConUnSoloIdentificador() {
        var sesion = sesiones.getCurrentSession();
        for (TipoEventoGlobal tipo : TipoEventoGlobal.values()) {
            EventoGlobal evento = new EventoGlobal();
            evento.setNombre(tipo.name());
            evento.setDescripcion("Descripción completa con acentos: trámite, inundación y piquete.");
            evento.setTipoEventoGlobal(tipo);
            evento.setTipoEfecto(TipoEfectoEventoGlobal.MOVER_CASILLEROS);
            evento.setValor(0);
            sesion.persist(evento);
        }
        EventoCasillero casillero = new EventoCasillero(null, "Casillero", "Evento de casillero",
                TipoEfectoEvento.SUMA, 100, null, null);
        sesion.persist(casillero);
        sesion.flush();
        sesion.clear();

        List<EventoGlobal> eventos = new RepositorioEventoGlobalImpl(sesiones).obtenerTodos();
        assertEquals(6, eventos.size());
        assertEquals(6, eventos.stream().map(EventoGlobal::getTipoEventoGlobal).distinct().count());
        for (EventoGlobal evento : eventos) {
            assertInstanceOf(EventoGlobal.class, sesion.find(Evento.class, evento.getId()));
            assertEquals("Descripción completa con acentos: trámite, inundación y piquete.", evento.getDescripcion());
            Number filas = (Number) sesion.createNativeQuery(
                    "SELECT COUNT(*) FROM Evento padre JOIN EventoGlobal hijo ON padre.id = hijo.id WHERE padre.id = :id",
                    Long.class).setParameter("id", evento.getId()).getSingleResult();
            assertEquals(1, filas.intValue());
        }
        assertEquals(1, new RepositorioEventoCasilleroImpl(sesiones).obtenerTodos().size());
        assertEquals(List.of("tipoEfecto", "valor", "tipoEventoGlobal"),
                Arrays.stream(EventoGlobal.class.getDeclaredFields()).map(java.lang.reflect.Field::getName).toList());
    }
}
