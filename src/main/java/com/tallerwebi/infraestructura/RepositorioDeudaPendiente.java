package com.tallerwebi.infraestructura;
import com.tallerwebi.dominio.entidades.DeudaPendiente;

public interface RepositorioDeudaPendiente {
    void guardar(DeudaPendiente deuda);
    DeudaPendiente buscarPorPartidaYJugador(String codigoPartida,Long idJugador);
    void eliminar(DeudaPendiente deuda);
}