package com.tallerwebi.config;

import com.tallerwebi.dominio.entidades.eventos.Evento;
import com.tallerwebi.dominio.enums.TipoEfecto;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InicializadorDeEventos implements ApplicationListener<ContextRefreshedEvent> {

    private final SessionFactory sessionFactory;

    @Autowired
    public InicializadorDeEventos(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional
    @SuppressWarnings("PMD.CloseResource")
    public void onApplicationEvent(ContextRefreshedEvent event) {
        Session session = sessionFactory.getCurrentSession();

        //verificamos si ya hay eventos guardados para no duplicarlos
        Long cantidadEventos = session.createQuery("SELECT count(e) FROM Evento e", Long.class).uniqueResult();

        if (cantidadEventos == 0) {

            //ganar plata
            guardarEvento(session, "Ganaste el quini 6: Le pegaste a los números que jugaste, que capo.", TipoEfecto.GANAR_PLATA, 1000);
            guardarEvento(session, "Changa de fin de mes: Te salió un laburito. Alta noche.", TipoEfecto.GANAR_PLATA, 500);

            //perder plata
            guardarEvento(session, "Control de tránsito: Fuiste parado para un control en camino de cintura. Multa por VTV.", TipoEfecto.PERDER_PLATA, 500);
            guardarEvento(session, "Factura de Edenor: Prendiste el aire cinco minutos y te vino una fortuna.", TipoEfecto.PERDER_PLATA, 500);

            //perder turno
            guardarEvento(session, "Colectivo Lleno: Pasó el 242 hasta las manos y no te abrió.", TipoEfecto.PERDER_TURNO, 1);
            guardarEvento(session, "Trámite en Anses: La fila no avanza y el que atiende toma mates.", TipoEfecto.PERDER_TURNO, 1);

            //retroceder
            guardarEvento(session, "Te pasaste de parada en el 620: Venías distraído con el celular.", TipoEfecto.MOVER_CASILLEROS, -2);
            guardarEvento(session, "El perro de la cuadra te corrió: El Firulais te sacó cagando.", TipoEfecto.MOVER_CASILLEROS, -1);

            //avanzar
            guardarEvento(session, "Semáforo en verde: Ibas por ruta 3 y agarraste todos en verde.", TipoEfecto.MOVER_CASILLEROS, 2);

            //avanzar hasta x casillero
            guardarEvento(session, "Te alcanzó un amigo en moto: Avanzas hasta la UNLaM.", TipoEfecto.IR_A_CASILLERO, 15);
            guardarEvento(session, "Sale joda: Hoy salis con los pibes, avanza hasta Pinar de Rocha.", TipoEfecto.IR_A_CASILLERO, 25);

            /*//ir en cana
            guardarEvento(session, "Caiste en cana: Te agarraron por robarte un termidor.", TipoEfecto.IR_A_CARCEL, 0);
            guardarEvento(session, "Caiste en cana: Te detuvieron por tráfico de drogas.", TipoEfecto.IR_A_CARCEL, 0);
            guardarEvento(session, "Caiste en cana: Te la diste y estuviste en pelotas por Av Rivadavia.", TipoEfecto.IR_A_CARCEL, 0);
            guardarEvento(session, "Caiste en cana: Te agarraste a piñas afuera del boliche y te retuvieron en la comisaría.", TipoEfecto.IR_A_CARCEL, 0);
            guardarEvento(session, "Caiste en cana: Chupaste toda la nochebuena y te metieron en cana por conducir alcoholizado.", TipoEfecto.IR_A_CARCEL, 0);*/
        }
    }

    private void guardarEvento(Session session, String descripcion, TipoEfecto tipoEfecto, Integer valor) {
        Evento evento = new Evento();
        evento.setDescripcion(descripcion);
        evento.setTipoEfecto(tipoEfecto);
        evento.setValor(valor);
        session.save(evento);
    }
}