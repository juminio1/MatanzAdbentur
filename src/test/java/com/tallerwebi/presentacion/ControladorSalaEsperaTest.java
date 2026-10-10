package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.UsuarioNoEncontradoException;
import com.tallerwebi.dominio.servicios.ServicioSalaDeEspera;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.ModelAndView;

public class ControladorSalaEsperaTest {

    private ControladorSalaEspera controladorSalaEspera;
    private ServicioSalaDeEspera servicioSalaDeEsperaMock;
    private HttpServletRequest requestMock;
    private HttpSession sessionMock;
    private SalaDeEspera salaDeEsperaMock;

    @BeforeEach
    public void init() {
        this.servicioSalaDeEsperaMock = mock(ServicioSalaDeEspera.class);
        this.controladorSalaEspera = new ControladorSalaEspera(servicioSalaDeEsperaMock);
        this.requestMock = mock(HttpServletRequest.class);
        this.sessionMock = mock(HttpSession.class);
        this.salaDeEsperaMock = mock(SalaDeEspera.class);
    }

    @Test
    public void quieroCrearUnaSalaDeEsperaExitosamente() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenReturn(this.salaDeEsperaMock);
        when(this.salaDeEsperaMock.getCodigoUnico()).thenReturn("ABC123");

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

        // Actualizado para reflejar la redirección con el código único
        assertThat(
            model.getViewName(),
            equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123")
        );
        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);
    }

    @Test
    public void quieroCrearUnaSalaDeEsperaConUnUsuarioNoEncontrado() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenThrow(
            UsuarioNoEncontradoException.class
        );

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/login"));
        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);
    }

    @Test
    public void queAlCrearUnaSalaDeEsperaSeUtiliceElIdDelUsuarioGuardadoEnSesion() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenReturn(this.salaDeEsperaMock);
        when(this.salaDeEsperaMock.getCodigoUnico()).thenReturn("ABC123");

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);

        // Actualizado para reflejar la redirección con el código único
        assertThat(
            model.getViewName(),
            equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123")
        );
    }

    @Test
    public void verSalaDeEsperaExitosamente() throws Exception {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        SalaDeEspera salaMock = mock(SalaDeEspera.class);
        when(this.servicioSalaDeEsperaMock.obtenerSalaPorCodigo("ABC123")).thenReturn(salaMock);
        when(salaMock.getUsuarios()).thenReturn(new java.util.ArrayList<>());

        ModelAndView model = this.controladorSalaEspera.verSalaDeEspera("ABC123", requestMock);

        assertThat(model.getViewName(), equalToIgnoringCase("sala-de-espera"));
        verify(this.servicioSalaDeEsperaMock).obtenerSalaPorCodigo("ABC123");
    }

    @Test
    public void verSalaDeEsperaRedirigeSiNoExiste() throws Exception {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        when(this.servicioSalaDeEsperaMock.obtenerSalaPorCodigo("INEXISTENTE")).thenThrow(
            new com.tallerwebi.dominio.excepcion.SalaNoEncontradaException()
        );

        ModelAndView model = this.controladorSalaEspera.verSalaDeEspera("INEXISTENTE", requestMock);

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/home"));
    }

    @Test
    public void unirseASalaExitosamente() throws Exception {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        SalaDeEspera salaMock = mock(SalaDeEspera.class);
        when(this.servicioSalaDeEsperaMock.unirseASalaDeEspera(1L, "ABC123")).thenReturn(salaMock);
        when(salaMock.getCodigoUnico()).thenReturn("ABC123");

        org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes = mock(
            org.springframework.web.servlet.mvc.support.RedirectAttributes.class
        );

        ModelAndView model = this.controladorSalaEspera.unirseASala(
            "ABC123",
            requestMock,
            redirectAttributes
        );

        assertThat(
            model.getViewName(),
            equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123")
        );
        verify(this.servicioSalaDeEsperaMock).unirseASalaDeEspera(1L, "ABC123");
    }
}
