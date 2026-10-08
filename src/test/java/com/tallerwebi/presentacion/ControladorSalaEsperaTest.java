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
    private SalaDeEspera salaMock;

    @BeforeEach //Hace que init() se ejecute antes de cada test.
    public void init() {
        this.servicioSalaDeEsperaMock = mock(ServicioSalaDeEspera.class); // Crea un servicio falso.
        this.controladorSalaEspera = new ControladorSalaEspera(servicioSalaDeEsperaMock); // Crea el controlador real y le pasa el servicio falso.
        this.requestMock = mock(HttpServletRequest.class); // Crea un request falso.
        this.sessionMock = mock(HttpSession.class); // Crea una sesión falsa.
        this.salaMock = mock(SalaDeEspera.class);
    }

    /* @Test
    public void quieroirALaSalaDeEspera() {
        ModelAndView salaDeEspera = controladorSalaEspera.irASalaDeEspera();

        assertEquals("sala-de-espera", salaDeEspera.getViewName());
    } */

    @Test
    public void quieroCrearUnaSalaDeEsperaExitosamente() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenReturn(this.salaMock);

        when(this.salaMock.getCodigoUnico()).thenReturn("ABC123");

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123"));

        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);
    }

    @Test
    public void quieroCrearUnaSalaDeEsperaConUnUsuarioNoEncontrado() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenThrow(UsuarioNoEncontradoException.class);

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/login"));

        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);
    }

    @Test
    public void queAlCrearUnaSalaDeEsperaSeUtiliceElIdDelUsuarioGuardadoEnSesion() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenReturn(this.salaMock);

        when(this.salaMock.getCodigoUnico()).thenReturn("ABC123");

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123"));
    }

        @Test
    public void verSalaExistenteMuestraSalaDeEspera() throws Exception {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);
        when(servicioSalaDeEsperaMock.obtenerSalaPorCodigo("ABC123")).thenReturn(salaMock);
        when(salaMock.getUsuarios()).thenReturn(new java.util.ArrayList<>());

        ModelAndView model = controladorSalaEspera.verSalaDeEspera("ABC123", requestMock);

        assertThat(model.getViewName(), equalToIgnoringCase("sala-de-espera"));
    }

    @Test
    public void unirseASalaExitoso() throws Exception {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);
        when(servicioSalaDeEsperaMock.unirseASalaDeEspera(2L, "ABC123")).thenReturn(salaMock);
        when(salaMock.getCodigoUnico()).thenReturn("ABC123");

        ModelAndView model = controladorSalaEspera.unirseASala("ABC123", requestMock, mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class));

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123"));
    }

    @Test
    public void unirseASalaLlenaVaAHome() throws Exception {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);
        when(servicioSalaDeEsperaMock.unirseASalaDeEspera(2L, "ABC123")).thenThrow(com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException.class);

        ModelAndView model = controladorSalaEspera.unirseASala("ABC123", requestMock, mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class));

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/home"));
    }
}
