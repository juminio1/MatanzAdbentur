// Determina la ruta base y construye la URL correcta.
const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
const urlParams = new URLSearchParams(window.location.search);
const codigoPartida = urlParams.get("codigo") || window.codigoSala || "";

// Obtiene los elementos del HTML por su ID.
const dado1 = document.getElementById("dado-1");
const dado2 = document.getElementById("dado-2");
const botonTirar = document.getElementById("btn-tirar");

// Evita enviar varias peticiones simultáneas.
let estaTirando = false;

// Registra si el jugador actual ya tiró en este turno.
let yaTiroEnEsteTurno = false;

// Une el frontend con el backend de dados.
async function obtenerResultadoDados() {
    // Obtenemos el ID del jugador desde la variable global de la sesión
    const idJugador = window.usuarioSesionId;

    // Agregamos codigoUnico e idJugador a la URL de la petición
    const respuesta = await fetch(`${basePath}/partida/tirar-dados?codigoUnico=${codigoPartida}&idJugador=${idJugador}`, {
        method: "POST"
    });

    if (!respuesta.ok) {
        throw new Error("No se pudieron tirar los dados");
    }

    return await respuesta.json();
}

// Escucha el clic y ejecuta la tirada.
if (botonTirar) {
    botonTirar.addEventListener("click", async () => {
        if (estaTirando || yaTiroEnEsteTurno || botonTirar.disabled) {
            return;
        }

        estaTirando = true;
        botonTirar.disabled = true;
        let tiradaExitosa = false;

        try {
            const resultado = await obtenerResultadoDados();

            if (dado1) dado1.textContent = resultado.dado1;
            if (dado2) dado2.textContent = resultado.dado2;

            console.log("Resultado completo:", resultado);

            // No permitir otra tirada hasta que cambie el turno.
            yaTiroEnEsteTurno = true;
            tiradaExitosa = true;
        } catch (error) {
            console.error("Error al tirar los dados:", error);
            yaTiroEnEsteTurno = false;
        } finally {
            estaTirando = false;
        }

        if (!tiradaExitosa) {
            botonTirar.disabled = false;
            return;
        }

        // TEMPORAL: finalizar el turno al terminar de tirar los dados.
        // Cuando exista avanzar ficha, esta llamada deberá realizarse después.
        try {
            await finalizarTurno();
        } catch (error) {
            console.error("Falló el cambio de turno:", error);
        }
    });
}

// ELIMINAR cuando esté implementada la funcionalidad de fichas.
const casilleros = document.querySelectorAll(".casillero");
console.log("Casilleros encontrados:", casilleros.length);

// GESTIÓN DE TURNOS: SINCRONIZACIÓN WEBSOCKET.
conectarWebSocketTurnos(basePath, codigoPartida);

function conectarWebSocketTurnos(base, codigo) {
    if (!codigo || typeof SockJS === "undefined" || typeof Stomp === "undefined") {
        return;
    }

    const socket = new SockJS(`${base}/ws`);
    const stompClient = Stomp.over(socket);
    stompClient.debug = null;

    stompClient.connect({}, () => {
        stompClient.subscribe(`/topic/partida/${codigo}/turno`, (mensaje) => {
            const turnoDTO = JSON.parse(mensaje.body);

            actualizarTurnoEnPantalla(turnoDTO);
            // GESTIÓN DE RONDAS: el backend informa la ronda a todos.
            actualizarRondaActual(turnoDTO.rondaActual);
        });
    });
}

function actualizarTurnoEnPantalla(turnoDTO) {
    const idJugadorActual = String(turnoDTO.idJugadorActual);
    const usuarioSesionId = String(window.usuarioSesionId);

    // Si cambia el jugador activo, comienza un nuevo turno.
    if (window.idJugadorTurnoAnterior !== idJugadorActual) {
        yaTiroEnEsteTurno = false;
        window.idJugadorTurnoAnterior = idJugadorActual;
    }

    // Limpiar indicadores de turno previos.
    document.querySelectorAll(".contenedor-turno").forEach((contenedor) => {
        contenedor.innerHTML = "";
    });

    // Mostrar a quién le toca jugar.
    const cardJugador = document.getElementById(idJugadorActual);
    if (cardJugador) {
        const contenedorTurno = cardJugador.querySelector(".contenedor-turno");
        if (contenedorTurno) {
            const span = document.createElement("span");
            span.className = "indicador-turno";
            span.textContent = idJugadorActual === usuarioSesionId
                ? "Tu turno"
                : `Turno de ${turnoDTO.apodoJugadorActual}`;
            contenedorTurno.appendChild(span);
        }
    }

    // Bloquear o habilitar el botón según el jugador actual.
    if (botonTirar && window.usuarioSesionId !== undefined && window.usuarioSesionId !== null) {
        botonTirar.disabled = usuarioSesionId !== idJugadorActual;
    }
}

// GESTIÓN DE RONDAS: solo se actualiza la visualización.
function actualizarRondaActual(rondaActual) {
    const contadorRonda = document.getElementById("ronda-actual");

    if (contadorRonda && rondaActual != null) {
        contadorRonda.textContent = rondaActual;
    }
}

// TEMPORAL: finalizar turno después de tirar dados.
async function finalizarTurno() {
    console.log("Intentando avanzar el turno...");

    const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/avanzar-turno`, {
        method: "POST",
    });

    console.log("Estado HTTP:", respuesta.status);

    if (!respuesta.ok) {
        throw new Error("No se pudo avanzar el turno");
    }

    const turnoDTO = await respuesta.json();

    console.log("Siguiente jugador:", turnoDTO.apodoJugadorActual);
    console.log("Ronda actual:", turnoDTO.rondaActual);

    return turnoDTO;
}
