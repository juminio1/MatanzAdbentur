package com.tallerwebi.config;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import org.hsqldb.jdbc.JDBCConnection;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HibernateConfigTest {
    @Test
    void reutilizaLaConexionFisicaEntreSolicitudesYRestableceAutoCommit() throws Exception {
        HikariDataSource fuente = (HikariDataSource) new HibernateConfig().dataSource();
        // Base aislada: no usa las credenciales ni la base de la partida real.
        fuente.setDriverClassName("org.hsqldb.jdbcDriver");
        fuente.setJdbcUrl("jdbc:hsqldb:mem:pool_partida");
        fuente.setUsername("sa");
        fuente.setPassword("");
        fuente.setMaximumPoolSize(1);
        fuente.setMinimumIdle(1);
        try (fuente) {
            JDBCConnection fisica;
            try (Connection conexion = fuente.getConnection()) {
                fisica = conexion.unwrap(JDBCConnection.class);
                conexion.setAutoCommit(false);
                conexion.rollback();
            }
            for (int solicitud = 0; solicitud < 20; solicitud++) {
                try (Connection conexion = fuente.getConnection()) {
                    assertSame(fisica, conexion.unwrap(JDBCConnection.class));
                    assertTrue(conexion.getAutoCommit());
                    assertTrue(conexion.isValid(1));
                }
            }
            assertEquals(1, fuente.getHikariPoolMXBean().getTotalConnections());
        }
        assertTrue(fuente.isClosed());
    }
}
