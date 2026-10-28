package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.List;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
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
class RepositorioPropiedadTest {

    @Autowired
    private SessionFactory sessionFactory;

    private RepositorioPropiedad repositorio;

    @BeforeEach
    void preparar() {
        repositorio = new RepositorioPropiedadImpl(sessionFactory);
    }

    @Test
    void guardaUnaPropiedadNuevaYRecuperaSusDatosPorId() {
        Propiedad propiedad = crearPropiedad("San Justo", GrupoPropiedades.SAN_JUSTO);

        repositorio.guardarPropiedad(propiedad);
        vaciarSesion();

        Propiedad persistida = sessionFactory.getCurrentSession()
                .createQuery("FROM Propiedad WHERE nombre = :nombre", Propiedad.class)
                .setParameter("nombre", "San Justo")
                .getSingleResult();
        Long id = persistida.getId();
        sessionFactory.getCurrentSession().clear();
        Propiedad recuperada = repositorio.buscarPropiedadPorId(id);

        assertNotNull(recuperada);
        assertEquals(id, recuperada.getId());
        assertEquals("San Justo", recuperada.getNombre());
        assertEquals(300, recuperada.getPrecioCompra());
        assertEquals(50, recuperada.getPrecioAlquiler());
        assertEquals(GrupoPropiedades.SAN_JUSTO, recuperada.getGrupo());
    }

    @Test
    void actualizaUnaPropiedadSeparadaDeLaSesion() {
        Propiedad propiedad = crearPropiedad("San Justo", GrupoPropiedades.SAN_JUSTO);
        sessionFactory.getCurrentSession().persist(propiedad);
        vaciarSesion();
        propiedad.setPrecioAlquiler(100);
        propiedad.setNivelMejora(1);

        repositorio.guardarPropiedad(propiedad);
        vaciarSesion();

        Propiedad recuperada = repositorio.buscarPropiedadPorId(propiedad.getId());
        assertNotNull(recuperada);
        assertEquals(100, recuperada.getPrecioAlquiler());
        assertEquals(1, recuperada.getNivelMejora());
    }

    @Test
    void devuelveNullSiNoExisteLaPropiedadBuscada() {
        assertNull(repositorio.buscarPropiedadPorId(-1L));
    }

    @Test
    void buscaSolamenteLasPropiedadesDelGrupoSolicitado() {
        Propiedad primera = crearPropiedad("Primera", GrupoPropiedades.SAN_JUSTO);
        Propiedad segunda = crearPropiedad("Segunda", GrupoPropiedades.SAN_JUSTO);
        Propiedad otroGrupo = crearPropiedad("Otra", GrupoPropiedades.RAMOS_MEJIA);
        sessionFactory.getCurrentSession().persist(primera);
        sessionFactory.getCurrentSession().persist(segunda);
        sessionFactory.getCurrentSession().persist(otroGrupo);
        vaciarSesion();

        List<Propiedad> encontradas = repositorio.buscarPropiedadesPorGrupo(GrupoPropiedades.SAN_JUSTO);

        assertEquals(java.util.Set.of(primera.getId(), segunda.getId()),
                encontradas.stream().map(Propiedad::getId).collect(java.util.stream.Collectors.toSet()));
        assertEquals(2, encontradas.size());
    }

    private Propiedad crearPropiedad(String nombre, GrupoPropiedades grupo) {
        return new Propiedad(null, nombre, 300, 50, null, grupo);
    }

    private void vaciarSesion() {
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();
    }
}
