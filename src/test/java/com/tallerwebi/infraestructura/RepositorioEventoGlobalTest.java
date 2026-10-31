package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.Evento.Evento;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEfectoEvento;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
@Rollback
class RepositorioEventoGlobalTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    void dadoQueSoloHayEventosPorCasilleroCuandoSeBuscaUnGlobalEntoncesDevuelveNull() {
        sessionFactory.getCurrentSession().persist(crearEventoCasillero());
        RepositorioEvento repositorio = new RepositorioEventoImpl(sessionFactory);

        EventoGlobal resultado = repositorio.obtenerEventoGlobalAleatorio();

        assertNull(resultado);
    }

    @Test
    void dadoQueHayUnEventoGlobalCuandoSeRecuperaEntoncesConservaLosDatosDeLaRaizYDelSubtipo() {
        EventoCasillero casillero = crearEventoCasillero();
        EventoGlobal global = crearEventoGlobal("Global de prueba");
        sessionFactory.getCurrentSession().persist(casillero);
        sessionFactory.getCurrentSession().persist(global);
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        EventoGlobal resultado = new RepositorioEventoImpl(sessionFactory).obtenerEventoGlobalAleatorio();

        assertNotNull(resultado);
        assertEquals(global.getId(), resultado.getId());
        assertEquals(Integer.class, resultado.getId().getClass());
        assertNotEquals(casillero.getId(), resultado.getId());
        assertEquals(global.getNombre(), resultado.getNombre());
        assertEquals(global.getDescripcion(), resultado.getDescripcion());
        assertEquals(global.getTipoEfecto(), resultado.getTipoEfecto());
        assertEquals(global.getValor(), resultado.getValor());
        assertEquals(global.getTipoEventoGlobal(), resultado.getTipoEventoGlobal());
        Evento raiz = sessionFactory.getCurrentSession().find(Evento.class, resultado.getId());
        assertTrue(raiz instanceof EventoGlobal);
    }

    @Test
    void dadoQueHayDosGlobalesDelMismoTipoCuandoSePersistenEntoncesSeConservanAmbosRegistros() {
        EventoGlobal primero = crearEventoGlobal("Global original uno");
        EventoGlobal segundo = crearEventoGlobal("Global original dos");
        sessionFactory.getCurrentSession().persist(primero);
        sessionFactory.getCurrentSession().persist(segundo);
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        List<EventoGlobal> resultado = sessionFactory.getCurrentSession()
                .createQuery("FROM EventoGlobal", EventoGlobal.class).list();

        assertEquals(2, resultado.size());
        assertNotEquals(primero.getId(), segundo.getId());
        assertTrue(resultado.stream().anyMatch(evento -> primero.getId().equals(evento.getId())));
        assertTrue(resultado.stream().anyMatch(evento -> segundo.getId().equals(evento.getId())));
    }

    private EventoGlobal crearEventoGlobal(String nombre) {
        EventoGlobal evento = new EventoGlobal();
        evento.setNombre(nombre);
        evento.setDescripcion("Inundacion de prueba");
        evento.setTipoEfecto(TipoEfecto.MOVER_CASILLEROS);
        evento.setValor(-3);
        evento.setTipoEventoGlobal(TipoEventoGlobal.INUNDACION);
        return evento;
    }

    private EventoCasillero crearEventoCasillero() {
        return new EventoCasillero(null, "Premio de prueba", "Cobrar premio",
                TipoEfectoEvento.SUMA, 200, null, null);
    }
}
