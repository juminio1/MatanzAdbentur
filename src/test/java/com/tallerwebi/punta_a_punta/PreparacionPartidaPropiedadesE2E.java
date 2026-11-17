package com.tallerwebi.punta_a_punta;

import com.tallerwebi.punta_a_punta.vistas.VistaSalaEspera;
import java.util.List;

/** Coordina los pasos de interfaz disponibles en el escenario compartido. */
final class PreparacionPartidaPropiedadesE2E {
    private PreparacionPartidaPropiedadesE2E() {}

    static String iniciarConTresJugadores(EscenarioPartidaE2E escenario) {
        List<String> colores = List.of("ROJO", "AZUL", "VERDE");
        for (int indice = 0; indice < escenario.paginas().size(); indice++) {
            escenario.dadoQueUnUsuarioSeRegistraEIniciaSesion(escenario.paginas().get(indice),
                escenario.nombres().get(indice), "mejora" + indice + "@unlam.edu.ar");
        }
        String codigo = escenario.cuandoElAnfitrionCreaUnaSala();
        escenario.cuandoElInvitadoSeUneALaSala(escenario.paginaInvitadoUno, codigo);
        escenario.cuandoElInvitadoSeUneALaSala(escenario.paginaInvitadoDos, codigo);
        for (int indice = 0; indice < escenario.paginas().size(); indice++) {
            VistaSalaEspera sala = new VistaSalaEspera(escenario.paginas().get(indice));
            sala.esperarConexionWebSocket();
            sala.elegirFicha(colores.get(indice));
            sala.verificarFichaDeJugador(escenario.nombres().get(indice), colores.get(indice));
        }
        escenario.iniciarPartidaYEsperarTableros(codigo);
        return codigo;
    }
}
