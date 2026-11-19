package com.tallerwebi.dominio;

import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.entidades.Evento.EventoGlobal;
import com.tallerwebi.infraestructura.RepositorioEventoCasillero;
import com.tallerwebi.infraestructura.RepositorioEventoCasilleroImpl;
import com.tallerwebi.infraestructura.RepositorioEventoGlobal;
import com.tallerwebi.infraestructura.RepositorioEventoGlobalImpl;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Hace deterministas los eventos durante las pruebas E2E, igual que ConfiguracionDadosE2E con los dados.
 * Sin cabecera, los catálogos se devuelven completos y el sorteo sigue siendo aleatorio.
 */
@Configuration
@Profile("e2e")
public class ConfiguracionEventosE2E {

    /** Nombre del evento de casillero (codificado como URL) que debe salir en la tirada. */
    public static final String CABECERA_EVENTO_CASILLERO = "X-E2E-Evento";
    /** Tipo del evento global (por ejemplo PIQUETE) que debe salir al comenzar la ronda de evento. */
    public static final String CABECERA_EVENTO_GLOBAL = "X-E2E-Evento-Global";

    @Bean
    @Primary
    RepositorioEventoCasillero repositorioEventoCasilleroE2E(SessionFactory sessionFactory) {
        RepositorioEventoCasillero catalogo = new RepositorioEventoCasilleroImpl(sessionFactory);
        return () -> {
            List<EventoCasillero> eventos = catalogo.obtenerTodos();
            String nombre = leerCabecera(CABECERA_EVENTO_CASILLERO);
            if (nombre == null) {
                return eventos;
            }
            List<EventoCasillero> elegidos = eventos.stream()
                .filter(evento -> nombre.equals(evento.getNombre()))
                .toList();
            if (elegidos.isEmpty()) {
                throw new IllegalArgumentException("No existe el evento de casillero E2E: " + nombre);
            }
            return elegidos;
        };
    }

    @Bean
    @Primary
    RepositorioEventoGlobal repositorioEventoGlobalE2E(SessionFactory sessionFactory) {
        RepositorioEventoGlobal catalogo = new RepositorioEventoGlobalImpl(sessionFactory);
        return () -> {
            List<EventoGlobal> eventos = catalogo.obtenerTodos();
            String tipo = leerCabecera(CABECERA_EVENTO_GLOBAL);
            if (tipo == null) {
                return eventos;
            }
            EventoGlobal elegido = eventos.stream()
                .filter(evento -> evento.getTipoEventoGlobal().name().equals(tipo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe el evento global E2E: " + tipo));
            // Se conserva el tamaño del catálogo (ServicioEventoImpl lo valida) y cualquier sorteo elige este evento.
            return Collections.nCopies(eventos.size(), elegido);
        };
    }

    private static String leerCabecera(String nombre) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            String valor = atributos.getRequest().getHeader(nombre);
            if (valor != null && !valor.isBlank()) {
                return URLDecoder.decode(valor, StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
