package com.tallerwebi.dominio.servicios;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.RepositorioChat;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioChatImpl implements ServicioChat {
    private final ServicioPartida partidas;
    private final RepositorioChat repositorio;
    public ServicioChatImpl(ServicioPartida partidas, RepositorioChat repositorio) {
        this.partidas = partidas; this.repositorio = repositorio;
    }
    @Override @Transactional(readOnly = true)
    public List<Mensaje> listar(String codigo, Long jugador) {
        Partida partida = cargar(codigo, jugador);
        return repositorio.ultimos(partida.getId()).stream().sorted(Comparator.comparing(MensajeChat::getId))
            .map(this::datos).toList();
    }
    @Override public Mensaje enviar(String codigo, Long jugador, String texto) {
        Partida partida = cargar(codigo, jugador);
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isBlank() || limpio.length() > 500) {
            throw new IllegalArgumentException("Escribí un mensaje de entre 1 y 500 caracteres.");
        }
        Jugador autor = partida.getJugadores().stream().filter(j -> Objects.equals(j.getId(), jugador)).findFirst().orElseThrow();
        if (Boolean.TRUE.equals(autor.getAbandono())) throw new IllegalStateException("Ya abandonaste la partida.");
        MensajeChat mensaje = new MensajeChat();
        mensaje.setPartida(partida); mensaje.setAutor(autor); mensaje.setTexto(limpio);
        mensaje.setFecha(System.currentTimeMillis());
        repositorio.guardar(mensaje);
        return datos(mensaje);
    }
    private Partida cargar(String codigo, Long jugador) {
        Partida partida = partidas.obtenerPartida(codigo);
        if (partida == null || jugador == null || partida.getJugadores().stream().noneMatch(j -> Objects.equals(j.getId(), jugador))) {
            throw new SecurityException("No pertenecés a esta partida.");
        }
        return partida;
    }
    private Mensaje datos(MensajeChat mensaje) {
        return new Mensaje(mensaje.getId(), mensaje.getAutor().getId(), mensaje.getAutor().getApodo(), mensaje.getTexto(), mensaje.getFecha());
    }
}
