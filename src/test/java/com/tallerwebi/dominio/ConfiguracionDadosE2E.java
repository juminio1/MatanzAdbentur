package com.tallerwebi.dominio;

import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Profile("e2e")
public class ConfiguracionDadosE2E {

    public static final int DADO_UNO = 1;
    public static final int DADO_DOS = 3;
    public static final String CABECERA_TIRADA = "X-E2E-Tirada";

    @Bean
    @Primary
    ServicioDado servicioDadoE2E() {
        return this::tirarDadosControlados;
    }

    private ResultadoTirada tirarDadosControlados() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String tirada = atributos.getRequest().getHeader(CABECERA_TIRADA);
            if (tirada != null) {
                if (!tirada.matches("[1-6],[1-6]")) {
                    throw new IllegalArgumentException("La tirada E2E debe contener dos dados entre 1 y 6");
                }
                return new ResultadoTirada(Character.digit(tirada.charAt(0), 10), Character.digit(tirada.charAt(2), 10));
            }
        }
        return new ResultadoTirada(DADO_UNO, DADO_DOS);
    }
}
