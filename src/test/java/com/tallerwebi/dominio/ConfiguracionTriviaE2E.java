package com.tallerwebi.dominio;

import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.excepcion.ClienteIAException;
import com.tallerwebi.dominio.servicios.ServicioRuletaImpl;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Hace determinista la trivia durante las pruebas E2E, igual que ConfiguracionDadosE2E con los dados.
 * La dificultad de la ruleta se fija con una cabecera cuando el jugador la gira, y las preguntas salen
 * siempre del banco de data.sql: así el test puede buscar la respuesta correcta y no gasta tokens de Gemini.
 */
@Configuration
@Profile("e2e")
public class ConfiguracionTriviaE2E {

    /** Dificultad (FACIL, MEDIO o DIFICIL) que debe salir cuando el jugador gira la ruleta. */
    public static final String CABECERA_DIFICULTAD = "X-E2E-Dificultad";

    @Bean
    @Primary
    ServicioRuleta servicioRuletaE2E() {
        ServicioRuleta azar = new ServicioRuletaImpl(new Random());
        return () -> {
            String dificultad = leerCabecera(CABECERA_DIFICULTAD);
            // Sin cabecera (por ejemplo, en el giro automático) el sorteo sigue siendo aleatorio.
            return dificultad == null ? azar.sortearDificultad() : DificultadRuleta.valueOf(dificultad);
        };
    }

    @Bean
    @Primary
    ClienteIA clienteIAE2E() {
        return (dificultad, cantidad) -> {
            throw new ClienteIAException("Gemini está desactivado en las pruebas E2E");
        };
    }

    private static String leerCabecera(String nombre) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String valor = atributos.getRequest().getHeader(nombre);
            if (valor != null && !valor.isBlank()) {
                return valor;
            }
        }
        return null;
    }
}
