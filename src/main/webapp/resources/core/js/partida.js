
// DADOS

const dado1 = document.getElementById("dado-1");
const dado2 = document.getElementById("dado-2");
const botonTirar = document.getElementById("btn-tirar");

console.log("PARTIDA.JS ACTUALIZADO");

// Une el front con el backend para tirar dados.
async function obtenerResultadoDados() {

    const respuesta = await fetch("http://localhost:8080/spring/partida/tirar-dados", {
            method: "POST"
        }
    );

    if (!respuesta.ok) {
        throw new Error("No se pudieron tirar los dados");
    }

    return await respuesta.json();
}

botonTirar.addEventListener("click", async () => {

    try {
        const resultado = await obtenerResultadoDados();

        console.log("Resultado completo:", resultado);

        dado1.textContent = resultado.dado1;
        dado2.textContent = resultado.dado2;

    } catch (error) {
        console.error("Error al tirar los dados:", error);
    }
});


// CASILLEROS

const casilleros = document.querySelectorAll(".casillero");

console.log("Casilleros encontrados:", casilleros.length);


// WEBSOCKETS DE PARTIDA

let stompClientPartida = null;

function conectarWebSocketPartida(codigoSala, urlWebSocket) {

    const socket = new SockJS(urlWebSocket);

    stompClientPartida = Stomp.over(socket);

    stompClientPartida.connect({}, function () {

        console.log("Conectado al WebSocket de partida");

        // Escuchamos las posiciones de todas las fichas.

        stompClientPartida.subscribe(
            "/topic/partida/" + codigoSala + "/posiciones",
            function (mensaje) {

                const posiciones = JSON.parse(mensaje.body);

                console.log("Posiciones recibidas:", posiciones);

                actualizarPosicionesFichas(posiciones);
            }
        );

        // Solicitamos el estado después de suscribirnos.
        stompClientPartida.send(
            "/app/partida/" + codigoSala + "/solicitar-posiciones",
            {},
            ""
        );

    }, function (error) {

        console.error(
            "Error al conectar WebSocket de partida:",
            error
        );
    });
}


// ACTUALIZAR POSICIONES DE LAS FICHAS

function actualizarPosicionesFichas(posiciones) {

    if (!Array.isArray(posiciones)) {
        console.error("Las posiciones recibidas no son válidas");
        return;
    }

    // Eliminamos las fichas anteriores para evitar duplicados.
    document.querySelectorAll(".fichas-en-casillero .ficha")
        .forEach(function (ficha) {
            ficha.remove();
        });

    posiciones.forEach(function (jugador) {

        const casilleroDestino = document.querySelector(
            `.casillero[data-casillero="${jugador.posicion}"]`
        );

        if (!casilleroDestino) {
            console.error(
                "No existe el casillero:",
                jugador.posicion
            );
            return;
        }

        const contenedor = casilleroDestino.querySelector(
            ".fichas-en-casillero"
        );

        if (!contenedor) {
            return;
        }

        const ficha = document.createElement("div");

        ficha.classList.add(
            "ficha",
            "ficha-" + jugador.color.toLowerCase()
        );

        ficha.dataset.jugadorId = jugador.idJugador;
        ficha.title = jugador.apodo;

        contenedor.appendChild(ficha);
    });
}


// INICIALIZAR WEBSOCKET

document.addEventListener("DOMContentLoaded", function () {

    const datosPartida = document.getElementById("datos-partida");

    if (!datosPartida) {
        console.error("No se encontraron los datos de partida");
        return;
    }

    const codigoSala = datosPartida.dataset.codigo;
    const urlWebSocket = datosPartida.dataset.wsUrl;

    if (!codigoSala || !urlWebSocket) {
        console.error("Falta el código o la URL del WebSocket");
        return;
    }

    conectarWebSocketPartida(codigoSala, urlWebSocket);
});
