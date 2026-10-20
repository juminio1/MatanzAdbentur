package com.tallerwebi.infraestructura;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
class RepositorioSalaDeEsperaTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    @Transactional
    @Rollback
    void persisteYRecuperaLosCuatroColoresDeFicha() {
        Usuario creador = new Usuario();
        creador.setEmail("colores@example.test");
        creador.setUsername("PruebaColores");
        sessionFactory.getCurrentSession().persist(creador);
        SalaDeEspera sala = new SalaDeEspera();
        sala.setCodigoUnico("COLORES");
        sala.setCreador(creador);
        sala.getFichasOcupadas().addAll(Arrays.asList(ColorFicha.values()));
        RepositorioSalaDeEspera repositorio = new RepositorioSalaDeEsperaImpl(sessionFactory);
        repositorio.guardar(sala);
        sessionFactory.getCurrentSession().flush();
        sessionFactory.getCurrentSession().clear();

        SalaDeEspera recuperada = repositorio.buscarPorCodigo("COLORES");

        assertEquals(4, recuperada.getFichasOcupadas().size());
        assertEquals(java.util.Set.of(ColorFicha.values()),
                java.util.Set.copyOf(recuperada.getFichasOcupadas()));
    }
}
