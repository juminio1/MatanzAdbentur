package com.tallerwebi.dominio.entidades;

import com.tallerwebi.dominio.enums.GrupoPropiedades;
import com.tallerwebi.dominio.excepcion.PropiedadInvalidaException;
import com.tallerwebi.dominio.excepcion.PropiedadNoDisponibleException;
import jakarta.persistence.*;

@Entity
public class Propiedad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer precioCompra;
    private Integer precioAlquiler;
    private Integer nivelMejora = 0;
    private String nombre;

    @ManyToOne
    private Jugador propietario;

    @Enumerated(EnumType.STRING)
    private GrupoPropiedades grupo;

    // tiene q haber un contructor vacio, para el JPA/hibernate, para crear entidades
    public Propiedad() {
    }

    //Se crea la propiedad, con
    public Propiedad(Long id, String nombre, Integer precioCompra, Integer precioAlquiler, Jugador propietario, GrupoPropiedades grupo) {
        this.id = id;
        this.nombre = nombre;
        this.precioCompra = precioCompra;
        this.precioAlquiler = precioAlquiler;
        this.propietario = propietario;
        this.grupo = grupo;
    }

    //Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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
    public void setNivelMejora(Integer nivelMejora) {
        this.nivelMejora = nivelMejora;
    }
    public Integer getNivelMejora() {
        return nivelMejora;
    }
    public GrupoPropiedades getGrupo() {
        return grupo;
    }

    public void validarPropietarioDeVenta(Long idJugador) {
        if (propietario == null || !idJugador.equals(propietario.getId())) {
            throw new PropiedadNoDisponibleException("Solo puedes vender una propiedad que te pertenece.");
        }
    }

    public Integer venderAlBanco(Long idJugador) {
        validarPropietarioDeVenta(idJugador);
        Integer precioVenta = calcularPrecioVenta();
        setPropietario(null);
        setNivelMejora(0);
        return precioVenta;
    }

    private Integer calcularPrecioVenta() {
        if (precioCompra == null || precioCompra < 0) {
            throw new PropiedadInvalidaException("El precio de compra de la propiedad no es válido.");
        }
        return precioCompra / 2;
    }

}
