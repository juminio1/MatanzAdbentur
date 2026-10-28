package com.tallerwebi.dominio.entidades.eventos;

import com.tallerwebi.dominio.enums.TipoEfecto;
import jakarta.persistence.*;

@Entity(name = "EventoGlobalBase")
@Table(name = "EventoGlobalRegistro")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoEfecto tipoEfecto;

    private Integer valor;

    public Evento() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public TipoEfecto getTipoEfecto() {
        return tipoEfecto;
    }

    public void setTipoEfecto(TipoEfecto tipoEfecto) {
        this.tipoEfecto = tipoEfecto;
    }

    public Integer getValor() {
        return valor;
    }

    public void setValor(Integer valor) {
        this.valor = valor;
    }
}