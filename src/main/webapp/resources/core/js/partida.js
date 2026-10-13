document.addEventListener("DOMContentLoaded", () => {
    const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";

    const urlParams = new URLSearchParams(window.location.search);
    const codigoPartida = urlParams.get("codigo") || window.codigoSala || "";

    const dado1 = document.getElementById("dado-1");
    const dado2 = document.getElementById("dado-2");
    const botonTirar = document.getElementById("btn-accion-turno");

    // Conecta el frontend con el controlador que tira los dados.
    /*async function obtenerResultadoDados() {
        const respuesta = await fetch(`${basePath}/partida/tirar-dados`, {
            method: "POST",
        });

        if (!respuesta.ok) {
            throw new Error("No se pudieron tirar los dados");
        }

        return await respuesta.json();
    }*/

    //HARCODEO DE DADOS
    async function obtenerResultadoDados() { 
    return {
        dado1: Math.floor(Math.random() * 6) + 1,
        dado2: Math.floor(Math.random() * 6) + 1
    };
}

    if (botonTirar) {
    botonTirar.addEventListener("click", async () => {

        botonTirar.disabled = true;

        try {
            const resultado = await obtenerResultadoDados();

            console.log("Resultado completo:", resultado);

            if (dado1) {
                dado1.textContent = resultado.dado1;
            }

            if (dado2) {
                dado2.textContent = resultado.dado2;
            }

            // Temporalmente finalizamos el turno después de tirar dados
            await finalizarTurno();

        } catch (error) {
            console.error("Error al ejecutar el turno:", error);
            botonTirar.disabled = false;
        }
    });
}

    // Busca los casilleros del tablero.
    const casilleros = document.querySelectorAll(".casillero");

    console.log("Casilleros encontrados:", casilleros.length);

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

                // Actualiza el jugador al que le toca.
                actualizarTurnoEnPantalla(turnoDTO);

                // Actualiza la ronda para todos los clientes.
                actualizarRondaActual(turnoDTO.rondaActual);
            });
        });
    }

    function actualizarTurnoEnPantalla(turnoDTO) {
        const idJugadorActual = String(turnoDTO.idJugadorActual);
        const usuarioSesionId = String(window.usuarioSesionId);

        // Limpiar indicadores de turno anteriores.
        document.querySelectorAll(".contenedor-turno").forEach((contenedor) => {
            contenedor.innerHTML = "";
        });

        // Mostrar qué jugador tiene el turno.
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

        // Habilitar el botón únicamente al jugador actual.
        if (botonTirar && window.usuarioSesionId != null) {
            botonTirar.disabled = usuarioSesionId !== idJugadorActual;
        }
    }

    function actualizarRondaActual(rondaActual) {
        const contadorRonda = document.getElementById("ronda-actual");

        if (contadorRonda && rondaActual != null) {
            contadorRonda.textContent = rondaActual;
        }
    }

    //El turno termina después de tirar los dados
   async function finalizarTurno() {
    console.log("Intentando avanzar el turno...");

    const respuesta = await fetch(
        `${basePath}/partida/${codigoPartida}/avanzar-turno`,
        {
            method: "POST"
        }
    );

    console.log("Estado HTTP:", respuesta.status);

    if (!respuesta.ok) {
        throw new Error("No se pudo avanzar el turno");
    }

    const turnoDTO = await respuesta.json();

    console.log("Siguiente jugador:", turnoDTO.apodoJugadorActual);
    console.log("Ronda actual:", turnoDTO.rondaActual);

    return turnoDTO;
}
});
