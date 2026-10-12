document.addEventListener("DOMContentLoaded", () => {
    // ==========================================
    // CÓDIGO DE TU COMPAÑERO: DADOS Y TABLERO
    // ==========================================
    const dado1 = document.getElementById("dado-1");
    const dado2 = document.getElementById("dado-2");
    const botonTirar = document.getElementById("btn-tirar");

    // Une el front con el backend. Conecta con controladorPartida metodo tirar dados.
    async function obtenerResultadoDados() {
        const respuesta = await fetch("http://localhost:8080/spring/partida/tirar-dados", {
            method: "POST",
        });
        if (!respuesta.ok) {
            throw new Error("No se pudieron tirar los dados");
        }
        return await respuesta.json();
    }

    if (botonTirar) {
        botonTirar.addEventListener("click", async () => {
            try {
                const resultado = await obtenerResultadoDados();
                console.log("Resultado completo:", resultado);
                if (dado1) dado1.textContent = resultado.dado1;
                if (dado2) dado2.textContent = resultado.dado2;
            } catch (error) {
                console.error("Error al tirar los dados:", error);
            }
        });
    }

    // Busca todos los elementos del html que tenga la clase casillero
    const casilleros = document.querySelectorAll(".casillero");
    console.log("Casilleros encontrados:", casilleros.length);
    if (casilleros.length > 0) {
        const ficha = document.createElement("div");
        ficha.classList.add("ficha");
        casilleros[0].appendChild(ficha);
    }

    // ==========================================
    // GESTIÓN DE TURNOS: SINCRONIZACIÓN WEBSOCKET
    // ==========================================
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
                span.textContent = (idJugadorActual === usuarioSesionId)
                    ? "Tu turno"
                    : `Turno de ${turnoDTO.apodoJugadorActual}`;
                contenedorTurno.appendChild(span);
            }
        }

        // Bloquear o habilitar el botón de dados según si es el turno del cliente actual
        if (botonTirar && window.usuarioSesionId !== undefined && window.usuarioSesionId !== null) {
            botonTirar.disabled = (usuarioSesionId !== idJugadorActual);
        }
    }
});