package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Jugador;
import com.tallerwebi.dominio.entidades.Propiedad;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.excepcion.*;
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
    private final RepositorioPropiedad repositorioPropiedad;
    private final ServicioPartida servicioPartida;
    private final ServicioCasillero servicioCasillero;
    private static final int NIVEL_MAXIMO_MEJORA = 3;
    private static final Set<Integer> POSICIONES_PARADA = Set.of(6, 12, 17, 24);

    @Autowired
    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad, ServicioPartida servicioPartida,
                                 ServicioCasillero servicioCasillero) {
        this.repositorioPropiedad = repositorioPropiedad;
        this.servicioPartida = servicioPartida;
        this.servicioCasillero = servicioCasillero;
    }

    public ServicioPropiedadImpl(RepositorioPropiedad repositorioPropiedad, ServicioPartida servicioPartida) {
        this(repositorioPropiedad, servicioPartida, null);
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
    public boolean estaDisponible(Propiedad propiedad) {
        return propiedad != null && propiedad.getPropietario() == null;
    }

    @Override
    public void comprarPropiedad(Long idPropiedad, Jugador jugador) {
        validarJugador(jugador);
        Jugador comprador = jugador.getId() == null ? null : servicioPartida.obtenerJugadorPorId(jugador.getId());
        if (comprador == null) comprador = jugador;
        ejecutarCompra(idPropiedad, comprador);
        jugador.setDinero(comprador.getDinero());
    }

    @Override
    public void comprarPropiedadParaJugador(Long idPropiedad, Long idJugador) {
        if (idJugador == null) throw new JugadorInvalidoException("El jugador no puede ser nulo.");
        Jugador comprador = servicioPartida.obtenerJugadorPorId(idJugador);
        if (comprador == null) throw new JugadorInvalidoException("No se encontró el jugador que compra.");
        ejecutarCompra(idPropiedad, comprador);
    }

    private void ejecutarCompra(Long idPropiedad, Jugador comprador) {
        Propiedad propiedadEncontrada = obtenerPropiedadExistente(idPropiedad);
        if (!estaDisponible(propiedadEncontrada)) {
            throw new PropiedadNoDisponibleException("La propiedad no está disponible.");
        }

        if (comprador.getDinero() < propiedadEncontrada.getPrecioCompra()) {
            throw new DineroInsuficienteException("El jugador no tiene dinero suficiente para realizar la operación.");
        }

        servicioPartida.restarDinero(comprador, propiedadEncontrada.getPrecioCompra());
        propiedadEncontrada.setPropietario(comprador);
        repositorioPropiedad.guardarPropiedad(propiedadEncontrada);
        servicioPartida.actualizarJugador(comprador);
    }

    @Override
    public Integer venderPropiedadParaJugador(Long idPropiedad, Long idJugador) {
        Jugador vendedor = obtenerVendedor(idJugador);
        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);
        validarPropietarioDeVenta(propiedad, idJugador);
        int precioVenta = calcularPrecioVenta(propiedad);
        propiedad.setPropietario(null);
        propiedad.setNivelMejora(0);
        servicioPartida.sumarDinero(vendedor, precioVenta);
        repositorioPropiedad.guardarPropiedad(propiedad);
        servicioPartida.actualizarJugador(vendedor);
        return precioVenta;
    }

    private Jugador obtenerVendedor(Long idJugador) {
        if (idJugador == null) throw new JugadorInvalidoException("El jugador no puede ser nulo.");
        Jugador vendedor = servicioPartida.obtenerJugadorPorId(idJugador);
        if (vendedor == null) throw new JugadorInvalidoException("No se encontró el jugador que vende.");
        return vendedor;
    }

    private void validarPropietarioDeVenta(Propiedad propiedad, Long idJugador) {
        Jugador propietario = propiedad.getPropietario();
        if (propietario == null || !idJugador.equals(propietario.getId())) {
            throw new PropiedadNoDisponibleException("Solo puedes vender una propiedad que te pertenece.");
        }
    }

    private int calcularPrecioVenta(Propiedad propiedad) {
        Integer precioCompra = propiedad.getPrecioCompra();
        if (precioCompra == null || precioCompra < 0) {
            throw new PropiedadInvalidaException("El precio de compra de la propiedad no es válido.");
        }
        return precioCompra / 2;
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador) {
        validarPropiedad(propiedad);
        validarJugador(jugador);
        pagarAlquiler(propiedad, jugador, calcularAlquiler(propiedad));
    }

    @Override
    public void pagarAlquiler(Propiedad propiedad, Jugador jugador, Integer montoAlquiler) {
        validarPropiedad(propiedad);
        validarJugador(jugador);
        if (montoAlquiler == null || montoAlquiler < 0) {
            throw new PropiedadInvalidaException("El precio del alquiler no es válido.");
        }
        Propiedad propiedadPersistida = repositorioPropiedad.buscarPropiedadPorId(propiedad.getId());
        if (propiedadPersistida == null) propiedadPersistida = propiedad;
        Jugador propietario = propiedadPersistida.getPropietario();
        if (propietario == null || propietario.getId().equals(jugador.getId())) {
            return;
        }
        Jugador pagador = servicioPartida.obtenerJugadorPorId(jugador.getId());
        if (pagador == null) pagador = jugador;
        servicioPartida.restarDinero(pagador, montoAlquiler);
        jugador.setDinero(pagador.getDinero());
        servicioPartida.actualizarJugador(pagador);
        servicioPartida.sumarDinero(propietario, montoAlquiler);
        servicioPartida.actualizarJugador(propietario);
    }

    @Override
    public Integer calcularAlquiler(Propiedad propiedad) {
        validarPropiedad(propiedad);

        Integer alquilerBase = propiedad.getPrecioAlquiler();
        Integer nivelMejora = propiedad.getNivelMejora();
        if (alquilerBase == null || alquilerBase < 0) {
            throw new PropiedadInvalidaException("El precio del alquiler no es válido.");
        }

        if (nivelMejora == null || nivelMejora < 0) {
            throw new PropiedadInvalidaException("El nivel de mejora no es válido.");
        }

        //Calcula el aumento del 50 % por cada mejora.
        double multiplicador = 1 + (0.5 * nivelMejora);

        //Devuelve el alquiler actualizado, redondeado hacia abajo al convertirlo a
        return (int) (alquilerBase * multiplicador);
    }

    @Override
    public Integer calcularAlquiler(CasilleroPropiedad casillero, Propiedad propiedad) {
        Long idPartida = casillero.getPartidaId();
        if (!POSICIONES_PARADA.contains(casillero.getPosicion())) return calcularAlquiler(propiedad);
        Jugador propietario = propiedad.getPropietario();
        if (propietario == null || servicioCasillero == null || idPartida == null) return 25;
        long cantidadParadas = servicioCasillero.obtenerTodosLosCasilleros().stream()
                .filter(CasilleroPropiedad.class::isInstance)
                .map(CasilleroPropiedad.class::cast)
                .filter(c -> POSICIONES_PARADA.contains(c.getPosicion()) && c.getPropiedad() != null)
                .filter(c -> idPartida.equals(c.getPartidaId()))
                .filter(c -> c.getPropiedad().getPropietario() != null
                        && Objects.equals(propietario.getId(), c.getPropiedad().getPropietario().getId()))
                .count();
        return (int) Math.min(200, 25 * (1L << Math.max(0, cantidadParadas - 1)));
    }

    @Override
    public void mejorarPropiedad(Long idPropiedad, Jugador jugador) {
        validarJugador(jugador);

        Propiedad propiedad = obtenerPropiedadExistente(idPropiedad);

        validarPropietario(propiedad, jugador);
        validarGrupoCompleto(propiedad, jugador);
        validarNivelMejora(propiedad);

        //Aumenta el nivel de mejora en uno.
        propiedad.setNivelMejora(propiedad.getNivelMejora() + 1);
    }

    //Limita el nivel de mejoras hasta 3
    private void validarNivelMejora(Propiedad propiedad) {
        Integer nivelMejora = propiedad.getNivelMejora();

        if (nivelMejora == null ||
                nivelMejora < 0 ||
                nivelMejora >= NIVEL_MAXIMO_MEJORA) {
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
        GrupoPropiedades grupo = propiedad.getGrupo();

        if (!esDuenioDeTodoElGrupo(grupo, jugador)) {
            throw new PropiedadNoMejorableException("Debes ser dueño de todas las propiedades de la localidad para mejorarlas.");
        }
    }

    //Busca la propiepiedad que pertenecen a esa localidad.
    private boolean esDuenioDeTodoElGrupo(GrupoPropiedades grupo, Jugador jugador) {
        List<Propiedad> propiedades = repositorioPropiedad.buscarPropiedadesPorGrupo(grupo);

        if (propiedades.isEmpty()) {
            return false;
        }

        for (Propiedad propiedad : propiedades) {
            //verifica q todas tengan al jugador como propietario
            if (propiedad.getPropietario() == null || !propiedad.getPropietario().getId().equals(jugador.getId())) {
                return false;
            }
        }
        return true;
    }


}
