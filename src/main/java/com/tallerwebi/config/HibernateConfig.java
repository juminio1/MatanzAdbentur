package com.tallerwebi.config;

import java.util.Properties;
import java.util.TimeZone;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.LocalSessionFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    private static final String ZONA_BUENOS_AIRES_ANTERIOR = "America/Buenos_Aires";

    @Bean
    public DataSource dataSource() {
        if (ZONA_BUENOS_AIRES_ANTERIOR.equals(TimeZone.getDefault().getID())) {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();

        String dbHost = System.getenv("DB_HOST");
        String dbPort = System.getenv("DB_PORT");
        String dbName = System.getProperty("DB_NAME", System.getenv("DB_NAME"));
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbHost == null) dbHost = "localhost";
        if (dbPort == null) dbPort = "5432";
        if (dbName == null) dbName = "tallerwebi"; //Nombre de la base de datos
        if (dbUser == null) dbUser = "user"; //Usuario
        if (dbPassword == null) dbPassword = "user"; //Password

        String url = String.format(
            "jdbc:postgresql://%s:%s/%s",
            dbHost,
            dbPort,
            dbName
        );

        dataSource.setUrl(url);
        dataSource.setUsername(dbUser);
        dataSource.setPassword(dbPassword);
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        LocalSessionFactoryBean sessionFactory = new LocalSessionFactoryBean();
        sessionFactory.setDataSource(dataSource);
        sessionFactory.setPackagesToScan("com.tallerwebi.dominio");
        sessionFactory.setHibernateProperties(hibernateProperties());
        return sessionFactory;
    }

    @Bean
    public HibernateTransactionManager transactionManager() {
        return new HibernateTransactionManager(sessionFactory(dataSource()).getObject());
    }

    private Properties hibernateProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.hbm2ddl.auto", "update"); // Actualiza la estructura de las tablas existentes sin recrearlas en cada ejecución/levantar el proyecto.
        return properties;
    }
}
