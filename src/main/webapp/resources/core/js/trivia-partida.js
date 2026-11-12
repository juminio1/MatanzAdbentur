import { crearModalTrivia } from "./trivia.js?v=4";
import { crearApiTrivia } from "./trivia-api.js?v=2";

// Tiempo para ver la respuesta correcta antes de la ronda siguiente o del final.
const PAUSA_TRAS_CIERRE = 2500;

export function filtrarPropiedadesParaRobar(propiedades, idJugador) {
    return propiedades.filter(propiedad =>
        propiedad.propietarioId != null && String(propiedad.propietarioId) !== String(idJugador));
}

// Usa el cliente STOMP existente y los datos del servidor para cada ronda.
export function conectarTriviaPartida({ cliente, codigo, base = "", documento = document,
    idJugador, programarCierre = function (cerrar) { setTimeout(cerrar, 2500); },
    esperarTrasCierre = function (seguir) { setTimeout(seguir, PAUSA_TRAS_CIERRE); }, alPremioEntregado = function () {}, estaPreso = function () { return false; },
    modal = crearModalTrivia(), api = crearApiTrivia({ base, codigo }) } = {}) {
    let revision = 0;
    let preguntaActual = null;
    let estadoActual = null;
    let yaRespondio = false;
    let enviando = false;
    let cerrada = false;
    let opcionElegida = null;
    let entregaConfirmada = false;
    let enviandoPremio = false;
    let finalMostrado = false;
    let versionTorneo = 0;

    function nombres(participantes) {
        const lista = [];
        for (const jugador of participantes) lista.push(jugador.apodo);
        return lista;
    }

    function estaEnJuego() {
        if (!estadoActual || estaPreso()) return false;
        for (const jugador of estadoActual.enJuego) {
            if (String(jugador.id) === String(idJugador)) return true;
        }
        return false;
    }

    function puedeResponder() {
        return preguntaActual && !cerrada && !estadoActual.finalizada && estaEnJuego()
            && !yaRespondio && !enviando && Date.now() < preguntaActual.venceEn;
    }

    function actualizarParticipantes() {
        if (!preguntaActual) return;
        let espectador = null;
        if (estaPreso()) espectador = "preso";
        else if (!estaEnJuego()) espectador = "eliminado";
        modal.actualizarParticipantes({
            enJuego: nombres(estadoActual.enJuego), espectador,
            permitirResponder: Boolean(puedeResponder()),
        });
    }

    async function responder(opcion, pregunta) {
        if (pregunta !== preguntaActual || !puedeResponder()) return;
        enviando = true;
        opcionElegida = opcion;
        actualizarParticipantes();
        const revisionEnvio = ++revision;
        try {
            await api.enviarRespuesta(pregunta.id, opcion);
            // El resultado HTTP no revela la correcta ni cambia una ronda mas reciente.
            if (pregunta !== preguntaActual || revisionEnvio !== revision) return;
            yaRespondio = true;
            enviando = false;
            actualizarParticipantes();
        } catch (error) {
            if (pregunta !== preguntaActual || revisionEnvio !== revision) return;
            modal.mostrarMensaje("No se pudo confirmar la respuesta. Consultando al servidor...");
            // Incluso si se corto la red, el servidor pudo haber aceptado la respuesta.
            await recuperar();
        }
    }

    async function elegirPremio(eleccion) {
        if (enviandoPremio || entregaConfirmada || !estadoActual?.premioPendiente
                || !estadoActual.puedeElegirPremio || estadoActual.cancelada
                || String(estadoActual.idGanador) !== String(idJugador)) return;
        enviandoPremio = true;
        const revisionEnvio = ++revision;
        try {
            const estado = await api.elegirPremio(eleccion);
            if (revisionEnvio !== revision) return;
            aplicar(estado);
        } catch (error) {
            if (revisionEnvio !== revision) return;
            await recuperar();
            if (!estadoActual?.premioPendiente || entregaConfirmada || estadoActual.cancelada) return;
            throw error;
        } finally {
            enviandoPremio = false;
        }
    }

    function mostrarFinal(estado, notificarEntrega) {
        cerrada = true;
        if (entregaConfirmada) return;
        if (estado.premioEntregado && !notificarEntrega) {
            entregaConfirmada = true;
            modal.cerrar();
            return;
        }
        if (finalMostrado && !estado.cancelada && !estado.premioEntregado) return;
        finalMostrado = true;
        const desempate = [];
        for (const jugador of estado.enJuego) {
            desempate.push({ nombre: jugador.apodo, milisegundos: jugador.milisegundos });
        }
        modal.mostrarFinal({
            desempate,
            ganador: estado.ganador, soyGanador: String(estado.idGanador) === String(idJugador),
            cancelada: estado.cancelada, montoPremio: estado.montoPremio, tipoPremio: estado.tipoPremio,
            puedeElegirPremio: estado.puedeElegirPremio, premioPendiente: estado.premioPendiente,
            obtenerPropiedadesParaRobar: async function () {
                return filtrarPropiedadesParaRobar(await api.obtenerPropiedades(), idJugador);
            },
            alElegirPremio: elegirPremio,
        });
        if (estado.premioEntregado) {
            entregaConfirmada = true;
            const version = versionTorneo;
            programarCierre(function () {
                if (version !== versionTorneo) return;
                modal.cerrar();
                alPremioEntregado();
            });
        }
    }

    function aplicar(estado, pregunta, tipo) {
        if (!estado) return;
        const notificarEntrega = Boolean(tipo) || Boolean(estadoActual?.premioPendiente);
        if (estado.finalizada && (entregaConfirmada || estadoActual?.cancelada)) return;
        estadoActual = estado;
        if (estado.finalizada) {
            mostrarFinal(estado, notificarEntrega);
            return;
        }
        if (!pregunta) {
            actualizarParticipantes();
            return;
        }
        const esNueva = !preguntaActual || pregunta.ronda !== preguntaActual.ronda
            || pregunta.venceEn !== preguntaActual.venceEn;
        if (esNueva) {
            versionTorneo++;
            entregaConfirmada = false;
            finalMostrado = false;
            preguntaActual = pregunta;
            cerrada = false;
            yaRespondio = estado.yaRespondio === true;
            enviando = false;
            opcionElegida = null;
            const panel = documento.getElementById("panel-ruleta");
            if (panel) panel.hidden = true;
            documento.dispatchEvent(new documento.defaultView.Event("trivia:iniciada"));
            modal.mostrarPregunta(pregunta, {
                numeroRonda: pregunta.ronda, totalRondas: pregunta.totalRondas,
                segundosParaResponder: pregunta.segundosParaResponder,
                venceEn: pregunta.venceEn,
                enJuego: nombres(estado.enJuego),
                permitirResponder: Boolean(puedeResponder()),
                alResponder: function (opcion) { return responder(opcion, pregunta); },
            });
        } else if (typeof estado.yaRespondio === "boolean") {
            yaRespondio = estado.yaRespondio;
            enviando = false;
        }
        actualizarParticipantes();
        if (yaRespondio && !cerrada) modal.mostrarMensaje("Respuesta registrada. Esperando el cierre de la ronda...");
    }

    async function recuperar(reintentos = 1) {
        const revisionInicial = ++revision;
        try {
            const estado = await api.obtenerEstado();
            if (revisionInicial !== revision || !estado) return;
            if (estado.finalizada) {
                aplicar(estado);
                return;
            }
            const pregunta = await api.obtenerPregunta();
            if (revisionInicial !== revision || !pregunta) return;
            if (estado.ronda !== pregunta.ronda || estado.venceEn != null && estado.venceEn !== pregunta.venceEn) {
                if (reintentos > 0) await recuperar(reintentos - 1);
                return;
            }
            aplicar(estado, pregunta);
        } catch (error) {
            if (revisionInicial !== revision) return;
            console.error("No se pudo recuperar la trivia:", error);
            if (preguntaActual) modal.mostrarMensaje("No se pudo recuperar el estado. Recarga la pagina para reintentar.");
        }
    }

    // Tras cerrar una ronda, el servidor manda enseguida la siguiente o el final.
    // Se guardan un momento para que todos vean la correcta y quiénes quedaron afuera.
    let enPausa = false;
    const enEspera = [];

    function procesar(evento) {
        revision++;
        if (evento.tipo === "RONDA_CERRADA") {
            if (!preguntaActual || evento.rondaCerrada !== preguntaActual.ronda) return;
            cerrada = true;
            estadoActual = evento.estado;
            modal.mostrarCierre(opcionElegida, evento.opcionCorrecta,
                nombres(evento.eliminados), nombres(evento.estado.enJuego));
            enPausa = true;
            esperarTrasCierre(function () {
                enPausa = false;
                while (enEspera.length > 0 && !enPausa) procesar(enEspera.shift());
            });
            return;
        }
        aplicar(evento.estado, evento.tipo === "RONDA_NUEVA" ? evento.pregunta : null, evento.tipo);
        // Si se ingreso durante un cambio de participantes, recuperar la pregunta perdida.
        if (!preguntaActual && !evento.estado.finalizada) recuperar();
    }

    cliente.subscribe("/topic/partida/" + codigo + "/trivia", function (mensaje) {
        const evento = JSON.parse(mensaje.body);
        if (enPausa) {
            enEspera.push(evento);
            return;
        }
        procesar(evento);
    });
    recuperar();
    return { recuperar };
}
