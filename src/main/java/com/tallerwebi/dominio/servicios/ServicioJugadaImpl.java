

package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.*;

import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;

import com.tallerwebi.dominio.enums.ResultadoDeuda;

import com.tallerwebi.dominio.excepcion.*;

import com.tallerwebi.dominio.resultados.*;

import com.tallerwebi.dominio.servicios.interfaces.*;

import java.util.Objects;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

//Coordina una jugada completa dentro de una misma transacción.

@Service

@Transactional(rollbackFor = Exception.class)

// La jugada se ejecuta dentro de una trasaccion de base de datos. Protege los cambios transaccionales de base de datos.

public class ServicioJugadaImpl implements ServicioJugada {

    private final ServicioParticipacion participacion;

    private final ServicioDado dados;

    private final ServicioPartida partidas;

    private final ServicioCasillero casilleros;

    private final ServicioPropiedad propiedades;

    private final ServicioEventoCasillero eventos;

    private final ServicioGestionQuiebra quiebras;

    private final ServicioMovimiento movimiento;

    //inyecta las dependencias por constructor

    public ServicioJugadaImpl(ServicioPartida partidas, ServicioCasillero casilleros,

                              ServicioPropiedad propiedades, ServicioEventoCasillero eventos,

                              ServicioGestionQuiebra quiebras, ServicioMovimiento movimiento,

                              ServicioParticipacion participacion, ServicioDado dados) {

        this.dados = dados;

        this.participacion = participacion;

        this.partidas = partidas;

        this.casilleros = casilleros;

        this.propiedades = propiedades;

        this.eventos = eventos;

        this.quiebras = quiebras;

        this.movimiento = movimiento;

    }

    //coordina toda la jugada y devuelve el rtdo

    // (el id del jugador, la posicion anterior, la posicion actualizada,

    // el casillero donde cayó, el rtdo de los dados, el evento q se aplica, la suma de dinero por pasar por inicio,

    // los saldos actualizados de todos los jugadores y la info del alquiler si corresponde).

    @Override

    public ResultadoJugada tirarYMover(String codigo, Long idJugador) // :|

            throws PartidaNoEncontradaException, JugadorNoEncontradoException, ActivacionEventoInvalidoException, EventoGlobalInvalidoException, EventoGlobalActivoException, EventoInexistenteException {

        Partida partida = partidas.obtenerPartida(codigo);

        if (partida == null) throw new PartidaNoEncontradaException();

        //busca jugador y valida que el jugador cumpla con las condiciones.

        Jugador jugador = obtenerJugador(partida, idJugador);

        validarTirada(codigo, jugador);

        //guarda la posicion anterior del jugador.

        Integer anterior = jugador.getPosicionActual();

        jugador.setTiroEnTurno(true); //marca que el juagador ya tiró sus dados en su turno.

        //Llama al ServicioDado y devuelve un objeto ResultadoTirada, que representa el resultado obtenido.

        ResultadoTirada tirada = dados.tirarDados();

        //Obtiene la suma de ambos dados y le solicita a ServicioMovimiento que avance al jugador.

        Integer premio = movimiento.avanzar(jugador, tirada.sumarDados());

        Integer caida = jugador.getPosicionActual();// guarda la nueva posicion

        //Revisa si corresponde aplicar algún evento al jugador.

        ResultadoEvento evento = aplicarEvento(codigo, jugador);

        //Si hubo un evento, suma el dinero ganado por pasar por Inicio.

        if (evento != null) {

            premio = premio + evento.dineroPorInicio();

        }

        partidas.actualizarJugador(jugador);//actualiza el estado del juagdor.

        //Revisa donde termino el jugador despues de tirar y comprueba si la jugada puede terminar y pasar al siguiente jugador.

        Destino destino = resolverDestino(partida, jugador);

        avanzarTurnoSiCorresponde(codigo, jugador, destino.requiereDecision());

        //verifican que la partida este actualizada.

        Partida actualizada = partidas.obtenerPartida(codigo);

        Partida paraSaldos;

        if (actualizada == null) {

            paraSaldos = partida;

        } else {

            paraSaldos = actualizada;

        }

        //Prepara la información del evento para devolverla al frontend.

        ResultadoJugada.EventoAplicado eventoAplicado = null;

        if (evento != null && evento.evento() != null) {

            EventoCasillero eventoCasillero = evento.evento();

            eventoAplicado = new ResultadoJugada.EventoAplicado(

                    eventoCasillero.getNombre(),

                    eventoCasillero.getDescripcion(),

                    eventoCasillero.getTipoEfecto().name());

        }

        //Devuelve el resultado completo de la jugada.

        return new ResultadoJugada(idJugador, anterior, jugador.getPosicionActual(), caida, tirada, eventoAplicado, premio,

                //obtiene la lista de jugadores y la recorre para hacer una operacion con c/uno (.stream)

                paraSaldos.getJugadores().stream()

                        //Ese metodo convierte un objeto Jugador en un objeto SaldoJugador

                        .map(SaldoJugador::desdeJugador)

                        .toList(),//crea la lista

                destino.alquiler());

    }

    //busca al jugador por id.

    private Jugador obtenerJugador(Partida partida, Long idJugador)

            throws JugadorNoEncontradoException {

        return partida.getJugadores().stream()

                .filter(j -> Objects.equals(j.getId(), idJugador))

                .findFirst()

                .orElseThrow(() -> new JugadorNoEncontradoException(

                        "No se encontró el jugador en esta partida"));

    }

    //valida que el jugador pueda tirar los dados en su turno.

    private void validarTirada(String codigo, Jugador jugador) {

        validarQueNoHayaEventoGlobalActivo(codigo);

        Jugador actual = partidas.obtenerJugadorActual(codigo);

        //verifica que el jugador este activo y que sea su turno

        if (!participacion.estaActivo(jugador) || actual == null || !Objects.equals(actual.getId(), jugador.getId())) {

            throw new TiradaNoPermitidaException( "No podés tirar los dados en este turno.");

        }

        //verifica que el jugador no haya tirado los dados en ese turno

        if (Boolean.TRUE.equals(jugador.getTiroEnTurno())) {

            throw new TiradaNoPermitidaException(  "Ya tiraste los dados en este turno.");

        }

        //si tiene deuda pendiente no puede volver a tirar hasta pagarla.

        if (quiebras.obtenerDeudaPendiente(codigo, jugador.getId()) != null) {

            throw new TiradaNoPermitidaException( "Tenés una deuda pendiente. Debés resolverla antes de volver a tirar los dados.");

        }

    }

    private void validarQueNoHayaEventoGlobalActivo(String codigo) {

        if (Boolean.TRUE.equals(partidas.verificarSiHayEventoGlobalActivo(codigo))) {

            throw new TiradaNoPermitidaException("La partida está pausada por un evento global.");

        }

    }

    //revisa si corresponde aplicar un evento al jugador y devuelve el rtdo del evento o null si no hubo.

    private ResultadoEvento aplicarEvento(String codigo, Jugador jugador) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {

        if (eventos == null) {

            return null; }

        return eventos.aplicarSiCorresponde(codigo, jugador).orElse(null);

    }

    //revisa en que casillero terminó el jugador y resuelve lo que corresponda

    private Destino resolverDestino(Partida partida, Jugador jugador) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {

        //si el jugador no esta activo o pierde el turno, no procesa la propiedad

        if (!participacion.estaActivo(jugador) || Boolean.TRUE.equals(jugador.getPierdeTurno()))

            return new Destino(false, null);

        //busca el casillero de propiedad donde terminó el jugador

        CasilleroPropiedad casillero = casilleros.buscarOCrearPropiedadEnPosicion(partida, jugador.getPosicionActual());

        //si no es un casillero de propiedad no hay nada que resolver

        if (casillero == null) return new Destino(false, null);

        Propiedad propiedad = casillero.getPropiedad();

        //si la propiedad es disponible, espera a que el jugador quiera comprarla

        if (propiedades.estaDisponible(propiedad))

            return new Destino(true, null);

        Jugador propietario = propiedad.getPropietario();

        //si no tiene propietario o es del mismo jugador, no paga alquiler

        if (propietario == null || Objects.equals(propietario.getId(), jugador.getId()))

            return new Destino(false, null);

        //si es de otro jugador, calcula el alquiler y procesa el pago

        Integer alquiler = propiedades.calcularAlquiler(casillero, propiedad);

        ResultadoDeuda deuda = quiebras.procesarAlquiler(partida.getCodigoUnico(), propiedad, jugador, alquiler);

        //devuelve si quedó una deuda pendiente y los datos del alquiler

        return new Destino(deuda == ResultadoDeuda.PENDIENTE, new AlquilerJugada(jugador.getId(), jugador.getApodo(), propietario.getApodo(), propiedad.getNombre(), alquiler));

    }

    private void avanzarTurnoSiCorresponde(String codigo, Jugador jugador, boolean requiereDecision) throws ActivacionEventoInvalidoException, PartidaNoEncontradaException, EventoGlobalInvalidoException, JugadorNoEncontradoException, EventoGlobalActivoException, EventoInexistenteException {

        if (requiereDecision || !participacion.estaActivo(jugador)) return;

        if (quiebras.obtenerDeudaPendiente(codigo, jugador.getId()) != null) return;

        partidas.avanzarTurno(codigo);

    }

    //Record: crea clases pequeñas para guardar datos

    //indica si existe algo q el jugador debe resolver y guarda la info. del alquiler

    private record Destino(Boolean requiereDecision, AlquilerJugada alquiler) {

    }

}
