package com.tallerwebi.punta_a_punta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Page;
import com.tallerwebi.dominio.ConfiguracionDadosE2E;
import com.tallerwebi.punta_a_punta.vistas.VistaPartida;
import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class GestionPropiedadesE2EIT extends EscenarioPartidaE2E {

    @Test
    void cuandoElJugadorEnTurnoCompraPlazaSanJustoEntoncesSeActualizaLaPropiedadYLosSaldosEnLasTresSesiones()
        throws IOException {
        List<ObservadorPartidaE2E> observadores = paginas().stream()
            .map(ObservadorPartidaE2E::new).toList();
        String codigo = iniciarPartida();
        String topic = "/topic/partida/" + codigo;
        for (ObservadorPartidaE2E observador : observadores) {
            observador.esperarSuscripcion(topic + "/propiedades");
            observador.esperarSuscripcion(topic + "/dinero");
            observador.esperarSuscripcion(topic + "/turno");
        }

        String comprador = obtenerJugadorEnTurno(paginaAnfitrion);
        Page paginaComprador = verificarPermisoDeTirada(comprador);
        VistaPartida vistaComprador = new VistaPartida(paginaComprador);
        long idComprador = vistaComprador.obtenerIdDeSesion();
        String color = List.of("rojo", "azul", "verde").get(nombres().indexOf(comprador));
        String marca = UUID.randomUUID().toString();
        for (Page pagina : paginas()) {
            VistaPartida vista = new VistaPartida(pagina);
            vista.verificarInicio();
            for (String nombre : nombres()) vista.verificarSaldo(nombre, 2000);
            vista.verificarDisponible(4, 200);
            pagina.evaluate("marca => window.__e2eDocumentoPropiedades = marca", marca);
        }

        JsonNode tirada;
        paginaComprador.setExtraHTTPHeaders(Map.of(ConfiguracionDadosE2E.CABECERA_TIRADA, "1,3"));
        try {
            tirada = new ObjectMapper().readTree(vistaComprador.tirarDados().text());
        } finally {
            paginaComprador.setExtraHTTPHeaders(Map.of());
        }
        assertEquals(1, tirada.path("dado1").asInt(-1));
        assertEquals(3, tirada.path("dado2").asInt(-1));
        assertEquals(4, tirada.path("suma").asInt(-1));
        assertEquals(0, tirada.path("posicionAnterior").asInt(-1));
        assertEquals(4, tirada.path("nuevaPosicion").asInt(-1));
        assertEquals(idComprador, tirada.path("idJugador").asLong(-1));
        for (Page pagina : paginas()) new VistaPartida(pagina).verificarPosicion(idComprador, 4);
        vistaComprador.verificarOportunidadDeCompra("Plaza San Justo", 200);
        observadores.forEach(ObservadorPartidaE2E::descartarMensajesAnteriores);

        JsonNode compra = new ObjectMapper().readTree(vistaComprador.comprar(4).text());
        verificarCompra(compra, idComprador, comprador);
        for (ObservadorPartidaE2E observador : observadores) {
            observador.esperarMensaje(topic + "/propiedades", cuerpo ->
                cuerpo.path("posicion").asInt(-1) == 4
                    && cuerpo.path("propietarioId").asLong(-1) == idComprador
                    && comprador.equals(cuerpo.path("propietario").asText())
                    && cuerpo.path("disponible").isBoolean() && !cuerpo.path("disponible").asBoolean()
                    && cuerpo.path("dineroJugador").asInt(-1) == 2000 - 200);
            observador.esperarMensaje(topic + "/dinero", cuerpo ->
                cuerpo.path("idJugador").asLong(-1) == idComprador
                    && cuerpo.path("dinero").asInt(-1) == 2000 - 200);
        }
        for (Page pagina : paginas()) {
            VistaPartida vista = new VistaPartida(pagina);
            vista.verificarPropietario(4, comprador, color);
            for (String nombre : nombres()) {
                vista.verificarSaldo(nombre, nombre.equals(comprador) ? 2000 - 200 : 2000);
            }
            assertEquals(marca, pagina.evaluate("() => window.__e2eDocumentoPropiedades"),
                "Cada sesion debe actualizarse sin recargar el documento");
        }
        pausarSiCorresponde("fin");
    }

    private void verificarCompra(JsonNode compra, long idComprador, String comprador) {
        assertEquals(4, compra.path("posicion").asInt(-1));
        assertEquals("Plaza San Justo", compra.path("nombre").asText());
        assertEquals(200, compra.path("precioCompra").asInt(-1));
        assertEquals(idComprador, compra.path("propietarioId").asLong(-1));
        assertEquals(comprador, compra.path("propietario").asText());
        assertTrue(compra.path("disponible").isBoolean());
        assertEquals(false, compra.path("disponible").booleanValue());
        assertEquals(2000 - 200, compra.path("dineroJugador").asInt(-1));
    }

    private String iniciarPartida() {
        List<String> colores = List.of("ROJO", "AZUL", "VERDE");
        for (int indice = 0; indice < paginas().size(); indice++) {
            dadoQueUnUsuarioSeRegistraEIniciaSesion(paginas().get(indice), nombres().get(indice),
                "propiedades" + indice + "@unlam.edu.ar");
        }
        String codigo = cuandoElAnfitrionCreaUnaSala();
        cuandoElInvitadoSeUneALaSala(paginaInvitadoUno, codigo);
        cuandoElInvitadoSeUneALaSala(paginaInvitadoDos, codigo);
        for (int indice = 0; indice < paginas().size(); indice++) {
            VistaSalaEspera sala = new VistaSalaEspera(paginas().get(indice));
            sala.esperarConexionWebSocket();
            sala.elegirFicha(colores.get(indice));
            sala.verificarFichaDeJugador(nombres().get(indice), colores.get(indice));
        }
        iniciarPartidaYEsperarTableros(codigo);
        return codigo;
    }
}
