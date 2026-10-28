package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroEvento;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.enums.TipoEvento;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
@Transactional
@Rollback
class RepositorioCasilleroTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    void guardaYRecuperaCasillerosConSusSubtiposYDatos() {
        RepositorioCasillero repositorio = new RepositorioCasilleroImpl(sessionFactory);
        Casillero inicio = new Casillero(null, 0, "Inicio", "Comienzo del recorrido");
        CasilleroEvento evento = new CasilleroEvento(
                null, 3, "Premio", "Cobrar premio", TipoEvento.SUMA, 200);
        Propiedad propiedad = new Propiedad(
                null, "San Justo", 300, 50, null, GrupoPropiedades.SAN_JUSTO);
        CasilleroPropiedad casilleroPropiedad = new CasilleroPropiedad(
                null, 2, "San Justo", "Propiedad disponible", propiedad);
        casilleroPropiedad.setPartidaId(10L);

        repositorio.guardar(inicio);
        repositorio.guardar(evento);
        repositorio.guardar(casilleroPropiedad);
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        List<Casillero> recuperados = repositorio.obtenerTodos();

        assertEquals(3, recuperados.size());
        assertEquals(Set.of(inicio.getId(), evento.getId(), casilleroPropiedad.getId()),
                recuperados.stream().map(Casillero::getId).collect(Collectors.toSet()));
        Casillero inicioRecuperado = buscarPorId(recuperados, inicio.getId());
        assertEquals(0, inicioRecuperado.getPosicion());
        assertEquals("Inicio", inicioRecuperado.getNombre());
        assertEquals("Comienzo del recorrido", inicioRecuperado.getDescripcion());
        CasilleroEvento eventoRecuperado = assertInstanceOf(
                CasilleroEvento.class, buscarPorId(recuperados, evento.getId()));
        assertEquals(3, eventoRecuperado.getPosicion());
        assertEquals("Premio", eventoRecuperado.getNombre());
        assertEquals("Cobrar premio", eventoRecuperado.getDescripcion());
        assertEquals(TipoEvento.SUMA, eventoRecuperado.getTipoEvento());
        assertEquals(200, eventoRecuperado.getMonto());
        CasilleroPropiedad propiedadRecuperada = assertInstanceOf(
                CasilleroPropiedad.class, buscarPorId(recuperados, casilleroPropiedad.getId()));
        assertEquals(2, propiedadRecuperada.getPosicion());
        assertEquals("San Justo", propiedadRecuperada.getNombre());
        assertEquals("Propiedad disponible", propiedadRecuperada.getDescripcion());
        assertEquals(10L, propiedadRecuperada.getPartidaId());
        assertNotNull(propiedadRecuperada.getPropiedad());
        assertEquals(propiedad.getId(), propiedadRecuperada.getPropiedad().getId());
        assertEquals("San Justo", propiedadRecuperada.getPropiedad().getNombre());
        assertEquals(300, propiedadRecuperada.getPropiedad().getPrecioCompra());
    }

    private Casillero buscarPorId(List<Casillero> casilleros, Long id) {
        return casilleros.stream()
                .filter(casillero -> id.equals(casillero.getId()))
                .findFirst()
                .orElseThrow();
    }
}
