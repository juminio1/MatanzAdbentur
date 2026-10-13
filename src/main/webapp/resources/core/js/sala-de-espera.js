
let stompClient = null;
let redirigiendo = false;

// CONEXIÓN WEBSOCKET

function conectarWebSocket(codigoSala, urlWebSocket) {
    const socket = new SockJS(urlWebSocket);

    stompClient = Stomp.over(socket);

    stompClient.connect({}, function () {

        console.log("Conectado al WebSocket");

        // ACTUALIZACIÓN DE JUGADORES

        stompClient.subscribe(
            "/topic/sala/" + codigoSala,
            function (mensaje) {

                const salaActualizada = JSON.parse(mensaje.body);

                console.log("Sala actualizada:", salaActualizada);

                actualizarJugadores(salaActualizada);
            }
        );

        // SELECCIÓN DE FICHAS

        stompClient.subscribe(
            "/topic/partida/" + codigoSala,
            function (mensaje) {

                const seleccion = JSON.parse(mensaje.body);

                console.log("Ficha seleccionada:", seleccion);

                const datosSala = document.getElementById("datos-sala");

                if (!datosSala) {
                    return;
                }

                const idUsuario = datosSala.dataset.usuarioId;

                if (String(seleccion.idUsuario) === idUsuario) {
                    actualizarSeleccionVisual(seleccion.fichaSeleccionada);
                }
            }
        );

        // ERRORES AL SELECCIONAR FICHA

        stompClient.subscribe(
            "/topic/partida/" + codigoSala + "/errores",
            function (mensaje) {

                const errorContainer =
                    document.getElementById("mensajeFichaError");

                if (errorContainer) {
                    errorContainer.textContent = mensaje.body;
                    errorContainer.hidden = false;
                }
            }
        );

        // INICIO DE PARTIDA
        // Cuando el creador inicia la partida,
        // todos los jugadores son redirigidos al tablero.

        stompClient.subscribe(
            "/topic/sala/" + codigoSala + "/iniciar",
            function (mensaje) {

                if (mensaje.body === "INICIAR") {

                    console.log("La partida comenzó");

                    redirigirAlTablero(codigoSala, urlWebSocket);
                }
            }
        );

    }, function (error) {

        console.error("Error al conectar con WebSocket:", error);

    });
}

// REDIRECCIÓN AL TABLERO

function redirigirAlTablero(codigoSala, urlWebSocket) {

    if (redirigiendo) {
        return;
    }

    redirigiendo = true;

    const rutaBase = urlWebSocket.replace(/\/ws\/?$/, "");

    window.location.href =
        rutaBase + "/partida?codigo=" + encodeURIComponent(codigoSala);
}

// ACTUALIZACIÓN DE JUGADORES

function actualizarJugadores(salaActualizada) {

    const lista = document.getElementById("lista-jugadores");
    const contador = document.getElementById("contador-jugadores");

    if (!lista || !contador) {
        return;
    }

    const usuarios = salaActualizada.usernames;

    if (!Array.isArray(usuarios)) {
        console.error("La lista de jugadores no es válida");
        return;
    }

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

    // ESPACIOS VACÍOS DE LA SALA

    for (let i = usuarios.length; i < 4; i++) {

        const vacio = document.createElement("div");

        vacio.className =
            "salaEspera__jugador salaEspera__jugador--vacio";

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

// INICIALIZAR WEBSOCKET AL CARGAR LA PÁGINA

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

    conectarWebSocket(codigoSala, urlWebSocket);
});

// SELECCIONAR FICHA

function seleccionarFicha(colorFicha) {

    const errorContainer = document.getElementById("mensajeFichaError");

    if (errorContainer) {
        errorContainer.hidden = true;
        errorContainer.textContent = "";
    }

    if (!stompClient || !stompClient.connected) {

        if (errorContainer) {
            errorContainer.textContent = "No hay conexión con el servidor.";
            errorContainer.hidden = false;
        }

        return;
    }

    const datosSala = document.getElementById("datos-sala");

    if (!datosSala) {
        console.error("No se encontraron los datos de la sala");
        return;
    }

    const codigoSala = datosSala.dataset.codigo;
    const idUsuario = datosSala.dataset.usuarioId;

    const seleccion = {
        codigoUnicoPartida: codigoSala,
        idUsuario: Number(idUsuario),
        fichaSeleccionada: colorFicha
    };

    stompClient.send(
        "/app/seleccionar-ficha",
        { "content-type": "application/json" },
        JSON.stringify(seleccion)
    );
}

// ACTUALIZAR SELECCIÓN VISUAL DE LA FICHA

function actualizarSeleccionVisual(colorFicha) {

    const botones = document.querySelectorAll(".salaEspera__ficha");

    botones.forEach(function (btn) {

        const check = btn.querySelector(".salaEspera__check");

        btn.classList.remove("salaEspera__ficha--seleccionada");

        if (check) {
            check.style.display = "none";
        }

        if (btn.getAttribute("data-ficha") === colorFicha) {

            btn.classList.add("salaEspera__ficha--seleccionada");

            if (check) {
                check.style.display = "flex";
            }
        }
    });
}

document.addEventListener("DOMContentLoaded", function () {
    const btnCopiar = document.querySelector(".salaEspera__copiar");
    const datosSala = document.getElementById("datos-sala");

    if (btnCopiar && datosSala) {
        btnCopiar.addEventListener("click", function () {
            // Obtenemos el código directamente del atributo data-codigo
            const codigo = datosSala.getAttribute("data-codigo");

            if (!codigo) return;

            // Usamos la API del portapapeles
            navigator.clipboard.writeText(codigo).then(() => {
                // Feedback visual rápido
                const textoOriginal = btnCopiar.textContent;
                btnCopiar.textContent = "Copiado";
                btnCopiar.style.backgroundColor = "#22c55e"; // Verde éxito
                btnCopiar.style.color = "#fff";

                // Lo volvemos a la normalidad a los 2 segundos
                setTimeout(() => {
                    btnCopiar.textContent = textoOriginal;
                    btnCopiar.style.backgroundColor = "";
                    btnCopiar.style.color = "";
                }, 2000);
            }).catch(err => {
                console.error("Error al copiar al portapapeles:", err);
            });
        });
    }
});
