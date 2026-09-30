package com.tallerwebi.dominio;

import java.util.List;

import com.tallerwebi.dominio.entidades.Usuario;

public class SalaDeEspera {


private String nombre;
private String codigo;
private  List<Usuario> usuarios;

public String getNombre() {
    return nombre;
}

public void setNombre(String nombre) {
    this.nombre = nombre;
}

public String getCodigo() {
    return codigo;
}

public void setCodigo(String codigo) {
    this.codigo = codigo;
}

public List<Usuario> getUsuarios() {
    return usuarios;
}

public void setUsuarios(List<Usuario> usuarios) {
    this.usuarios = usuarios;
}

public SalaDeEspera(){

}


}
