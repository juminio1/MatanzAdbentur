package com.tallerwebi.presentacion.mapeadores;
import com.tallerwebi.dominio.resultados.ResultadoJugada;
import com.tallerwebi.presentacion.DTO.*;
import java.util.UUID;
public final class MapeadorMovimiento {
    private MapeadorMovimiento() { }
    public static RespuestaMovimientoDTO desde(ResultadoJugada jugada) {
        RespuestaMovimientoDTO dto = new RespuestaMovimientoDTO();
        dto.setIdMovimiento(UUID.randomUUID().toString());
        dto.setDado1(jugada.tirada().getDado1());
        dto.setDado2(jugada.tirada().getDado2());
        dto.setSuma(jugada.tirada().sumarDados());
        dto.setNuevaPosicion(jugada.nuevaPosicion());
        dto.setIdJugador(jugada.idJugador());
        dto.setPosicionAnterior(jugada.posicionAnterior());
        dto.setPosicionCaidaEvento(jugada.posicionCaidaEvento());
        dto.setDineroPorInicio(jugada.dineroPorInicio());
        if (jugada.evento() != null) {
            dto.setNombreEvento(jugada.evento().nombre());
            dto.setDescripcionEvento(jugada.evento().descripcion());
            dto.setTipoEfectoEvento(jugada.evento().tipoEfecto());
        }
        dto.setSaldos(jugada.saldos().stream().map(s -> new SaldoJugadorDTO(s.idJugador(), s.dinero(), s.enBancarrota())).toList());
        if (jugada.alquiler() != null) {
            var alquiler = jugada.alquiler();
            dto.setAlquiler(new AlquilerMovimientoDTO(alquiler.idJugadorPagador(), alquiler.pagador(), alquiler.propietario(), alquiler.propiedad(), alquiler.monto()));
        }
        return dto;
    }
}
