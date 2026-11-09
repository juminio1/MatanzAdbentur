package com.tallerwebi.postgresql;

import static org.junit.jupiter.api.Assertions.*;

import com.tallerwebi.config.DatabaseInitializationConfig;
import com.tallerwebi.config.HibernateConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfectoEventoGlobal;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.infraestructura.RepositorioEventoGlobalImpl;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class EventosGlobalesPostgreSQLTest {
    private static String esquema;
    private HikariDataSource fuenteAdministrador;
    private JdbcTemplate administrador;

    @Configuration
    static class Config extends HibernateConfig {
        @Bean(destroyMethod = "close")
        @Override
        public DataSource dataSource() {
            HikariDataSource fuente = (HikariDataSource) super.dataSource();
            fuente.setJdbcUrl(fuente.getJdbcUrl() + "?currentSchema=" + esquema);
            return fuente;
        }
    }

    @BeforeEach
    void aislarEsquema() {
        esquema = "eventos_" + UUID.randomUUID().toString().replace("-", "");
        fuenteAdministrador = (HikariDataSource) new HibernateConfig().dataSource();
        administrador = new JdbcTemplate(fuenteAdministrador);
        administrador.execute("CREATE SCHEMA " + esquema);
    }

    @AfterEach
    void eliminarSoloElEsquemaDeEstaPrueba() {
        administrador.execute("DROP SCHEMA " + esquema + " CASCADE");
        fuenteAdministrador.close();
    }

    @Test
    void dadoQueLaBaseEstaVaciaCuandoIniciaLaAplicacionDaComoResultadoSeisTiposJoinedConDatosCompletos() {
        try (var contexto = iniciar()) {
            verificarCatalogo(contexto);
            var jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM Partida", Integer.class));
            assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM Jugador", Integer.class));
            assertEquals(6, jdbc.queryForObject("SELECT count(*) FROM Evento e JOIN EventoGlobal g ON g.id = e.id", Integer.class));
        }
    }

    @Test
    void dadoQueElScriptSeEjecutaVariasVecesCuandoSeInicializaDaComoResultadoLosMismosIdentificadoresSinDuplicados() {
        try (var contexto = iniciar()) {
            var jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
            List<Map<String, Object>> originales = jdbc.queryForList("SELECT * FROM EventoGlobal ORDER BY id");
            List<Map<String, Object>> casilleros = jdbc.queryForList("SELECT * FROM Evento e JOIN EventoCasillero c ON c.id = e.id ORDER BY e.id");
            for (int intento = 0; intento < 3; intento++) {
                contexto.getBean(DataSourceInitializer.class).afterPropertiesSet();
                verificarCatalogo(contexto);
            }
            assertEquals(originales, jdbc.queryForList("SELECT * FROM EventoGlobal ORDER BY id"));
            assertEquals(casilleros, jdbc.queryForList("SELECT * FROM Evento e JOIN EventoCasillero c ON c.id = e.id ORDER BY e.id"));
            assertEquals(20, jdbc.queryForObject("SELECT count(*) FROM Evento", Integer.class));
        }
    }

    @Test
    void dadoQueYaExisteElTipoConOtroNombreCuandoSeInicializaDaComoResultadoDatosActualizadosConElMismoId() {
        // Preparación
        try (var contexto = iniciar()) {
            var jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
            Integer identificador = jdbc.queryForObject("SELECT id FROM EventoGlobal WHERE tipoEventoGlobal = 'ROBO'", Integer.class);
            jdbc.update("UPDATE Evento SET nombre = 'Robo anterior', descripcion = 'Descripción antigua' WHERE id = ?", identificador);
            jdbc.update("UPDATE EventoGlobal SET valor = 7, tipoEfecto = 'GANAR_PLATA' WHERE id = ?", identificador);
            assertEquals("Robo anterior", jdbc.queryForObject("SELECT nombre FROM Evento WHERE id = ?", String.class, identificador));

            // Ejecución
            contexto.getBean(DataSourceInitializer.class).afterPropertiesSet();

            // Verificación
            verificarCatalogo(contexto);
            assertEquals(identificador, jdbc.queryForObject("SELECT id FROM EventoGlobal WHERE tipoEventoGlobal = 'ROBO'", Integer.class));
            assertEquals("ROBO", jdbc.queryForObject("SELECT nombre FROM Evento WHERE id = ?", String.class, identificador));
        }
    }

    @Test
    void dadoQueFaltaLaEspecializacionCuandoSeInicializaDaComoResultadoLaReutilizacionDelPadreExistente() {
        try (var contexto = iniciar()) {
            var jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
            Integer identificador = jdbc.queryForObject("SELECT id FROM EventoGlobal WHERE tipoEventoGlobal = 'ROBO'", Integer.class);
            jdbc.update("DELETE FROM EventoGlobal WHERE id = ?", identificador);
            contexto.getBean(DataSourceInitializer.class).afterPropertiesSet();
            verificarCatalogo(contexto);
            assertEquals(identificador, jdbc.queryForObject("SELECT id FROM EventoGlobal WHERE tipoEventoGlobal = 'ROBO'", Integer.class));
        }
    }

    @Test
    void dadoQueLaDescripcionAnteriorAdmite255CaracteresCuandoSeInicializaDaComoResultadoAfipSinTruncar() {
        administrador.execute("CREATE TABLE " + esquema + ".Evento (id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, nombre varchar(255) UNIQUE, descripcion varchar(255))");
        try (var contexto = iniciar()) {
            verificarCatalogo(contexto);
            var jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
            assertEquals(255, jdbc.queryForObject("SELECT character_maximum_length FROM information_schema.columns WHERE table_schema = ? AND table_name = 'evento' AND column_name = 'descripcion'", Integer.class, esquema));
        }
    }

    @Test
    void dadoQueRoboTeniaMilCuandoSeInicializaDaComoResultadoQuinientosSinCambiarTarjetasNiIdentificador() {
        try (var contexto = iniciar()) {
            var jdbc = new JdbcTemplate(contexto.getBean(DataSource.class));
            Integer identificador = jdbc.queryForObject("SELECT id FROM EventoGlobal WHERE tipoEventoGlobal = 'ROBO'", Integer.class);
            var tarjetas = jdbc.queryForList("SELECT * FROM Evento e JOIN EventoCasillero c ON c.id = e.id ORDER BY e.id");
            jdbc.update("UPDATE EventoGlobal SET valor = 1000 WHERE id = ?", identificador);
            jdbc.update("UPDATE Evento SET descripcion = 'Robo de $1000' WHERE id = ?", identificador);
            contexto.getBean(DataSourceInitializer.class).afterPropertiesSet();
            assertEquals(500, jdbc.queryForObject("SELECT valor FROM EventoGlobal WHERE id = ?", Integer.class, identificador));
            assertTrue(jdbc.queryForObject("SELECT descripcion FROM Evento WHERE id = ?", String.class, identificador).contains("$500"));
            assertEquals(tarjetas, jdbc.queryForList("SELECT * FROM Evento e JOIN EventoCasillero c ON c.id = e.id ORDER BY e.id"));
        }
    }

    private AnnotationConfigApplicationContext iniciar() {
        return new AnnotationConfigApplicationContext(Config.class, DatabaseInitializationConfig.class);
    }

    private void verificarCatalogo(AnnotationConfigApplicationContext contexto) {
        SessionFactory sesiones = contexto.getBean(SessionFactory.class);
        new TransactionTemplate(contexto.getBean(PlatformTransactionManager.class)).executeWithoutResult(estado -> {
            List<EventoGlobal> eventos = new RepositorioEventoGlobalImpl(sesiones).obtenerTodos();
            assertEquals(6, eventos.size());
            assertEquals(6, eventos.stream().map(EventoGlobal::getTipoEventoGlobal).distinct().count());
            int[] valores = {0, -3, 0, 500, 400, 1};
            var efectos = List.of(TipoEfectoEventoGlobal.MOVER_CASILLEROS, TipoEfectoEventoGlobal.MOVER_CASILLEROS,
                    TipoEfectoEventoGlobal.VOLVER_INICIO, TipoEfectoEventoGlobal.PERDER_PLATA,
                    TipoEfectoEventoGlobal.GANAR_PLATA, TipoEfectoEventoGlobal.PERDER_TURNO);
            Map<TipoEventoGlobal, String> descripciones = Map.of(
                    TipoEventoGlobal.TRAMITES_AFIP, "Caíste a hacer un trámite y hay terrible fila. Todos tiran los dados y el que saque la mayor suma se saltea toda la fila y avanza hasta el casillero del jugador más adelantado. Si hay empate, los empatados vuelven a tirar hasta definir un ganador.",
                    TipoEventoGlobal.INUNDACION, "Se largó el diluvio y el barrio parece una pileta. Todos tiran los dados y el que saque la mayor suma se lo lleva el agua puesto y retrocede 3 casilleros. Si hay empate, los empatados vuelven a tirar.",
                    TipoEventoGlobal.PIQUETE, "Quedaste atrapado en un piquete de volqueteros. Todos tiran los dados y el que saque la menor suma vuelve al Inicio. Si hay empate, los empatados vuelven a tirar.",
                    TipoEventoGlobal.ROBO, "¡Cuidado, andan robando por la zona! [nombre del jugador] fue víctima de un robo y perdió $500.",
                    TipoEventoGlobal.FIAT_600, "Se le quedó el Fiat 600 a Germancho. [nombre del jugador] se puso la 10 para ayudarlo a empujar la máquina y recibió unos mangos por la changuita.",
                    TipoEventoGlobal.ACCIDENTE_SARMIENTO, "Hubo un accidente en el tren Sarmiento y el servicio quedó parado. Justo [nombre del jugador] venía viajando y perdió el turno esperando que vuelva a funcionar.");
            for (EventoGlobal evento : eventos) {
                assertEquals(valores[evento.getTipoEventoGlobal().ordinal()], evento.getValor());
                assertEquals(efectos.get(evento.getTipoEventoGlobal().ordinal()), evento.getTipoEfecto());
                assertEquals(descripciones.get(evento.getTipoEventoGlobal()), evento.getDescripcion());
            }
        });
    }
}
