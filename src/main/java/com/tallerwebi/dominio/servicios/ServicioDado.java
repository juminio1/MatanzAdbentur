package com.tallerwebi.dominio.servicios;

import com.tallerwebi.presentacion.DTO.ResultadoTiradaDTO;

@FunctionalInterface
public interface ServicioDado {
    ResultadoTiradaDTO tirarDados();
}