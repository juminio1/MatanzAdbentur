package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.resultados.ResultadoTirada;

@FunctionalInterface
public interface ServicioDado {
    ResultadoTirada tirarDados();
}
