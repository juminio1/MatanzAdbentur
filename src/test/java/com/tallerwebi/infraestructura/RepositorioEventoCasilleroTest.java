package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
class RepositorioEventoCasilleroTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    void recuperaTodosLosEventosPersistidosConSusDatos() {
        RepositorioEventoCasillero repositorio = new RepositorioEventoCasilleroImpl(sessionFactory);
        EventoCasillero premio = new EventoCasillero(
                null, "Premio", "Cobrar premio", TipoEfectoEvento.SUMA, 200, null, null);
        EventoCasillero avance = new EventoCasillero(
                null, "Avance", "Avanzar tres casilleros", TipoEfectoEvento.AVANZA_CASILLEROS,
                null, 3, null);
        EventoCasillero destino = new EventoCasillero(
                null, "Destino", "Ir al casillero diez", TipoEfectoEvento.AVANZA_A_POSICION,
                null, null, 10);
        List<EventoCasillero> esperados = List.of(premio, avance, destino);
        esperados.forEach(evento -> sessionFactory.getCurrentSession().persist(evento));
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        List<EventoCasillero> recuperados = repositorio.obtenerTodos();

        assertEquals(3, recuperados.size());
        assertEquals(Set.of(premio.getId(), avance.getId(), destino.getId()),
                recuperados.stream().map(EventoCasillero::getId).collect(Collectors.toSet()));
        for (EventoCasillero esperado : esperados) {
            EventoCasillero recuperado = recuperados.stream()
                    .filter(evento -> esperado.getId().equals(evento.getId()))
                    .findFirst()
                    .orElseThrow();
            assertEquals(esperado.getNombre(), recuperado.getNombre());
            assertEquals(esperado.getDescripcion(), recuperado.getDescripcion());
            assertEquals(esperado.getTipoEfecto(), recuperado.getTipoEfecto());
            assertEquals(esperado.getMonto(), recuperado.getMonto());
            assertEquals(esperado.getCantidadCasilleros(), recuperado.getCantidadCasilleros());
            assertEquals(esperado.getPosicionDestino(), recuperado.getPosicionDestino());
        }
    }
}
