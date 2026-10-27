package com.tallerwebi.dominio.catalogo;
import java.util.*;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
public final class CatalogoTablero {
    public static final int TOTAL_CASILLEROS = 30;
    public static final int PRECIO_COMPRA = 300;
    private CatalogoTablero() { }
    public static final Set<Integer> POSICIONES_PROPIEDAD = Set.of(
            1, 2, 4, 5, 6, 7, 9, 11, 12, 13, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27, 29
    );

    public static final Set<Integer> POSICIONES_PARADA = Set.of(6, 12, 17, 24);
    private static final String DESCRIPCION_PARADA = "Parada de bondi";
    private static final String LOCALIDAD_SAN_JUSTO = "San Justo";
    private static final String GRUPO_SAN_JUSTO = GrupoPropiedades.SAN_JUSTO.name();
    private static final String LOCALIDAD_ISIDRO_CASANOVA = "Isidro Casanova";
    private static final String GRUPO_ISIDRO_CASANOVA = GrupoPropiedades.ISIDRO_CASANOVA.name();
    public static final Map<Integer, List<String>> CATALOGO_PROPIEDADES = Map.ofEntries(
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

    public static int alquilerBase(Integer posicion) { return POSICIONES_PARADA.contains(posicion) ? 20 : 50; }
    public static Set<Integer> posicionesDelGrupo(GrupoPropiedades grupo) {
        if (grupo == null) return Set.of();
        return CATALOGO_PROPIEDADES.entrySet().stream()
            .filter(e -> grupo.name().equals(e.getValue().get(2)))
            .map(Map.Entry::getKey).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
