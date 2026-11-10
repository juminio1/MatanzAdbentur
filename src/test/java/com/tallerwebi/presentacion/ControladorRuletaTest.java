package com.tallerwebi.presentacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.tallerwebi.dominio.servicios.interfaces.ServicioRuleta;

public class ControladorRuletaTest {

    private ServicioRuleta servicioRuletaMock;
    private ControladorRuleta controladorRuleta;

    @BeforeEach
    public void init() {

        this.servicioRuletaMock = mock(ServicioRuleta.class);
        this.controladorRuleta = new ControladorRuleta(servicioRuletaMock);
    }

    @Test
    public void deberiaDevolverDificultadFacilCuandoElServicioLaSortea() {

        when(servicioRuletaMock.sortearDificultad()).thenReturn("FACIL");

        String dificultad = controladorRuleta.sortearDificultad();

        assertEquals("FACIL", dificultad);

        verify(servicioRuletaMock).sortearDificultad();
    }


@Test
public void deberiaExponerElSorteoMediantePost() throws NoSuchMethodException { //Verifica que el metodo tenga las anotaciones necesarias, no comprueba una petición HTTP real

    Method metodo = ControladorRuleta.class.getMethod("sortearDificultad");

    PostMapping postMapping = metodo.getAnnotation(PostMapping.class);

    ResponseBody responseBody = metodo.getAnnotation(ResponseBody.class);

    assertNotNull(postMapping);
    assertNotNull(responseBody);
}


@Test
public void deberiaResponderConLaDificultadSorteadaAlRecibirUnaPeticionPost() throws Exception {

    when(servicioRuletaMock.sortearDificultad()).thenReturn("FACIL");

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controladorRuleta).build(); //Prepara un entorno mvc de prueba utilizando nuestro controlador, sin levantar la app

    //             Simula una petición HTTP POST    verifica q el servidor responda con 200
    mockMvc.perform(post("/ruleta/sortear")).andExpect(status().isOk()).andExpect(content().string("FACIL")); // Comprueba que la respuesta contenga la dificultad sorteada

    verify(servicioRuletaMock).sortearDificultad(); // Comprueba que el controlador delegó la operación al servicio
}


}
