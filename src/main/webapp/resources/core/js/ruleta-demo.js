
import { Wheel } from "./vendor/spin-wheel-esm.js";
import { solicitarSorteoPartida, consultarEstadoRuleta } from "./servicio-ruleta.js";
import { calcularSegundosRestantes } from "./temporizador-ruleta.js";

import { crearRuleta, girarRuleta } from "./ruleta.js";

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
    consultar = consultarEstadoRuleta
) {

    const botonAbrir = documento.getElementById("btn-abrir-ruleta");
    const botonGirar = documento.getElementById("btn-girar-ruleta");
    const botonCerrar = documento.getElementById("btn-cerrar-ruleta");

    const panel = documento.getElementById("panel-ruleta");
    const contenedor = documento.getElementById("ruleta-visual");
    const resultado = documento.getElementById("ruleta-resultado");
    const contador = documento.getElementById("ruleta-temporizador");

    if (!botonGirar || !botonCerrar || !panel || !contenedor || !resultado || !contador) {
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
            botonCerrar.disabled = false;

            return false;
        }
    }

    function abrirPanel() {

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

        activacionActual = estado;
        dificultadElegida = null;
        girando = false;
        giroCompletado = false;
        animacionIniciada = false;
        solicitudEnCurso = false;

        limpiarEstilosResultado();

        if (!abrirPanel()) {
            return;
        }

        resultado.textContent = esJugadorHabilitado()
            ? "¡Probá tu suerte!"
            : "Esperando que el jugador habilitado gire la ruleta...";

        botonCerrar.disabled = true;

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
        solicitudEnCurso = false;
        girando = true;
        giroCompletado = true;
        dificultadElegida = estado.dificultad;

        botonGirar.disabled = true;
        botonCerrar.disabled = true;
        contador.hidden = true;

        limpiarEstilosResultado();

        if (!abrirPanel()) {
            girando = false;
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
            botonCerrar.disabled = false;
        }
    }

    function procesarEstadoRuleta(estado) {

        if (!estado || !estado.activacionId || !codigoPartida) {
            return;
        }

        if (estado.codigoPartida !== codigoPartida) {
            return;
        }

        if (estado.estado !== "ESPERANDO" && estado.estado !== "SORTEADA") {
            return;
        }

        const mismaActivacion = activacionActual?.activacionId === estado.activacionId;

        // No volvemos a mostrar una activación ya resuelta.
        if (!mismaActivacion && ultimaActivacionResuelta === estado.activacionId) {
            return;
        }

        // Una respuesta atrasada no puede retroceder de SORTEADA a ESPERANDO.
        if (mismaActivacion && activacionActual.estado === "SORTEADA" && estado.estado === "ESPERANDO") {
            return;
        }

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
        botonCerrar.disabled = true;

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
        botonCerrar.disabled = false;
    }

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
                    botonCerrar.disabled = true;
                } else {
                    botonGirar.disabled = true;
                    botonCerrar.disabled = false;
                    contador.hidden = true;
                }

                return;
            }

            // La apertura manual sirve solamente como vista previa.
            limpiarEstilosResultado();

            resultado.textContent = "Esperando activación desde Trivia...";
            botonGirar.disabled = true;
            botonCerrar.disabled = false;
            contador.hidden = true;
        });
    }

    botonGirar.addEventListener("click", () => {
        iniciarGiro();
    });

    botonCerrar.addEventListener("click", () => {

        if (!girando && !botonCerrar.disabled) {

            limpiarTemporizador();

            panel.hidden = true;
            contador.hidden = true;
        }
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

            if (estado) {
                procesarEstadoRuleta(estado);
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
