
let stompClient = null;
let redirigiendo = false;
let miFichaSeleccionada = null;
let miNombreUsuario;
const AVATAR_POR_DEFECTO = "https://pub-d107f234b4134823bfda878a81c2c3de.r2.dev/default.png";

// CONEXIÓN WEBSOCKET

function conectarWebSocket(codigoSala, urlWebSocket) {
    let ultimaRevision = Number(document.getElementById("datos-sala").dataset.revision);
    const socket = new SockJS(urlWebSocket);

    stompClient = Stomp.over(socket);

    stompClient.connect({}, function () {

        console.log("Conectado al WebSocket");

        // ACTUALIZACIÓN DE JUGADORES Y DISPONIBILIDAD DE FICHAS
        stompClient.subscribe(
            "/topic/sala/" + codigoSala,
            function (mensaje) {
                const salaActualizada = JSON.parse(mensaje.body);
                const revision = salaActualizada.revision;
                if (!Number.isSafeInteger(revision) || revision <= ultimaRevision) return;
                ultimaRevision = revision;

                miFichaSeleccionada = salaActualizada.fichasAsignadas?.[miNombreUsuario] ?? null;
                actualizarJugadores(salaActualizada);
                actualizarDisponibilidadFichas(salaActualizada); // <-- Actualiza todas las fichas
            }
        );

        stompClient.subscribe(
            "/topic/partida/" + codigoSala + "/errores/" + document.getElementById("datos-sala").dataset.usuarioId,
            mensaje => mostrarAvisoFicha(mensaje.body)
        );

        // INICIO DE PARTIDA
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

    if (!lista || !contador) return;

    const usuarios = salaActualizada.usernames;
    if (!Array.isArray(usuarios)) {
        console.error("La lista de jugadores no es válida");
        return;
    }

    const fichasAsignadas = salaActualizada.fichasAsignadas || {};
    const avatares = salaActualizada.avatares || {};
    contador.textContent = usuarios.length + "/4";
    lista.replaceChildren();

    usuarios.forEach(function (username) {
        const colorFicha = fichasAsignadas[username]; // Ej: "ROJO", "VERDE" o undefined

        const jugador = document.createElement("div");
        jugador.className = "salaEspera__jugador";

        // Avatar: la imagen que el usuario eligió en su perfil
        const avatar = document.createElement("div");
        avatar.className = "avatar";
        const imagenAvatar = document.createElement("img");
        imagenAvatar.src = avatares[username] || AVATAR_POR_DEFECTO;
        imagenAvatar.alt = `Avatar de ${username}`;
        avatar.appendChild(imagenAvatar);

const info = document.createElement("div");
        info.className = "salaEspera__jugadorInfo";

        const nombreWrapper = document.createElement("div");
        nombreWrapper.className = "salaEspera__nombre";

        const nombre = document.createElement("strong");
        nombre.textContent = username;
        nombreWrapper.appendChild(nombre);

        // Si es el creador de la sala, le agregamos la corona dorada SVG
        if (salaActualizada.creador && username === salaActualizada.creador) {
            const corona = document.createElementNS("http://www.w3.org/2000/svg", "svg");
            corona.setAttribute("class", "corona");
            corona.setAttribute("width", "16");
            corona.setAttribute("height", "16");
            corona.setAttribute("viewBox", "0 0 24 24");
            corona.setAttribute("fill", "#f59e0b");
            corona.setAttribute("stroke", "#d97706");
            corona.setAttribute("stroke-width", "1.2");
            corona.setAttribute("title", "Anfitrión de la sala");
            corona.innerHTML = '<path d="M2 4l3 12h14l3-12-6 7-4-7-4 7-6-7zm3 16h14v2H5v-2z"/>';
            nombreWrapper.appendChild(corona);
        }

        info.appendChild(nombreWrapper);

        // Estado: verde con "Listo" si eligió ficha, gris con "Sin ficha" si aún no eligió
        const listo = document.createElement("div");
        listo.className = "salaEspera__listo";

        const punto = document.createElement("span");
        if (!colorFicha) {
            punto.style.background = "#8b96a4";
            listo.style.color = "#8b96a4";
        }

        listo.appendChild(punto);
        listo.appendChild(document.createTextNode(colorFicha ? "Listo" : "Sin ficha"));

        info.appendChild(listo);

        // INDICADOR DE FICHA (muestra el círculo de color o el ?)
        const badgeFicha = document.createElement("div");
        badgeFicha.className = "salaEspera__jugadorFicha";

        if (colorFicha) {
            badgeFicha.classList.add(`ficha-${colorFicha.toLowerCase()}`, `salaEspera__jugadorFicha--${colorFicha.toLowerCase()}`);
            badgeFicha.title = `Ficha elegida: ${colorFicha}`;
        } else {
            badgeFicha.classList.add("ficha-vacia", "salaEspera__jugadorFicha--vacia");
            badgeFicha.textContent = "?";
            badgeFicha.title = "Aún no seleccionó ficha";
        }

        jugador.appendChild(avatar);
        jugador.appendChild(info);
        jugador.appendChild(badgeFicha);

        lista.appendChild(jugador);
    });

    // ESPACIOS VACÍOS DE LA SALA
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

        const badgeVacio = document.createElement("div");
        badgeVacio.className = "salaEspera__jugadorFicha salaEspera__jugadorFicha--vacia";
        badgeVacio.style.visibility = "hidden";

        vacio.appendChild(avatar);
        vacio.appendChild(info);
        vacio.appendChild(badgeVacio);

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

    const miFila = document.querySelector('#lista-jugadores [data-jugador-id="' + datosSala.dataset.usuarioId + '"]');
    miNombreUsuario = miFila?.dataset.username;
    const jugadoresConFicha = [...document.querySelectorAll("#lista-jugadores [data-ficha-inicial]")];
    miFichaSeleccionada = jugadoresConFicha.find(
        jugador => jugador.dataset.jugadorId === datosSala.dataset.usuarioId
    )?.dataset.fichaInicial ?? null;
    actualizarDisponibilidadFichas({
        fichasOcupadas: jugadoresConFicha.map(jugador => jugador.dataset.fichaInicial)
    });
    conectarWebSocket(codigoSala, urlWebSocket);
});

// SELECCIONAR FICHA

function seleccionarFicha(colorFicha) {
    console.log("Clic en ficha:", colorFicha);

    // 1. Buscar el elemento por data-ficha (más permisivo)
    const boton = document.querySelector(`[data-ficha="${colorFicha}"]`);
    if (boton?.getAttribute("aria-disabled") === "true" || miFichaSeleccionada === colorFicha) {
        mostrarAvisoFicha("La ficha ya se encuentra seleccionada");
        console.warn("Ficha bloqueada, no se puede seleccionar:", colorFicha);
        return;
    }

    // 2. Verificar conexión STOMP
    if (!stompClient || !stompClient.connected) {
        mostrarAvisoFicha("Esperá a que se conecte la sala y volvé a elegir tu ficha.");
        return;
    }

    const datosSala = document.getElementById("datos-sala");
    if (!datosSala) {
        console.error("No se encontró el elemento #datos-sala");
        return;
    }

    const codigoSala = datosSala.dataset.codigo;
    const idUsuario = datosSala.dataset.usuarioId;

    const seleccion = {
        codigoUnicoPartida: codigoSala,
        idUsuario: Number(idUsuario),
        fichaSeleccionada: colorFicha
    };

    console.log("Enviando selección al servidor:", seleccion);

    // 3. Enviar al WebSocket
    mostrarAvisoFicha("");
    stompClient.send("/app/seleccionar-ficha", {}, JSON.stringify(seleccion));
}

function mostrarAvisoFicha(mensaje) {
    const aviso = document.getElementById("mensajeFichaError");
    aviso.textContent = mensaje;
    aviso.hidden = !mensaje;
}

function actualizarDisponibilidadFichas(salaActualizada) {
    const fichasOcupadas = salaActualizada.fichasOcupadas || [];
    const botones = document.querySelectorAll(".salaEspera__ficha");

    botones.forEach(function (boton) {
        const color = boton.getAttribute("data-ficha");
        const check = boton.querySelector(".salaEspera__check");
        const estaOcupada = fichasOcupadas.includes(color);
        const esMia = (miFichaSeleccionada === color);

        // Limpiamos estados con la clase exacta de tu CSS
        boton.classList.remove("salaEspera__ficha--ocupada", "salaEspera__ficha--seleccionada");
        boton.removeAttribute("aria-disabled");
        boton.removeAttribute("title");
        if (check) check.style.display = "none";

        if (esMia) {
            // Tu ficha elegida
            boton.classList.add("salaEspera__ficha--seleccionada");
            if (check) check.style.display = "flex";
        } else if (estaOcupada) {
            // Ficha tomada por otro jugador (usa tu clase .salaEspera__ficha--ocupada)
            boton.classList.add("salaEspera__ficha--ocupada");
            boton.setAttribute("aria-disabled", "true");
            boton.title = "Ficha seleccionada por otro jugador";
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
