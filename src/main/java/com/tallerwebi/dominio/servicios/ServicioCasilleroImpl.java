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
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service("servicioCasillero")
@Transactional
public class ServicioCasilleroImpl implements ServicioCasillero {

    private static final Set<Integer> POSICIONES_PROPIEDAD = Set.of(
            1, 2, 4, 5, 6, 7, 9, 11, 12, 13, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27, 29
    );

    private static final Set<Integer> POSICIONES_PARADA = Set.of(6, 12, 17, 24);
    private static final String DESCRIPCION_PARADA = "Parada de bondi";
    private static final String LOCALIDAD_SAN_JUSTO = "San Justo";
    private static final String GRUPO_SAN_JUSTO = GrupoPropiedades.SAN_JUSTO.name();
    private static final String LOCALIDAD_ISIDRO_CASANOVA = "Isidro Casanova";
    private static final String GRUPO_ISIDRO_CASANOVA = GrupoPropiedades.ISIDRO_CASANOVA.name();
    private static final Map<Integer, List<String>> CATALOGO_PROPIEDADES = Map.ofEntries(
            Map.entry(1, List.of("Plaza Barrio Marina", "Villa Luzuriaga", "VILLA_LUZURIAGA")),
            Map.entry(2, List.of("Parroquia Stella Maris", "Villa Luzuriaga", "VILLA_LUZURIAGA")),
            Map.entry(4, List.of("Plaza San Justo", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(5, List.of("UNLaM", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(6, List.of("Colectivo 242", DESCRIPCION_PARADA, "")),
            Map.entry(7, List.of("San Justo Shopping", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(9, List.of("Cancha de Brown", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(11, List.of("Jesse James", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(12, List.of("Colectivo 96", DESCRIPCION_PARADA, "")),
            Map.entry(13, List.of("Plaza Hipólito Yrigoyen", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(15, List.of("Feria Carlos Casares", "Rafael Castillo", "RAFAEL_CASTILLO")),
            Map.entry(16, List.of("Plaza Barrio La Casona", "Rafael Castillo", "RAFAEL_CASTILLO")),
            Map.entry(17, List.of("Colectivo 621", DESCRIPCION_PARADA, "")),
            Map.entry(18, List.of("Estación Laferrere", "Laferrere", "LAFERRERE")),
            Map.entry(19, List.of("Av. Luro", "Laferrere", "LAFERRERE")),
            Map.entry(21, List.of("Puente 12", "Ciudad Evita", "CIUDAD_EVITA")),
            Map.entry(22, List.of("Superchino Luna", "Ciudad Evita", "CIUDAD_EVITA")),
            Map.entry(23, List.of("Cementerio Israelita", "La Tablada", "LA_TABLADA")),
            Map.entry(24, List.of("Colectivo 620", DESCRIPCION_PARADA, "")),
            Map.entry(25, List.of("Regimiento de La Tablada", "La Tablada", "LA_TABLADA")),
            Map.entry(26, List.of("Bingo Ramos Mejía", "Ramos Mejía", "RAMOS_MEJIA")),
            Map.entry(27, List.of("Estación Ramos Mejía", "Ramos Mejía", "RAMOS_MEJIA")),
            Map.entry(29, List.of("Pinar de Rocha", "Ramos Mejía", "RAMOS_MEJIA"))
    );

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

    private CasilleroPropiedad crearCasilleroPropiedad(Partida partida, Integer posicion) {
        List<String> datos = CATALOGO_PROPIEDADES.get(posicion);
        if (datos == null) return null;
        Integer alquilerBase = obtenerAlquilerBaseCatalogo(posicion);
        Propiedad propiedad = new Propiedad(null, datos.get(0), 300, alquilerBase, null,
                POSICIONES_PARADA.contains(posicion) ? null : GrupoPropiedades.valueOf(datos.get(2)));
        CasilleroPropiedad casillero = new CasilleroPropiedad(null, posicion, datos.get(0), datos.get(1), propiedad);
        casillero.setPartidaId(partida.getId());
        guardarCasillero(casillero);
        return casillero;
    }

    @Override
    public Integer obtenerAlquilerBaseCatalogo(Integer posicion) {
        return POSICIONES_PARADA.contains(posicion) ? 20 : 50;
    }

    @Override
    public List<String> obtenerDatosCatalogoPropiedad(Integer posicion) {
        return CATALOGO_PROPIEDADES.get(posicion);
    }
}
