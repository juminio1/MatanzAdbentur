package com.tallerwebi.dominio.catalogo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CatalogoTableroTest {

    // posición -> [precio, alquiler base]
    private static final Map<Integer, List<Integer>> PRECIOS_ESPERADOS = Map.ofEntries(
        Map.entry(1, List.of(150, 20)), Map.entry(2, List.of(150, 20)),
        Map.entry(4, List.of(200, 30)), Map.entry(5, List.of(200, 30)), Map.entry(7, List.of(200, 30)),
        Map.entry(9, List.of(250, 40)), Map.entry(11, List.of(250, 40)), Map.entry(13, List.of(250, 40)),
        Map.entry(15, List.of(300, 50)), Map.entry(16, List.of(300, 50)),
        Map.entry(18, List.of(350, 60)), Map.entry(19, List.of(350, 60)),
        Map.entry(21, List.of(400, 70)), Map.entry(22, List.of(400, 70)),
        Map.entry(23, List.of(450, 80)), Map.entry(25, List.of(450, 80)),
        Map.entry(26, List.of(500, 100)), Map.entry(27, List.of(500, 100)), Map.entry(29, List.of(500, 100))
    );

    @Test
    void dadoCadaBarrioCuandoConsultoElCatalogoDaComoResultadoSuPrecioYAlquiler() {
        PRECIOS_ESPERADOS.forEach((posicion, valores) -> {
            assertEquals(valores.get(0), CatalogoTablero.precioCompra(posicion), "precio en " + posicion);
            assertEquals(valores.get(1), CatalogoTablero.alquilerBase(posicion), "alquiler en " + posicion);
        });
    }

    @Test
    void dadoLasParadasCuandoConsultoElCatalogoDaComoResultadoPrecioYAlquilerDeParada() {
        for (Integer posicion : CatalogoTablero.POSICIONES_PARADA) {
            assertEquals(CatalogoTablero.PRECIO_PARADA, CatalogoTablero.precioCompra(posicion));
            assertEquals(CatalogoTablero.ALQUILER_PARADA, CatalogoTablero.alquilerBase(posicion));
        }
    }

    @Test
    void dadoTodasLasPropiedadesCuandoConsultoElPrecioDaComoResultadoUnValorParaCadaUna() {
        int propiedadesConPrecio = PRECIOS_ESPERADOS.size() + CatalogoTablero.POSICIONES_PARADA.size();
        assertEquals(CatalogoTablero.POSICIONES_PROPIEDAD.size(), propiedadesConPrecio);
    }

    @Test
    void dadoCadaNivelDeMejoraCuandoCalculoElAlquilerDaComoResultadoElBaseMultiplicado() {
        assertEquals(20, CatalogoTablero.alquilerConMejora(20, 0));
        assertEquals(60, CatalogoTablero.alquilerConMejora(20, 1));
        assertEquals(100, CatalogoTablero.alquilerConMejora(20, 2));
        assertEquals(160, CatalogoTablero.alquilerConMejora(20, 3));
        assertEquals(240, CatalogoTablero.alquilerConMejora(20, 4));
        assertEquals(320, CatalogoTablero.alquilerConMejora(20, 5));
        assertEquals(1600, CatalogoTablero.alquilerConMejora(100, 5));
    }

    @Test
    void dadoUnNivelMayorAlMaximoCuandoCalculoElAlquilerDaComoResultadoElDelNivelMaximo() {
        assertEquals(1600, CatalogoTablero.alquilerConMejora(100, 8));
    }

    @Test
    void dadaUnaPropiedadSinMejorasCuandoCalculoElPrecioDeVentaDaComoResultadoLaMitadDelPrecio() {
        assertEquals(250, CatalogoTablero.precioVenta(500, 0));
    }

    @Test
    void dadaUnaPropiedadMejoradaCuandoCalculoElPrecioDeVentaDaComoResultadoLaMitadDeLoInvertido() {
        // Ramos Mejía con 3 mejoras: (500 + 3 * 250) / 2
        assertEquals(625, CatalogoTablero.precioVenta(500, 3));
    }

    @Test
    void dadoUnNivelFueraDeRangoCuandoCalculoElPrecioDeVentaLoLimitaEntreCeroYElMaximo() {
        assertEquals(250, CatalogoTablero.precioVenta(500, -2));
        assertEquals(875, CatalogoTablero.precioVenta(500, 9));
    }

    @Test
    void dadoElBarrioCompletoSinMejorasCuandoCalculoElAlquilerDaComoResultadoElDoble() {
        assertEquals(40, CatalogoTablero.alquilerDeBarrio(20, 0, true));
    }

    @Test
    void dadoElBarrioIncompletoCuandoCalculoElAlquilerNoSeDuplica() {
        assertEquals(20, CatalogoTablero.alquilerDeBarrio(20, 0, false));
    }

    @Test
    void dadoElBarrioCompletoConMejorasCuandoCalculoElAlquilerNoSeDuplica() {
        assertEquals(60, CatalogoTablero.alquilerDeBarrio(60, 1, true));
    }

    @Test
    void dadoElBarrioCompletoCuandoMejoroUnaVezElAlquilerSubeIgual() {
        int barrioCompleto = CatalogoTablero.alquilerDeBarrio(20, 0, true);
        int primeraMejora = CatalogoTablero.alquilerConMejora(20, 1);
        assertTrue(primeraMejora > barrioCompleto);
    }

    @Test
    void dadoElPrecioDeUnaPropiedadCuandoCalculoElCostoDeMejoraDaComoResultadoLaMitad() {
        assertEquals(75, CatalogoTablero.costoMejora(150));
        assertEquals(250, CatalogoTablero.costoMejora(500));
    }

    @Test
    void dadoUnCasilleroQueNoEsPropiedadCuandoConsultoElPrecioDaComoResultadoError() {
        for (Integer posicion : List.of(0, 3, 10, 20)) {
            assertThrows(IllegalArgumentException.class, () -> CatalogoTablero.precioCompra(posicion));
            assertThrows(IllegalArgumentException.class, () -> CatalogoTablero.alquilerBase(posicion));
        }
    }
}
