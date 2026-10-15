package com.tallerwebi.config;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.tallerwebi.dominio.entidades.eventos.EventoGlobal;
import com.tallerwebi.dominio.enums.TipoEfecto;
import com.tallerwebi.dominio.enums.TipoEventoGlobal;

@Component //Spring detecta que esta clase debe ser administrada por él
public class InicializadorDeEventosGlobales implements ApplicationListener<ContextRefreshedEvent> {

    //Cuando Spring termina de inicializar o refrescar su contexto, ejecuta el método onApplicationEvent()

    private final SessionFactory sessionFactory; //Es un componente de Hibernate que lo que hace es obtener sesiones de la bdd

    @Autowired
    public InicializadorDeEventosGlobales(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional // Permite ejecutar las operaciones de bdd dentro de una transacción
    @SuppressWarnings("PMD.CloseResource") // Evita una advertencia de PMD sobre el cierre de la sesión
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Session session = sessionFactory.getCurrentSession(); // Obtiene la sesión de Hibernate asociada a la transacción actual

        Long cantidadEventos = session
            .createQuery("SELECT count(e) FROM EventoGlobal  e", Long.class).uniqueResult(); // Queremos saber cuantos eventos globales estan guardados 

        if (cantidadEventos == 0) {

            //AVANZAR
            //TIRAR DADOS
            guardarEventoGlobal(session, "Tramites en AFIP: Caíste a hacer un trámite y hay terrible fila. Todos tiran los dados y el que saque la mayor suma de dados se saltea toda la fila y avanza hasta el casillero del jugador más adelantado. Si hay empate, los que empataron con la mayor suma vuelven a tirar hasta que uno se gane el pase VIP. ", TipoEfecto.MOVER_CASILLEROS, TipoEventoGlobal.TRAMITES_AFIP, 0);

            //RETROCEDER
            //TIRAR DADOS 
            guardarEventoGlobal(session, "Se inundó el rancho: Se largó el diluvio en La Matanza y el barrio parece una pileta municipal. Todos tiran los dados, el que saque la mayor suma de dados se lo lleva el agua puesto y retrocede 3 casilleros. Si hay empate, los que empataron con la mayor suma vuelven a tirar hasta que uno se coma el garrón. ", TipoEfecto.MOVER_CASILLEROS, TipoEventoGlobal.INUNDACION, -3);

            //VOLVER AL INICIO
            //TIRAR DADOS
            guardarEventoGlobal(session, "Piquete: Quedaste atrapado en un piquete de volqueteros. Todos tiran los dados, el que saque la menor cantidad de numero en dados, vuelve al inicio. Si hay empate, los que empataron con la menor suma vuelven a tirar hasta que uno se coma el garrón.", TipoEfecto.VOLVER_INCICO, TipoEventoGlobal.PIQUETE, 0);

            //PERDER PLATA
            //ALEATORIO
            guardarEventoGlobal(session, "El Kevin anda suelto: El Kevin se escapó del penal y anda haciendo de las suyas por La Matanza. Se cruzo a [nombre del jugador], te regalaste moño y te afanó $1000.", TipoEfecto.PERDER_PLATA, TipoEventoGlobal.EL_KEVIN, 1000);

            //GANAR PLATA
            //ALEATORIO
            guardarEventoGlobal(session, "Se le quedó el Fiat 600 a Germancho! Se quedó a pata en plena Ruta 3 y [nombre del jugador] se puso la 10 para ayudarlo a empujar la máquina. Germancho le tiró 400 mangos por la changuita. ", TipoEfecto.GANAR_PLATA, TipoEventoGlobal.FIAT_600, 400);

            //PERDER TURNO
            //ALEATORIO
            guardarEventoGlobal(session, "Hubo un accidente en el tren Sarmiento y el servicio quedó parado. Justo [nombre del jugador] venía viajando y quedó atrapado en el medio del quilombo. Mala ahí, [nombre del jugador] perdiste el turno esperando que vuelva a funcionar.", TipoEfecto.PERDER_TURNO, TipoEventoGlobal.ACCIDENTE_SARMIENTO, 1);
        
            //Vendemedias
            
        
        }
    }


    private void guardarEventoGlobal(Session session, String descripcion, TipoEfecto tipoEfecto, TipoEventoGlobal tipoEventoGlobal, Integer valor){
        EventoGlobal eventoGlobal = new EventoGlobal();
        eventoGlobal.setDescripcion(descripcion);
        eventoGlobal.setTipoEfecto(tipoEfecto);
        eventoGlobal.setTipoEventoGlobal(tipoEventoGlobal);
        eventoGlobal.setValor(valor);
        session.persist(eventoGlobal);
    }
}
