package com.tallerwebi.infraestructura;
import com.tallerwebi.dominio.entidades.MensajeChat;
import java.util.List;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
@Repository
public class RepositorioChatImpl implements RepositorioChat {
    private final SessionFactory sesiones;
    public RepositorioChatImpl(SessionFactory sesiones) { this.sesiones = sesiones; }
    @Override public void guardar(MensajeChat mensaje) { sesiones.getCurrentSession().persist(mensaje); }
    @Override public List<MensajeChat> ultimos(Long partida) {
        return sesiones.getCurrentSession().createQuery(
            "FROM MensajeChat m JOIN FETCH m.autor WHERE m.partida.id = :partida ORDER BY m.id DESC", MensajeChat.class)
            .setParameter("partida", partida).setMaxResults(100).getResultList();
    }
}
