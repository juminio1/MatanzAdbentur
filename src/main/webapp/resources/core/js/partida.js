//Obtiene los elementos del HTML, lo buscan por id
const dado1 = document.getElementById("dado-1");
const dado2 = document.getElementById("dado-2");
const botonTirar = document.getElementById("btn-tirar");

// Evita enviar varias peticiones simultáneas.
let estaTirando = false;

// Registra si el jugador actual ya tiró en este turno.
let yaTiroEnEsteTurno = false;

// Une el front con el backend.
async function obtenerResultadoDados() {
    //determina la ruta base, utiliza el prefijo /spring y construye la URL correcta
    const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
    //Envia la peticion HTTP
    const respuesta = await fetch(`${basePath}/partida/tirar-dados`, { method: "POST" });

    //Comprueba si hubo un error HTTP
    if (!respuesta.ok) {
        throw new Error("No se pudieron tirar los dados");
    }
    //Convierte la respuesta a JSON
    return await respuesta.json();
}

//Escucha el Click y ejecuta la tirada.
if (botonTirar) {
    botonTirar.addEventListener("click", async () => {
        //La tirada no comienza si se cumple alguna de estas condiciones
        if (estaTirando || yaTiroEnEsteTurno || botonTirar.disabled) {
            return;
        }

        //bloquea mientras se procesa
        estaTirando = true;
        botonTirar.disabled = true;

        try {
            const resultado = await obtenerResultadoDados();
            //Se actualiza y visualiza el nro de c/dado
            if (dado1) dado1.textContent = resultado.dado1;
            if (dado2) dado2.textContent = resultado.dado2;

            console.log("Resultado completo:", resultado);

            // No permitir otra tirada hasta que cambie el turno.
            yaTiroEnEsteTurno = true;
        } catch (error) {
            console.error("Error al tirar los dados:", error);
        } finally {
            estaTirando = false;

            // Si falló la petición, permitir reintentar,
            // siempre que el botón no estuviera bloqueado por el turno.
            if (!yaTiroEnEsteTurno) {
                botonTirar.disabled = false;
            }
        }
    });
}

//ELIMINAR. CUANDO ESTE HECHO ELEGIR FICHA Y FICHA.
// Busca todos los elementos del html que tenga la clase casillero
const casilleros = document.querySelectorAll(".casillero");
console.log("Casilleros encontrados:", casilleros.length);
Se;
// GESTIÓN DE TURNOS: SINCRONIZACIÓN WEBSOCKET
const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
const urlParams = new URLSearchParams(window.location.search);
const codigoPartida = urlParams.get("codigo") || window.codigoSala || "";

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
    // Limpiar indicadores de turno previos en las tarjetas
    document.querySelectorAll(".contenedor-turno").forEach((contenedor) => {
        contenedor.innerHTML = "";
    });

    // Insertar cartel en el jugador activo
    const cardJugador = document.getElementById(idJugadorActual);
    if (cardJugador) {
        const contenedorTurno = cardJugador.querySelector(".contenedor-turno");
        if (contenedorTurno) {
            const span = document.createElement("span");
            span.className = "indicador-turno";
            span.textContent =
                idJugadorActual === usuarioSesionId
                    ? "Tu turno"
                    : `Turno de ${turnoDTO.apodoJugadorActual}`;
            contenedorTurno.appendChild(span);
        }
    }

    // Bloquear o habilitar el botón de dados según si es el turno del cliente actual
    if (botonTirar && window.usuarioSesionId !== undefined && window.usuarioSesionId !== null) {
        botonTirar.disabled = usuarioSesionId !== idJugadorActual;
    }
}
