package com.tallerwebi.dominio.servicios;

import com.tallerwebi.dominio.entidades.Casillero.Casillero;
import com.tallerwebi.dominio.servicios.interfaces.ServicioCasillero;
import com.tallerwebi.infraestructura.RepositorioCasillero;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("servicioCasillero")
@Transactional
public class ServicioCasilleroImpl implements ServicioCasillero {

    private RepositorioCasillero repositorioCasillero;

    @Autowired
    public ServicioCasilleroImpl(RepositorioCasillero repositorioCasillero) {
        this.repositorioCasillero = repositorioCasillero;
    }

    @Override
    public List<Casillero> obtenerTodosLosCasilleros() {
        return repositorioCasillero.obtenerTodos();
    }

    @Override
    public void guardarCasillero(Casillero casillero) {
        repositorioCasillero.guardar(casillero);
    }

}
