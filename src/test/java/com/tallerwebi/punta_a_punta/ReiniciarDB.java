package com.tallerwebi.punta_a_punta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ReiniciarDB {

  public static void limpiarBaseDeDatos() {
    String dbHost = variable("DB_HOST", "localhost");
    String dbPort = variable("DB_PORT", "5432");
    String dbName = variable("DB_NAME", "tallerwebi");
    String url = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);

    try (Connection connection = DriverManager.getConnection(
        url, variable("DB_USER", "user"), variable("DB_PASSWORD", "user"))) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("DELETE FROM Usuario");
        statement.execute("ALTER TABLE Usuario ALTER COLUMN id RESTART WITH 1");
        statement.executeUpdate(
            "INSERT INTO Usuario(email, username, password, rol, activo) "
                + "VALUES ('test@unlam.edu.ar', 'test123', 'test', 'ADMIN', true)");
        connection.commit();
      } catch (SQLException e) {
        connection.rollback();
        throw e;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("No se pudo reiniciar la base de datos de pruebas", e);
    }
  }

  private static String variable(String nombre, String valorPredeterminado) {
    String valor = System.getenv(nombre);
    return valor != null ? valor : valorPredeterminado;
  }
}
