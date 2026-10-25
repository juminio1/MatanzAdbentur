package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.entidades.Usuario;
import com.tallerwebi.dominio.enums.ColorFicha;
import com.tallerwebi.dominio.excepcion.FichaOcupadaException;
import com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException;
import com.tallerwebi.dominio.excepcion.SalaNoEncontradaException;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.infraestructura.RepositorioSalaDeEspera;
import com.tallerwebi.infraestructura.RepositorioUsuario;
import com.tallerwebi.dominio.eventos.EventoSalaActualizada;
import com.tallerwebi.dominio.publicadores.PublicadorEventos;
import com.tallerwebi.dominio.servicios.interfaces.ServicioSalaDeEspera;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service("servicioSalaDeEspera")
@Transactional
public class ServicioSalaDeEsperaImpl implements ServicioSalaDeEspera {

    private static final Integer LIMITE_JUGADORES = 4;

    private final RepositorioSalaDeEspera repositorioSalaDeEspera;
    private final RepositorioUsuario repositorioUsuario;
    private final PublicadorEventos<EventoSalaActualizada> publicadorSala;

    @Autowired
    public ServicioSalaDeEsperaImpl(RepositorioSalaDeEspera repositorioSalaDeEspera, RepositorioUsuario repositorioUsuario, PublicadorEventos<EventoSalaActualizada> publicadorSala) {

        this.repositorioSalaDeEspera = repositorioSalaDeEspera;
        this.repositorioUsuario = repositorioUsuario;
        this.publicadorSala = publicadorSala;
    }

    @Override
    public SalaDeEspera crearSalaDeEspera(Long idUsuario) throws UsuarioNoEncontradoException {
        Usuario creador = obtenerUsuarioExistente(idUsuario);

        creador.setFicha(null);
        repositorioUsuario.modificar(creador);

        SalaDeEspera nuevaSala = new SalaDeEspera();

        nuevaSala.setCodigoUnico(generarCodigoUnico());
        nuevaSala.setCreador(creador);
        nuevaSala.getUsuarios().add(creador);

        repositorioSalaDeEspera.guardar(nuevaSala);

        return nuevaSala;
    }

    @Override
    public SalaDeEspera unirseASalaDeEspera(Long idUsuario, String codigoUnico)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException, SalaDeEsperaLlenaException {
        Usuario usuario = obtenerUsuarioExistente(idUsuario);

        SalaDeEspera sala = obtenerSalaPorCodigo(codigoUnico);

        Boolean yaEstaEnSala = sala
            .getUsuarios()
            .stream()
            .anyMatch(u -> u.getId() != null && u.getId().equals(usuario.getId()));

        if (yaEstaEnSala) {
            return sala;
        }

        if (sala.getUsuarios().size() >= LIMITE_JUGADORES) {
            throw new SalaDeEsperaLlenaException();
        }

        usuario.setFicha(null);
        repositorioUsuario.modificar(usuario);

        sala.getUsuarios().add(usuario);

        repositorioSalaDeEspera.modificar(sala);

        notificarCambioDeSala(sala);

        return sala;
    }

    @Override
    public void abandonarSala(Long idUsuario, String codigoUnico)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException {
        Usuario usuario = obtenerUsuarioExistente(idUsuario);

        SalaDeEspera sala = obtenerSalaPorCodigo(codigoUnico);

        if (usuario.getFicha() != null) {
            sala.getFichasOcupadas().remove(usuario.getFicha());
            usuario.setFicha(null);
            repositorioUsuario.modificar(usuario);
        }

        Boolean removido = sala
            .getUsuarios()
            .removeIf(u -> u.getId() != null && u.getId().equals(usuario.getId()));

        if (removido) {
            repositorioSalaDeEspera.modificar(sala);

            notificarCambioDeSala(sala);
        }
    }

    @Override
    public SalaDeEspera obtenerSalaPorCodigo(String codigoUnico) throws SalaNoEncontradaException {
        SalaDeEspera sala = repositorioSalaDeEspera.buscarPorCodigo(codigoUnico);

        if (sala == null) {
            throw new SalaNoEncontradaException();
        }

        return sala;
    }

    private EventoSalaActualizada obtenerEstadoSala(SalaDeEspera sala) {
        List<String> usernames = sala.getUsuarios().stream().map(Usuario::getUsername).toList();
        List<String> fichas = sala.getFichasOcupadas().stream().map(ColorFicha::name).toList();

        Map<String, String> asignadas = new java.util.HashMap<>();
        for (Usuario u : sala.getUsuarios()) {
            if (u.getFicha() != null) {
                asignadas.put(u.getUsername(), u.getFicha().name());
            }
        }

        String creador = sala.getCreador() == null ? null : sala.getCreador().getUsername();
        return new EventoSalaActualizada(sala.getCodigoUnico(), usernames, fichas, asignadas, creador);
    }

    private void notificarCambioDeSala(SalaDeEspera sala) {
        EventoSalaActualizada estadoActualizado = obtenerEstadoSala(sala);

        if (
            TransactionSynchronizationManager.isSynchronizationActive() &&
            TransactionSynchronizationManager.isActualTransactionActive()
        ) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // Aviso después de confirmar la transacción.
                        publicadorSala.notificar(estadoActualizado);
                    }
                }
            );
        } else {
            publicadorSala.notificar(estadoActualizado);
        }
    }

    private Usuario obtenerUsuarioExistente(Long idUsuario) {
        Usuario usuario = repositorioUsuario.buscarUsuarioPorId(idUsuario);
        if (usuario == null) {
            throw new UsuarioNoEncontradoException();
        }
        return usuario;
    }

    private String generarCodigoUnico() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    @Override
    public void seleccionarFicha(String codigoUnico, Long idUsuario, ColorFicha ficha)
        throws UsuarioNoEncontradoException, SalaNoEncontradaException, FichaOcupadaException {
        Usuario usuario = obtenerUsuarioExistente(idUsuario);

        SalaDeEspera sala = obtenerSalaPorCodigo(codigoUnico);

        ColorFicha fichaAnterior = usuario.getFicha();

        // Si vuelve a elegir su misma ficha, no hacemos nada
        if (ficha == fichaAnterior) {
            return;
        }

        // Verificamos que la nueva ficha esté disponible
        if (sala.getFichasOcupadas().contains(ficha)) {
            throw new FichaOcupadaException("La ficha ya se encuentra ocupada");
        }

        // Liberamos la ficha anterior si estaba ocupada en esta sala
        if (fichaAnterior != null) {
            sala.getFichasOcupadas().remove(fichaAnterior);
        }

        // Guardamos la nueva selección
        usuario.setFicha(ficha);
        sala.getFichasOcupadas().add(ficha);

        repositorioUsuario.modificar(usuario);
        repositorioSalaDeEspera.modificar(sala);

        notificarCambioDeSala(sala);
    }
}
