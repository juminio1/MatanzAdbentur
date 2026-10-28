package com.tallerwebi.presentacion.mapeadores;
import java.util.*;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import static com.tallerwebi.dominio.catalogo.CatalogoTablero.POSICIONES_PARADA;
public final class MapeadorPropiedad {
    private final ServicioCasillero servicioCasillero;
    private final ServicioPropiedad servicioPropiedad;
    public MapeadorPropiedad(ServicioCasillero casilleros, ServicioPropiedad propiedades) {
        servicioCasillero = casilleros;
        servicioPropiedad = propiedades;
    }
    public Map<String, Object> desdeCatalogo(Integer posicion) {
        List<String> datos = servicioCasillero.obtenerDatosCatalogoPropiedad(posicion);
        if (datos == null) return Map.of();
        Map<String, Object> dto = new HashMap<>();
        dto.put("posicion", posicion);
        dto.put("nombre", datos.get(0));
        dto.put("descripcion", datos.get(1));
        dto.put("precioCompra", com.tallerwebi.dominio.catalogo.CatalogoTablero.PRECIO_COMPRA);
        dto.put("precioVenta", servicioPropiedad.calcularPrecioVenta(com.tallerwebi.dominio.catalogo.CatalogoTablero.PRECIO_COMPRA));
        Integer alquilerBase = servicioCasillero.obtenerAlquilerBaseCatalogo(posicion);
        dto.put("alquiler", alquilerBase);
        dto.put("alquilerBase", alquilerBase);
        dto.put("nivelMejora", 0);
        dto.put("grupo", POSICIONES_PARADA.contains(posicion) ? null : datos.get(2));
        dto.put("propietario", null);
        dto.put("propietarioId", null);
        dto.put("disponible", true);
        return dto;
    }

    public Map<String, Object> desdePropiedad(CasilleroPropiedad casillero, Propiedad propiedad) {
        Map<String, Object> dto = new HashMap<>();
        List<String> datosCatalogo = servicioCasillero.obtenerDatosCatalogoPropiedad(casillero.getPosicion());
        Jugador propietario = propiedad.getPropietario();
        // Uso la posición para identificar el casillero.
        dto.put("posicion", casillero.getPosicion());
        dto.put("nombre", datosCatalogo == null ? casillero.getNombre() : datosCatalogo.get(0));
        dto.put(
                "descripcion",
                datosCatalogo == null ? casillero.getDescripcion() : datosCatalogo.get(1)
        );
        dto.put("precioCompra", propiedad.getPrecioCompra());
        dto.put(
                "precioVenta",
                servicioPropiedad.calcularPrecioVenta(propiedad.getPrecioCompra())
        );
        dto.put("alquiler", servicioPropiedad.calcularAlquiler(casillero, propiedad));
        dto.put("propietario", propietario == null ? null : propietario.getApodo());
        dto.put("propietarioId", propietario == null ? null : propietario.getId());
        dto.put("disponible", propietario == null);
        dto.put("idPropiedad", propiedad.getId());
        dto.put(
                "alquilerBase",
                POSICIONES_PARADA.contains(casillero.getPosicion()) ? 20 : propiedad.getPrecioAlquiler()
        );
        dto.put("nivelMejora", propiedad.getNivelMejora() == null ? 0 : propiedad.getNivelMejora());
        dto.put(
                "grupo",
                POSICIONES_PARADA.contains(casillero.getPosicion()) || propiedad.getGrupo() == null
                        ? null
                        : propiedad.getGrupo().name()
        );
        return dto;
    }

    /** Devuelve todas las propiedades del tablero: las registradas en la partida y, el resto, desde el catálogo. */
    public List<Map<String, Object>> catalogoTablero(Partida partida) {
        List<CasilleroPropiedad> casilleros = servicioCasillero.obtenerCasillerosPropiedadDePartida(partida);
        return servicioCasillero.obtenerPosicionesPropiedad().stream().map(posicion -> casilleros.stream()
                        .filter(casillero -> posicion.equals(casillero.getPosicion()))
                        .findFirst()
                        .map(casillero -> desdePropiedad(casillero, casillero.getPropiedad()))
                        .orElseGet(() -> desdeCatalogo(posicion)))
                .toList();
    }

}
