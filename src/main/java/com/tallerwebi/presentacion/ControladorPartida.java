package com.tallerwebi.presentacion;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.EventoCasillero;
import com.tallerwebi.dominio.excepcion.JugadorNoEncontradoException;
import com.tallerwebi.dominio.excepcion.DineroInsuficienteException;
import com.tallerwebi.dominio.excepcion.EstadoPartidaInvalidoException;
import com.tallerwebi.dominio.excepcion.PartidaNoEncontradaException;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEventoCasillero;
import com.tallerwebi.presentacion.DTO.RespuestaMovimientoDTO;
import com.tallerwebi.presentacion.DTO.SaldoJugadorDTO;
import com.tallerwebi.presentacion.DTO.AlquilerMovimientoDTO;
import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;
import com.tallerwebi.presentacion.DTO.TurnoDTO;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import com.tallerwebi.presentacion.coordinadores.CoordinadorTurnoPartida;
import com.tallerwebi.presentacion.sesion.ResolutorJugadorSesion;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private static final Set<Integer> POSICIONES_PARADA = Set.of(6, 12, 17, 24);
    private final ServicioPartida servicioPartida;
    private final SimpMessagingTemplate notificadorWebSocket;
    private final ServicioCasillero servicioCasillero;
    private final ServicioEventoCasillero servicioEventoCasillero;
    private final ServicioPropiedad servicioPropiedad;
    private final CoordinadorTurnoPartida coordinadorTurno;
    private final NotificadorPartida notificadorPartida;

    @Autowired
    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
                              ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
                              ServicioEventoCasillero servicioEventoCasillero, NotificadorPartida notificadorPartida
    ) {
        this.notificadorWebSocket = notificadorWebSocket;
        this.servicioPartida = servicioPartida;
        this.servicioCasillero = servicioCasillero;
        this.servicioEventoCasillero = servicioEventoCasillero;
        this.servicioPropiedad = servicioPropiedad;
        this.coordinadorTurno = new CoordinadorTurnoPartida(servicioPartida);
        this.notificadorPartida = notificadorPartida;
    }

    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
                              ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad) {
        this(servicioPartida, notificadorWebSocket, servicioCasillero, servicioPropiedad,
                null, new NotificadorPartida(notificadorWebSocket));
    }

    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
                              ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
                              ServicioEventoCasillero servicioEventoCasillero) {
        this(servicioPartida, notificadorWebSocket, servicioCasillero, servicioPropiedad,
                servicioEventoCasillero, new NotificadorPartida(notificadorWebSocket));
    }

    public ControladorPartida(ServicioPartida servicioPartida, SimpMessagingTemplate notificadorWebSocket,
                              ServicioCasillero servicioCasillero, ServicioPropiedad servicioPropiedad,
                              NotificadorPartida notificadorPartida) {
        this(servicioPartida, notificadorWebSocket, servicioCasillero, servicioPropiedad,
                null, notificadorPartida);
    }

    @GetMapping("/partida/{codigoPartida}/propiedades/{posicion}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerInfoPropiedad(
        @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
        @PathVariable(PARAM_POSICION) Integer posicion
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) return ResponseEntity.notFound().build();
        CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null) {
            return ResponseEntity.ok(dtoCatalogoPropiedad(posicion));
        }
        return ResponseEntity.ok(dtoPropiedad(casillero, casillero.getPropiedad()));
    }

    @GetMapping("/partida/{codigoPartida}/propiedades")
    @ResponseBody
    public ResponseEntity<?> obtenerPropiedadesTablero(
        @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
        HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!ResolutorJugadorSesion.pertenece(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of(CLAVE_ERROR, "No pertenecés a esta partida.")
            );
        }

        return ResponseEntity.ok(obtenerCatalogoTablero(partida));
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/subir-alquiler")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> subirAlquilerPropiedad(
        @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
        @PathVariable(PARAM_POSICION) Integer posicion,
        HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (partida == null) return ResponseEntity.notFound().build();
        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);
        if (!puedeParticiparEnPartida(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of(CLAVE_ERROR, "No podés administrar propiedades en esta partida.")
            );
        }
        Jugador jugadorActual = servicioPartida.obtenerJugadorActual(codigoPartida);
        if (!esTurnoDeJugador(jugadorActual, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of(CLAVE_ERROR, "Solo podés subir alquileres durante tu turno.")
            );
        }
        CasilleroPropiedad casillero = servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
        if (!esPropietarioDelCasillero(casillero, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                Map.of(CLAVE_ERROR, "Solo podés subir el alquiler de tus propiedades.")
            );
        }
        return procesarMejoraPropiedad(codigoPartida, idJugador, casillero);
    }

    private ResponseEntity<Map<String, Object>> procesarMejoraPropiedad(
        String codigoPartida,
        Long idJugador,
        CasilleroPropiedad casillero
    ) {
        try {
            Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
            servicioPropiedad.mejorarPropiedad(casillero.getPropiedad().getId(), jugador);
            Propiedad propiedad = servicioPropiedad.obtenerPropiedadExistente(
                casillero.getPropiedad().getId()
            );
            Map<String, Object> dto = dtoPropiedad(casillero, propiedad);
            Jugador jugadorActualizado = servicioPartida.obtenerJugadorPorId(idJugador);
            dto.put("idJugador", idJugador);
            dto.put("dineroJugador", jugadorActualizado.getDinero());
            dto.put("enBancarrota", jugadorActualizado.getEnBancarrota());
            notificadorWebSocket.convertAndSend(
                TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades",
                dto
            );
            return ResponseEntity.ok(dto);
        } catch (RuntimeException ex) {
            String mensaje =
                ex.getMessage() == null ? "No se pudo subir el alquiler." : ex.getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/comprar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> comprarPropiedad(
        @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
        @PathVariable(PARAM_POSICION) Integer posicion
    ) {
        try {
            ServicioPropiedad.CompraPreparada compra = servicioPropiedad.prepararCompra(codigoPartida, posicion);
            if (compra == null) return ResponseEntity.notFound().build();
            return procesarCompraPropiedad(compra);
        } catch (EstadoPartidaInvalidoException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        } catch (RuntimeException ex) {
            LOGGER.error(
                "Error interno al comprar la propiedad {} en la partida {}",
                posicion,
                codigoPartida,
                ex
            );
            String detalle =
                ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(CLAVE_ERROR, "Error interno al comprar la propiedad: " + detalle)
            );
        }
    }

    @PostMapping("/partida/{codigoPartida}/propiedades/{posicion}/vender")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> venderPropiedad(
        @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida,
        @PathVariable(PARAM_POSICION) Integer posicion,
        HttpServletRequest request
    ) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) {
            return ResponseEntity.notFound().build();
        }
        Long jugadorSesionId = ResolutorJugadorSesion.resolver(partida, request);
        CasilleroPropiedad casillero;
        try {
            casillero = servicioPropiedad.prepararVenta(partida, jugadorSesionId, posicion);
        } catch (EstadoPartidaInvalidoException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(CLAVE_ERROR, ex.getMessage()));
        }
        if (casillero == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of(CLAVE_ERROR, "La propiedad no está disponible para vender.")
            );
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
            String mensaje = obtenerMensajeError(ex, "No se pudo vender la propiedad.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }
    @PostMapping("/partida/{codigoPartida}/abandonar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> abandonarPartida(
            @PathVariable("codigoPartida") String codigoPartida, HttpServletRequest request) {
        Partida partida = servicioPartida.obtenerPartida(codigoPartida);

        if (partida == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of(CLAVE_ERROR, "La partida no existe.")
            );
        }

        Long idJugador = ResolutorJugadorSesion.resolver(partida, request);

        if (!ResolutorJugadorSesion.pertenece(partida, idJugador)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                    Map.of(CLAVE_ERROR, "No pertenecés a esta partida.")
            );
        }
        Jugador jugador = partida.getJugadores().stream().filter(j -> idJugador.equals(j.getId())).findFirst().orElseThrow(()
                -> new IllegalArgumentException("El jugador no pertenece a la partida."));

        return ejecutarAbandono(partida, jugador);
    }

    private ResponseEntity<Map<String, Object>> ejecutarAbandono(
            Partida partida, Jugador jugador) {

        try {
            servicioPartida.abandonarPartida(partida.getCodigoUnico(), jugador.getId());

            notificadorPartida.notificarAbandonoPorWebSocket(partida, jugador);

            return ResponseEntity.ok(Map.of("mensaje", "Abandonaste la partida correctamente."));

        } catch (PartidaNoEncontradaException excepcion) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(CLAVE_ERROR, "No se encontró la partida."));

        } catch (IllegalArgumentException excepcion) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(CLAVE_ERROR, "No se encontró el jugador."));
        }
    }

    private Boolean puedeParticiparEnPartida(Partida partida, Long idJugador) {
        if (idJugador == null) return false;
        Jugador jugador = servicioPartida.obtenerJugadorPorId(idJugador);
        return partida.permiteAdministrarPropiedades(jugador, idJugador);
    }

    private ResponseEntity<Map<String, Object>> procesarCompraPropiedad(ServicioPropiedad.CompraPreparada compra) {
        String codigoPartida = compra.codigoPartida();
        Jugador jugador = compra.jugador();
        CasilleroPropiedad casillero = compra.casillero();
        try {
            servicioPropiedad.comprarPropiedadParaJugador(
                casillero.getPropiedad().getId(),
                jugador.getId()
            );
            Jugador compradorActualizado = servicioPartida.obtenerJugadorPorId(jugador.getId());
            Map<String, Object> actualizado = dtoPropiedad(
                casillero,
                servicioPropiedad.obtenerPropiedadExistente(casillero.getPropiedad().getId())
            );
            actualizado.put("idJugadorPropietario", jugador.getId());
            actualizado.put("dineroJugador", compradorActualizado.getDinero());
            actualizado.put("enBancarrota", compradorActualizado.getEnBancarrota());
            notificadorWebSocket.convertAndSend(
                TOPIC_PARTIDA_PREFIX + codigoPartida + "/propiedades",
                actualizado
            );
            NotificadorPartida.notificarSaldos(
                notificadorWebSocket,
                servicioPartida,
                codigoPartida
            );
            TurnoDTO turno = coordinadorTurno.avanzarYObtenerTurno(codigoPartida);
            if (turno != null) actualizado.put("idJugadorActual", turno.getIdJugadorActual());
            return ResponseEntity.ok(actualizado);
        } catch (DineroInsuficienteException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("codigoError", "DINERO_INSUFICIENTE", CLAVE_ERROR, ex.getMessage())
            );
        } catch (RuntimeException ex) {
            String mensaje = obtenerMensajeError(ex, "Error al guardar la compra de la propiedad.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(CLAVE_ERROR, mensaje));
        }
    }

    private Boolean esTurnoDeJugador(Jugador jugadorActual, Long idJugador) {
        return jugadorActual != null && idJugador.equals(jugadorActual.getId());
    }

    private Boolean esPropietarioDelCasillero(CasilleroPropiedad casillero, Long idJugador) {
        return (
            casillero != null &&
            casillero.getPropiedad().getPropietario() != null &&
            idJugador.equals(casillero.getPropiedad().getPropietario().getId())
        );
    }

    private String obtenerMensajeError(RuntimeException excepcion, String mensajePredeterminado) {
        String mensaje = excepcion.getMessage();
        return mensaje == null || mensaje.isBlank() ? mensajePredeterminado : mensaje;
    }

    private Map<String, Object> dtoCatalogoPropiedad(Integer posicion) {
        List<String> datos = servicioCasillero.obtenerDatosCatalogoPropiedad(posicion);
        if (datos == null) return Map.of();
        Map<String, Object> dto = new HashMap<>();
        dto.put("posicion", posicion);
        dto.put("nombre", datos.get(0));
        dto.put("descripcion", datos.get(1));
        dto.put("precioCompra", 300);
        dto.put("precioVenta", 150);
        Integer alquilerBase = servicioCasillero.obtenerAlquilerBaseCatalogo(posicion);
        dto.put("alquiler", alquilerBase);
        dto.put("alquilerBase", alquilerBase);
        dto.put("nivelMejora", 0);
        dto.put("grupo", POSICIONES_PARADA.contains(posicion) ? null : datos.get(2));
        dto.put("propietario", null);
        dto.put("propietarioId", null);
        dto.put("disponible", true);
        return dto;
    }

    private Map<String, Object> dtoPropiedad(CasilleroPropiedad casillero, Propiedad propiedad) {
        Map<String, Object> dto = new HashMap<>();
        List<String> datosCatalogo = servicioCasillero.obtenerDatosCatalogoPropiedad(casillero.getPosicion());
        Jugador propietario = propiedad.getPropietario();
        // Uso la posición para identificar el casillero.
        dto.put("posicion", casillero.getPosicion());
        dto.put("nombre", datosCatalogo == null ? casillero.getNombre() : datosCatalogo.get(0));
        dto.put(
            "descripcion",
            datosCatalogo == null ? casillero.getDescripcion() : datosCatalogo.get(1)
        );
        dto.put("precioCompra", propiedad.getPrecioCompra());
        dto.put(
            "precioVenta",
            propiedad.getPrecioCompra() == null ? 0 : propiedad.getPrecioCompra() / 2
        );
        dto.put("alquiler", servicioPropiedad.calcularAlquiler(casillero, propiedad));
        dto.put("propietario", propietario == null ? null : propietario.getApodo());
        dto.put("propietarioId", propietario == null ? null : propietario.getId());
        dto.put("disponible", propietario == null);
        dto.put("idPropiedad", propiedad.getId());
        dto.put(
            "alquilerBase",
            POSICIONES_PARADA.contains(casillero.getPosicion()) ? 20 : propiedad.getPrecioAlquiler()
        );
        dto.put("nivelMejora", propiedad.getNivelMejora() == null ? 0 : propiedad.getNivelMejora());
        dto.put(
            "grupo",
            POSICIONES_PARADA.contains(casillero.getPosicion()) || propiedad.getGrupo() == null
                ? null
                : propiedad.getGrupo().name()
        );
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

        Long jugadorSesionId = ResolutorJugadorSesion.resolver(
            partidaReal,
            usuarioSesionId,
            nombreSesion
        );
        Jugador jugadorSesion =
            jugadorSesionId == null ? null : servicioPartida.obtenerJugadorPorId(jugadorSesionId);

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
        List<CasilleroPropiedad> casilleros = servicioCasillero.obtenerCasillerosPropiedadDePartida(partida);

        return servicioCasillero.obtenerPosicionesPropiedad().stream().map(posicion -> casilleros.stream()
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
        @PathVariable(PARAM_CODIGO_PARTIDA) String codigoPartida
    ) {
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
        ResultadoTiradaDTO resultado = servicioPartida.tirarDados(); 

        if (!servicioPartida.puedeJugadorAvanzar(jugadorActual, resultado)) {
            servicioPartida.actualizarJugador(jugadorActual); 
            
            RespuestaMovimientoDTO respuesta = crearRespuestaMovimiento(jugadorActual, posicionAnterior, resultado);
            
            respuesta.setSaldos(servicioPartida.obtenerPartida(codigoUnico).getJugadores().stream()
                .map(SaldoJugadorDTO::desdeJugador).toList());
            
            coordinadorTurno.avanzarYObtenerTurno(codigoUnico);
            servicioPartida.prepararTiempoTurno(codigoUnico, respuesta.calcularSegundosPresentacion());
            respuesta.setTurnoSiguiente(coordinadorTurno.obtenerTurnoActual(codigoUnico));
            
            notificadorWebSocket.convertAndSend(TOPIC_PARTIDA_PREFIX + codigoUnico, respuesta);
            return respuesta; 
        }

        servicioPartida.moverJugador(jugadorActual, resultado.sumarDados(), 30); //se mueve ficha
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
        CasilleroPropiedad casilleroDestino = servicioCasillero.buscarOCrearPropiedadEnPosicion(partida,
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
        Jugador jugador,
        Integer posicionAnterior,
        ResultadoTiradaDTO resultado
    ) {
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

    private Partida obtenerPartidaParaTirada(String codigoUnico)
        throws PartidaNoEncontradaException {
        Partida partida = servicioPartida.obtenerPartida(codigoUnico);
        if (partida == null) throw new PartidaNoEncontradaException();
        return partida;
    }

    private Jugador obtenerJugadorParaTirada(Partida partida, Long idJugador)
        throws JugadorNoEncontradoException {
        return partida
            .getJugadores()
            .stream()
            .filter(jugador -> jugador.getId().equals(idJugador))
            .findFirst()
            .orElseThrow(() ->
                new JugadorNoEncontradoException("No se encontró el jugador en esta partida")
            );
    }

    private void validarTurnoDeTirada(String codigoUnico, Long idJugador, Jugador jugador)
        throws ResponseStatusException {
        Jugador jugadorDelTurno = servicioPartida.obtenerJugadorActual(codigoUnico);
        if (jugador.getEnBancarrota() || !esTurnoDeJugador(jugadorDelTurno, idJugador)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "No podés tirar los dados en este turno."
            );
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
