import { crearModalTrivia } from "./trivia.js?v=8";
import { crearApiTrivia } from "./trivia-api.js?v=3";

// Tiempo para ver la respuesta correcta antes de la ronda siguiente o del final.
const PAUSA_TRAS_CIERRE = 2500;
// Cierre automático de premios no monetarios; cada cliente cierra explícitamente un premio en dinero.
const PAUSA_FINAL = 8000;
const premiosAnimadosPorDocumento = new WeakMap();

export function filtrarPropiedadesParaRobar(propiedades, idJugador) {
    return propiedades.filter(propiedad =>
        propiedad.propietarioId != null && String(propiedad.propietarioId) !== String(idJugador));
}

// Usa el cliente STOMP existente y los datos del servidor para cada ronda.
export function conectarTriviaPartida({ cliente, codigo, base = "", documento = document,
    idJugador, programarCierre = function (cerrar) { setTimeout(cerrar, PAUSA_FINAL); },
    esperarTrasCierre = function (seguir, demora = PAUSA_TRAS_CIERRE) { setTimeout(seguir, demora); }, alPremioEntregado = function () {}, estaPreso = function () { return false; },
    actualizarSaldo = function () {}, animarGanancia = function () {},
    registrarEnHistorial = function () {},
    esperarTableroVisible = async function () {
        const ventana = documento.defaultView;
        const pintar = () => ventana?.requestAnimationFrame
            ? new Promise(resolve => ventana.requestAnimationFrame(() => ventana.requestAnimationFrame(resolve)))
            : Promise.resolve();
        await pintar();
        const animaciones = documento.getElementById("trivia")?.getAnimations?.({subtree:true}) ?? [];
        await Promise.allSettled(animaciones.filter(animacion =>
            animacion.effect?.getComputedTiming?.().iterations !== Infinity).map(animacion => animacion.finished));
        await pintar();
    },
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
    const resultadosRegistrados = new Map();

    function animarPremioUnaVez(estado) {
        if (!estado.idTorneo) return;
        const clave = `trivia:premio-animado:${codigo}:${idJugador}:${estado.idTorneo}`;
        let vistos = premiosAnimadosPorDocumento.get(documento);
        if (!vistos) {
            vistos = new Set();
            premiosAnimadosPorDocumento.set(documento, vistos);
        }
        if (vistos.has(clave)) return;
        try {
            const almacen = documento.defaultView.sessionStorage;
            if (almacen.getItem(clave)) { vistos.add(clave); return; }
        } catch { /* La protección en memoria también funciona sin almacenamiento del navegador. */ }
        if (animarGanancia(estado.idGanador, estado.montoPremio) === false) return;
        vistos.add(clave);
        try { documento.defaultView.sessionStorage.setItem(clave, "si"); } catch { /* Sin almacenamiento. */ }
    }

    function registrarResultado(estado) {
        if (!estado?.finalizada || !estado.idTorneo) return;
        const anterior = resultadosRegistrados.get(estado.idTorneo);
        const pendiente = !estado.cancelada && estado.idGanador != null && !estado.premioEntregado;
        if (anterior?.definitivo) return;
        let texto;
        if (estado.cancelada) texto = "La trivia fue cancelada.";
        else if (estado.idGanador == null) texto = "La trivia terminó sin ganador.";
        else if (pendiente) texto = `${estado.ganador} ganó la trivia. Premio pendiente.`;
        else if (estado.tipoPremio === "ROBAR") {
            texto = `${estado.ganador} ganó la trivia y recibió ${estado.propiedadRobada}, robada a ${estado.propietarioAnterior}.`;
        } else {
            texto = `${estado.ganador} ganó la trivia y recibió $${Number(estado.montoPremio).toLocaleString("es-AR")}.`;
        }
        if (anterior?.texto === texto) return;
        resultadosRegistrados.set(estado.idTorneo, { texto, definitivo: !pendiente });
        registrarEnHistorial(texto, estado.idGanador ?? null, "trivia:" + estado.idTorneo);
    }

    async function recuperarHistorial() {
        if (typeof api.obtenerHistorial !== "function") return;
        try {
            for (const estado of await api.obtenerHistorial() ?? []) registrarResultado(estado);
        } catch (error) {
            console.error("No se pudo recuperar el historial de trivia:", error);
        }
    }

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
        const alVolverAlTablero = modal.mostrarFinal({
            desempate,
            ganador: estado.ganador, soyGanador: String(estado.idGanador) === String(idJugador),
            cancelada: estado.cancelada, montoPremio: estado.montoPremio, tipoPremio: estado.tipoPremio,
            propiedadRobada: estado.propiedadRobada, propietarioAnterior: estado.propietarioAnterior,
            puedeElegirPremio: estado.puedeElegirPremio, premioPendiente: estado.premioPendiente,
            obtenerPropiedadesParaRobar: async function () {
                return filtrarPropiedadesParaRobar(await api.obtenerPropiedades(), idJugador);
            },
            alElegirPremio: elegirPremio,
        });
        if (estado.premioEntregado) {
            entregaConfirmada = true;
            const premioEnDinero = estado.tipoPremio === "PLATA" && estado.idGanador != null
                && Number.isFinite(estado.saldoGanador) && Number.isFinite(estado.montoPremio);
            if (premioEnDinero) actualizarSaldo(estado.idGanador, estado.saldoGanador);
            const version = versionTorneo;
            let actualizado = false;
            const volverAlTablero = async function () {
                if (actualizado || version !== versionTorneo) return;
                actualizado = true;
                modal.cerrar();
                if (premioEnDinero) {
                    await esperarTableroVisible();
                    if (version !== versionTorneo) return;
                    animarPremioUnaVez(estado);
                }
                alPremioEntregado(estado);
            };
            if (typeof alVolverAlTablero?.then === "function") alVolverAlTablero.then(volverAlTablero);
            // Cada cliente cierra su propio resultado antes de ver y escuchar el premio.
            if (!premioEnDinero) programarCierre(volverAlTablero);
        }
    }

    function aplicar(estado, pregunta, tipo) {
        if (!estado) return;
        if (pregunta && estado.inicioCoordinado) {
            const panel = documento.getElementById("panel-ruleta");
            if (panel?.dataset.presentacionPendiente === "true") {
                documento.dispatchEvent(new documento.defaultView.Event("trivia:iniciada"));
            }
        }
        if (esperandoPresentacionRuleta()) {
            enEspera.push({ tipo: tipo || (pregunta ? "RONDA_NUEVA" : "PARTICIPANTES_ACTUALIZADOS"), estado, pregunta });
            return;
        }
        const hastaInicio = pregunta && estado.inicioCoordinado
            ? pregunta.venceEn - pregunta.segundosParaResponder * 1000 - Date.now() : 0;
        if (enPausa || hastaInicio > 0) {
            enEspera.push({ tipo: tipo || (pregunta ? "RONDA_NUEVA" : "PARTICIPANTES_ACTUALIZADOS"), estado, pregunta });
            if (!enPausa) pausarEventos(hastaInicio);
            return;
        }
        registrarResultado(estado);
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
            if (preguntaActual) modal.mostrarMensaje("No se pudo recuperar el estado. Recargá la página para reintentar.");
        }
    }

    // Tras cerrar una ronda, el servidor manda enseguida la siguiente o el final.
    // Se guardan un momento para que todos vean la correcta y quiénes quedaron afuera.
    let enPausa = false;
    const enEspera = [];

    function esperandoPresentacionRuleta() {
        return documento.getElementById("panel-ruleta")?.dataset?.presentacionPendiente === "true";
    }

    function continuarEventos() {
        while (enEspera.length > 0 && !enPausa && !esperandoPresentacionRuleta()) procesar(enEspera.shift());
    }

    documento.addEventListener("ruleta:presentacion-finalizada", continuarEventos);
    documento.addEventListener("ruleta:cancelada", () => { enEspera.length = 0; });

    function pausarEventos(demora = PAUSA_TRAS_CIERRE) {
        enPausa = true;
        esperarTrasCierre(function () {
            enPausa = false;
            continuarEventos();
        }, demora);
    }

    function procesar(evento) {
        revision++;
        if (evento.tipo === "RONDA_CERRADA") {
            if (cerrada || !preguntaActual || evento.rondaCerrada !== preguntaActual.ronda) return;
            cerrada = true;
            estadoActual = evento.estado;
            modal.mostrarCierre(opcionElegida, evento.opcionCorrecta,
                nombres(evento.eliminados), nombres(evento.estado.enJuego));
            pausarEventos();
            return;
        }
        aplicar(evento.estado, evento.tipo === "RONDA_NUEVA" ? evento.pregunta : null, evento.tipo);
        // Si se ingreso durante un cambio de participantes, recuperar la pregunta perdida.
        if (!preguntaActual && !evento.estado.finalizada) recuperar();
    }

    cliente.subscribe("/topic/partida/" + codigo + "/trivia", function (mensaje) {
        const evento = JSON.parse(mensaje.body);
        if (evento.tipo === "RONDA_NUEVA" && evento.estado.inicioCoordinado && esperandoPresentacionRuleta()) {
            for (const pendiente of enEspera.splice(0)) registrarResultado(pendiente.estado);
            documento.dispatchEvent(new documento.defaultView.Event("trivia:iniciada"));
            enPausa = false;
        }
        if (enPausa || esperandoPresentacionRuleta()) {
            revision++;
            enEspera.push(evento);
            return;
        }
        procesar(evento);
    });
    const panelRuleta = documento.getElementById("panel-ruleta");
    if (panelRuleta) panelRuleta.dataset.triviaSuscrita = "true";
    documento.dispatchEvent(new documento.defaultView.Event("trivia:suscrita"));
    recuperar();
    recuperarHistorial();
    return { recuperar, recuperarHistorial };
}
