package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.excepcion.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEconomia;
import com.tallerwebi.dominio.servicios.interfaces.ServicioTurno;
import static com.tallerwebi.dominio.catalogo.CatalogoTablero.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service("servicioPropiedad")
@Transactional
public class ServicioPropiedadImpl implements ServicioPropiedad {
    private final ServicioParticipacion participacion;

    private final RepositorioPropiedad repositorioPropiedad;
    private final RepositorioPartida repositorioPartida;
    private final ServicioEconomia economia;
    private final ServicioTurno turnos;
    private final ServicioCasillero servicioCasillero;
    private static final Integer NIVEL_MAXIMO_MEJORA = 5;
    private static final List<Integer> ALQUILERES_MEJORADOS = List.of(200, 300, 500, 1000, 1500);
    private static final List<Integer> ALQUILERES_POR_PARADAS = List.of(0, 20, 50, 100, 200);
    @Autowired
    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad, RepositorioPartida repositorioPartida,
                                 ServicioCasillero servicioCasillero, ServicioEconomia economia, ServicioTurno turnos, ServicioParticipacion participacion) {
        this.participacion = participacion;
        this.repositorioPropiedad = repositorioPropiedad;
        this.repositorioPartida = repositorioPartida;
        this.servicioCasillero = servicioCasillero;
        this.economia = economia;
        this.turnos = turnos;
    }

    @Override
    public Propiedad obtenerPropiedadExistente(Long idPropiedad) {
        Propiedad propiedad = repositorioPropiedad.buscarPropiedadPorId(idPropiedad);

        if (propiedad == null) {
            throw new PropiedadNoEncontradaException("No se encontró la propiedad solicitada.");
        }
        return propiedad;
    }

    @Override
    public Boolean estaDisponible(Propiedad propiedad) {
        return propiedad != null && propiedad.getPropietario() == null;
    }

    @Override
    public CompraPreparada prepararCompra(String codigoPartida, Integer posicion)
            throws EstadoPartidaInvalidoException {
        // La consulta y la materialización participan de la transacción del servicio.
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        if (!servicioCasillero.existePosicionDePropiedad(partida, posicion)) return null;
        Jugador jugador = turnos.obtenerJugadorActual(partida);
        if (jugador == null) {
            throw new EstadoPartidaInvalidoException("No hay un jugador activo en esta partida.");
        }
        if (!Objects.equals(jugador.getPosicionActual(), posicion)) {
            throw new EstadoPartidaInvalidoException(
                    "El jugador activo está en el casillero " + jugador.getPosicionActual()
                            + ", pero se intentó comprar el casillero " + posicion + ".");
        }
        CasilleroPropiedad casillero = servicioCasillero.buscarOCrearPropiedadEnPosicion(partida, posicion);
        return new CompraPreparada(partida.getCodigoUnico(), jugador, casillero);
    }

    @Override
    public void comprarPropiedad(Long idPropiedad, Jugador jugador) {
        validarJugador(jugador);
        Jugador comprador = jugador.getId() == null ? null : repositorioPartida.buscarJugadorPorId(jugador.getId());
        if (comprador == null) comprador = jugador;
        ejecutarCompra(idPropiedad, comprador);
        jugador.setDinero(comprador.getDinero());
    }

    @Override
    public void comprarPropiedadParaJugador(Long idPropiedad, Long idJugador) {
        Jugador comprador = obtenerJugadorExistente(idJugador, "No se encontró el jugador que compra.");
        ejecutarCompra(idPropiedad, comprador);
    }

    private void ejecutarCompra(Long idPropiedad, Jugador comprador) {
        Propiedad propiedadEncontrada = obtenerPropiedadExistente(idPropiedad);
        if (!estaDisponible(propiedadEncontrada)) {
            throw new PropiedadNoDisponibleException("La propiedad no está disponible.");
        }

        if (!economia.tieneDineroSuficiente(comprador, propiedadEncontrada.getPrecioCompra())) {
            throw new DineroInsuficienteException("El jugador no tiene dinero suficiente para realizar la operación.");
        }

        economia.restarDinero(comprador, propiedadEncontrada.getPrecioCompra());
        propiedadEncontrada.setPropietario(comprador);
        repositorioPropiedad.guardarPropiedad(propiedadEncontrada);
        repositorioPartida.actualizarJugador(comprador);
    }

    @Override
    public CasilleroPropiedad prepararVenta(Partida partida, Long idJugador, Integer posicion)
            throws EstadoPartidaInvalidoException {
        if (idJugador == null) {
            throw new EstadoPartidaInvalidoException("No perteneces a esta partida.");
        }
        Jugador jugador = repositorioPartida.buscarJugadorPorId(idJugador);
        if (!participacion.permiteAdministrarPropiedades(partida, jugador, idJugador)) {
            throw new EstadoPartidaInvalidoException("No perteneces a esta partida.");
        }
        return servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
    }

    @Override
    public Integer calcularPrecioVenta(Integer precioCompra) {
        if (precioCompra == null || precioCompra < 0) {
            throw new PropiedadInvalidaException("El precio de compra de la propiedad no es válido.");
        }
        return precioCompra / 2;
    }

    @Override
    public Integer venderPropiedadParaJugador(Long idPropiedad, Long idJugador) {
        Jugador vendedor = obtenerJugadorExistente(idJugador, "No se encontró el jugador que vende.");
        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);
        validarPropietarioDeVenta(propiedad, idJugador);
        Integer precioVenta = calcularPrecioVenta(propiedad.getPrecioCompra());
        propiedad.setPropietario(null);
        propiedad.setNivelMejora(0);
        economia.sumarDinero(vendedor, precioVenta);
        repositorioPropiedad.guardarPropiedad(propiedad);
        repositorioPartida.actualizarJugador(vendedor);
        return precioVenta;
    }

    @Override
    public void transferirPropiedadParaJugadores(Long idPropiedad, Long idVendedor, Long idComprador, Integer precio) {
        if (precio == null || precio < 0) throw new PropiedadInvalidaException("El precio ofrecido no es válido.");
        Jugador vendedor = obtenerJugadorExistente(idVendedor, "No se encontró el jugador que vende.");
        Jugador comprador = obtenerJugadorExistente(idComprador, "No se encontró el jugador que vende.");
        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);
        validarPropietarioDeVenta(propiedad, idVendedor);
        if (Objects.equals(idVendedor, idComprador)) {
            throw new PropiedadInvalidaException("No podés comprarle una propiedad a vos mismo.");
        }
        if (!economia.tieneDineroSuficiente(comprador, precio)) {
            throw new DineroInsuficienteException("El comprador no tiene dinero suficiente para aceptar la oferta.");
        }
        economia.restarDinero(comprador, precio);
        economia.sumarDinero(vendedor, precio);
        propiedad.setPropietario(comprador);
        repositorioPartida.actualizarJugador(comprador);
        repositorioPartida.actualizarJugador(vendedor);
        repositorioPropiedad.guardarPropiedad(propiedad);
    }

    private Jugador obtenerJugadorExistente(Long idJugador, String mensajeNoEncontrado) {
        if (idJugador == null) throw new JugadorInvalidoException("El jugador no puede ser nulo.");
        Jugador vendedor = repositorioPartida.buscarJugadorPorId(idJugador);
        if (vendedor == null) throw new JugadorInvalidoException(mensajeNoEncontrado);
        return vendedor;
    }

    private void validarPropietarioDeVenta(Propiedad propiedad, Long idJugador) {
        if (propiedad.getPropietario() == null || !Objects.equals(idJugador, propiedad.getPropietario().getId())) {
            throw new PropiedadNoDisponibleException("Solo puedes vender una propiedad que te pertenece.");
        }
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador) {
        validarPropiedad(propiedad);
        validarJugador(jugador);
        ejecutarPagoAlquiler(propiedad, jugador, calcularAlquilerValidada(propiedad));
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler) {
        validarPropiedad(propiedad);
        validarJugador(jugador);
        ejecutarPagoAlquiler(propiedad, jugador, montoAlquiler);
    }

    private void ejecutarPagoAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler) {
        if (montoAlquiler == null || montoAlquiler < 0) {
            throw new PropiedadInvalidaException("El precio del alquiler no es válido.");
        }
        Propiedad propiedadPersistida = repositorioPropiedad.buscarPropiedadPorId(propiedad.getId());
        if (propiedadPersistida == null) propiedadPersistida = propiedad;
        Jugador propietario = propiedadPersistida.getPropietario();
        if (propietario == null || propietario.getId().equals(jugador.getId())) {
            return;
        }
        Jugador pagador = repositorioPartida.buscarJugadorPorId(jugador.getId());
        if (pagador == null) pagador = jugador;
        economia.restarDinero(pagador, montoAlquiler);
        jugador.setDinero(pagador.getDinero());
        repositorioPartida.actualizarJugador(pagador);
        economia.sumarDinero(propietario, montoAlquiler);
        repositorioPartida.actualizarJugador(propietario);
    }
//calcula el alquiler automáticamente
    @Override
    public Integer calcularAlquiler(Propiedad propiedad) {
        validarPropiedad(propiedad);
        return calcularAlquilerValidada(propiedad);
    }

    private Integer calcularAlquilerValidada(Propiedad propiedad) {
        Integer alquilerBase = propiedad.getPrecioAlquiler();
        Integer nivelMejora = propiedad.getNivelMejora() == null ? 0 : propiedad.getNivelMejora();
        if (alquilerBase == null || alquilerBase < 0) {
            throw new PropiedadInvalidaException("El precio del alquiler no es válido.");
        }

        if (nivelMejora < 0) {
            throw new PropiedadInvalidaException("El nivel de mejora no es válido.");
        }
        if (nivelMejora == 0) return alquilerBase;
        return ALQUILERES_MEJORADOS.get(Math.min(nivelMejora, NIVEL_MAXIMO_MEJORA) - 1);
    }
//Recibe el importe ya calculado
    @Override
    public Integer calcularAlquiler(CasilleroPropiedad casillero, Propiedad propiedad) {
        Long idPartida = casillero.getPartidaId();
        if (!POSICIONES_PARADA.contains(casillero.getPosicion())) return calcularAlquiler(propiedad);
        Jugador propietario = propiedad.getPropietario();
        if (propietario == null || idPartida == null) return 20;
        Long cantidadParadas = servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(c -> POSICIONES_PARADA.contains(c.getPosicion()) && c.getPropiedad() != null)
                .filter(c -> idPartida.equals(c.getPartidaId()))
                .filter(c -> c.getPropiedad().getPropietario() != null
                        && Objects.equals(propietario.getId(), c.getPropiedad().getPropietario().getId()))
                .count();
        return ALQUILERES_POR_PARADAS.get(
                Math.toIntExact(Math.min(cantidadParadas, ALQUILERES_POR_PARADAS.size() - 1)));
    }

    @Override
    public void mejorarPropiedad(Long idPropiedad, Jugador jugador) {
        validarJugador(jugador);

        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);

        validarPropietario(propiedad, jugador);
        validarGrupoCompleto(propiedad, jugador);
        validarNivelMejora(propiedad);

        Integer nivelActual = propiedad.getNivelMejora() == null ? 0 : propiedad.getNivelMejora();
        Integer costoMejora = ALQUILERES_MEJORADOS.get(nivelActual);
        Jugador jugadorActualizado = jugador.getId() == null ? null : repositorioPartida.buscarJugadorPorId(jugador.getId());
        if (jugadorActualizado == null) jugadorActualizado = jugador;
        if (!economia.tieneDineroSuficiente(jugadorActualizado, costoMejora)) {
            throw new DineroInsuficienteException("No tenés dinero suficiente para subir el alquiler.");
        }
        economia.restarDinero(jugadorActualizado, costoMejora);
        repositorioPartida.actualizarJugador(jugadorActualizado);
        jugador.setDinero(jugadorActualizado.getDinero());
        jugador.setEnBancarrota(jugadorActualizado.getEnBancarrota());
        propiedad.setNivelMejora(nivelActual + 1);
    }

    //Limita el nivel de mejoras hasta 5
    private void validarNivelMejora(Propiedad propiedad) {
        Integer nivelMejora = propiedad.getNivelMejora();

        if (nivelMejora != null && (nivelMejora < 0 || nivelMejora >= NIVEL_MAXIMO_MEJORA)) {
            throw new PropiedadNoMejorableException("El nivel de mejora no es válido o alcanzó el máximo.");
        }
    }

    private void validarJugador(Jugador jugador) {
        if (jugador == null) {
            throw new JugadorInvalidoException("El jugador no puede ser nulo.");
        }
    }

    private void validarPropiedad(Propiedad propiedad) {
        if (propiedad == null) {
            throw new PropiedadInvalidaException("No se puede calcular el alquiler de una propiedad inexistente.");
        }
    }

    private void validarPropietario(Propiedad propiedad, Jugador jugador) {
        Jugador propietario = propiedad.getPropietario();

        if (propietario == null ||
                !propietario.getId().equals(jugador.getId())) {
            throw new PropiedadNoMejorableException("Solo puedes mejorar tus propias propiedades.");
        }
    }

    private void validarGrupoCompleto(Propiedad propiedad, Jugador jugador) {
        if (!esDuenioDeTodoElGrupo(propiedad, jugador)) {
            throw new PropiedadNoMejorableException("Debes ser dueño de todas las propiedades de la localidad para mejorarlas.");
        }
    }

    private Boolean esDuenioDeTodoElGrupo(Propiedad propiedad, Jugador jugador) {
        Set<Integer> posiciones = posicionesDelGrupo(propiedad.getGrupo());
        if (posiciones.isEmpty()) return false;
        CasilleroPropiedad objetivo = buscarCasilleroDePropiedad(propiedad.getId());
        if (objetivo == null || objetivo.getPartidaId() == null
                || !posiciones.contains(objetivo.getPosicion())) return false;
        Set<Integer> posicionesPropias = servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(casillero -> Objects.equals(objetivo.getPartidaId(), casillero.getPartidaId()))
                .filter(casillero -> perteneceAJugador(casillero.getPropiedad(), jugador))
                .map(CasilleroPropiedad::getPosicion)
                .collect(java.util.stream.Collectors.toSet());
        return posicionesPropias.containsAll(posiciones);
    }

    private boolean perteneceAJugador(Propiedad propiedad, Jugador jugador) {
        return propiedad != null && propiedad.getPropietario() != null
                && Objects.equals(jugador.getId(), propiedad.getPropietario().getId());
    }

    private CasilleroPropiedad buscarCasilleroDePropiedad(Long idPropiedad) {
        if (idPropiedad == null) return null;
        return servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(casillero -> casillero.getPropiedad() != null
                        && idPropiedad.equals(casillero.getPropiedad().getId()))
                .findFirst().orElse(null);
    }

}
