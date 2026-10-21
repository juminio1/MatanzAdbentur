package com.tallerwebi.postgresql;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tallerwebi.config.DatabaseInitializationConfig;
import com.tallerwebi.config.HibernateConfig;
import com.tallerwebi.dominio.entidades.Usuario;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "RUN_POSTGRES_TESTS", matches = "true")
class PostgreSQLTest {

  @Test
  void inicializaDosVecesYPermiteGuardarYConsultarUsuarios() {
    for (int intento = 0; intento < 2; intento++) {
      try (var context = new AnnotationConfigApplicationContext(
          HibernateConfig.class, DatabaseInitializationConfig.class)) {
        var sessions = context.getBean(SessionFactory.class);
        var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        transaction.executeWithoutResult(status -> {
          var session = sessions.getCurrentSession();
          assertEquals(1L, session.createQuery(
              "select count(u) from Usuario u where u.email = :email", Long.class)
              .setParameter("email", "test@unlam.edu.ar").uniqueResult());
          var usuario = new Usuario();
          String username = "pg_" + UUID.randomUUID();
          usuario.setUsername(username);
          usuario.setEmail(username + "@example.com");
          usuario.setPassword("prueba");
          session.persist(usuario);
          session.flush();
          assertNotNull(usuario.getId());
          session.clear();
          assertEquals(username, session.find(Usuario.class, usuario.getId()).getUsername());
          status.setRollbackOnly();
        });
      }
    }
  }
}
