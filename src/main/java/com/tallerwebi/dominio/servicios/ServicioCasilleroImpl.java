package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.infraestructura.RepositorioCasillero;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import static com.tallerwebi.dominio.catalogo.CatalogoTablero.*;
import java.util.Objects;

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

    @Override
    public void guardarCasillero(Casillero casillero) {
        repositorioCasillero.guardar(casillero);
    }

    @Override
    public CasilleroPropiedad buscarCasilleroPropiedad(Partida partida, Integer posicion) {
        if (posicion == null || !existePosicionDePropiedad(partida, posicion)) return null;
        return obtenerCasillerosPropiedadDePartida(partida).stream()
                .filter(casillero -> posicion.equals(casillero.getPosicion()))
                .findFirst().orElse(null);
    }

    @Override
    public List<CasilleroPropiedad> obtenerCasillerosPropiedadDePartida(Partida partida) {
        return obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(casillero -> casillero.getPropiedad() != null)
                .filter(casillero -> Objects.equals(partida.getId(), casillero.getPartidaId()))
                .toList();
    }

    @Override
    public Boolean existePosicionDePropiedad(Partida partida, Integer posicion) {
        return partida != null && POSICIONES_PROPIEDAD.contains(posicion);
    }

    @Override
    public List<Integer> obtenerPosicionesPropiedad() {
        return POSICIONES_PROPIEDAD.stream().sorted().toList();
    }

    @Override
    public CasilleroPropiedad buscarOCrearPropiedadEnPosicion(Partida partida, Integer posicion) {
        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        return casillero != null ? casillero : crearCasilleroPropiedad(partida, posicion);
    }

    @Override
    public Integer obtenerAlquilerBaseCatalogo(Integer posicion) {
        return alquilerBase(posicion);
    }

    @Override
    public List<String> obtenerDatosCatalogoPropiedad(Integer posicion) {
        return CATALOGO_PROPIEDADES.get(posicion);
    }

     private CasilleroPropiedad crearCasilleroPropiedad(Partida partida, Integer posicion) {
        List<String> datos = CATALOGO_PROPIEDADES.get(posicion);
        if (datos == null) return null;
        Integer alquilerBase = obtenerAlquilerBaseCatalogo(posicion);
        Propiedad propiedad = new Propiedad(null, datos.get(0), PRECIO_COMPRA, alquilerBase, null,
                POSICIONES_PARADA.contains(posicion) ? null : GrupoPropiedades.valueOf(datos.get(2)));
        CasilleroPropiedad casillero = new CasilleroPropiedad(null, posicion, datos.get(0), datos.get(1), propiedad);
        casillero.setPartidaId(partida.getId());
        guardarCasillero(casillero);
        return casillero;
    }
}
