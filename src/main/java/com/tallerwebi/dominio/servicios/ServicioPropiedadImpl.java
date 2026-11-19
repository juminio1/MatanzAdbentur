package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.excepcion.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioAlquiler;
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

import java.util.Objects;

@Service("servicioPropiedad")
@Transactional
public class ServicioPropiedadImpl implements ServicioPropiedad {
    private final ServicioParticipacion participacion;

    private final RepositorioPropiedad repositorioPropiedad;
    private final RepositorioPartida repositorioPartida;
    private final ServicioEconomia economia;
    private final ServicioTurno turnos;
    private final ServicioCasillero servicioCasillero;
    @Autowired(required = false)
    private com.tallerwebi.infraestructura.RepositorioDeudaPendiente deudas;
    private final ServicioAlquiler alquileres;
    private final BarrioDePropiedad barrios;

    @Autowired
    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad, RepositorioPartida repositorioPartida,
                                 ServicioCasillero servicioCasillero, ServicioEconomia economia, ServicioTurno turnos,
                                 ServicioParticipacion participacion, ServicioAlquiler alquileres) {
        this.participacion = participacion;
        this.repositorioPropiedad = repositorioPropiedad;
        this.repositorioPartida = repositorioPartida;
        this.servicioCasillero = servicioCasillero;
        this.economia = economia;
        this.turnos = turnos;
        this.alquileres = alquileres;
        this.barrios = new BarrioDePropiedad(servicioCasillero);
    }

    // Mantiene la forma de construirlo que ya usan los tests: arma el servicio de alquiler con las mismas dependencias.
    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad, RepositorioPartida repositorioPartida,
                                 ServicioCasillero servicioCasillero, ServicioEconomia economia, ServicioTurno turnos,
                                 ServicioParticipacion participacion) {
        this(repositorioPropiedad, repositorioPartida, servicioCasillero, economia, turnos, participacion,
                new ServicioAlquilerImpl(repositorioPropiedad, repositorioPartida, servicioCasillero, economia, participacion));
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
        repositorioPartida.bloquearPartida(partida);
        participacion.validarCompraEnPartida(partida);
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

    @Override
    public CompraPreparada responderCompraDeEvento(String codigoPartida, Long idJugador, Integer ronda, Boolean aceptar) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        repositorioPartida.bloquearPartida(partida);
        participacion.validarAccionOrdinaria(partida);
        Jugador jugador = obtenerJugadorExistente(idJugador, "No se encontró el comprador.");
        participacion.validarCompraPorEvento(partida, jugador, ronda, aceptar);
        if (!aceptar) {
            partida.getEventoGlobal().ofrecerCompra(null);
            return null;
        }
        validarTurnoParaCompraDeEvento(partida, jugador);
        Integer posicion = partida.getEventoGlobal().getPosicionCompra();
        if (!Objects.equals(posicion, jugador.getPosicionActual())) {
            throw new PropiedadNoDisponibleException("Ya no estás en el casillero de esta oportunidad de compra.");
        }
        CasilleroPropiedad casillero = servicioCasillero.buscarOCrearPropiedadEnPosicion(partida, posicion);
        ejecutarCompra(casillero.getPropiedad().getId(), jugador);
        partida.getEventoGlobal().ofrecerCompra(null);
        return new CompraPreparada(codigoPartida, jugador, casillero);
    }

    // La compra que habilita un evento global solo se hace en el turno del jugador, antes de tirar.
    private void validarTurnoParaCompraDeEvento(Partida partida, Jugador jugador) {
        Jugador actual = turnos.obtenerJugadorActual(partida);
        if (actual == null || !Objects.equals(actual.getId(), jugador.getId())) {
            throw new PropiedadNoDisponibleException("Solo podés comprar esta propiedad durante tu turno.");
        }
        if (Boolean.TRUE.equals(jugador.getTiroEnTurno())) {
            throw new PropiedadNoDisponibleException("La oportunidad de compra se pierde al tirar los dados.");
        }
    }

    private void ejecutarCompra(Long idPropiedad, Jugador comprador) {
        validarPartidaDelJugador(comprador.getId());
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
        repositorioPartida.bloquearPartida(partida);
        participacion.validarPartidaEnCurso(partida);
        participacion.validarSinTrivia(partida);
        if (idJugador == null) {
            throw new EstadoPartidaInvalidoException("No perteneces a esta partida.");
        }
        Jugador jugador = repositorioPartida.buscarJugadorPorId(idJugador);
        if (!participacion.permiteAdministrarPropiedades(partida, jugador, idJugador)) {
            throw new EstadoPartidaInvalidoException("No perteneces a esta partida.");
        }
        participacion.validarPartidaEnCurso(partida);
        return servicioCasillero.buscarCasilleroPropiedad(partida, posicion);
    }

    @Override
    public Integer calcularPrecioVenta(Integer precioCompra) {
        if (precioCompra == null || precioCompra < 0) {
            throw new PropiedadInvalidaException("El precio de compra de la propiedad no es válido.");
        }
        return precioVenta(precioCompra, 0);
    }

    // Incluye lo invertido en mejoras.
    @Override
    public Integer calcularPrecioVenta(Propiedad propiedad) {
        calcularPrecioVenta(propiedad.getPrecioCompra()); // valida el precio de compra
        return precioVenta(propiedad.getPrecioCompra(), Objects.requireNonNullElse(propiedad.getNivelMejora(), 0));
    }

    @Override
    public Integer venderPropiedadParaJugador(Long idPropiedad, Long idJugador) {
        Jugador vendedor = obtenerJugadorExistente(idJugador, "No se encontró el jugador que vende.");
        Partida partida = repositorioPartida.buscarPartidaPorJugador(idJugador);
        repositorioPartida.bloquearPartida(partida);
        participacion.validarPartidaEnCurso(partida);
        participacion.validarSinTrivia(partida);
        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);
        validarPropietarioDeVenta(propiedad, idJugador);
        Integer precioVenta = calcularPrecioVenta(propiedad);
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
        Partida partidaVendedor = repositorioPartida.buscarPartidaPorJugador(idVendedor);
        repositorioPartida.bloquearPartida(partidaVendedor);
        participacion.validarPartidaEnCurso(partidaVendedor);
        participacion.validarPartidaEnCurso(partidaVendedor);
        Partida partidaComprador = repositorioPartida.buscarPartidaPorJugador(idComprador);
        participacion.validarPartidaEnCurso(partidaComprador);
        participacion.validarParticipantesDeTransferencia(partidaVendedor, partidaComprador, vendedor, comprador);
        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);
        validarPropietarioDeVenta(propiedad, idVendedor);
        if (!economia.tieneDineroSuficiente(comprador, precio)) {
            throw new DineroInsuficienteException("El comprador no tiene dinero suficiente para aceptar la oferta.");
        }
        economia.restarDinero(comprador, precio);
        economia.sumarDinero(vendedor, precio);
        propiedad.setPropietario(comprador);
        propiedad.setNivelMejora(0);
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

    private Partida validarPartidaDelJugador(Long idJugador) {
        Partida partida = repositorioPartida.buscarPartidaPorJugador(idJugador);
        repositorioPartida.bloquearPartida(partida);
        participacion.validarAccionOrdinaria(partida);
        return partida;
    }

    private void validarPropietarioDeVenta(Propiedad propiedad, Long idJugador) {
        if (propiedad.getPropietario() == null || !Objects.equals(idJugador, propiedad.getPropietario().getId())) {
            throw new PropiedadNoDisponibleException("Solo puedes vender una propiedad que te pertenece.");
        }
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador) {
        alquileres.pagarAlquiler(propiedad, jugador);
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler) {
        alquileres.pagarAlquiler(propiedad, jugador, montoAlquiler);
    }

    @Override
    public Integer calcularAlquiler(Propiedad propiedad) {
        return alquileres.calcularAlquiler(propiedad);
    }

    @Override
    public Integer calcularAlquiler(CasilleroPropiedad casillero, Propiedad propiedad) {
        return alquileres.calcularAlquiler(casillero, propiedad);
    }

    @Override
    public void mejorarPropiedad(Long idPropiedad, Jugador jugador) {
        validarJugador(jugador);
        Partida partida = validarPartidaDelJugador(jugador.getId());
        participacion.validarGestionAntesDeTirar(partida, jugador.getId(), false);

        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);

        validarPropietario(propiedad, jugador);
        validarGrupoCompleto(propiedad, jugador);
        validarNivelMejora(propiedad);

        Integer nivelActual = propiedad.getNivelMejora() == null ? 0 : propiedad.getNivelMejora();
        Integer costoMejora = calcularCostoMejora(propiedad);
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

    private Integer calcularCostoMejora(Propiedad propiedad) {
        Integer precioCompra = propiedad.getPrecioCompra();
        if (precioCompra == null || precioCompra < 0) {
            throw new PropiedadInvalidaException("El precio de la propiedad no es válido.");
        }
        return costoMejora(precioCompra);
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

    private void validarPropietario(Propiedad propiedad, Jugador jugador) {
        Jugador propietario = propiedad.getPropietario();

        if (propietario == null ||
                !propietario.getId().equals(jugador.getId())) {
            throw new PropiedadNoMejorableException("Solo puedes mejorar tus propias propiedades.");
        }
    }

    private void validarGrupoCompleto(Propiedad propiedad, Jugador jugador) {
        CasilleroPropiedad casillero = barrios.buscarCasilleroDePropiedad(propiedad.getId());
        if (!barrios.esDuenioDeTodoElBarrio(propiedad, casillero, jugador)) {
            throw new PropiedadNoMejorableException("Debes ser dueño de todas las propiedades de la localidad para mejorarlas.");
        }
    }

}
