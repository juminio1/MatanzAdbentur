package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;

@FunctionalInterface
public interface ServicioDado {
    ResultadoTiradaDTO tirarDados();
}
