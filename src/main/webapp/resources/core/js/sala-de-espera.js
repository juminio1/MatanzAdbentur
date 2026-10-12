
let stompClient = null;

function conectarWebSocket(codigoSala, urlWebSocket) {

    const socket = new SockJS(urlWebSocket);

    stompClient = Stomp.over(socket);

    stompClient.connect({}, function () {

        console.log("Conectado al WebSocket");

        // Actualización de los jugadores de la sala
        stompClient.subscribe(
            "/topic/sala/" + codigoSala,
            function (mensaje) {

                const salaActualizada = JSON.parse(mensaje.body);

                console.log("Sala actualizada:", salaActualizada);

                actualizarJugadores(salaActualizada);
            }
        );

        // Notificación de ficha seleccionada
        stompClient.subscribe(
            "/topic/partida/" + codigoSala,
            function (mensaje) {

                const seleccion = JSON.parse(mensaje.body);

                console.log("Ficha seleccionada:", seleccion);

                const datosSala = document.getElementById("datos-sala");
                const idUsuario = datosSala.dataset.usuarioId;

                if (String(seleccion.idUsuario) === idUsuario) {
                    actualizarSeleccionVisual(seleccion.fichaSeleccionada);
                }
            }
        );

        // Errores al seleccionar ficha
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

    }, function (error) {

        console.error("Error al conectar con WebSocket:", error);

    });
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

// Función auxiliar para actualizar las clases visuales de la ficha activa
function actualizarSeleccionVisual(colorFicha) {
    const botones = document.querySelectorAll(".salaEspera__ficha");

    botones.forEach((btn) => {
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
