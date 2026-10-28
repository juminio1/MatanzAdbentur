package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
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
class RepositorioDeudaPendienteTest {

    @Autowired
    private SessionFactory sessionFactory;

    private RepositorioDeudaPendiente repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioDeudaPendienteImpl(sessionFactory);
    }

    @Test
    void guardaYRecuperaLaDeudaCorrespondienteALaPartidaYAlJugador() {
        Usuario creador = new Usuario();
        creador.setUsername("CreadorDeudas");
        creador.setEmail("deudas@example.test");
        sessionFactory.getCurrentSession().persist(creador);
        Partida partida = crearPartida("DEUDA-1", creador);
        Partida otraPartida = crearPartida("DEUDA-2", creador);
        Jugador deudor = crearJugador("Deudor");
        Jugador otroJugador = crearJugador("Otro jugador");
        DeudaPendiente deuda = crearDeuda(partida, deudor, otroJugador, 250);

        repositorio.guardar(deuda);
        sessionFactory.getCurrentSession().persist(crearDeuda(partida, otroJugador, deudor, 100));
        sessionFactory.getCurrentSession().persist(crearDeuda(otraPartida, deudor, otroJugador, 500));
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        DeudaPendiente recuperada = repositorio.buscarPorPartidaYJugador("DEUDA-1", deudor.getId());

        assertNotNull(recuperada);
        assertEquals(deuda.getId(), recuperada.getId());
        assertEquals(partida.getId(), recuperada.getPartida().getId());
        assertEquals(deudor.getId(), recuperada.getJugadorDeudor().getId());
        assertEquals(otroJugador.getId(), recuperada.getJugadorAcreedor().getId());
        assertEquals(250, recuperada.getMontoPendiente());
        assertEquals("Alquiler pendiente", recuperada.getMotivo());
        assertNull(repositorio.buscarPorPartidaYJugador("NO_EXISTE", deudor.getId()));
        assertNull(repositorio.buscarPorPartidaYJugador("DEUDA-1", -1L));
    }

    private Partida crearPartida(String codigo, Usuario creador) {
        Partida partida = new Partida();
        partida.setCodigoUnico(codigo);
        partida.setCreador(creador);
        partida.setEstado(EstadoPartida.EN_CURSO);
        sessionFactory.getCurrentSession().persist(partida);
        return partida;
    }

    private Jugador crearJugador(String apodo) {
        Jugador jugador = new Jugador();
        jugador.setApodo(apodo);
        sessionFactory.getCurrentSession().persist(jugador);
        return jugador;
    }

    private DeudaPendiente crearDeuda(Partida partida, Jugador deudor, Jugador acreedor, int monto) {
        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setPartida(partida);
        deuda.setJugadorDeudor(deudor);
        deuda.setJugadorAcreedor(acreedor);
        deuda.setMontoPendiente(monto);
        deuda.setMotivo("Alquiler pendiente");
        return deuda;
    }
}
