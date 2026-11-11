package com.tallerwebi.infraestructura;
import com.tallerwebi.dominio.entidades.MensajeChat;
import java.util.List;
public interface RepositorioChat {
    void guardar(MensajeChat mensaje);
    List<MensajeChat> ultimos(Long partida);
}
