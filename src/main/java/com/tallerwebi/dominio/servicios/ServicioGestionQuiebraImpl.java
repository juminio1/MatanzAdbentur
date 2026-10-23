package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.enums.ResultadoDeuda;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.eventos.EventoJugadorQuebrado;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.interfaces.ServicioGestionQuiebra;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPropiedad;
import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gestiona pagos de alquiler, deudas pendientes y quiebras de jugadores. */
@Service
@Transactional//permite que la operación se realice dentro de una transacción de base de datos.
public class ServicioGestionQuiebraImpl implements ServicioGestionQuiebra {
    //se necesitan dos repositorios:
    //guarda la deuda
    private final RepositorioDeudaPendiente repositorioDeudaPendiente;
    //a comprobar que la partida existe y que los jugadores pertenecen a ella.
    private final RepositorioPartida repositorioPartida;
    private final ServicioPropiedad servicioPropiedad;
    private final RepositorioPropiedad repositorioPropiedad;
    private final PublicadorEventos<EventoJugadorQuebrado> publicadorQuiebra;

    @Autowired
    public ServicioGestionQuiebraImpl(
            RepositorioDeudaPendiente repositorioDeudaPendiente,
            RepositorioPartida repositorioPartida,
            ServicioPropiedad servicioPropiedad,
            RepositorioPropiedad repositorioPropiedad,
            PublicadorEventos<EventoJugadorQuebrado> publicadorQuiebra) {

        this.repositorioDeudaPendiente = repositorioDeudaPendiente;
        this.repositorioPartida = repositorioPartida;
        this.servicioPropiedad = servicioPropiedad;
        this.repositorioPropiedad = repositorioPropiedad;
        this.publicadorQuiebra = publicadorQuiebra;
    }

    @Override
    public void registrarDeuda(
            String codigoPartida,
            Jugador deudor,
            Jugador acreedor,
            Integer monto,
            String motivo) {

        if (deudor == null || monto == null || monto <= 0) {
            throw new IllegalArgumentException("Datos de deuda inválidos");
        }

        Partida partida = repositorioPartida
                .buscarPartidaActivaPorCodigoUnico(codigoPartida);

        if (partida == null) {
            throw new IllegalArgumentException("La partida no existe");
        }

        if (repositorioDeudaPendiente.buscarPorPartidaYJugador(
                codigoPartida, deudor.getId()) != null) {
            throw new IllegalStateException(
                    "El jugador ya tiene un pago pendiente");
        }

        DeudaPendiente deuda = new DeudaPendiente();
        deuda.setPartida(partida);
        deuda.setJugadorDeudor(deudor);
        deuda.setJugadorAcreedor(acreedor);
        deuda.setMontoPendiente(monto);
        deuda.setMotivo(motivo);

        repositorioDeudaPendiente.guardar(deuda);
    }

    //Busca la deuda, verifica el saldo, descuenta el importe, actualiza al acreedor, elimina la deuda.
    @Override
    public Boolean intentarResolverDeuda(
            String codigoPartida,
            Long idJugador) {

        DeudaPendiente deuda = obtenerDeudaPendiente(codigoPartida, idJugador);

        if (deuda == null) {
            return false;
        }

        Jugador deudor = deuda.getJugadorDeudor();
        Integer monto = deuda.getMontoPendiente();

        if (deudor.getDinero() == null
                || monto == null
                || monto <= 0
                || deudor.getDinero() < monto) {
            return false;
        }

        deudor.setDinero(deudor.getDinero() - monto);

        Jugador acreedor = deuda.getJugadorAcreedor();

        if (acreedor != null) {
            acreedor.setDinero(acreedor.getDinero() + monto);
            repositorioPartida.actualizarJugador(acreedor);
        }

        repositorioPartida.actualizarJugador(deudor);
        repositorioDeudaPendiente.eliminar(deuda);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public DeudaPendiente obtenerDeudaPendiente(String codigoPartida, Long idJugador) {
        return repositorioDeudaPendiente.buscarPorPartidaYJugador(codigoPartida, idJugador);
    }

    @Override
    public Boolean venderPropiedadYResolverDeuda(String codigoPartida, Long idJugador, Long idPropiedad) {
        DeudaPendiente deuda = obtenerDeudaPendiente(codigoPartida, idJugador);

        if (deuda == null) {
            throw new IllegalStateException("El jugador no tiene un pago pendiente");
        }
        if (idPropiedad == null) {
            throw new IllegalArgumentException("Debe seleccionar una propiedad");
        }

        servicioPropiedad.venderPropiedadParaJugador(idPropiedad, idJugador);

        if (intentarResolverDeuda(codigoPartida, idJugador)) {
            return true;
        }

        evaluarDeuda(codigoPartida, idJugador);
        return false;
    }

    @Override
    public ResultadoDeuda evaluarDeuda(
            String codigoPartida, Long idJugador) {

        DeudaPendiente deuda =
                obtenerDeudaPendiente(codigoPartida, idJugador);

        if (deuda == null) {
            return ResultadoDeuda.SIN_DEUDA;
        }

        if (intentarResolverDeuda(codigoPartida, idJugador)) {
            return ResultadoDeuda.RESUELTA;
        }

        if (tienePropiedadesVendibles(deuda.getJugadorDeudor())) {
            return ResultadoDeuda.PENDIENTE;
        }

        declararQuiebra(codigoPartida, idJugador);
        return ResultadoDeuda.QUIEBRA;
    }

    // Declara la quiebra del jugador cuando tiene una deuda pendiente.
    @Override
    public void declararQuiebra(String codigoPartida, Long idJugador) {
        Partida partida = validarPartidaParaQuiebra(codigoPartida, idJugador);
        DeudaPendiente deuda = obtenerDeudaPendiente(codigoPartida, idJugador);

        if (deuda == null) {
            throw new DeclaracionQuiebraInvalidaException("El jugador no tiene un pago pendiente.");
        }
        Jugador jugador = deuda.getJugadorDeudor();

        if (jugador.getAbandono() || jugador.getEnBancarrota()) {
            throw new DeclaracionQuiebraInvalidaException("El jugador ya abandonó o está en quiebra.");
        }

        jugador.setEnBancarrota(true);
        repositorioPartida.actualizarJugador(jugador);
        repositorioDeudaPendiente.eliminar(deuda);
        EventoJugadorQuebrado evento = new EventoJugadorQuebrado(partida, jugador);
        publicadorQuiebra.notificar(evento);
    }

    // Comprueba que la partida esté en curso y que el jugador participe en ella.
    private Partida validarPartidaParaQuiebra(String codigoPartida, Long idJugador) {
        Partida partida = repositorioPartida.buscarPartidaActivaPorCodigoUnico(codigoPartida);
        if (partida == null || partida.getEstado() != EstadoPartida.EN_CURSO) {
            throw new DeclaracionQuiebraInvalidaException("La partida no está en curso.");
        }
        if (idJugador == null || partida.getJugadores() == null
                || partida.getJugadores().stream().noneMatch(j -> idJugador.equals(j.getId()))) {
            throw new DeclaracionQuiebraInvalidaException("El jugador no pertenece a esta partida.");
        }
        return partida;
    }

    // Distingue una solicitud inválida de un fallo interno al declarar la quiebra.
    public static class DeclaracionQuiebraInvalidaException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public DeclaracionQuiebraInvalidaException(String mensaje) {
            super(mensaje);
        }
    }

    //Conecta los alquileres con el sistema de deuda.
    @Override
    public ResultadoDeuda procesarAlquiler(
            String codigoPartida,
            Propiedad propiedad,
            Jugador jugador,
            Integer montoAlquiler) {

        Jugador pagador = repositorioPartida.buscarJugadorPorId(jugador.getId());

        if (pagador == null) {
            throw new IllegalArgumentException("Jugador no encontrado");
        }

        if (obtenerDeudaPendiente(codigoPartida, pagador.getId()) != null) {
            throw new IllegalStateException("El jugador ya tiene un pago pendiente");
        }

        if (montoAlquiler == null || montoAlquiler < 0) {
            throw new IllegalArgumentException("Alquiler inválido");
        }

        if (pagador.getDinero() >= montoAlquiler) {
            servicioPropiedad.pagarAlquiler(propiedad, pagador, montoAlquiler);
            return ResultadoDeuda.RESUELTA;
        }

        registrarDeuda(
                codigoPartida,
                pagador,
                propiedad.getPropietario(),
                montoAlquiler,
                "ALQUILER"
        );

        return evaluarDeuda(codigoPartida, pagador.getId());
    }


    //busca el pago pendiente del jugador. Consulta las propiedades y si tiene alguna devuelve true y sino false.
    private Boolean tienePropiedadesVendibles(Jugador jugador) {
        return !repositorioPropiedad
                .buscarPropiedadesPorPropietario(jugador)
                .isEmpty();
    }
}
