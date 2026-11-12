package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.EstadoEventoGlobal;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.Evento.EventoCasillero;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;
import com.tallerwebi.dominio.resultados.ResultadoTirada;
import com.tallerwebi.dominio.resultados.ResultadoJugada.EventoAplicado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioDado;
import com.tallerwebi.dominio.servicios.interfaces.ServicioEvento;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.infraestructura.RepositorioEventoGlobal;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.temporizadores.GestorTemporizadorPartida;
import com.tallerwebi.infraestructura.transacciones.DespuesDeConfirmar;
import com.tallerwebi.presentacion.WebSocket.NotificadorPartida;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Serializa cada transición con el bloqueo de la partida, hasta confirmar la transacción. */
@Service
@Transactional
public class ServicioEventoImpl implements ServicioEvento {

    private static final Integer RONDAS_ENTRE_EVENTOS = 5;

    private static final Integer UNICO_PARTICIPANTE = 1;
    private final RepositorioEventoGlobal catalogo;
    private final RepositorioPartida repositorio;
    private final ServicioParticipacion participacion;
    private final ServicioDado dados;
    private final AplicadorConsecuenciaEventoGlobal aplicador;
    private final ServicioPartida partidas;
    private final GestorTemporizadorPartida temporizador;
    private final NotificadorPartida notificador;
    private RandomGenerator aleatorio = new Random();

    @Autowired
    public ServicioEventoImpl(
        RepositorioEventoGlobal catalogo,
        RepositorioPartida repositorio,
        ServicioParticipacion participacion,
        ServicioDado dados,
        AplicadorConsecuenciaEventoGlobal aplicador,
        @Lazy ServicioPartida partidas,
        GestorTemporizadorPartida temporizador,
        NotificadorPartida notificador
    ) {
        this.catalogo = catalogo;
        this.repositorio = repositorio;
        this.participacion = participacion;
        this.dados = dados;
        this.aplicador = aplicador;
        this.partidas = partidas;
        this.temporizador = temporizador;
        this.notificador = notificador;
    }

    // Permite controlar las elecciones en pruebas sin endpoints ni probabilidades especiales.
    void usarAleatorio(RandomGenerator generador) {
        aleatorio = Objects.requireNonNull(generador);
    }

    @Override
    public Boolean activarSiCorresponde(Partida partida) {
        repositorio.bloquearPartida(partida);
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        int ronda = partida.getRondaActual();
        if (
            partida.getEstado() != EstadoPartida.EN_CURSO ||
            estado.estaActivo() ||
            ronda % RONDAS_ENTRE_EVENTOS != 0 ||
            estado.getUltimaRonda() >= ronda
        ) return estado.estaActivo();
        List<com.tallerwebi.dominio.entidades.Evento.EventoGlobal> eventos = catalogo.obtenerTodos();
        if (eventos.size() != TipoEventoGlobal.values().length) {
            throw new IllegalStateException("El catálogo debe contener los seis eventos globales.");
        }
        Set<Long> activos = activos(partida);
        if (activos.size() < 2 || aplicador.hayDeudas(partida)) return false;
        estado.iniciar(eventos.get(aleatorio.nextInt(eventos.size())), ronda, activos);
        DespuesDeConfirmar.ejecutar(() ->
            temporizador.cancelarTemporizador(partida.getCodigoUnico())
        );
        if (!estado.getEvento().getTipoEventoGlobal().requiereDados()) {
            List<Jugador> candidatos = partida
                .getJugadores()
                .stream()
                .filter(participacion::estaActivo)
                .sorted(Comparator.comparing(Jugador::getOrden))
                .toList();
            resolver(partida, candidatos.get(aleatorio.nextInt(candidatos.size())).getId());
        }
        publicarYProgramar(partida);
        return true;
    }

    @Override
    public Map<String, Object> consultar(String codigo, Long jugador) {
        Partida partida = cargar(codigo);
        validarPertenencia(partida, jugador);
        participantesCambiaron(partida);
        if (partida.getEventoGlobal().estaActivo()) programar(partida);
        return vista(partida);
    }

    @Override
    public Map<String, Object> tirar(String codigo, Long jugador, Integer ronda, Integer etapa) {
        Partida partida = cargar(codigo);
        validarActivo(partida, jugador);
        participantesCambiaron(partida);
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        if (
            !estado.estaActivo() ||
            estado.estaResuelto() ||
            estado.getUltimaRonda() != ronda ||
            estado.getEtapa() != etapa ||
            !estado.getHabilitados().contains(jugador) ||
            estado.tirada(jugador) != null ||
            !Instant.now().isBefore(estado.getVencimiento())
        ) {
            throw new IllegalStateException("No podés tirar en esta etapa del evento.");
        }
        registrarTirada(estado, jugador);
        resolverEtapa(partida);
        publicarYProgramar(partida);
        return vista(partida);
    }

    @Override
    public Map<String, Object> confirmar(String codigo, Long jugador, Integer ronda) {
        Partida partida = cargar(codigo);
        validarActivo(partida, jugador);
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        if (!estado.estaActivo() || estado.getUltimaRonda() != ronda) 
            return vista(partida);
        if (!estado.estaResuelto()) 
            throw new IllegalStateException(
            "El evento todavía espera tiradas."
        );
        estado.confirmar(jugador);
        intentarCerrar(partida);
        publicarYProgramar(partida);
        return vista(partida);
    }

    @Override
    public void participantesCambiaron(Partida partida) {
        repositorio.bloquearPartida(partida);
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        if (!estado.estaActivo()) return;
        long revision = estado.getRevision();
        Set<Long> activos = activos(partida);
        estado.excluirInactivos(activos);
        if (partida.getEstado() != EstadoPartida.EN_CURSO || activos.size() < 2) {
            cerrar(partida);
        } else if (estado.estaResuelto()) {
            intentarCerrar(partida);
        } else {
            resolverEtapa(partida);
        }
        if (estado.getRevision() != revision) publicarYProgramar(partida);
    }

    private void registrarTirada(EstadoEventoGlobal estado, Long jugador) {
        ResultadoTirada tirada = dados.tirarDados();
        estado.registrar(jugador, tirada.getDado1(), tirada.getDado2());
    }

    private void resolverEtapa(Partida partida) {
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        Set<Long> habilitados = estado.getHabilitados();
        if (habilitados.isEmpty()) {
            cerrar(partida);
            return;
        }
        if (habilitados.size() == UNICO_PARTICIPANTE) {
            resolver(partida, habilitados.iterator().next());
            return;
        }
        if (habilitados.stream().anyMatch(jugador -> estado.tirada(jugador) == null))
             return;
        Boolean menor = estado.getEvento().getTipoEventoGlobal() == TipoEventoGlobal.PIQUETE;
        Integer extremo = habilitados.stream().mapToInt(jugador -> suma(estado.tirada(jugador))).reduce(menor ? Math::min : Math::max).orElseThrow();
        Set<Long> empatados = habilitados.stream().filter(jugador -> suma(estado.tirada(jugador)) == extremo).collect(Collectors.toSet());

        if (empatados.size() == UNICO_PARTICIPANTE) resolver(partida, empatados.iterator().next());
        else estado.desempatar(empatados);
    }

    private int suma(int tirada) {
        return tirada / 10 + (tirada % 10);
    }

    private void resolver(Partida partida, Long seleccionado) {
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        if (estado.estaResuelto()) return;
        Jugador jugador = partida
            .getJugadores()
            .stream()
            .filter(candidato -> seleccionado.equals(candidato.getId()))
            .findFirst()
            .orElseThrow();
        Boolean resuelveDestino = estado.getEvento().getTipoEventoGlobal() == TipoEventoGlobal.INUNDACION
            || estado.getEvento().getTipoEventoGlobal() == TipoEventoGlobal.TRAMITES_AFIP;
        // La caída puede notificar una quiebra y volver a consultar este mismo evento.
        // Se reserva su resolución dentro de la transacción antes de esos callbacks.
        if (resuelveDestino) estado.resolver(seleccionado, "");
        String consecuencia = aplicador.aplicar(partida, estado.getEvento(), jugador);
        // Una consulta de los servicios económicos puede materializar de nuevo el componente
        // embebido. Se modifica el estado que conserva la entidad administrada por Hibernate.
        if (resuelveDestino) partida.getEventoGlobal().actualizarConsecuencia(consecuencia);
        else partida.getEventoGlobal().resolver(seleccionado, consecuencia);
    }

    private void intentarCerrar(Partida partida) {
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        Boolean vencido = !Instant.now().isBefore(estado.getVencimiento());
        if (
            (vencido || estado.getConfirmados().containsAll(activos(partida))) &&
            !aplicador.hayDeudas(partida)
        ) cerrar(partida);
    }

    
    private void cerrar(Partida partida) {
        partida.getEventoGlobal().cerrar();

        long revision = partida.getEventoGlobal().getRevision();

        DespuesDeConfirmar.ejecutar(() -> temporizador.cancelarTemporizadorEvento(clave(partida), revision));

        if (partida.getEstado() == EstadoPartida.EN_CURSO) {
            String codigoPartida = partida.getCodigoUnico();

            // Reanudar después de confirmar la transacción,
            // para que la penalización ya esté persistida.
            DespuesDeConfirmar.ejecutar(() ->
                partidas.reanudarDespuesDeEvento(codigoPartida)
            );
        }
    }


    private Partida cargar(String codigo) {
        Partida partida = repositorio.buscarPartidaPorCodigoUnico(codigo);
        if (partida == null) throw new IllegalArgumentException("La partida no existe.");
        repositorio.bloquearPartida(partida);
        return partida;
    }

    private void validarPertenencia(Partida partida, Long jugador) {
        if (!participacion.pertenece(partida, jugador)) throw new SecurityException(
            "No pertenecés a esta partida."
        );
    }

    private void validarActivo(Partida partida, Long jugador) {
        validarPertenencia(partida, jugador);
        participacion.validarPartidaEnCurso(partida);
        if (!activos(partida).contains(jugador)) throw new SecurityException(
            "Ya no participás del evento."
        );
    }

    private Set<Long> activos(Partida partida) {
        return partida.getJugadores().stream().filter(participacion::estaActivo).map(Jugador::getId).collect(Collectors.toSet());
    }

    private String clave(Partida partida) {
        return "evento:" + partida.getCodigoUnico();
    }

    private void publicarYProgramar(Partida partida) {
        Map<String, Object> respuesta = vista(partida);
        DespuesDeConfirmar.ejecutar(() ->
            notificador.notificarEventoGlobal(partida.getCodigoUnico(), respuesta)
        );
        programar(partida);
    }

    private void programar(Partida partida) {
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        if (!estado.estaActivo()) return;
        if (
            estado.estaResuelto() &&
            aplicador.hayDeudas(partida) &&
            !Instant.now().isBefore(estado.getVencimiento())
        ) return;
        Instant vencimiento = estado.getVencimiento();
        Integer segundos = (int) Math.max(
            0,
            (vencimiento.toEpochMilli() - System.currentTimeMillis() + 999) / 1000
        );
        Integer ronda = estado.getUltimaRonda();
        Integer etapa = estado.getEtapa();
        long revision = estado.getRevision();
        DespuesDeConfirmar.ejecutar(() ->
            temporizador.programarTemporizadorEvento(clave(partida), revision, segundos, () ->
                vencer(partida.getCodigoUnico(), ronda, etapa, vencimiento)
            )
        );
    }

    private void vencer(String codigo, int ronda, int etapa, Instant vencimiento) {
        Partida partida = cargar(codigo);
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        if (
            !estado.estaActivo() ||
            estado.getUltimaRonda() != ronda ||
            estado.getEtapa() != etapa ||
            !Objects.equals(estado.getVencimiento(), vencimiento)
        ) return;
        participantesCambiaron(partida);
        if (!estado.estaActivo()) return;
        if (estado.estaResuelto()) intentarCerrar(partida);
        else {
            estado
                .getHabilitados()
                .stream()
                .sorted()
                .filter(jugador -> estado.tirada(jugador) == null)
                .forEach(jugador -> registrarTirada(estado, jugador));
            resolverEtapa(partida);
        }
        publicarYProgramar(partida);
    }

    private Map<String, Object> vista(Partida partida) {
        EstadoEventoGlobal estado = partida.getEventoGlobal();
        Map<String, Object> vista = new HashMap<>();
        vista.put("activo", estado.estaActivo());
        vista.put("revision", estado.getRevision());
        vista.put("ronda", estado.getUltimaRonda());
        vista.put("etapa", estado.getEtapa());
        vista.put("resuelto", estado.estaResuelto());
        vista.put(
            "requiereDados",
            estado.estaActivo() && estado.getEvento().getTipoEventoGlobal().requiereDados()
        );
        vista.put("rondaActual", partida.getRondaActual());
        vista.put("turnosFinalizados", partida.getTurnosFinalizadosEnRonda());
        vista.put(
            "idJugadorActual",
            partida.getJugadores().stream().filter(jugador ->
                    Objects.equals(jugador.getOrden(), partida.getIndiceTurnoActual())).filter(participacion::estaActivo).map(Jugador::getId).findFirst().orElse(null)
        );
        vista.put("vence", estado.getVencimiento() == null ? null : estado.getVencimiento().toEpochMilli());
        vista.put("nombre", estado.getEvento() != null ? estado.getEvento().getNombre() : null);
        vista.put("descripcion", estado.getEvento() != null ? estado.getEvento().getDescripcion() : null);
        vista.put("seleccionado", estado.getSeleccionado());
        vista.put("consecuencia", estado.getConsecuencia());
        vista.put("tarjeta", tarjetaAplicada(estado));
        vista.put("compraPendiente", estado.getPosicionCompra());
        vista.put("habilitados", estado.getHabilitados());
        vista.put("confirmados", estado.getConfirmados());
        vista.put("tiradas", estado.getTiradas());
        vista.put("deudaPendiente", estado.estaActivo() && aplicador.hayDeudas(partida));
        vista.put(
            "jugadores",
            partida.getJugadores().stream().map(jugador ->
                    Map.of(
                        "id", jugador.getId(), "apodo", jugador.getApodo(), "dinero", jugador.getDinero(), 
                        "posicion", jugador.getPosicionActual(), "activo", participacion.estaActivo(jugador),
                        "encarcelado", Boolean.TRUE.equals(jugador.getEstadoCarcel().getEncarcelado()),
                        "intentosCarcel", Objects.requireNonNullElse(jugador.getEstadoCarcel().getIntentosSalirCarcel(), 0))).toList());
        return vista;
    }

    private EventoAplicado tarjetaAplicada(EstadoEventoGlobal estado) {
        EventoCasillero tarjeta = estado.getTarjeta();
        return tarjeta == null ? null : new EventoAplicado(
            tarjeta.getNombre(), tarjeta.getDescripcion(), tarjeta.getTipoEfecto().name());
    }
}
