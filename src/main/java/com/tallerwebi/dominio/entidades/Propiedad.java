package com.tallerwebi.dominio.entidades;
import jakarta.persistence.*;

@Entity
public class Propiedad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer precioCompra;
    private Integer precioAlquiler;

    @ManyToOne
    private Jugador propietario;

    public Propiedad(Long id, Integer precioCompra, Integer precioAlquiler, Jugador propietario) {
        this.id = id;
        this.precioCompra = precioCompra;
        this.precioAlquiler = precioAlquiler;
        this.propietario = propietario;
    }


    //Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(Integer precioCompra) {
        this.precioCompra = precioCompra;
    }

    public Integer getPrecioAlquiler() {
        return precioAlquiler;
    }

    public void setPrecioAlquiler(Integer precioAlquiler) {
        this.precioAlquiler = precioAlquiler;
    }

    public Jugador getPropietario() {
        return propietario;
    }

    public void setPropietario(Jugador propietario) {
        this.propietario = propietario;
    }

    public boolean estaDisponible() {
        return false;
    }

    public void asignarPropietario(Jugador jugador) {

    }
}
