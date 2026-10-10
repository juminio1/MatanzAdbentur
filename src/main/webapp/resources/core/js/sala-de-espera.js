
let stompClient = null;

function conectarWebSocket(codigoSala, urlWebSocket) {

  const socket = new SockJS(urlWebSocket);

  stompClient = Stomp.over(socket);

  stompClient.connect({}, function () {

    console.log("Conectado al WebSocket");

    stompClient.subscribe(
      "/topic/sala/" + codigoSala,
      function (mensaje) {

        const salaActualizada = JSON.parse(mensaje.body);

        console.log("Sala actualizada:", salaActualizada);

        actualizarJugadores(salaActualizada);
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

// Función para enviar la selección de ficha al servidor (Spring MVC)
function seleccionarFicha(colorFicha) {
    const errorContainer = document.getElementById("mensajeFichaError");

    // Ocultar mensaje de error previo
    if (errorContainer) {
        errorContainer.setAttribute("hidden", "true");
        errorContainer.innerText = "";
    }

    // Petición al endpoint del Controlador de Spring
    fetch("/spring/sala-de-espera/elegir-ficha", {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
        },
        body: new URLSearchParams({
            ficha: colorFicha,
        }),
    })
        .then((response) => {
            if (!response.ok) {
                return response.text().then((text) => {
                    throw new Error(text || "La ficha seleccionada no está disponible.");
                });
            }
            // En lugar de response.json(), leemos como texto o vacío ya que el backend responde OK
            return response.text();
        })
        .then((data) => {
            // Si el backend responde bien, actualizamos la ficha seleccionada en la vista
            actualizarSeleccionVisual(colorFicha);
        })
        .catch((error) => {
            // Mostrar cartel de error usando el atributo hidden
            if (errorContainer) {
                errorContainer.innerText = error.message;
                errorContainer.removeAttribute("hidden");
            }
        });
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
