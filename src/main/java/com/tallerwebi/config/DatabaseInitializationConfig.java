package com.tallerwebi.config;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
public class DatabaseInitializationConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    @DependsOn("sessionFactory")
    public DataSourceInitializer dataSourceInitializer() {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("migraciones/normalizar-colores-fichas.sql"));
        populator.addScript(new ClassPathResource("migraciones/actualizar-nombres-casilleros.sql"));
        populator.addScript(new ClassPathResource("data.sql"));

        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(populator);

        return initializer;
    }
}
