
import { Wheel } from "./vendor/spin-wheel-esm.js";
import { solicitarSorteoPartida, consultarEstadoRuleta, confirmarPresentacionRuleta } from "./servicio-ruleta.js?v=coordinada-1";
import { calcularSegundosRestantes } from "./temporizador-ruleta.js";

import { crearRuleta, girarRuleta } from "./ruleta.js?v=sin-texto-1";

export function inicializarRuleta(contenedor) {
    return crearRuleta(contenedor, Wheel);
}

export function ejecutarGiroRuleta(dificultad, ruleta) {
    girarRuleta(dificultad, ruleta);
}

export function configurarRuleta(
    documento = document,
    solicitar = solicitarSorteoPartida,
    crear = inicializarRuleta,
    animar = ejecutarGiroRuleta,
    consultar = consultarEstadoRuleta,
    confirmar = confirmarPresentacionRuleta
) {

    const botonAbrir = documento.getElementById("btn-abrir-ruleta");
    const botonGirar = documento.getElementById("btn-girar-ruleta");

    const panel = documento.getElementById("panel-ruleta");
    const contenedor = documento.getElementById("ruleta-visual");
    const resultado = documento.getElementById("ruleta-resultado");
    const contador = documento.getElementById("ruleta-temporizador");

    if (!botonGirar || !panel || !contenedor || !resultado || !contador) {
        return;
    }

    // Determina la ruta base de la aplicación.
    // En Jasmine no existe window, por eso verificamos antes de utilizarlo.
    const basePath = typeof window !== "undefined" &&
        window.location.pathname.startsWith("/spring") ? "/spring" : "";

    const parametros = typeof window !== "undefined"
        ? new URLSearchParams(window.location.search) : new URLSearchParams();

    const codigoPartida = parametros.get("codigo") ||
        (typeof window !== "undefined" ? window.codigoSala : null) || "";

    let ruleta = null;
    let girando = false;
    let giroCompletado = false;
    let dificultadElegida = null;

    let activacionActual = null;
    let animacionIniciada = false;
    let solicitudEnCurso = false;
    let intervaloContador = null;
    let ultimaActivacionResuelta = null;
    let revisionEstado = 0;
    let esperandoNuevaActivacion = false;
    const activacionesConTrivia = new Set();
    const activacionesCanceladas = new Set();
    const movimientos = new Set();
    const llegadasATrivia = new Set();
    const activacionesPresentadas = new Set();
    const estadosPendientes = [];
    let temporizadorPresentacion = null;
    const presentacionesAvisadas = new Set();
    let confirmacionPendiente = null;

    function confirmarAlEstarSuscrito(activacionId) {
        if (panel.dataset?.triviaSuscrita !== "true") {
            confirmacionPendiente = activacionId;
            return;
        }
        confirmacionPendiente = null;
        Promise.resolve(confirmar(basePath, codigoPartida, activacionId)).catch(error =>
            console.warn("No se pudo confirmar la presentacion de trivia.", error));
    }

    documento.addEventListener("trivia:suscrita", () => {
        if (confirmacionPendiente) confirmarAlEstarSuscrito(confirmacionPendiente);
    });

    function cancelarPresentacion() {
        if (temporizadorPresentacion !== null) clearTimeout(temporizadorPresentacion);
        temporizadorPresentacion = null;
    }

    function programarFinPresentacion() {
        const id = activacionActual?.activacionId;
        if (temporizadorPresentacion !== null || presentacionesAvisadas.has(id)) return;
        temporizadorPresentacion = setTimeout(() => {
            temporizadorPresentacion = null;
            if (activacionActual?.activacionId === id && !girando) avisarFinDePresentacion(id);
        }, 3000);
    }

    function marcarPresentacionPendiente(pendiente) {
        if (panel.dataset) panel.dataset.presentacionPendiente = String(pendiente);
    }

    function avisarFinDePresentacion(activacionId) {
        if (presentacionesAvisadas.has(activacionId)) return;
        presentacionesAvisadas.add(activacionId);
        const dificultad = dificultadElegida;
        const avisar = () => {
            if (activacionActual?.activacionId !== activacionId || activacionesConTrivia.has(activacionId)
                    || activacionesCanceladas.has(activacionId)) return;
            marcarPresentacionPendiente(false);
            resultado.textContent = "Esperando preguntas del servidor...";
            confirmarAlEstarSuscrito(activacionId);
            const Evento = documento.defaultView?.CustomEvent ?? globalThis.CustomEvent;
            documento.dispatchEvent?.(new Evento("ruleta:presentacion-finalizada", { detail: { dificultad, activacionId } }));
        };
        // La dificultad permaneció visible durante tres segundos.
        const ventana = documento.defaultView;
        if (ventana?.requestAnimationFrame) ventana.requestAnimationFrame(() => ventana.requestAnimationFrame(avisar));
        else avisar();
    }

    const nombres = {
        FACIL: "FÁCIL",
        MEDIO: "MEDIA",
        DIFICIL: "DIFÍCIL"
    };

    function limpiarEstilosResultado() {

        resultado.classList.remove(
            "ruleta-resultado--facil",
            "ruleta-resultado--medio",
            "ruleta-resultado--dificil"
        );
    }

    function mostrarResultado(dificultad) {

        limpiarEstilosResultado();

        resultado.classList.add(`ruleta-resultado--${dificultad.toLowerCase()}`);
        resultado.textContent = `Resultado: ${nombres[dificultad]}`;
    }

    function limpiarTemporizador() {

        if (intervaloContador !== null) {
            clearInterval(intervaloContador);
            intervaloContador = null;
        }
    }

    function esJugadorHabilitado() {

        if (!activacionActual || typeof window === "undefined") {
            return false;
        }

        return window.usuarioSesionId != null &&
            String(activacionActual.idJugador) === String(window.usuarioSesionId);
    }

    function inicializarVisual() {

        if (ruleta) {
            return true;
        }

        try {

            ruleta = crear(contenedor);
            ruleta.onRest = finalizarGiro;

            return true;

        } catch (error) {

            console.error("No se pudo crear la ruleta:", error);

            resultado.textContent = "Error al cargar la ruleta";
            botonGirar.disabled = true;

            return false;
        }
    }

    function abrirPanel() {

        if (esperandoNuevaActivacion) return false;
        panel.hidden = false;

        return inicializarVisual();
    }

    function actualizarCuentaRegresiva() {

        if (!activacionActual || activacionActual.estado !== "ESPERANDO") {
            limpiarTemporizador();
            contador.hidden = true;
            return;
        }

        const segundosRestantes = calcularSegundosRestantes(activacionActual.venceEn);

        contador.hidden = false;
        contador.textContent = segundosRestantes > 0
            ? `Giro automático en ${segundosRestantes} segundos`
            : "Esperando resultado del servidor...";

        // El backend realiza el giro automático.
        // El frontend solamente representa el tiempo restante.
        botonGirar.disabled = solicitudEnCurso || !esJugadorHabilitado() || segundosRestantes === 0;
    }

    function iniciarCuentaRegresiva() {

        limpiarTemporizador();

        actualizarCuentaRegresiva();

        // Actualiza el contador visual sin solicitar sorteos al backend.
        intervaloContador = setInterval(actualizarCuentaRegresiva, 250);
    }

    function prepararNuevaActivacion(estado) {
        cancelarPresentacion();

        activacionActual = estado;
        dificultadElegida = null;
        girando = false;
        giroCompletado = false;
        animacionIniciada = false;
        solicitudEnCurso = false;
        marcarPresentacionPendiente(true);

        limpiarEstilosResultado();

        if (!abrirPanel()) {
            return;
        }

        resultado.textContent = esJugadorHabilitado()
            ? "¡Probá tu suerte!"
            : "Esperando que el jugador habilitado gire la ruleta...";


        iniciarCuentaRegresiva();
    }

    function mostrarEstadoSorteado(estado) {

        if (!estado || !["FACIL", "MEDIO", "DIFICIL"].includes(estado.dificultad)) {
            console.error("El backend devolvió una dificultad inválida.", estado);
            return;
        }

        // Una misma activación solamente puede iniciar una animación.
        if (animacionIniciada) {
            return;
        }

        limpiarTemporizador();

        animacionIniciada = true;
        marcarPresentacionPendiente(true);
        solicitudEnCurso = false;
        girando = true;
        giroCompletado = true;
        dificultadElegida = estado.dificultad;

        botonGirar.disabled = true;
        contador.hidden = true;

        limpiarEstilosResultado();

        if (!abrirPanel()) {
            girando = false;
            mostrarResultado(dificultadElegida);
            programarFinPresentacion();
            return;
        }

        resultado.textContent = "Girando...";

        try {

            // El backend realiza el sorteo definitivo.
            // La ruleta solamente anima el resultado recibido.
            animar(dificultadElegida, ruleta);

            // El desbloqueo de los botones se realiza en onRest.

        } catch (error) {

            console.error("No se pudo animar la ruleta:", error);

            resultado.textContent = `Resultado: ${nombres[dificultadElegida]}`;
            girando = false;
            programarFinPresentacion();
        }
    }

    function cancelarActivacion(estado) {
        if (activacionesCanceladas.has(estado.activacionId)) return;
        activacionesCanceladas.add(estado.activacionId);
        const estabaPendiente = estadosPendientes.some(pendiente => pendiente.activacionId === estado.activacionId);
        for (let indice = estadosPendientes.length - 1; indice >= 0; indice--) {
            if (estadosPendientes[indice].activacionId === estado.activacionId) estadosPendientes.splice(indice, 1);
        }
        if (confirmacionPendiente === estado.activacionId) confirmacionPendiente = null;
        if (activacionActual && activacionActual.activacionId !== estado.activacionId
                && (!estabaPendiente || !activacionesConTrivia.has(activacionActual.activacionId))) return;
        if (estadosPendientes.length > 0) return;
        cancelarPresentacion();
        limpiarTemporizador();
        marcarPresentacionPendiente(false);
        activacionActual = estado;
        panel.hidden = true;
        contador.hidden = true;
        botonGirar.disabled = true;
        girando = false;
        solicitudEnCurso = false;
        dificultadElegida = null;
        const Evento = documento.defaultView?.CustomEvent ?? globalThis.CustomEvent;
        documento.dispatchEvent?.(new Evento("ruleta:cancelada", {detail:{activacionId:estado.activacionId}}));
    }

    function procesarEstadoRuleta(estado, recuperado = false) {

        if (!estado || !estado.activacionId || !codigoPartida) {
            return;
        }

        if (estado.codigoPartida !== codigoPartida) {
            return;
        }

        if (estado.estado === "CANCELADA") {
            cancelarActivacion(estado);
            return;
        }
        if (activacionesCanceladas.has(estado.activacionId)) return;

        if (estado.estado !== "ESPERANDO" && estado.estado !== "SORTEADA") {
            return;
        }

        if (activacionesConTrivia.has(estado.activacionId)) return;
        // Al recuperar trivia podemos conocer la pregunta antes que su ruleta.
        if (esperandoNuevaActivacion && estado.estado === "SORTEADA") {
            activacionesConTrivia.add(estado.activacionId);
            return;
        }
        if (estado.estado === "ESPERANDO") esperandoNuevaActivacion = false;

        const mismaActivacion = activacionActual?.activacionId === estado.activacionId;

        // No volvemos a mostrar una activación ya resuelta.
        if (!mismaActivacion && ultimaActivacionResuelta === estado.activacionId) {
            return;
        }

        // Una respuesta atrasada no puede retroceder de SORTEADA a ESPERANDO.
        if (mismaActivacion && activacionActual.estado === "SORTEADA" && estado.estado === "ESPERANDO") {
            return;
        }

        const jugador = String(estado.idJugador);
        const hayFicha = documento.querySelector?.(`.ficha[data-jugador-id="${jugador}"]`);
        if (!activacionesPresentadas.has(estado.activacionId)
                && (movimientos.has(jugador) || hayFicha?.dataset.movimientoEnCurso === "true"
                    || !recuperado && hayFicha && !llegadasATrivia.has(jugador))) {
            marcarPresentacionPendiente(true);
            estadosPendientes.push(estado);
            return;
        }
        activacionesPresentadas.add(estado.activacionId);
        llegadasATrivia.delete(jugador);

        if (!mismaActivacion) {

            // No interrumpimos una animación que todavía está girando.
            if (girando) {
                console.warn("Llegó otra activación mientras la ruleta estaba girando.");
                return;
            }

            limpiarTemporizador();

            activacionActual = null;
            animacionIniciada = false;
            solicitudEnCurso = false;
        }

        if (estado.estado === "ESPERANDO") {

            if (!mismaActivacion) {
                prepararNuevaActivacion(estado);
            } else {
                activacionActual = estado;
                actualizarCuentaRegresiva();
            }

            return;
        }

        activacionActual = estado;
        ultimaActivacionResuelta = estado.activacionId;

        mostrarEstadoSorteado(estado);
    }

    async function iniciarGiro() {

        // Impide otro sorteo si ya se está girando,
        // si la activación no existe o si el jugador no está habilitado.
        if (girando || giroCompletado || solicitudEnCurso || !ruleta || panel.hidden ||
            !activacionActual || activacionActual.estado !== "ESPERANDO" ||
            !esJugadorHabilitado() || calcularSegundosRestantes(activacionActual.venceEn) === 0) {
            return;
        }

        solicitudEnCurso = true;
        botonGirar.disabled = true;

        resultado.textContent = "Sorteando dificultad...";

        const activacionSolicitada = activacionActual.activacionId;

        try {

            // Solicita al backend el sorteo de la activación actual.
            const estado = await solicitar(basePath, codigoPartida, activacionSolicitada);

            // También puede llegar por WebSocket antes de esta respuesta.
            procesarEstadoRuleta(estado);

        } catch (error) {

            console.error("Error al sortear la dificultad:", error);

            // Si el WebSocket ya confirmó el sorteo, no anulamos el resultado.
            if (activacionActual?.activacionId === activacionSolicitada &&
                activacionActual.estado === "ESPERANDO") {

                resultado.textContent = "No se pudo realizar el giro";

                solicitudEnCurso = false;

                // Permite reintentar solamente mientras el backend espera.
                actualizarCuentaRegresiva();
            }
        }
    }

    function finalizarGiro(evento) {

        if (!ruleta || !dificultadElegida) {
            return;
        }

        // Obtiene el segmento que realmente señala la flecha.
        const indiceReal = evento.currentIndex;
        const segmento = ruleta.items[indiceReal];

        if (!segmento) {

            resultado.textContent = "No se pudo identificar el resultado";

        } else if (segmento.value !== dificultadElegida) {

            // No mostramos un ganador incorrecto.
            resultado.textContent = "Error: el giro no coincide con el resultado";

            console.error("Inconsistencia en la ruleta:", {
                esperado: dificultadElegida,
                obtenido: segmento.value,
                indiceReal
            });

        } else {
            mostrarResultado(segmento.value);

        }

        girando = false;
        solicitudEnCurso = false;

        // Una misma activación permite un único sorteo.
        botonGirar.disabled = true;
        programarFinPresentacion();
        const Evento = documento.defaultView?.CustomEvent ?? globalThis.CustomEvent;
        documento.dispatchEvent?.(new Evento("ruleta:frenada", {
            detail: { dificultad: dificultadElegida, activacionId: activacionActual?.activacionId },
        }));
    }

    documento.addEventListener("tablero:movimiento-iniciado", evento => {
        const jugador = String(evento.detail.idJugador);
        movimientos.add(jugador);
        llegadasATrivia.delete(jugador);
    });
    documento.addEventListener("tablero:movimiento-finalizado", evento => {
        const jugador = String(evento.detail.idJugador);
        movimientos.delete(jugador);
        if (evento.detail.enTrivia) llegadasATrivia.add(jugador);
        const pendientes = estadosPendientes.filter(estado => String(estado.idJugador) === jugador);
        for (const estado of pendientes) {
            estadosPendientes.splice(estadosPendientes.indexOf(estado), 1);
            activacionesPresentadas.add(estado.activacionId);
            procesarEstadoRuleta(estado);
        }
    });

    documento.addEventListener("trivia:iniciada", () => {
        cancelarPresentacion();
        for (const estado of estadosPendientes) activacionesConTrivia.add(estado.activacionId);
        estadosPendientes.length = 0;
        marcarPresentacionPendiente(false);
        if (activacionActual) activacionesConTrivia.add(activacionActual.activacionId);
        esperandoNuevaActivacion = true;
        revisionEstado++;
        limpiarTemporizador();
        panel.hidden = true;
        contador.hidden = true;
        botonGirar.disabled = true;
        girando = false;
        dificultadElegida = null;
    });

    // Recibe los estados enviados por WebSocket desde partida.js.
    documento.addEventListener("ruleta:estado", evento => {
        revisionEstado++;
        procesarEstadoRuleta(evento.detail);
    });

    if (botonAbrir) {

        botonAbrir.addEventListener("click", () => {

            // Evita reiniciar el contador si el panel ya está abierto.
            if (!panel.hidden) {
                return;
            }

            if (!abrirPanel()) {
                return;
            }

            if (activacionActual) {

                if (activacionActual.estado === "ESPERANDO") {
                    actualizarCuentaRegresiva();
                } else {
                    botonGirar.disabled = true;
                    contador.hidden = true;
                }

                return;
            }

            // La apertura manual sirve solamente como vista previa.
            limpiarEstilosResultado();

            resultado.textContent = "Esperando activación desde Trivia...";
            botonGirar.disabled = true;
            contador.hidden = true;
        });
    }

    botonGirar.addEventListener("click", () => {
        iniciarGiro();
    });

    async function recuperarEstadoActual() {

        if (!codigoPartida) {
            return;
        }

        const revisionAntesDeConsultar = revisionEstado;

        try {

            const estado = await consultar(basePath, codigoPartida);

            // Si recibimos un WebSocket mientras consultábamos,
            // evitamos reemplazarlo con una respuesta anterior.
            if (revisionEstado !== revisionAntesDeConsultar) {
                return;
            }

            // Una ruleta ya sorteada no se vuelve a girar al recargar:
            // su trivia ya empezó (o terminó) y tiene su propio modal.
            if (estado?.estado === "SORTEADA") {
                ultimaActivacionResuelta = estado.activacionId;
                confirmarAlEstarSuscrito(estado.activacionId);
                return;
            }

            if (estado) {
                procesarEstadoRuleta(estado, true);
            }

        } catch (error) {
            console.warn("No se pudo recuperar el estado de la ruleta.", error);
        }
    }

    recuperarEstadoActual();
}

// Inicializa la ruleta solamente cuando existe document.
// Esto evita errores al importar el módulo desde Jasmine en Node.js.
if (typeof document !== "undefined") {

    if (document.readyState === "loading") {

        document.addEventListener("DOMContentLoaded", () => {
            configurarRuleta();
        });

    } else {
        configurarRuleta();
    }
}
