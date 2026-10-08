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

    // tiene q haber un contructor vacio, para el JPA/hibernate, para crear entidades
    public Propiedad() {
    }

    //Se crea la propiedad, con
    public Propiedad(Integer precioCompra, Integer precioAlquiler) {
        this.precioCompra = precioCompra;
        this.precioAlquiler = precioAlquiler;
    }

    public boolean estaDisponible() {
        return propietario == null;
    }

    public void asignarPropietario(Jugador jugador) {
        this.propietario = jugador;
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


}
