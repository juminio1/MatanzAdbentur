package com.tallerwebi.infraestructura;
import static org.junit.jupiter.api.Assertions.*;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
class RepositorioChatTest {
    @Autowired private SessionFactory sesiones;
    @Test void conservaLosMensajesAlRecargarYAislaCadaPartidaConUnMaximoDeCien() {
        var sesion = sesiones.getCurrentSession();
        Usuario creador = new Usuario(); creador.setUsername("chat"); creador.setEmail("chat@example.test");
        sesion.persist(creador);
        Jugador ana = new Jugador(); ana.setApodo("Ana");
        Partida primera = partida("UNO", creador, ana);
        Jugador pepe = new Jugador(); pepe.setApodo("Pepe");
        Partida segunda = partida("DOS", creador, pepe);
        var repositorio = new RepositorioChatImpl(sesiones);
        for (int i = 0; i < 101; i++) guardar(repositorio, primera, ana, "Mensaje " + i);
        guardar(repositorio, segunda, pepe, "Solo segunda partida");
        Long primeraId = primera.getId(); Long segundaId = segunda.getId();
        sesion.flush(); sesion.clear();

        var mensajes = repositorio.ultimos(primeraId);
        assertEquals(100, mensajes.size());
        assertEquals("Mensaje 100", mensajes.get(0).getTexto());
        assertEquals("Mensaje 1", mensajes.get(99).getTexto());
        assertTrue(mensajes.stream().allMatch(m -> m.getAutor().getApodo().equals("Ana")));
        assertEquals("Solo segunda partida", repositorio.ultimos(segundaId).get(0).getTexto());
    }
    private Partida partida(String codigo, Usuario creador, Jugador jugador) {
        Partida partida = new Partida(); partida.setCreador(creador); partida.setCodigoUnico(codigo);
        partida.setEstado(EstadoPartida.EN_CURSO); partida.setJugadores(List.of(jugador));
        sesiones.getCurrentSession().persist(partida); return partida;
    }
    private void guardar(RepositorioChat repositorio, Partida partida, Jugador autor, String texto) {
        MensajeChat mensaje = new MensajeChat(); mensaje.setPartida(partida); mensaje.setAutor(autor);
        mensaje.setTexto(texto); mensaje.setFecha(System.currentTimeMillis()); repositorio.guardar(mensaje);
    }
}
