package com.tallerwebi.punta_a_punta;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/** Observa STOMP sobre SockJS sin interceptar ni reemplazar el trafico real. */
final class ObservadorPartidaE2E {

    private static final ObjectMapper JSON = new ObjectMapper();
    private final Page pagina;
    private final Set<String> suscripciones = new HashSet<>();
    private final List<Mensaje> mensajes = new ArrayList<>();

    ObservadorPartidaE2E(Page pagina) {
        this.pagina = pagina;
        pagina.onWebSocket(socket -> {
            socket.onFrameSent(frame -> registrarSuscripciones(frame.text()));
            socket.onFrameReceived(frame -> registrarMensajes(frame.text()));
        });
    }

    void esperarSuscripcion(String destino) {
        pagina.waitForCondition(() -> suscripciones.contains(destino));
    }

    JsonNode esperarMensaje(String destino, Predicate<JsonNode> esperado) {
        pagina.waitForCondition(() -> mensajes.stream().anyMatch(
            mensaje -> destino.equals(mensaje.destino()) && esperado.test(mensaje.cuerpo())));
        return mensajes.stream().filter(mensaje -> destino.equals(mensaje.destino())
            && esperado.test(mensaje.cuerpo())).findFirst().orElseThrow().cuerpo();
    }

    void descartarMensajesAnteriores() {
        mensajes.clear();
    }

    private void registrarSuscripciones(String trama) {
        if (trama == null || !trama.contains("SUBSCRIBE")) return;
        try {
            for (JsonNode elemento : JSON.readTree(trama)) {
                elemento.asText().lines().filter(linea -> linea.startsWith("destination:"))
                    .map(linea -> linea.substring("destination:".length()))
                    .forEach(suscripciones::add);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer la suscripcion SockJS", ex);
        }
    }

    private void registrarMensajes(String trama) {
        if (trama == null || !trama.startsWith("a[")) return;
        try {
            for (JsonNode elemento : JSON.readTree(trama.substring(1))) {
                String mensaje = elemento.asText();
                int separador = mensaje.indexOf("\n\n");
                if (!mensaje.startsWith("MESSAGE\n") || separador < 0) continue;
                String destino = mensaje.substring(0, separador).lines()
                    .filter(linea -> linea.startsWith("destination:"))
                    .map(linea -> linea.substring("destination:".length())).findFirst().orElse("");
                if (!destino.matches("/topic/partida/[^/]+(?:/(propiedades|dinero|turno|ofertas))?")) continue;
                mensajes.add(new Mensaje(destino,
                    JSON.readTree(mensaje.substring(separador + 2).replace("\0", ""))));
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("No se pudo leer el mensaje SockJS", ex);
        }
    }

    private record Mensaje(String destino, JsonNode cuerpo) {}
}
