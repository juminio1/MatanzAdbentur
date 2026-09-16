/*package com.tallerwebi.dominio;

import jakarta.persistence.*;

// import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

 //   private Instant tiempoInicio;
    @OneToOne(cascade = CascadeType.PERSIST)
    private Tablero tablero;
    @ManyToMany
    private List<Usuario> usuarios;
   // private Boolean activa;

    public Partida(){
        this.usuarios = new ArrayList<>();
    //    this.activa = true;
    }

  /*  public Boolean agregarUsuario(Usuario usuario){
        if (this.usuarios.size() < 4){
            this.usuarios.add(usuario);
            return true;
        }
        return false;
    }
     

    public void setTablero(Tablero tablero) {
        this.tablero = tablero;
    }

     public void setTiempoInicio(Instant tiempoInicio) {
        this.tiempoInicio = tiempoInicio;
    }

    public void finalizar(){
        this.activa = false;
    }
}*/
