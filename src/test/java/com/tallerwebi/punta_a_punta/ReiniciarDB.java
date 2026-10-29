package com.tallerwebi.punta_a_punta;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class ReiniciarDB {

  public static void limpiarBaseDeDatos() {
    String dbHost = variable("DB_HOST", "localhost");
    String dbPort = variable("DB_PORT", "5432");
    // Solo se permite borrar datos de la base exclusiva de las pruebas E2E.
    String dbName = System.getProperty("DB_NAME", System.getenv("DB_NAME"));
    if (!"tallerwebi_e2e".equals(dbName)) {
      throw new IllegalStateException(
          "Limpieza E2E bloqueada: DB_NAME debe ser exactamente tallerwebi_e2e");
    }
    String url = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);

    try (Connection connection = DriverManager.getConnection(
        url, variable("DB_USER", "user"), variable("DB_PASSWORD", "user"))) {
      connection.setAutoCommit(false);
      try (Statement statement = connection.createStatement()) {
        statement.executeUpdate("DELETE FROM saladeespera_fichasocupadas");
        statement.executeUpdate("DELETE FROM sala_usuario");
        statement.executeUpdate("DELETE FROM sala_de_espera");
        statement.executeUpdate("DELETE FROM Usuario");
        statement.execute("ALTER TABLE Usuario ALTER COLUMN id RESTART WITH 1");
        String hashTest =
            "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08";
        statement.executeUpdate(
            "INSERT INTO Usuario(email, username, password, rol, activo) "
                + "VALUES ('test@unlam.edu.ar', 'test123', '"
                + hashTest
                + "', 'ADMIN', true)");
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
