
package com.tallerwebi.config;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tallerwebi.dominio.servicios.ServicioRuletaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;

@Configuration
public class ConfiguracionRuleta {// Se utiliza esta clase porq Spring por si solo no sabe como crear ciertos objetos q necesitan ser inyectados en los servicios.

    @Bean // Le indica a Spring que registre el objeto que devuelve el método
    public ServicioRuleta servicioRuleta() {
        return new ServicioRuletaImpl(new Random());
    }

    @Bean(destroyMethod = "shutdownNow")
    public ScheduledExecutorService planificadorRuleta() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
