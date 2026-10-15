package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioCasillero;
import com.tallerwebi.dominio.servicios.ServicioPropiedad;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
public class ControladorPartida {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControladorPartida.class);
    private static final String CLAVE_ERROR = "error";
    private static final String PARAM_CODIGO_PARTIDA = "codigoPartida";
    private static final String PARAM_POSICION = "posicion";
    private static final String VISTA_PARTIDA = "partida";
    private static final String TOPIC_PARTIDA_PREFIX = "/topic/partida/";
    private static final Set<Integer> POSICIONES_PROPIEDAD = Set.of(
            1, 2, 4, 5, 6, 7, 9, 11, 12, 13, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27, 29
    );
    private static final Set<Integer> POSICIONES_PARADA = Set.of(6, 12, 17, 24);
    private static final String LOCALIDAD_SAN_JUSTO = "San Justo";
    private static final String GRUPO_SAN_JUSTO = GrupoPropiedades.SAN_JUSTO.name();
    private static final String LOCALIDAD_ISIDRO_CASANOVA = "Isidro Casanova";
    private static final String GRUPO_ISIDRO_CASANOVA = GrupoPropiedades.ISIDRO_CASANOVA.name();
    private static final Map<Integer, List<String>> CATALOGO_PROPIEDADES = Map.ofEntries(
            Map.entry(1, List.of("Plaza Barrio Marina", "Villa Luzuriaga", "VILLA_LUZURIAGA")),
            Map.entry(2, List.of("Parroquia Stella Maris", "Villa Luzuriaga", "VILLA_LUZURIAGA")),
            Map.entry(4, List.of("Plaza San Justo", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(5, List.of("UNLaM", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(6, List.of("Colectivo 242", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(7, List.of("Villa Palito", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(9, List.of("Cancha de Brown", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(11, List.of("Jesse James", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(12, List.of("Parada 96", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(13, List.of("Villa San Pete", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(15, List.of("Feria Carlos Casares", "Rafael Castillo", "RAFAEL_CASTILLO")),
            Map.entry(16, List.of("Plaza Barrio La Casona", "Rafael Castillo", "RAFAEL_CASTILLO")),
            Map.entry(17, List.of("Parada 621", "Rafael Castillo", "RAFAEL_CASTILLO")),
            Map.entry(18, List.of("Estación Laferrere", "Laferrere", "LAFERRERE")),
            Map.entry(19, List.of("Av. Luro", "Laferrere", "LAFERRERE")),
            Map.entry(21, List.of("Puente 12", "Ciudad Evita", "CIUDAD_EVITA")),
            Map.entry(22, List.of("Superchino Luna", "Ciudad Evita", "CIUDAD_EVITA")),
            Map.entry(23, List.of("Cementerio Israelita", "La Tablada", "LA_TABLADA")),
            Map.entry(24, List.of("Colectivo 620", "La Tablada", "LA_TABLADA")),
            Map.entry(25, List.of("Regimiento de La Tablada", "La Tablada", "LA_TABLADA")),
            Map.entry(26, List.of("Bingo Ramos Mejía", "Ramos Mejía", "RAMOS_MEJIA")),
            Map.entry(27, List.of("Estación Ramos Mejía", "Ramos Mejía", "RAMOS_MEJIA")),
            Map.entry(29, List.of("Pinar de Rocha", "Ramos Mejía", "RAMOS_MEJIA"))
    );
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;
    private final ServicioCasillero servicioCasillero;
    private final ServicioPropiedad servicioPropiedad;
    private final CoordinadorTurnoPartida coordinadorTurno;

    @Autowired
    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
                              ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad
    ) {
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioPartida = servicioPartida;
        this.servicioCasillero = servicioCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.coordinadorTurno = new CoordinadorTurnoPartida(servicioPartida);
    }

    @GetMapping("/partida/{codigoPartida}/propiedades/{posicion}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerInfoPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        if (!POSICIONES_PROPIEDAD.contains(posicion)) return ResponseEntity.notFound().build();
        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null || casillero.getPropiedad() == null) {
            return ResponseEntity.ok(dtoCatalogoPropiedad(posicion));
        }
        return ResponseEntity.ok(dtoPropiedad(casillero, casillero.getPropiedad()));
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/comprar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> comprarPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion) {
        try {
            return validarYProcesarCompra(codigoPartida, posicion);
        } catch (RuntimeException ex) {
            LOGGER.error("Error interno al comprar la propiedad {} en la partida {}", posicion, codigoPartida, ex);
            String detalle = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(CLAVE_ERROR, "Error interno al comprar la propiedad: " + detalle));
        }
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/vender")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> venderPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion, HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        if (!POSICIONES_PROPIEDAD.contains(posicion)) return ResponseEntity.notFound().build();

        Long usuarioSesionId = ResolutorJugadorSesion.resolverUsuarioSesionId(request);
        String nombreSesion = (String) request.getSession().getAttribute("NOMBRE");
        Long jugadorSesionId = ResolutorJugadorSesion.resolver(partida, usuarioSesionId, nombreSesion);
        boolean jugadorEnPartida = partida.getJugadores() != null && partida.getJugadores().stream()
                .anyMatch(jugador -> jugadorSesionId != null && jugadorSesionId.equals(jugador.getId()));
        if (!jugadorEnPartida) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR, "No perteneces a esta partida."));
        }

        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null || casillero.getPropiedad() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(CLAVE_ERROR, "La propiedad no está disponible para vender."));
        }
        try {
            Integer precioVenta = servicioPropiedad.venderPropiedadParaJugador(
                    casillero.getPropiedad().getId(), jugadorSesionId);
            Jugador vendedorActualizado = servicioPartida.obtenerJugadorPorId(jugadorSesionId);
            Map<String, Object> actualizado = dtoPropiedad(casillero,
                    servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId()));
            actualizado.put("idJugador", jugadorSesionId);
            actualizado.put("dineroJugador", vendedorActualizado.getDinero());
            actualizado.put("precioVenta", precioVenta);
            notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", actualizado);
            NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, codigoPartida);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException ex) {
            String mensaje = ex.getMessage();
            if (mensaje == null || mensaje.isBlank()) mensaje = "No se pudo vender la propiedad.";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    private ResponseEntity<Map<String, Object>> validarYProcesarCompra(String codigoPartida, Integer posicion) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        if (!POSICIONES_PROPIEDAD.contains(posicion)) return ResponseEntity.notFound().build();
        String errorValidacion = validarJugadorActivoEnPosicion(codigoPartida, posicion);
        if (errorValidacion != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(CLAVE_ERROR, errorValidacion));
        }
        Jugador jugador = servicioPartida.obtenerJugadorActual(codigoPartida);
        return procesarCompraPropiedad(partida, posicion, jugador);
    }

    private String validarJugadorActivoEnPosicion(String codigoPartida, Integer posicion) {
        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (jugadorActual == null) return "No hay un jugador activo en esta partida.";
        if (!posicion.equals(jugadorActual.getPosicionActual())) {
            return "El jugador activo está en el casillero " + jugadorActual.getPosicionActual()
                    + ", pero se intentó comprar el casillero " + posicion + ".";
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> procesarCompraPropiedad(Partida partida,
                                                                         Integer posicion, Jugador jugador) {
        String codigoPartida = partida.getCodigoUnico();
        CasilleroPropiedad casillero = buscarOCrearPropiedadEnPosicion(partida, posicion);
        try {
            servicioPropiedad.comprarPropiedadParaJugador(casillero.getPropiedad().getId(), jugador.getId());
            Jugador compradorActualizado = servicioPartida.obtenerJugadorPorId(jugador.getId());
            Map<String, Object> actualizado = dtoPropiedad(casillero,
                    servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId()));
            actualizado.put("idJugadorPropietario", jugador.getId());
            actualizado.put("dineroJugador", compradorActualizado.getDinero());
            notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", actualizado);
            TurnoDTO turno = coordinadorTurno.avanzarYObtenerTurno(codigoPartida);
            if (turno != null) actualizado.put("idJugadorActual", turno.getIdJugadorActual());
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException ex) {
            String mensaje = ex.getMessage();
            if (mensaje == null || mensaje.isBlank()) mensaje = "Error al guardar la compra de la propiedad.";
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    private CasilleroPropiedad buscarCasilleroPropiedad(Partida partida, Integer posicion) {
        if (partida == null || posicion == null || !POSICIONES_PROPIEDAD.contains(posicion)) return null;
        return servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance).map(CasilleroPropiedad.class::cast)
                .filter(c -> posicion.equals(c.getPosicion()) && c.getPropiedad() != null)
                .filter(c -> java.util.Objects.equals(partida.getId(), c.getPartidaId()))
                .findFirst().orElse(null);
    }

    private CasilleroPropiedad buscarOCrearPropiedadEnPosicion(Partida partida, Integer posicion) {
        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        return casillero != null ? casillero : crearCasilleroPropiedad(partida, posicion);
    }

    private CasilleroPropiedad crearCasilleroPropiedad(Partida partida, Integer posicion) {
        List<String> datos = CATALOGO_PROPIEDADES.get(posicion);
        if (datos == null) return null;
        int alquilerBase = POSICIONES_PARADA.contains(posicion) ? 25 : 50;
        Propiedad propiedad = new Propiedad(null, datos.get(0), 300, alquilerBase, null,
                GrupoPropiedades.valueOf(datos.get(2)));
        CasilleroPropiedad casillero = new CasilleroPropiedad(null, posicion, datos.get(0), datos.get(1), propiedad);
        casillero.setPartidaId(partida.getId());
        servicioCasillero.guardarCasillero(casillero);
        return casillero;
    }

    private Map<String, Object> dtoCatalogoPropiedad(Integer posicion) {
        List<String> datos = CATALOGO_PROPIEDADES.get(posicion);
        if (datos == null) return Map.of();
        Map<String, Object> dto = new HashMap<>();
        dto.put("posicion", posicion);
        dto.put("nombre", datos.get(0));
        dto.put("descripcion", datos.get(1));
        dto.put("precioCompra", 300);
        dto.put("precioVenta", 150);
        dto.put("alquiler", POSICIONES_PARADA.contains(posicion) ? 25 : 50);
        dto.put("propietario", null);
        dto.put("propietarioId", null);
        dto.put("disponible", true);
        return dto;
    }

    private Map<String, Object> dtoPropiedad(CasilleroPropiedad casillero, Propiedad propiedad) {
        Map<String, Object> dto = new HashMap<>();
        List<String> datosCatalogo = CATALOGO_PROPIEDADES.get(casillero.getPosicion());
        Jugador propietario = propiedad.getPropietario();
        // La posición del tablero es el identificador canónico: evita que registros
        // viejos de la base muestren o vendan con el nombre de otro casillero.
        dto.put("posicion", casillero.getPosicion());
        dto.put("nombre", datosCatalogo == null ? casillero.getNombre() : datosCatalogo.get(0));
        dto.put("descripcion", datosCatalogo == null ? casillero.getDescripcion() : datosCatalogo.get(1));
        dto.put("precioCompra", propiedad.getPrecioCompra());
        dto.put("precioVenta", propiedad.getPrecioCompra() == null ? 0 : propiedad.getPrecioCompra() / 2);
        dto.put("alquiler", servicioPropiedad.calcularAlquiler(casillero, propiedad));
        dto.put("propietario", propietario == null ? null : propietario.getApodo());
        dto.put("propietarioId", propietario == null ? null : propietario.getId());
        dto.put("disponible", propietario == null);
        return dto;
    }

    /*@GetMapping("/partida")
    public ModelAndView verTablero(
        @RequestParam(value = "codigo", required = false) String codigoUnico,
        HttpServletRequest request
    ) {
        Map<String, Object> model = new HashMap<>();

        String codigoFinal = codigoUnico;

        if (codigoFinal == null || codigoFinal.isBlank()) {
            codigoFinal = (String) request.getSession().getAttribute("codigoSala");
        }

        if (codigoFinal == null || codigoFinal.isBlank()) {
            return new ModelAndView("redirect:/sala-espera");
        }

        Partida partidaReal = servicioPartida.obtenerPartida(codigoFinal);

        if (partidaReal == null) {
            return new ModelAndView("redirect:/sala-espera");
        }

        Long usuarioSesionId = (Long) request.getSession().getAttribute("IDUSUARIO");

        Long turnoActualId = null;
        if (partidaReal.getJugadorTurnoAleatorio() != null) {
            turnoActualId = (long) partidaReal.getJugadorTurnoAleatorio().getId();
        }

        model.put("partida", partidaReal);
        model.put("usuarioSesionId", usuarioSesionId);
        model.put("turnoActualId", turnoActualId);

        return new ModelAndView(VISTA_PARTIDA, model);
    }*/

    @GetMapping("/partida")
    public ModelAndView verTablero(
        @RequestParam(value = "codigo", required = false) String codigoUnico,
        HttpServletRequest request
    ) {
        String codigoFinal = ResolutorJugadorSesion.resolverCodigoPartida(codigoUnico, request);
        if (codigoFinal == null) {
            return new ModelAndView("redirect:/home");
        }

        Partida partidaReal = servicioPartida.obtenerPartida(codigoFinal);
        if (partidaReal == null) {
            return new ModelAndView("redirect:/sala-de-espera?codigoUnico=" + codigoFinal);
        }

        Long usuarioSesionId = ResolutorJugadorSesion.resolverUsuarioSesionId(request);
        String nombreSesion = (String) request.getSession().getAttribute("NOMBRE");

        if (usuarioSesionId == null && nombreSesion == null) {
            return new ModelAndView("redirect:/login");
        }

        Long jugadorSesionId = ResolutorJugadorSesion.resolver(partidaReal, usuarioSesionId, nombreSesion);

        Map<String, Object> model = new HashMap<>();
        model.put("partida", partidaReal);
        model.put("usuarioSesionId", jugadorSesionId);
        model.put("turnoActualId", resolverTurnoActualId(partidaReal, codigoFinal));

        return new ModelAndView(VISTA_PARTIDA, model);
    }

    private Long resolverTurnoActualId(Partida partida, String codigo) {
        Jugador jugador = servicioPartida.obtenerJugadorActual(codigo);
        if (jugador != null) {
            return jugador.getId();
        }
        if (partida.getJugadorTurnoAleatorio() != null) {
            return partida.getJugadorTurnoAleatorio().getId();
        }
        return null;
    }

    @GetMapping("/partida/{codigoPartida}/turno")
    @ResponseBody
    public ResponseEntity<TurnoDTO> obtenerTurnoActual(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida) {
        TurnoDTO turno = coordinadorTurno.obtenerTurnoActual(codigoPartida);
        return turno == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(turno);
    }

    @PostMapping("/partida/{codigoPartida}/avanzar-turno")
    @ResponseBody
    public ResponseEntity<TurnoDTO> avanzarTurno(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            HttpServletRequest request
    ) {
        Long usuarioSesionId = ResolutorJugadorSesion.resolverUsuarioSesionId(request);
        String nombreSesion = (String) request.getSession().getAttribute("NOMBRE");

        if (usuarioSesionId == null && nombreSesion == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Jugador jugadorActual = this.servicioPartida.obtenerJugadorActual(codigoPartida);
        if (jugadorActual == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean coincideId =
            usuarioSesionId != null && usuarioSesionId.equals(jugadorActual.getId());
        boolean coincideNombre =
            nombreSesion != null && nombreSesion.equalsIgnoreCase(jugadorActual.getApodo());

        if (!coincideId && !coincideNombre) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(coordinadorTurno.avanzarYObtenerTurno(codigoPartida));
    }

    @PostMapping("/partida/tirar-dados")
    @ResponseBody
    public RespuestaMovimientoDTO tirarDados(
        @RequestParam("codigoUnico") String codigoUnico,
        @RequestParam("idJugador") Long idJugador
    ) throws PartidaNoEncontradaException, JugadorNoEncontradoException {
        ResultadoTiradaDTO resultado = servicioPartida.tirarDados(); //Llama al servicio para generar la tirada
        Partida partida = servicioPartida.obtenerPartida(codigoUnico); //Se trae la partida

        if (partida == null) {
            throw new PartidaNoEncontradaException();
        }

        Jugador jugadorActual = null;
        for (Jugador j : partida.getJugadores()) {
            if (j.getId().equals(idJugador)) {
                jugadorActual = j;
                break;
            }
        } //Busca al jugador en la lista que tiro los dados en esa partida

        if (jugadorActual == null) {
            throw new JugadorNoEncontradoException("No se encontró el jugador en esta partida");
        }

        servicioPartida.moverJugador(jugadorActual, resultado.sumarDados(), 30);//se mueve ficha
        servicioPartida.actualizarJugador(jugadorActual);

        //se arma el paquete de datos para que el frontend lo dibuje
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setIdJugador(jugadorActual.getId()); //identifica que el jugador se movio
        respuesta.setDado1(resultado.getDado1());
        respuesta.setDado2(resultado.getDado2());
        respuesta.setSuma(resultado.sumarDados());
        respuesta.setNuevaPosicion(jugadorActual.getPosicionActual());

        // Solo una propiedad libre espera la decisión de compra. En eventos, cárcel,
        // salida y propiedades ya vendidas el servidor avanza el turno por sí mismo.
        CasilleroPropiedad casilleroDestino = buscarOCrearPropiedadEnPosicion(partida,
                jugadorActual.getPosicionActual());
        boolean requiereCompra = resolverPropiedadDestino(casilleroDestino, jugadorActual, codigoUnico);
        if (!requiereCompra) {
            coordinadorTurno.avanzarYObtenerTurno(codigoUnico);
        }

        // Notifica a todos los jugadores la nueva posición.
        notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoUnico, respuesta);
        return respuesta;
    }

    private boolean resolverPropiedadDestino(CasilleroPropiedad casillero, Jugador jugador, String codigoPartida) {
        if (casillero == null) return false;
        Propiedad propiedad = casillero.getPropiedad();
        if (servicioPropiedad.estaDisponible(propiedad)) return true;
        int alquiler = servicioPropiedad.calcularAlquiler(casillero, propiedad);
        Jugador propietario = propiedad.getPropietario();
        if (propietario != null && !propietario.getId().equals(jugador.getId())) {
            servicioPropiedad.pagarAlquiler(propiedad, jugador, alquiler);
            NotificadorPartida.notificarAlquiler(notificadorWebSocket, codigoPartida, jugador,
                    propietario, propiedad.getNombre(), alquiler);
        }
        NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, codigoPartida);
        return false;
    }
    @ExceptionHandler(JugadorNoEncontradoException.class)
    public ResponseEntity<String> manejarJugadorNoEncontrado(JugadorNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); //le devuelve al frontend un código 404 de "not found" y el mensaje de la excepción
    }
}
