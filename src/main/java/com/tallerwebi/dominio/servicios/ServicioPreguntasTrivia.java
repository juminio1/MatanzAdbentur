package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.PreguntaTrivia;
import com.tallerwebi.dominio.enums.DificultadRuleta;
import com.tallerwebi.dominio.enums.DificultadTrivia;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.excepcion.ClienteIAException;
import com.tallerwebi.dominio.resultados.PreguntaGenerada;
import com.tallerwebi.dominio.servicios.interfaces.ClienteIA;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPreguntaTrivia;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class ServicioPreguntasTrivia {

    private static final int MAXIMO_PREGUNTAS_CASILLERO = 5;
    private static final Pattern TILDES = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARADORES = Pattern.compile("[^\\p{L}\\p{N}]+");

    private final ClienteIA clienteIA;
    private final RepositorioPreguntaTrivia repositorioPreguntas;
    private final RepositorioPartida repositorioPartida;
    private final TransactionTemplate transaccion;

    public ServicioPreguntasTrivia(ClienteIA clienteIA, RepositorioPreguntaTrivia repositorioPreguntas,
                                   RepositorioPartida repositorioPartida,
                                   PlatformTransactionManager administradorTransacciones) {
        this.clienteIA = clienteIA;
        this.repositorioPreguntas = repositorioPreguntas;
        this.repositorioPartida = repositorioPartida;
        this.transaccion = new TransactionTemplate(administradorTransacciones);
    }

    public List<PreguntaTrivia> obtenerParaCasillero(String codigoPartida, DificultadRuleta dificultad) {
        return obtener(codigoPartida, dificultad, MAXIMO_PREGUNTAS_CASILLERO);
    }

    public List<PreguntaTrivia> obtenerParaCarcel(String codigoPartida) {
        return obtener(codigoPartida, DificultadRuleta.DIFICIL, 1);
    }

    /**
     * La lista puede tener menos preguntas que las solicitadas, incluso ninguna si se agotó el banco.
     * La modalidad que llama al servicio puede continuar el turno cuando no hay preguntas disponibles.
     */
    private List<PreguntaTrivia> obtener(String codigoPartida, DificultadRuleta dificultadRuleta, int cantidad) {
        DificultadTrivia dificultad = DificultadTrivia.desdeRuleta(dificultadRuleta);
        List<PreguntaTrivia> generadas = generar(dificultadRuleta, dificultad, cantidad);
        // La llamada HTTP termina antes de bloquear la partida.
        return transaccion.execute(estado -> seleccionarYRegistrar(codigoPartida, dificultad, cantidad, generadas));
    }

    private List<PreguntaTrivia> generar(DificultadRuleta dificultadRuleta, DificultadTrivia dificultad, int cantidad) {
        try {
            List<PreguntaGenerada> respuesta = clienteIA.generarPreguntas(dificultadRuleta.name(), cantidad);
            if (respuesta == null || respuesta.size() != cantidad) {
                return List.of();
            }
            return respuesta.stream()
                .map(pregunta -> validarYConvertir(pregunta, dificultadRuleta, dificultad))
                .toList();
        } catch (ClienteIAException | IllegalArgumentException ex) {
            return List.of();
        }
    }

    private PreguntaTrivia validarYConvertir(PreguntaGenerada pregunta, DificultadRuleta dificultadRuleta,
                                             DificultadTrivia dificultad) {
        if (pregunta == null || !dificultadRuleta.name().equals(pregunta.getDificultad())
                || pregunta.getRespuestaCorrecta() == null) {
            throw new IllegalArgumentException("La respuesta de la IA contiene una pregunta inválida");
        }
        return new PreguntaTrivia(pregunta.getEnunciado(), pregunta.getOpciones(),
            pregunta.getRespuestaCorrecta(), dificultad);
    }

    private List<PreguntaTrivia> seleccionarYRegistrar(String codigoPartida, DificultadTrivia dificultad, int cantidad,
                                                      List<PreguntaTrivia> generadas) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        if (partida == null) {
            return List.of();
        }
        repositorioPartida.bloquearPartida(partida);
        if (partida.getEstado() == EstadoPartida.FINALIZADA) {
            return List.of();
        }
        Set<UUID> usadas = repositorioPreguntas.buscarHuellasUsadas(partida);
        Map<UUID, PreguntaTrivia> seleccionadas = new LinkedHashMap<>();
        agregarSinRepetir(generadas, usadas, seleccionadas, cantidad);
        if (seleccionadas.size() < cantidad) {
            List<PreguntaTrivia> respaldo = new ArrayList<>(repositorioPreguntas.buscarPorDificultad(dificultad));
            Collections.shuffle(respaldo);
            agregarSinRepetir(respaldo, usadas, seleccionadas, cantidad);
        }
        seleccionadas.keySet().forEach(huella -> repositorioPreguntas.registrarUso(partida, huella));
        return List.copyOf(seleccionadas.values());
    }

    private void agregarSinRepetir(List<PreguntaTrivia> candidatas, Set<UUID> usadas,
                                   Map<UUID, PreguntaTrivia> seleccionadas, int cantidad) {
        for (PreguntaTrivia pregunta : candidatas) {
            if (seleccionadas.size() == cantidad) {
                break;
            }
            String enunciado = normalizar(pregunta.getEnunciado());
            if (!enunciado.isEmpty()) {
                UUID huella = UUID.nameUUIDFromBytes(enunciado.getBytes(StandardCharsets.UTF_8));
                if (usadas.add(huella)) {
                    seleccionadas.put(huella, pregunta);
                }
            }
        }
    }

    private String normalizar(String enunciado) {
        String descompuesto = Normalizer.normalize(enunciado.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        String sinTildes = TILDES.matcher(descompuesto).replaceAll("");
        return SEPARADORES.matcher(sinTildes).replaceAll(" ").strip();
    }
}
