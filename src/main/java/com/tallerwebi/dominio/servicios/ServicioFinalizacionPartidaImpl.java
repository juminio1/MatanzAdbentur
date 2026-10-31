package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.DeudaPendiente;
import com.tallerwebi.dominio.entidades.Partida;
import com.tallerwebi.dominio.entidades.FinalizarPartida;
import com.tallerwebi.dominio.enums.EstadoParticipante;
import com.tallerwebi.dominio.enums.EstadoPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioParticipacion;
import com.tallerwebi.infraestructura.RepositorioFinalizarPartida;
import com.tallerwebi.infraestructura.RepositorioPartida;
import com.tallerwebi.infraestructura.RepositorioPropiedad;
import com.tallerwebi.infraestructura.RepositorioDeudaPendiente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import com.tallerwebi.dominio.servicios.interfaces.ServicioFinalizacionPartida;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ServicioFinalizacionPartidaImpl implements ServicioFinalizacionPartida {
    private static final int MAXIMO_RONDAS = 30;
    private static final long UNICO_JUGADOR_ACTIVO = 1L;

    @Override
    public boolean cumpleCondicionFinalizacion(Partida partida) {
        if (partida == null || partida.getEstado() != EstadoPartida.EN_CURSO) {
            return false;
        }
        long activos = partida.getJugadores().stream()
                .filter(participacion::estaActivo)
                .count();
        if (activos == 0) {
            return false;
        }
        return activos == UNICO_JUGADOR_ACTIVO
                || partida.getRondaActual() != null && partida.getRondaActual() > MAXIMO_RONDAS;
    }

    private final RepositorioPropiedad repositorioPropiedad;
    private final RepositorioDeudaPendiente repositorioDeudaPendiente;
    private final RepositorioFinalizarPartida repositorioFinalizarPartida;
    private final ServicioParticipacion participacion;
    private final RepositorioPartida repositorioPartida;

    public ServicioFinalizacionPartidaImpl() {
        this(null, null);
    }

    public ServicioFinalizacionPartidaImpl(RepositorioPropiedad repositorioPropiedad,
                                          RepositorioDeudaPendiente repositorioDeudaPendiente) {
        this(repositorioPropiedad, repositorioDeudaPendiente, null, null);
    }

    public ServicioFinalizacionPartidaImpl(RepositorioPropiedad repositorioPropiedad,
                                          RepositorioDeudaPendiente repositorioDeudaPendiente,
                                          RepositorioFinalizarPartida repositorioFinalizarPartida,
                                          ServicioParticipacion participacion) {
        this(repositorioPropiedad, repositorioDeudaPendiente, repositorioFinalizarPartida,
                participacion, null);
    }

    @Autowired
    public ServicioFinalizacionPartidaImpl(RepositorioPropiedad repositorioPropiedad,
                                          RepositorioDeudaPendiente repositorioDeudaPendiente,
                                          RepositorioFinalizarPartida repositorioFinalizarPartida,
                                          ServicioParticipacion participacion,
                                          RepositorioPartida repositorioPartida) {
        this.repositorioPropiedad = repositorioPropiedad;
        this.repositorioDeudaPendiente = repositorioDeudaPendiente;
        this.repositorioFinalizarPartida = repositorioFinalizarPartida;
        this.participacion = participacion;
        this.repositorioPartida = repositorioPartida;
    }

    @Override
    @Transactional
    public boolean finalizarSiCorresponde(Partida partida) {
        if (!cumpleCondicionFinalizacion(partida)) {
            return false;
        }
        verificarResultadosDeEliminados(partida);
        for (Jugador jugador : partida.getJugadores()) {
            if (participacion.estaActivo(jugador)) {
                conservarResultadoEconomico(partida, jugador, EstadoParticipante.ACTIVO);
            }
        }
        determinarGanadores(partida);
        partida.setEstado(EstadoPartida.FINALIZADA);
        repositorioPartida.guardarPartida(partida);
        return true;
    }

    private void verificarResultadosDeEliminados(Partida partida) {
        for (Jugador jugador : partida.getJugadores()) {
            if (!participacion.estaActivo(jugador)
                    && repositorioFinalizarPartida.buscarPorPartidaYJugador(
                            partida.getId(), jugador.getId()) == null) {
                throw new IllegalStateException("Falta el resultado economico de un jugador eliminado.");
            }
        }
    }

    @Override
    @Transactional
    //devuelve una lista de rtdo ganadores. Se decidio por una lista porque puede haber empates.
    public List<FinalizarPartida> determinarGanadores(Partida partida) {
       //obtiene todos los rtdos economicos de esa partida.
        List<FinalizarPartida> resultados = repositorioFinalizarPartida.buscarPorPartida(partida.getId());
        List<FinalizarPartida> activos = new ArrayList<>();//lista donde se guardan los rtdos de aquellos jugadores que pueden ganar.
        int patrimonioMaximo = Integer.MIN_VALUE;
        //1° for: encontrar el patrimonio máximo entre los jugadores activos.
        for (FinalizarPartida resultado : resultados) {
            if (resultado.getEstadoParticipante() == EstadoParticipante.ACTIVO
                    && participacion.estaActivo(resultado.getJugador())) {
                //agrega ese rtdo a la lista de participantes activos.
                activos.add(resultado);
                //compara los dos patrimonios y devuelve el mayor.
                patrimonioMaximo = Math.max(patrimonioMaximo, resultado.getPatrimonioTotal());
            }
        }
        List<FinalizarPartida> ganadores = new ArrayList<>();
        //2° for: compara a todos los jugadores contra ese máximo y marca a quienes ganaron.
        for (FinalizarPartida resultado : resultados) {
            //Encuentra el ganador, si esta activo y si su patrimonio es el maximo.
            boolean ganador = activos.contains(resultado)
                    && resultado.getPatrimonioTotal() == patrimonioMaximo;
            resultado.setGanador(ganador); //actualiza el ganador
            //Si ganó, lo agrega a la lista q despues se devuelve.
            if (ganador) {
                ganadores.add(resultado);
            }
        }
        return ganadores;
    }

    @Override
    @Transactional
    //guardar el resultado económico de un jugador antes de que abandone o quiebre
    public FinalizarPartida conservarResultadoEconomico(
            Partida partida, Jugador jugador, EstadoParticipante estado) {
        //verifica que el jugador pertenezca a la partida
        if (jugador == null || !participacion.pertenece(partida, jugador.getId())) {
            throw new IllegalArgumentException("El jugador no pertenece a la partida.");
        }
        //busca en la base de datos si ese jugador ya tiene un resultado económico guardado para esa partida.
        FinalizarPartida previo = repositorioFinalizarPartida
                .buscarPorPartidaYJugador(partida.getId(), jugador.getId());
        if (previo != null) {
            return previo;
        }
        //Calcula el valor de todas las propiedades
        Integer valorPropiedades = 0;
        for (Propiedad propiedad : repositorioPropiedad.buscarPropiedadesPorPropietario(jugador)) {
            valorPropiedades += calcularValorPatrimonial(propiedad);
        }
        DeudaPendiente deuda = repositorioDeudaPendiente
                .buscarPorPartidaYJugador(partida.getCodigoUnico(), jugador.getId());
        Integer montoDeuda = deuda == null ? 0 : deuda.getMontoPendiente();
        //Se crea una nueva entidad, la cual se completa con los datos de la partida.
        FinalizarPartida resultado = new FinalizarPartida();
        resultado.setPartida(partida);
        resultado.setJugador(jugador);
        resultado.setDineroFinal(jugador.getDinero());
        resultado.setValorPropiedades(valorPropiedades);
        resultado.setDeudaPendiente(montoDeuda);
        //guarda una copia de los importes economicos del jugador en ese momento
        resultado.setPatrimonioTotal(jugador.getDinero() + valorPropiedades - montoDeuda);
        resultado.setGanador(false);
        resultado.setEstadoParticipante(estado);
        //guarda y devuelve el rtdo.
        repositorioFinalizarPartida.guardar(resultado);
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer calcularPatrimonioTotal(String codigoPartida, Jugador jugador) {
        Integer patrimonio = jugador.getDinero();//obtiene el dinero del jugador
        //Suma el valor de casa una de sus propiedades.
        for (Propiedad propiedad : repositorioPropiedad.buscarPropiedadesPorPropietario(jugador)) {
            //por c/propiedad calcula su valor patrimonial(Suma el valor de la propiedad + su mejora)
            patrimonio += calcularValorPatrimonial(propiedad);
        }

        //busca si el jugador se quedo con deuda.
        DeudaPendiente deuda = repositorioDeudaPendiente
                .buscarPorPartidaYJugador(codigoPartida, jugador.getId());
        //si encuentra la deuda, se la descuenta al patrimonio total
        if (deuda != null) {
            patrimonio -= deuda.getMontoPendiente();
        }
        return patrimonio;
    }

    @Override
    public Integer calcularValorPatrimonial(Propiedad propiedad) {
        //guarda los costos de cada mejora. Cada posicion representa una mejora sucesiva.
        final int[] costosMejoras = {200, 300, 500, 1000, 1500};

        int nivelMejora;
        // si el nivel de mejora es null le asigna 0
        if (propiedad.getNivelMejora() == null) {
            nivelMejora = 0;
        } else {
            nivelMejora = propiedad.getNivelMejora();
        }
        //comprueba que el nivel de mejora este entre 0 y 5, sino lanza excepcion.
        if (nivelMejora < 0 || nivelMejora > costosMejoras.length) {
            throw new IllegalArgumentException("El nivel de mejora debe estar entre 0 y 5.");
        }
        Integer valorPatrimonial = propiedad.getPrecioCompra();
       //Recorre los costos de las mejoras que tiene la propiedad y los suma al precio de compra.
        for (int nivel = 0; nivel < nivelMejora; nivel++) {
            valorPatrimonial += costosMejoras[nivel];
        }
        return valorPatrimonial;
    }
}
