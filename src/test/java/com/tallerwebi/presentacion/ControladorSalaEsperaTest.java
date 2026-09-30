package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualIgnoringCase.equalToIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @BeforeEach //Hace que init() se ejecute antes de cada test.
    public void init() {
        this.servicioSalaDeEsperaMock = mock(ServicioSalaDeEspera.class); // Crea un servicio falso.
        this.controladorSalaEspera = new ControladorSalaEspera(servicioSalaDeEsperaMock); // Crea el controlador real y le pasa el servicio falso.
        this.requestMock = mock(HttpServletRequest.class); // Crea un request falso.
        this.sessionMock = mock(HttpSession.class); // Crea una sesión falsa.
    }

    /* @Test
    public void quieroirALaSalaDeEspera() {
        ModelAndView salaDeEspera = controladorSalaEspera.irASalaDeEspera();

        assertEquals("sala-de-espera", salaDeEspera.getViewName());
    } */

    @Test
    public void quieroCrearUnaSalaDeEsperaExitosamente() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        // Cuando el controlador pida la sesión,
        // Mockito devuelve nuestra sesión falsa.

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);
        // Ejecuta la creación de la sala.

        assertThat(model.getViewName(), equalToIgnoringCase("sala-de-espera"));
        // Verifica que termine mostrando "sala-de-espera".
    }

    @Test
    public void quieroCrearUnaSalaDeEsperaConUnUsuarioNoEncontrado() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        // El request devuelve nuestra sesión falsa.

        when(sessionMock.getAttribute("id")).thenReturn(2L);
        // Simula que el usuario logueado tiene ID 2.

        when(this.servicioSalaDeEsperaMock.crearSalaDeEspera(2L)).thenThrow(
            UsuarioNoEncontradoException.class
        );
        // Simula que el usuario logueado tiene ID 2 y lanza una excepción.

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);
        // Ejecuta el controlador.
        // El controlador debería capturar esa excepción.

        assertThat(model.getViewName(), equalToIgnoringCase("login"));
        // Comprueba que ante ese error vuelva al login.
    }

    @Test
    public void queAlCrearUnaSalaDeEsperaSeUtiliceElIdDelUsuarioGuardadoEnSesion() {
        when(requestMock.getSession()).thenReturn(sessionMock);
        // El request devuelve nuestra sesión falsa.

        when(sessionMock.getAttribute("id")).thenReturn(2L);
        // Simula que en la sesión está guardado el ID 2.

        ModelAndView model = this.controladorSalaEspera.crearSalaDeEspera(requestMock);
        // Ejecuta el controlador.

        verify(this.servicioSalaDeEsperaMock).crearSalaDeEspera(2L);
        // Comprueba que el controlador llamó al servicio
        // usando exactamente el ID 2.

        assertThat(model.getViewName(), equalToIgnoringCase("sala-de-espera"));
        // Comprueba que finalmente muestra la sala de espera.
    }
}
