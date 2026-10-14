package com.tallerwebi.presentacion.DTO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RespuestaMovimientoDTOTest {

    // Verifica que el constructor vacío cree el objeto y que todos sus atributos comiencen en null.
    @Test
    public void dadoQueCreoElDTOQuieroQueSusAtributosSeanNulos() {
        // Preparación
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();

        // Verificación
        assertNotNull(respuesta);
        assertNull(respuesta.getDado1());
        assertNull(respuesta.getDado2());
        assertNull(respuesta.getSuma());
        assertNull(respuesta.getNuevaPosicion());
        assertNull(respuesta.getIdJugador());
    }
    // Verifica que el setter de dado1 guarde el valor y que el getter devuelva ese mismo valor.
    @Test
    public void dadoQueEstablezcoElDado1QuieroObtenerSuValor() {
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setDado1(3);
        assertEquals(3, respuesta.getDado1());
    }
    // Verifica que el setter de dado2 guarde el valor y que el getter devuelva ese mismo valor.
    @Test
    public void dadoQueEstablezcoElDado2QuieroObtenerSuValor() {
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setDado2(5);
        assertEquals(5, respuesta.getDado2());
    }
    // Verifica que el setter de suma guarde el resultado y que el getter permita recuperarlo correctamente.
    @Test
    public void dadoQueEstablezcoLaSumaQuieroObtenerSuValor() {
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setSuma(8);
        assertEquals(8, respuesta.getSuma());
    }
    // Verifica que el setter de nuevaPosicion guarde la posición y que el getter devuelva el casillero indicado.
    @Test
    public void dadoQueEstablezcoLaNuevaPosicionQuieroObtenerSuValor() {
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setNuevaPosicion(12);
        assertEquals(12, respuesta.getNuevaPosicion());
    }

    // Verifica que el setter de idJugador guarde el identificador y que el getter devuelva el ID correcto.
    @Test
    public void dadoQueEstablezcoElIdJugadorQuieroObtenerSuValor() {
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setIdJugador(59L);
        assertEquals(59L, respuesta.getIdJugador());
    }
    // Verifica que la posición se pueda modificar: primero se establece un valor y después se reemplaza
    // por otro. El getter debe devolver el último valor.
    @Test
    public void dadoQueActualizoLaNuevaPosicionQuieroObtenerElNuevoValor() {
        RespuestaMovimientoDTO respuesta = new RespuestaMovimientoDTO();
        respuesta.setNuevaPosicion(4);
        respuesta.setNuevaPosicion(9);
        assertEquals(9, respuesta.getNuevaPosicion());
    }
}
