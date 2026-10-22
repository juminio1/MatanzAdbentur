package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import com.tallerwebi.dominio.entidades.SalaDeEspera;
import com.tallerwebi.dominio.excepcion.*;
import com.tallerwebi.dominio.servicios.interfaces.ServicioPartida;
import com.tallerwebi.dominio.servicios.interfaces.ServicioSalaDeEspera;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public class ControladorSalaEsperaTest {

    private ControladorSalaEspera controladorSalaEspera;
    private ServicioSalaDeEspera servicioSalaDeEsperaMock;
    private ServicioPartida servicioPartidaMock;
    private SimpMessagingTemplate simpMessagingTemplateMock;
    private HttpServletRequest requestMock;
    private HttpSession sessionMock;
    private SalaDeEspera salaMock;

    @BeforeEach
    public void init() {
        this.servicioSalaDeEsperaMock = mock(ServicioSalaDeEspera.class);
        this.servicioPartidaMock = mock(ServicioPartida.class);
        this.simpMessagingTemplateMock = mock(SimpMessagingTemplate.class);

        this.controladorSalaEspera = new ControladorSalaEspera(
            servicioSalaDeEsperaMock,
            servicioPartidaMock,
            simpMessagingTemplateMock
        );

        this.requestMock = mock(HttpServletRequest.class);
        this.sessionMock = mock(HttpSession.class);
        this.salaMock = mock(SalaDeEspera.class);
    }

    @Test
    public void quieroCrearUnaSalaDeEsperaExitosamente() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenReturn(this.salaMock);

        when(this.salaMock.getCodigoUnico()).thenReturn("ABC123");

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

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

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenReturn(this.salaMock);

        when(this.salaMock.getCodigoUnico()).thenReturn("ABC123");

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);

        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);

        assertThat(
            model.getViewName(),
            equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123")
        );
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

        ModelAndView model = controladorSalaEspera.unirseASala(
            "ABC123",
            requestMock,
            mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class)
        );

        assertThat(
            model.getViewName(),
            equalToIgnoringCase("redirect:/sala-de-espera?codigoUnico=ABC123")
        );
    }

    @Test
    public void unirseASalaLlenaVaAHome() throws Exception {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(2L);
        when(servicioSalaDeEsperaMock.unirseASalaDeEspera(2L, "ABC123")).thenThrow(
            com.tallerwebi.dominio.excepcion.SalaDeEsperaLlenaException.class
        );

        ModelAndView model = controladorSalaEspera.unirseASala(
            "ABC123",
            requestMock,
            mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class)
        );

        assertThat(model.getViewName(), equalToIgnoringCase("redirect:/home"));
    }

    @Test
    public void dadoQueUnUsuarioNoEstaEnSesionAlIniciarPartidaDebeRedirigirALogin() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(null);
        when(sessionMock.getAttribute("IDUSUARIO")).thenReturn(null);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            "ABC123",
            requestMock,
            mock(RedirectAttributes.class)
        );

        assertEquals("redirect:/login", mav.getViewName());
    }

    @Test
    public void dadoQueElCodigoDeSalaEsInvalidoAlIniciarPartidaDebeRedirigirAHomeConError() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            "",
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/home", mav.getViewName());
        verify(redirectAttributesMock).addFlashAttribute(
            "errorIniciarPartida",
            "El código de la sala es inválido"
        );
    }

    @Test
    public void dadoQueSeIniciaPartidaExitosamenteDebeNotificarPorWebSocketYRedirigirAPartida()
        throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/partida", mav.getViewName());
        
        
        verify(servicioPartidaMock, times(1)).iniciarPartida(codigo, 1L);
        
        verify(sessionMock).setAttribute("codigoSala", codigo);
        verify(simpMessagingTemplateMock).convertAndSend(
            "/topic/sala/" + codigo + "/iniciar",
            "INICIAR"
        );
        verify(redirectAttributesMock).addAttribute("codigo", codigo);
    }

    @Test
    public void dadoQueLaSalaNoExisteAlIniciarPartidaDebeRedirigirAHomeConError() throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        doThrow(new SalaNoEncontradaException()).when(servicioPartidaMock).iniciarPartida(codigo, 1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/home", mav.getViewName());
        verify(redirectAttributesMock).addFlashAttribute(
            "errorIniciarPartida",
            "La sala de espera no existe"
        );
    }

    @Test
    public void dadoQueLaPartidaNoExisteAlIniciarPartidaDebeRedirigirASalaConError()
        throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        doThrow(new PartidaNoEncontradaException())
            .when(servicioPartidaMock)
            .iniciarPartida(codigo, 1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/sala-de-espera", mav.getViewName());
        verify(redirectAttributesMock).addFlashAttribute(
            "errorIniciarPartida",
            "No se encontró una partida para esta sala"
        );
        verify(redirectAttributesMock).addAttribute("codigoUnico", codigo);
    }

    @Test
    public void dadoQueHayJugadoresInsuficientesAlIniciarPartidaDebeRedirigirASalaConError()
        throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        doThrow(new CantidadJugadoresInsuficienteException("No hay suficientes jugadores"))
            .when(servicioPartidaMock)
            .iniciarPartida(codigo, 1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/sala-de-espera", mav.getViewName());
        verify(redirectAttributesMock).addFlashAttribute(
            "errorIniciarPartida",
            "No hay suficientes jugadores para iniciar la partida"
        );
    }

    @Test
    public void dadoQueSeSuperaCantidadMaximaDeJugadoresAlIniciarPartidaDebeRedirigirASalaConError()
        throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        doThrow(new CantidadMaximaJugadoresSuperadaException("El maximo son 4 jugadores"))
            .when(servicioPartidaMock)
            .iniciarPartida(codigo, 1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/sala-de-espera", mav.getViewName());
        verify(redirectAttributesMock).addFlashAttribute(
            "errorIniciarPartida",
            "Se superó la cantidad máxima de jugadores"
        );
    }

    @Test
    public void dadoQueLaPartidaYaEstaIniciadaDebeRedirigirDirectoAPartida() throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        doThrow(new PartidaIniciadaException()).when(servicioPartidaMock).iniciarPartida(codigo, 1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/partida", mav.getViewName());
        verify(sessionMock).setAttribute("codigoSala", codigo);
        verify(redirectAttributesMock).addAttribute("codigo", codigo);
    }

    @Test
    public void dadoUnErrorGenericoAlIniciarPartidaDebeRedirigirASalaConError() throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(1L);

        doThrow(new RuntimeException("Error inesperado"))
            .when(servicioPartidaMock)
            .iniciarPartida(codigo, 1L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/sala-de-espera", mav.getViewName());
        verify(redirectAttributesMock).addFlashAttribute(
            "errorIniciarPartida",
            "Ocurrió un error al iniciar la partida"
        );
    }

    @Test
    public void dadoQueElUsuarioUsaIDUSUARIOEnSesionAlIniciarPartidaExitosamenteDebePermitirlo()
        throws Exception {
        String codigo = "ABC123";
        when(requestMock.getSession()).thenReturn(sessionMock);
        when(sessionMock.getAttribute("id")).thenReturn(null);
        when(sessionMock.getAttribute("IDUSUARIO")).thenReturn(99L);

        RedirectAttributes redirectAttributesMock = mock(RedirectAttributes.class);

        ModelAndView mav = controladorSalaEspera.iniciarPartida(
            codigo,
            requestMock,
            redirectAttributesMock
        );

        assertEquals("redirect:/partida", mav.getViewName());
        verify(servicioPartidaMock, times(1)).iniciarPartida(codigo, 99L);
    }
}
