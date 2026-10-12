package com.tallerwebi.infraestructura;

import com.tallerwebi.dominio.entidades.SalaDeEspera;

public interface RepositorioSalaDeEspera {

    void guardar(SalaDeEspera sala);

    void modificar(SalaDeEspera sala);

    SalaDeEspera buscarPorCodigo(String codigoUnico);

    SalaDeEspera buscarPorId(Long id);
}
