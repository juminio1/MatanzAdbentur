package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.enums.TipoEvento;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.infraestructura.RepositorioCasillero;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("servicioCasillero")
@Transactional
public class ServicioCasilleroImpl implements ServicioCasillero {

    private RepositorioCasillero repositorioCasillero;

    @Autowired
    public ServicioCasilleroImpl(RepositorioCasillero repositorioCasillero) {
        this.repositorioCasillero = repositorioCasillero;
    }

    @Override
    public List<Casillero> obtenerTodosLosCasilleros() {
        return repositorioCasillero.obtenerTodos();
    }

    /*@Override
    public Casillero buscarPorId(Long id) {
        return null;
    }

    //Metodo para casilleroEvento
@Override
public boolean aplicarEvento(CasilleroEvento evento, Jugador jugador) {
    if (evento.getTipoEvento() == TipoEvento.SUMA) {
       // jugador.agregarDinero(evento.getMonto());
        return true;
    }

    if (evento.getTipoEvento() == TipoEvento.RESTA) {
        //jugador.quitarDinero(evento.getMonto());
        return true;
    }

    return false;
}
    //Metodos para casilleroPropiedad---------------
    @Override
    public boolean comprarPropiedad(Jugador jugador, Propiedad propiedad) {
        if(!propiedad.estaDisponible()){
            return false;
        }

        if(jugador.getDinero()< propiedad.getPrecioCompra()){
            return false;
        }

        jugador.restarDinero(propiedad.getPrecioCompra());
        propiedad.asignarPropietario(jugador);
        //jugador.agregarPropiedad(propiedad);
        return true;
    }

    @Override
    public void cobrarAlquiler(Jugador jugador, Propiedad propiedad) {
        Jugador propietario = propiedad.getPropietario();

        if(propietario == null){
            return;
        }

        Integer alquiler = propiedad.getPrecioAlquiler();
        jugador.restarDinero(alquiler);
       // propietario.agregarDinero(alquiler);
    }

     */
}
