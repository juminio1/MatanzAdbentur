package com.tallerwebi.integracion;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.tallerwebi.dominio.entidades.*;
import com.tallerwebi.dominio.entidades.Casillero.CasilleroPropiedad;
import com.tallerwebi.dominio.enums.*;
import com.tallerwebi.dominio.servicios.*;
import com.tallerwebi.dominio.servicios.interfaces.*;
import com.tallerwebi.infraestructura.*;
import com.tallerwebi.infraestructura.config.HibernateInfraestructuraTestConfig;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = HibernateInfraestructuraTestConfig.class)
class IntercambioPropiedadIntegracionTest {
    @Autowired private SessionFactory sesiones;
    @Autowired private PlatformTransactionManager transacciones;
    private TransactionTemplate tx;
    private ServicioIntercambioPropiedadImpl servicio;
    private long[] ids;
    private String codigo;
    private final AtomicBoolean fallar = new AtomicBoolean();
    private final AtomicInteger escrituras = new AtomicInteger();

    @BeforeEach
    void preparar() {
        tx = new TransactionTemplate(transacciones);
        codigo = "CAMBIO-" + UUID.randomUUID();
        ids = tx.execute(estado -> {
            Jugador ana = jugador("Ana", 900);
            Jugador luna = jugador("Luna", 1200);
            Usuario creador = new Usuario();
            creador.setUsername(codigo);
            creador.setEmail(codigo + "@example.test");
            sesiones.getCurrentSession().persist(creador);
            Partida partida = new Partida();
            partida.setCreador(creador);
            partida.setCodigoUnico(codigo);
            partida.setEstado(EstadoPartida.EN_CURSO);
            partida.setJugadores(List.of(ana, luna));
            sesiones.getCurrentSession().persist(partida);
            CasilleroPropiedad ofrecida = casillero(4, ana, 2, partida.getId());
            CasilleroPropiedad solicitada = casillero(9, luna, 1, partida.getId());
            sesiones.getCurrentSession().persist(ofrecida);
            sesiones.getCurrentSession().persist(solicitada);
            sesiones.getCurrentSession().flush();
            return new long[] {partida.getId(), ana.getId(), luna.getId(), ofrecida.getId(), solicitada.getId(),
                ofrecida.getPropiedad().getId(), solicitada.getPropiedad().getId(), creador.getId()};
        });
        ServicioPartida partidas = mock(ServicioPartida.class);
        when(partidas.obtenerPartida(codigo)).thenAnswer(i -> sesiones.getCurrentSession().get(Partida.class, ids[0]));
        RepositorioPartida repositorioPartida = new RepositorioPartidaImpl(sesiones);
        doAnswer(i -> { repositorioPartida.bloquearPartida(i.getArgument(0)); return null; })
            .when(partidas).bloquearPartida(any());
        RepositorioPropiedad reales = new RepositorioPropiedadImpl(sesiones);
        RepositorioPropiedad propiedades = mock(RepositorioPropiedad.class);
        doAnswer(i -> {
            if (escrituras.incrementAndGet() == 2 && fallar.get()) throw new IllegalStateException("Fallo al guardar la segunda propiedad");
            reales.guardarPropiedad(i.getArgument(0));
            return null;
        }).when(propiedades).guardarPropiedad(any());
        servicio = new ServicioIntercambioPropiedadImpl(partidas,
            new ServicioCasilleroImpl(new RepositorioCasilleroImpl(sesiones)), propiedades, new ServicioParticipacionImpl());
    }

    @Test
    void elIntercambioSeConfirmaEnLaBaseConAmbosDueniosYLasMejorasIntactas() {
        var propuesta = tx.execute(estado -> servicio.proponer(codigo, ids[1], ids[2], 4, 9));
        tx.executeWithoutResult(estado -> servicio.aceptar(codigo, propuesta.id(), ids[2]));
        verificarPropietarios(ids[2], ids[1]);
    }

    @Test
    void siFallaLaSegundaPropiedadSeReviertenAmbasYLaPropuestaSePuedeReintentar() {
        var propuesta = tx.execute(estado -> servicio.proponer(codigo, ids[1], ids[2], 4, 9));
        fallar.set(true);
        assertThrows(IllegalStateException.class,
            () -> tx.executeWithoutResult(estado -> servicio.aceptar(codigo, propuesta.id(), ids[2])));
        verificarPropietarios(ids[1], ids[2]);
        Integer cantidadPendientes = tx.execute(estado -> servicio.pendientes(codigo, ids[2]).size());
        assertEquals(1, cantidadPendientes.intValue());
        fallar.set(false);
        tx.executeWithoutResult(estado -> servicio.aceptar(codigo, propuesta.id(), ids[2]));
        verificarPropietarios(ids[2], ids[1]);
    }

    @Test
    void unaTransaccionFallidaNoDejaLaPropiedadReservada() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(estado -> {
            servicio.proponer(codigo, ids[1], ids[2], 4, 9);
            throw new IllegalStateException("Simular rollback");
        }));
        var propuesta = tx.execute(estado -> servicio.proponer(codigo, ids[1], ids[2], 4, 9));
        assertNotNull(propuesta);
        Integer cantidad = tx.execute(estado -> servicio.enviadas(codigo, ids[1]).size());
        assertEquals(1, cantidad.intValue());
    }

    @Test
    void dosEnviosDeLaMismaPropiedadEnUnaTransaccionSeRechazan() {
        assertThrows(IllegalStateException.class, () -> tx.executeWithoutResult(estado -> {
            servicio.proponer(codigo, ids[1], ids[2], 4, 9);
            servicio.proponer(codigo, ids[1], ids[2], 4, 9);
        }));
        assertNotNull(tx.execute(estado -> servicio.proponer(codigo, ids[1], ids[2], 4, 9)));
    }

    private void verificarPropietarios(long ofrecida, long solicitada) {
        tx.executeWithoutResult(estado -> {
            Propiedad primera = sesiones.getCurrentSession().get(Propiedad.class, ids[5]);
            Propiedad segunda = sesiones.getCurrentSession().get(Propiedad.class, ids[6]);
            assertEquals(ofrecida, primera.getPropietario().getId());
            assertEquals(solicitada, segunda.getPropietario().getId());
            assertEquals(2, primera.getNivelMejora());
            assertEquals(1, segunda.getNivelMejora());
            assertEquals(900, sesiones.getCurrentSession().get(Jugador.class, ids[1]).getDinero());
            assertEquals(1200, sesiones.getCurrentSession().get(Jugador.class, ids[2]).getDinero());
        });
    }

    @AfterEach
    void limpiar() {
        if (ids == null) return;
        tx.executeWithoutResult(estado -> {
            for (long id : new long[] {ids[3], ids[4]}) {
                CasilleroPropiedad casillero = sesiones.getCurrentSession().get(CasilleroPropiedad.class, id);
                Propiedad propiedad = casillero.getPropiedad();
                sesiones.getCurrentSession().remove(casillero);
                sesiones.getCurrentSession().flush();
                sesiones.getCurrentSession().remove(propiedad);
                sesiones.getCurrentSession().flush();
            }
            sesiones.getCurrentSession().remove(sesiones.getCurrentSession().get(Partida.class, ids[0]));
            sesiones.getCurrentSession().flush();
            sesiones.getCurrentSession().remove(sesiones.getCurrentSession().get(Usuario.class, ids[7]));
        });
    }

    private Jugador jugador(String apodo, int dinero) {
        Jugador jugador = new Jugador(); jugador.setApodo(apodo); jugador.setDinero(dinero); return jugador;
    }

    private CasilleroPropiedad casillero(int posicion, Jugador propietario, int nivel, Long partida) {
        Propiedad propiedad = new Propiedad(null, "Propiedad " + posicion, 300, 50, propietario, GrupoPropiedades.SAN_JUSTO);
        propiedad.setNivelMejora(nivel);
        CasilleroPropiedad casillero = new CasilleroPropiedad(null, posicion, "Propiedad " + posicion, "", propiedad);
        casillero.setPartidaId(partida);
        return casillero;
    }
}
