
package com.tallerwebi.dominio.servicios.interfaces;

import com.tallerwebi.dominio.enums.DificultadRuleta;

@FunctionalInterface 
public interface ServicioRuleta {

    public DificultadRuleta sortearDificultad();
}
