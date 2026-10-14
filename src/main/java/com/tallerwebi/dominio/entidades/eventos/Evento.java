package com.tallerwebi.dominio.entidades.eventos;

import com.tallerwebi.dominio.enums.TipoEfecto;
import jakarta.persistence.*;

@Entity
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String descripcion;

    @Enumerated(EnumType.STRING)
    private TipoEfecto tipoEfecto;

    private Integer valor;

    public Evento() {
    }

    // Dejá todos los getters y setters exactamente como los tenés...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public TipoEfecto getTipoEfecto() { return tipoEfecto; }
    public void setTipoEfecto(TipoEfecto tipoEfecto) { this.tipoEfecto = tipoEfecto; }
    public Integer getValor() { return valor; }
    public void setValor(Integer valor) { this.valor = valor; }
}
