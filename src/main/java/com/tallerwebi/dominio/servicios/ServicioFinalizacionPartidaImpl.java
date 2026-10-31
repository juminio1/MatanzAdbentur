package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import org.springframework.stereotype.Service;

@Service
public class ServicioFinalizacionPartidaImpl implements ServicioFinalizacionPartida {
    private final RepositorioPropiedad repositorioPropiedad;
    private final RepositorioDeudaPendiente repositorioDeudaPendiente;

    public ServicioFinalizacionPartidaImpl() {
        this(null, null);
    }

    @Autowired
    public ServicioFinalizacionPartidaImpl(RepositorioPropiedad repositorioPropiedad,
                                          RepositorioDeudaPendiente repositorioDeudaPendiente) {
        this.repositorioPropiedad = repositorioPropiedad;
        this.repositorioDeudaPendiente = repositorioDeudaPendiente;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer calcularPatrimonioTotal(String codigoPartida, Jugador jugador) {
        Integer patrimonio = jugador.getDinero();//obtiene el dinero del jugador
        //Suma el valor de casa una de sus propiedades.
        for (Propiedad propiedad : repositorioPropiedad.buscarPropiedadesPorPropietario(jugador)) {
            //por c/propiedad calcula su valor patrimonial(Suma el valor de la propiedad + su mejora)
            patrimonio += calcularValorPatrimonial(propiedad);
        }

        //busca si el jugador se quedo con deuda.
        DeudaPendiente deuda = repositorioDeudaPendiente
                .buscarPorPartidaYJugador(codigoPartida, jugador.getId());
        //si encuentra la deuda, se la descuenta al patrimonio total
        if (deuda != null) {
            patrimonio -= deuda.getMontoPendiente();
        }
        return patrimonio;
    }

    @Override
    public Integer calcularValorPatrimonial(Propiedad propiedad) {
        //guarda los costos de cada mejora. Cada posicion representa una mejora sucesiva.
        final int[] costosMejoras = {200, 300, 500, 1000, 1500};

        int nivelMejora;
        // si el nivel de mejora es null le asigna 0
        if (propiedad.getNivelMejora() == null) {
            nivelMejora = 0;
        } else {
            nivelMejora = propiedad.getNivelMejora();
        }
        //comprueba que el nivel de mejora este entre 0 y 5, sino lanza excepcion.
        if (nivelMejora < 0 || nivelMejora > costosMejoras.length) {
            throw new IllegalArgumentException("El nivel de mejora debe estar entre 0 y 5.");
        }
        Integer valorPatrimonial = propiedad.getPrecioCompra();
       //Recorre los costos de las mejoras que tiene la propiedad y los suma al precio de compra.
        for (int nivel = 0; nivel < nivelMejora; nivel++) {
            valorPatrimonial += costosMejoras[nivel];
        }
        return valorPatrimonial;
    }
}
