package com.tallerwebi.presentacion;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

public class LoginDTOTest {

    @Test 
    public void dadoQueExisteUnFormularioDeLoginDeberiaDevolverUnLoginConDatosCargados(){
        LoginDTO login = new LoginDTO();

        login.setEmail("juli@gmail.com");
        login.setPassword("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4");

        assertThat(login.getEmail(),is("juli@gmail.com"));
        assertThat(login.getPassword(),is("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"));
    }
    
}
