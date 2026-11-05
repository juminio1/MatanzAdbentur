package com.tallerwebi.dominio.servicios.interfaces;
import java.util.List;
public interface ServicioChat {
    record Mensaje(Long id, Long idJugador, String apodo, String texto, Long fecha) { }
    List<Mensaje> listar(String codigo, Long jugador);
    Mensaje enviar(String codigo, Long jugador, String texto);
}
