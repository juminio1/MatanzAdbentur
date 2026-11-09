package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;
import java.time.Clock;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioIntercambioPropiedadImpl implements ServicioIntercambioPropiedad {
    private final ServicioPartida partidas;
    private final ServicioCasillero casilleros;
    private final RepositorioPropiedad propiedades;
    private final ServicioParticipacion participacion;
    private final Clock reloj;
    private final Map<String, Propuesta> propuestas = new ConcurrentHashMap<>();
    private final Map<String, Propuesta> reservas = new ConcurrentHashMap<>();
    private static final Duration VIGENCIA = Duration.ofMinutes(2);

    @Autowired
    public ServicioIntercambioPropiedadImpl(ServicioPartida partidas, ServicioCasillero casilleros,
            RepositorioPropiedad propiedades, ServicioParticipacion participacion) {
        this(partidas, casilleros, propiedades, participacion, Clock.systemUTC());
    }

    public ServicioIntercambioPropiedadImpl(ServicioPartida partidas, ServicioCasillero casilleros,
            RepositorioPropiedad propiedades, ServicioParticipacion participacion, Clock reloj) {
        this.partidas = partidas;
        this.casilleros = casilleros;
        this.propiedades = propiedades;
        this.participacion = participacion;
        this.reloj = reloj;
    }

    @Override
    public Propuesta proponer(String codigo, Long actor, Long destinatario, Integer ofrecida, Integer solicitada) {
        Partida partida = cargar(codigo);
        Jugador proponente = participante(partida, actor);
        participante(partida, destinatario);
        if (Objects.equals(actor, destinatario)) throw new IllegalArgumentException("Elegí otro jugador para intercambiar.");
        CasilleroPropiedad propia = propiedadDe(partida, ofrecida, actor);
        CasilleroPropiedad ajena = solicitada == null ? null : propiedadDe(partida, solicitada, destinatario);
        Propuesta propuesta = new Propuesta(UUID.randomUUID().toString(), codigo, actor, destinatario,
            ofrecida, solicitada, proponente.getApodo(), propia.getNombre(),
            ajena == null ? null : ajena.getNombre(), reloj.instant().plus(VIGENCIA), solicitada == null);
        reservar(partida, propuesta);
        DespuesDeConfirmar.ejecutar(() -> propuestas.put(propuesta.id(), propuesta));
        return propuesta;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Propuesta> pendientes(String codigo, Long actor) {
        Partida partida = partidas.obtenerPartida(codigo);
        participacion.validarPartidaEnCurso(partida);
        participante(partida, actor);
        limpiarVencidas();
        return propuestas.values().stream().filter(p -> p.codigoPartida().equals(codigo)
                && p.idJugadorQueResponde().equals(actor))
            .filter(p -> participantesActivos(partida, p))
            .sorted(Comparator.comparing(Propuesta::vence)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Propuesta> enviadas(String codigo, Long actor) {
        Partida partida = partidas.obtenerPartida(codigo);
        participacion.validarPartidaEnCurso(partida);
        participante(partida, actor);
        limpiarVencidas();
        return propuestas.values().stream().filter(p -> p.codigoPartida().equals(codigo)
                && p.idProponente().equals(actor) && participantesActivos(partida, p))
            .sorted(Comparator.comparing(Propuesta::vence)).toList();
    }

    private String clave(Propuesta propuesta) {
        return propuesta.codigoPartida() + ":" + propuesta.posicionOfrecida();
    }

    private void reservar(Partida partida, Propuesta propuesta) {
        String clave = clave(propuesta);
        reservas.compute(clave, (llave, anterior) -> {
            if (anterior != null && anterior.vence().isAfter(reloj.instant())
                    && anterior.idProponente().equals(propuesta.idProponente())
                    && participantesActivos(partida, anterior)) {
                throw new IllegalStateException("Esta propiedad ya tiene una propuesta de intercambio pendiente.");
            }
            return propuesta;
        });
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()
                && org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int estado) {
                        if (estado != STATUS_COMMITTED) reservas.remove(clave, propuesta);
                    }
                });
        }
    }

    private void retirar(Propuesta propuesta) {
        propuestas.remove(propuesta.id(), propuesta);
        reservas.remove(clave(propuesta), propuesta);
    }

    private void limpiarVencidas() {
        propuestas.values().stream().filter(p -> !p.vence().isAfter(reloj.instant())).toList().forEach(this::retirar);
    }

    @Override
    public Propuesta elegirContrapropiedad(String codigo, String id, Long actor, Integer posicion) {
        Partida partida = cargar(codigo);
        Propuesta inicial = vigente(codigo, id, actor);
        if (!inicial.requiereConfirmacion() || inicial.esperaConfirmacionFinal()) {
            throw new IllegalStateException("La propuesta ya tiene una contrapropiedad.");
        }
        participante(partida, actor);
        participante(partida, inicial.idProponente());
        propiedadDe(partida, inicial.posicionOfrecida(), inicial.idProponente());
        CasilleroPropiedad elegida = propiedadDe(partida, posicion, actor);
        Propuesta respondida = new Propuesta(id, codigo, inicial.idProponente(), actor,
            inicial.posicionOfrecida(), posicion, inicial.apodoProponente(), inicial.nombreOfrecida(),
            elegida.getNombre(), reloj.instant().plus(VIGENCIA), true);
        DespuesDeConfirmar.ejecutar(() -> {
            propuestas.replace(id, inicial, respondida);
            reservas.replace(clave(inicial), inicial, respondida);
        });
        return respondida;
    }

    @Override
    public Resultado aceptar(String codigo, String id, Long actor) {
        Partida partida = cargar(codigo);
        Propuesta propuesta = vigente(codigo, id, actor);
        Jugador proponente = participante(partida, propuesta.idProponente());
        Jugador destinatario = participante(partida, propuesta.idDestinatario());
        CasilleroPropiedad ofrecida = propiedadDe(partida, propuesta.posicionOfrecida(), proponente.getId());
        CasilleroPropiedad solicitada = propiedadDe(partida, propuesta.posicionSolicitada(), destinatario.getId());
        ofrecida.getPropiedad().setPropietario(destinatario);
        solicitada.getPropiedad().setPropietario(proponente);
        propiedades.guardarPropiedad(ofrecida.getPropiedad());
        propiedades.guardarPropiedad(solicitada.getPropiedad());
        DespuesDeConfirmar.ejecutar(() -> retirar(propuesta));
        return new Resultado(propuesta, ofrecida, solicitada);
    }

    @Override
    public Propuesta rechazar(String codigo, String id, Long actor) {
        Partida partida = cargar(codigo);
        participante(partida, actor);
        Propuesta propuesta = vigente(codigo, id, actor);
        DespuesDeConfirmar.ejecutar(() -> retirar(propuesta));
        return propuesta;
    }

    private Partida cargar(String codigo) {
        Partida partida = partidas.obtenerPartida(codigo);
        participacion.validarPartidaEnCurso(partida);
        partidas.bloquearPartida(partida);
        participacion.validarPartidaEnCurso(partida);
        return partida;
    }

    private Jugador participante(Partida partida, Long id) {
        return partida.getJugadores().stream()
            .filter(j -> Objects.equals(j.getId(), id) && participacion.estaActivo(j))
            .findFirst().orElseThrow(() -> new IllegalStateException("Solo participan jugadores activos de esta partida."));
    }

    private boolean participantesActivos(Partida partida, Propuesta propuesta) {
        return partida.getJugadores().stream().filter(participacion::estaActivo)
            .filter(j -> Objects.equals(j.getId(), propuesta.idProponente())
                || Objects.equals(j.getId(), propuesta.idDestinatario())).count() == 2;
    }

    private CasilleroPropiedad propiedadDe(Partida partida, Integer posicion, Long propietario) {
        if (posicion == null) throw new IllegalStateException("Elegí una propiedad para intercambiar.");
        CasilleroPropiedad casillero = casilleros.buscarCasilleroPropiedad(partida, posicion);
        if (casillero == null || casillero.getPropiedad() == null
                || casillero.getPropiedad().getPropietario() == null
                || !Objects.equals(casillero.getPropiedad().getPropietario().getId(), propietario)) {
            throw new IllegalStateException("Una de las propiedades ya no pertenece al jugador indicado.");
        }
        return casillero;
    }

    private Propuesta vigente(String codigo, String id, Long actor) {
        Propuesta propuesta = id == null ? null : propuestas.get(id);
        if (propuesta == null || !propuesta.codigoPartida().equals(codigo)
                || !propuesta.idJugadorQueResponde().equals(actor) || !propuesta.vence().isAfter(reloj.instant())) {
            throw new IllegalStateException("El intercambio ya no está disponible para este jugador.");
        }
        return propuesta;
    }
}
