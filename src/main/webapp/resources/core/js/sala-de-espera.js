let stompClient = null;

function conectarWebSocket(codigoSala, urlWebSocket) {
    const socket = new SockJS(urlWebSocket);

    stompClient = Stomp.over(socket);

    stompClient.connect(
        {},
        function () {
            console.log("Conectado al WebSocket");

            // 1. Suscripción para actualizar la lista de jugadores
            stompClient.subscribe("/topic/sala/" + codigoSala, function (mensaje) {
                const salaActualizada = JSON.parse(mensaje.body);

                console.log("Sala actualizada:", salaActualizada);

                actualizarJugadores(salaActualizada);
            });

            // 2. Suscripción para escuchar cuando se inicia la partida y redirigir
            stompClient.subscribe("/topic/sala/" + codigoSala + "/iniciar", function (mensaje) {
                if (mensaje.body === "INICIAR") {
                    console.log("¡La partida ha comenzado! Redirigiendo al tablero...");
                    window.location.href = window.location.origin + "/spring/partida";
                }
            });
        },
        function (error) {
            console.error("Error al conectar con WebSocket:", error);
        },
    );
}

function actualizarJugadores(salaActualizada) {
    const lista = document.getElementById("lista-jugadores");

    const contador = document.getElementById("contador-jugadores");

    if (!lista || !contador) {
        return;
    }

    const usuarios = salaActualizada.usernames;

    contador.textContent = usuarios.length + "/4";

    lista.replaceChildren();

    usuarios.forEach(function (username) {
        const jugador = document.createElement("div");
        jugador.className = "salaEspera__jugador";

        const avatar = document.createElement("div");
        avatar.className = "avatar avatar--azul";

        avatar.textContent = username.charAt(0).toUpperCase();

        const info = document.createElement("div");
        info.className = "salaEspera__jugadorInfo";

        const nombre = document.createElement("strong");
        nombre.textContent = username;

        const listo = document.createElement("div");
        listo.className = "salaEspera__listo";

        const punto = document.createElement("span");

        listo.appendChild(punto);
        listo.appendChild(document.createTextNode("Listo"));

        info.appendChild(nombre);
        info.appendChild(listo);

        const menu = document.createElement("button");
        menu.className = "salaEspera__menu";
        menu.type = "button";
        menu.textContent = "⋮";

        jugador.appendChild(avatar);
        jugador.appendChild(info);
        jugador.appendChild(menu);

        lista.appendChild(jugador);
    });

    for (let i = usuarios.length; i < 4; i++) {
        const vacio = document.createElement("div");
        vacio.className = "salaEspera__jugador salaEspera__jugador--vacio";

        const avatar = document.createElement("div");
        avatar.className = "avatar avatar--vacio";

        const info = document.createElement("div");
        info.className = "salaEspera__jugadorInfo";

        const texto = document.createElement("strong");
        texto.textContent = "Esperando jugador...";

        const detalle = document.createElement("small");
        detalle.textContent = "Esperando que alguien se una";

        info.appendChild(texto);
        info.appendChild(detalle);

        const menu = document.createElement("button");
        menu.className = "salaEspera__menu";
        menu.type = "button";

        vacio.appendChild(avatar);
        vacio.appendChild(info);
        vacio.appendChild(menu);

        lista.appendChild(vacio);
    }
}

// Único bloque DOMContentLoaded unificado y optimizado
document.addEventListener("DOMContentLoaded", function () {
    const datosSala = document.getElementById("datos-sala");

    if (!datosSala) {
        console.error("No se encontraron los datos de la sala");
        return;
    }

    const codigoSala = datosSala.dataset.codigo;
    const urlWebSocket = datosSala.dataset.wsUrl;

    if (!codigoSala || !urlWebSocket) {
        console.error("Falta el código o la URL del WebSocket");
        return;
    }

    // 1. Conectamos una sola vez al WebSocket
    conectarWebSocket(codigoSala, urlWebSocket);

    // 2. Interceptamos el formulario de inicio para evitar la recarga de página por POST tradicional
    const formIniciar = document.getElementById("form-iniciar");
    if (formIniciar) {
        formIniciar.addEventListener("submit", function (e) {
            e.preventDefault();

            const codigoUnicoInput = document.getElementById("codigo-unico-input");
            const codigoUnico = codigoUnicoInput ? codigoUnicoInput.value : codigoSala;

            fetch(formIniciar.action, {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded",
                },
                body: "codigoUnico=" + encodeURIComponent(codigoUnico),
            })
                .then((response) => {
                    if (!response.ok) {
                        console.error("Error al iniciar la partida");
                    } else {
                        console.log("Petición de inicio enviada con éxito. Esperando WebSocket...");
                    }
                })
                .catch((error) => {
                    console.error("Error en la petición:", error);
                });
        });
    }
});
