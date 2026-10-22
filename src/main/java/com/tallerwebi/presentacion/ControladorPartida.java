package com.tallerwebi.presentacion;

import com.tallerwebi.presentacion.coordinadores.CoordinadorTurnoPartida;

import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.ServicioPartida;
import com.tallerwebi.dominio.servicios.ServicioCasillero;
import com.tallerwebi.dominio.servicios.ServicioEventoCasillero;
import com.tallerwebi.dominio.servicios.ServicioPropiedad;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.SaldoJugadorDTO;
import com.tallerwebi.presentacion.DTO.AlquilerMovimientoDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Atiendo las acciones de la partida y sus propiedades. */
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
    private static final String DESCRIPCION_PARADA = "Parada de bondi";
    private static final String LOCALIDAD_SAN_JUSTO = "San Justo";
    private static final String GRUPO_SAN_JUSTO = GrupoPropiedades.SAN_JUSTO.name();
    private static final String LOCALIDAD_ISIDRO_CASANOVA = "Isidro Casanova";
    private static final String GRUPO_ISIDRO_CASANOVA = GrupoPropiedades.ISIDRO_CASANOVA.name();
    private static final Map<Integer, List<String>> CATALOGO_PROPIEDADES = Map.ofEntries(
            Map.entry(1, List.of("Plaza Barrio Marina", "Villa Luzuriaga", "VILLA_LUZURIAGA")),
            Map.entry(2, List.of("Parroquia Stella Maris", "Villa Luzuriaga", "VILLA_LUZURIAGA")),
            Map.entry(4, List.of("Plaza San Justo", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(5, List.of("UNLaM", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(6, List.of("Colectivo 242", DESCRIPCION_PARADA, "")),
            Map.entry(7, List.of("San Justo Shopping", LOCALIDAD_SAN_JUSTO, GRUPO_SAN_JUSTO)),
            Map.entry(9, List.of("Cancha de Brown", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(11, List.of("Jesse James", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(12, List.of("Colectivo 96", DESCRIPCION_PARADA, "")),
            Map.entry(13, List.of("Plaza Hipólito Yrigoyen", LOCALIDAD_ISIDRO_CASANOVA, GRUPO_ISIDRO_CASANOVA)),
            Map.entry(15, List.of("Feria Carlos Casares", "Rafael Castillo", "RAFAEL_CASTILLO")),
            Map.entry(16, List.of("Plaza Barrio La Casona", "Rafael Castillo", "RAFAEL_CASTILLO")),
            Map.entry(17, List.of("Colectivo 621", DESCRIPCION_PARADA, "")),
            Map.entry(18, List.of("Estación Laferrere", "Laferrere", "LAFERRERE")),
            Map.entry(19, List.of("Av. Luro", "Laferrere", "LAFERRERE")),
            Map.entry(21, List.of("Puente 12", "Ciudad Evita", "CIUDAD_EVITA")),
            Map.entry(22, List.of("Superchino Luna", "Ciudad Evita", "CIUDAD_EVITA")),
            Map.entry(23, List.of("Cementerio Israelita", "La Tablada", "LA_TABLADA")),
            Map.entry(24, List.of("Colectivo 620", DESCRIPCION_PARADA, "")),
            Map.entry(25, List.of("Regimiento de La Tablada", "La Tablada", "LA_TABLADA")),
            Map.entry(26, List.of("Bingo Ramos Mejía", "Ramos Mejía", "RAMOS_MEJIA")),
            Map.entry(27, List.of("Estación Ramos Mejía", "Ramos Mejía", "RAMOS_MEJIA")),
            Map.entry(29, List.of("Pinar de Rocha", "Ramos Mejía", "RAMOS_MEJIA"))
    );
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;
    private final ServicioCasillero servicioCasillero;
    private final ServicioEventoCasillero servicioEventoCasillero;
    private final ServicioPropiedad servicioPropiedad;
    private final CoordinadorTurnoPartida coordinadorTurno;

    @Autowired
    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
                              ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
                              ServicioEventoCasillero servicioEventoCasillero
    ) {
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioPartida = servicioPartida;
        this.servicioCasillero = servicioCasillero;
        this.servicioEventoCasillero = servicioEventoCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.coordinadorTurno = new CoordinadorTurnoPartida(servicioPartida);
    }

    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
                              ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad) {
        this(servicioPartida, notificadorWebSocket, servicioCasillero, servicioPropiedad, null);
    }

    @GetMapping("/partida/{codigoPartida}/propiedades/{posicion}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerInfoPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null) {
            return ResponseEntity.ok(dtoCatalogoPropiedad(posicion));
        }
        return ResponseEntity.ok(dtoPropiedad(casillero, casillero.getPropiedad()));
    }

    @GetMapping("/partida/{codigoPartida}/propiedades")
    @ResponseBody
    public ResponseEntity<?> obtenerPropiedadesTablero(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!ResolutorJugadorSesion.pertenece(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR,
                    "No pertenecés a esta partida."));
        }

        return ResponseEntity.ok(obtenerCatalogoTablero(partida));
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/subir-alquiler")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subirAlquilerPropiedad(
            @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
            @PathVariable(PARAM_POSICION) Integer posicion,
            HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!puedeParticiparEnPartida(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR,
                    "No podés administrar propiedades en esta partida."));
        }
        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (!esTurnoDeJugador(jugadorActual, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR,
                    "Solo podés subir alquileres durante tu turno."));
        }
        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        if (!esPropietarioDelCasillero(casillero, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR,
                    "Solo podés subir el alquiler de tus propiedades."));
        }
        return procesarMejoraPropiedad(codigoPartida, idJugador, casillero);
    }

    private ResponseEntity<Map<String, Object>> procesarMejoraPropiedad(
            String codigoPartida, Long idJugador, CasilleroPropiedad casillero) {
        try {
            Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
            servicioPropiedad.mejorarPropiedad(casillero.getPropiedad().getId(), jugador);
            Propiedad propiedad = servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId());
            Map<String, Object> dto = dtoPropiedad(casillero, propiedad);
            Jugador jugadorActualizado = servicioPartida.obtenerJugadorPorId(idJugador);
            dto.put("idJugador", idJugador);
            dto.put("dineroJugador", jugadorActualizado.getDinero());
            dto.put("enBancarrota", jugadorActualizado.getEnBancarrota());
            notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", dto);
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            String mensaje = ex.getMessage() == null ? "No se pudo subir el alquiler." : ex.getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
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
        if (!existePosicionDePropiedad(partida, posicion)) {
            return ResponseEntity.notFound().build();
        }
        Long jugadorSesionId = ResolutorJugadorSesion.resolver(partida, request);
        if (!puedeParticiparEnPartida(partida, jugadorSesionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(CLAVE_ERROR, "No perteneces a esta partida."));
        }
        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(CLAVE_ERROR, "La propiedad no está disponible para vender."));
        }
        try {
            return procesarVentaPropiedad(codigoPartida, jugadorSesionId, casillero);
        } catch (RuntimeException ex) {
            String mensaje = obtenerMensajeError(ex, "No se pudo vender la propiedad.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    private ResponseEntity<Map<String, Object>> procesarVentaPropiedad(
            String codigoPartida, Long idJugador, CasilleroPropiedad casillero) {
        Integer precioVenta = servicioPropiedad.venderPropiedadParaJugador(
                casillero.getPropiedad().getId(), idJugador);
        Jugador vendedorActualizado = servicioPartida.obtenerJugadorPorId(idJugador);
        Map<String, Object> actualizado = dtoPropiedad(casillero,
                servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId()));
        actualizado.put("idJugador", idJugador);
        actualizado.put("dineroJugador", vendedorActualizado.getDinero());
        actualizado.put("precioVenta", precioVenta);
        notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", actualizado);
        NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, codigoPartida);
        return ResponseEntity.ok(actualizado);
    }

    private ResponseEntity<Map<String, Object>> validarYProcesarCompra(String codigoPartida, Integer posicion) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        Jugador jugador = servicioPartida.obtenerJugadorActual(codigoPartida);
        String errorValidacion = validarJugadorActivoEnPosicion(jugador, posicion);
        if (errorValidacion != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(CLAVE_ERROR, errorValidacion));
        }
        return procesarCompraPropiedad(partida, posicion, jugador);
    }


    private Boolean puedeParticiparEnPartida(Partida partida, Long idJugador) {
        if (idJugador == null) return false;
        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
        return jugador != null && !jugador.getEnBancarrota()
                && ResolutorJugadorSesion.pertenece(partida, idJugador);
    }


    private String validarJugadorActivoEnPosicion(Jugador jugadorActual, Integer posicion) {
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
            actualizado.put("enBancarrota", compradorActualizado.getEnBancarrota());
            notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades", actualizado);
            NotificadorPartida.notificarSaldos(notificadorWebSocket, servicioPartida, codigoPartida);
            TurnoDTO turno = coordinadorTurno.avanzarYObtenerTurno(codigoPartida);
            if (turno != null) actualizado.put("idJugadorActual", turno.getIdJugadorActual());
            return ResponseEntity.ok(actualizado);
        } catch (DineroInsuficienteException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("codigoError", "DINERO_INSUFICIENTE", CLAVE_ERROR, ex.getMessage()));
        } catch (RuntimeException ex) {
            String mensaje = obtenerMensajeError(ex, "Error al guardar la compra de la propiedad.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    private CasilleroPropiedad buscarCasilleroPropiedad(Partida partida, Integer posicion) {
        if (posicion == null || !existePosicionDePropiedad(partida, posicion)) return null;
        return obtenerCasillerosPropiedadDePartida(partida)
                .filter(casillero -> posicion.equals(casillero.getPosicion()))
                .findFirst().orElse(null);
    }

    private Boolean existePosicionDePropiedad(Partida partida, Integer posicion) {
        return partida != null && POSICIONES_PROPIEDAD.contains(posicion);
    }

    private Boolean esTurnoDeJugador(Jugador jugadorActual, Long idJugador) {
        return jugadorActual != null && idJugador.equals(jugadorActual.getId());
    }

    private Boolean esPropietarioDelCasillero(CasilleroPropiedad casillero, Long idJugador) {
        return casillero != null && casillero.getPropiedad().getPropietario() != null
                && idJugador.equals(casillero.getPropiedad().getPropietario().getId());
    }

    private String obtenerMensajeError(RuntimeException excepcion, String mensajePredeterminado) {
        String mensaje = excepcion.getMessage();
        return mensaje == null || mensaje.isBlank() ? mensajePredeterminado : mensaje;
    }

    private Stream<CasilleroPropiedad> obtenerCasillerosPropiedadDePartida(Partida partida) {
        return servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(casillero -> casillero.getPropiedad() != null)
                .filter(casillero -> java.util.Objects.equals(partida.getId(), casillero.getPartidaId()));
    }

    private CasilleroPropiedad buscarOCrearPropiedadEnPosicion(Partida partida, Integer posicion) {
        CasilleroPropiedad casillero = buscarCasilleroPropiedad(partida, posicion);
        return casillero != null ? casillero : crearCasilleroPropiedad(partida, posicion);
    }

    private CasilleroPropiedad crearCasilleroPropiedad(Partida partida, Integer posicion) {
        List<String> datos = CATALOGO_PROPIEDADES.get(posicion);
        if (datos == null) return null;
        Integer alquilerBase = obtenerAlquilerBaseCatalogo(posicion);
        Propiedad propiedad = new Propiedad(null, datos.get(0), 300, alquilerBase, null,
                POSICIONES_PARADA.contains(posicion) ? null : GrupoPropiedades.valueOf(datos.get(2)));
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
        Integer alquilerBase = obtenerAlquilerBaseCatalogo(posicion);
        dto.put("alquiler", alquilerBase);
        dto.put("alquilerBase", alquilerBase);
        dto.put("nivelMejora", 0);
        dto.put("grupo", POSICIONES_PARADA.contains(posicion) ? null : datos.get(2));
        dto.put("propietario", null);
        dto.put("propietarioId", null);
        dto.put("disponible", true);
        return dto;
    }

    private Integer obtenerAlquilerBaseCatalogo(Integer posicion) {
        return POSICIONES_PARADA.contains(posicion) ? 20 : 50;
    }

    private Map<String, Object> dtoPropiedad(CasilleroPropiedad casillero, Propiedad propiedad) {
        Map<String, Object> dto = new HashMap<>();
        List<String> datosCatalogo = CATALOGO_PROPIEDADES.get(casillero.getPosicion());
        Jugador propietario = propiedad.getPropietario();
        // Uso la posición para identificar el casillero.
        dto.put("posicion", casillero.getPosicion());
        dto.put("nombre", datosCatalogo == null ? casillero.getNombre() : datosCatalogo.get(0));
        dto.put("descripcion", datosCatalogo == null ? casillero.getDescripcion() : datosCatalogo.get(1));
        dto.put("precioCompra", propiedad.getPrecioCompra());
        dto.put("precioVenta", propiedad.getPrecioCompra() == null ? 0 : propiedad.getPrecioCompra() / 2);
        dto.put("alquiler", servicioPropiedad.calcularAlquiler(casillero, propiedad));
        dto.put("propietario", propietario == null ? null : propietario.getApodo());
        dto.put("propietarioId", propietario == null ? null : propietario.getId());
        dto.put("disponible", propietario == null);
        dto.put("idPropiedad", propiedad.getId());
        dto.put("alquilerBase", POSICIONES_PARADA.contains(casillero.getPosicion())
                ? 20 : propiedad.getPrecioAlquiler());
        dto.put("nivelMejora", propiedad.getNivelMejora() == null ? 0 : propiedad.getNivelMejora());
        dto.put("grupo", POSICIONES_PARADA.contains(casillero.getPosicion()) || propiedad.getGrupo() == null
                ? null : propiedad.getGrupo().name());
        return dto;
    }

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
        Jugador jugadorSesion = jugadorSesionId == null
                ? null
                : servicioPartida.obtenerJugadorPorId(jugadorSesionId);

        Map<String, Object> model = new HashMap<>();
        model.put("partida", partidaReal);
        model.put("usuarioSesionId", jugadorSesionId);
        model.put("dineroUsuarioSesion", jugadorSesion == null ? 0 : jugadorSesion.getDinero());
        model.put("usuarioEnBancarrota", jugadorSesion != null && jugadorSesion.getEnBancarrota());
        model.put("turnoActualId", resolverTurnoActualId(codigoFinal));
        model.put("propiedadesTablero", obtenerCatalogoTablero(partidaReal));

        return new ModelAndView(VISTA_PARTIDA, model);
    }

    private List<Map<String, Object>> obtenerCatalogoTablero(Partida partida) {
        List<CasilleroPropiedad> casilleros = obtenerCasillerosPropiedadDePartida(partida).toList();

        return POSICIONES_PROPIEDAD.stream().sorted().map(posicion -> casilleros.stream()
                .filter(casillero -> posicion.equals(casillero.getPosicion()))
                .findFirst()
                .map(casillero -> dtoPropiedad(casillero, casillero.getPropiedad()))
                .orElseGet(() -> dtoCatalogoPropiedad(posicion)))
                .toList();
    }

    private Long resolverTurnoActualId(String codigo) {
        Jugador jugador = servicioPartida.obtenerJugadorActual(codigo);
        return jugador == null ? null : jugador.getId();
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

        Boolean coincideId =
            usuarioSesionId != null && usuarioSesionId.equals(jugadorActual.getId());
        Boolean coincideNombre =
            nombreSesion != null && nombreSesion.equalsIgnoreCase(jugadorActual.getApodo());

        if ((!coincideId && !coincideNombre) || jugadorActual.getEnBancarrota()) {
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
        Partida partida = obtenerPartidaParaTirada(codigoUnico);
        Jugador jugadorActual = obtenerJugadorParaTirada(partida, idJugador);
        validarTurnoDeTirada(codigoUnico, idJugador, jugadorActual);
        servicioPartida.iniciarTemporizadorTurnoSiNoExiste(codigoUnico);

        Integer posicionAnterior = jugadorActual.getPosicionActual();
        ResultadoTiradaDTO resultado = servicioPartida.tirarDados(); //Llama al servicio para generar la tirada

        servicioPartida.moverJugador(jugadorActual, resultado.sumarDados(), 30);//se mueve ficha
        Boolean pasoPorInicio = posicionAnterior + resultado.sumarDados() >= 30;
        if (pasoPorInicio) {
            servicioPartida.sumarDinero(jugadorActual, 200);
        }

        RespuestaMovimientoDTO respuesta = crearRespuestaMovimiento(jugadorActual, posicionAnterior, resultado);
        Optional<EventoCasillero> evento = servicioEventoCasillero == null
                ? Optional.empty() : servicioEventoCasillero.aplicarSiCorresponde(jugadorActual);
        if (evento.isPresent()) {
            EventoCasillero eventoAplicado = evento.get();
            respuesta.setNombreEvento(eventoAplicado.getNombre());
            respuesta.setDescripcionEvento(eventoAplicado.getDescripcion());
            respuesta.setTipoEfectoEvento(eventoAplicado.getTipoEfecto().name());
            respuesta.setNuevaPosicion(jugadorActual.getPosicionActual());
        }
        respuesta.informarDineroPorInicio(pasoPorInicio);
        servicioPartida.actualizarJugador(jugadorActual);
        // Espero la decisión de compra solo si la propiedad está libre.
        CasilleroPropiedad casilleroDestino = buscarOCrearPropiedadEnPosicion(partida,
                jugadorActual.getPosicionActual());
        Boolean requiereCompra = jugadorActual.getEnBancarrota() || Boolean.TRUE.equals(jugadorActual.getPierdeTurno())
                ? false : resolverPropiedadDestino(casilleroDestino, jugadorActual, respuesta);
        respuesta.setSaldos(servicioPartida.obtenerPartida(codigoUnico).getJugadores().stream()
                .map(SaldoJugadorDTO::desdeJugador).toList());
        if (!requiereCompra) {
            coordinadorTurno.avanzarYObtenerTurno(codigoUnico);
        }
        servicioPartida.prepararTiempoTurno(codigoUnico, respuesta.calcularSegundosPresentacion());
        respuesta.setTurnoSiguiente(coordinadorTurno.obtenerTurnoActual(codigoUnico));
        notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoUnico, respuesta);
        return respuesta;
    }

    private RespuestaMovimientoDTO crearRespuestaMovimiento(
            Jugador jugador, Integer posicionAnterior, ResultadoTiradaDTO resultado) {
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setIdJugador(jugador.getId());
        respuesta.setPosicionAnterior(posicionAnterior);
        respuesta.setIdMovimiento(java.util.UUID.randomUUID().toString());
        respuesta.setDado1(resultado.getDado1());
        respuesta.setDado2(resultado.getDado2());
        respuesta.setSuma(resultado.sumarDados());
        respuesta.setNuevaPosicion(jugador.getPosicionActual());
        respuesta.setPosicionCaidaEvento(jugador.getPosicionActual());
        return respuesta;
    }

    private Partida obtenerPartidaParaTirada(String codigoUnico) throws PartidaNoEncontradaException {
        Partida partida = servicioPartida.obtenerPartida(codigoUnico);
        if (partida == null) throw new PartidaNoEncontradaException();
        return partida;
    }

    private Jugador obtenerJugadorParaTirada(Partida partida, Long idJugador)
            throws JugadorNoEncontradoException {
        return partida.getJugadores().stream()
                .filter(jugador -> jugador.getId().equals(idJugador))
                .findFirst()
                .orElseThrow(() -> new JugadorNoEncontradoException(
                        "No se encontró el jugador en esta partida"));
    }

    private void validarTurnoDeTirada(String codigoUnico, Long idJugador, Jugador jugador)
            throws ResponseStatusException {
        Jugador jugadorDelTurno = servicioPartida.obtenerJugadorActual(codigoUnico);
        if (jugador.getEnBancarrota() || !esTurnoDeJugador(jugadorDelTurno, idJugador)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No podés tirar los dados en este turno.");
        }
    }

    private Boolean resolverPropiedadDestino(CasilleroPropiedad casillero, Jugador jugador,
                                             RespuestaMovimientoDTO respuesta) {
        if (casillero == null) return false;
        Propiedad propiedad = casillero.getPropiedad();
        if (servicioPropiedad.estaDisponible(propiedad)) return true;
        Integer alquiler = servicioPropiedad.calcularAlquiler(casillero, propiedad);
        Jugador propietario = propiedad.getPropietario();
        if (propietario != null && !propietario.getId().equals(jugador.getId())) {
            servicioPropiedad.pagarAlquiler(propiedad, jugador, alquiler);
            respuesta.setAlquiler(new AlquilerMovimientoDTO(jugador.getId(), jugador.getApodo(),
                    propietario.getApodo(), propiedad.getNombre(), alquiler));
        }
        return false;
    }
    @ExceptionHandler(JugadorNoEncontradoException.class)
    public ResponseEntity<String> manejarJugadorNoEncontrado(JugadorNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()); //le devuelve al frontend un código 404 de "not found" y el mensaje de la excepción
    }
}
