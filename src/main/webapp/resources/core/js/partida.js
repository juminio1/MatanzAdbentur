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
    const respuesta = await fetch(
        `${basePath}/partida/tirar-dados?codigoUnico=${codigoPartida}&idJugador=${idJugador}`,
        {
            method: "POST",
        },
    );

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

            // El movimiento de la ficha lo realiza el WebSocket.
            console.log("Nueva posición:", resultado.nuevaPosicion);

            tiradaExitosa = true;
        } catch (error) {
            console.error("Error al tirar los dados o mover la ficha:", error);
        } finally {
            estaTirando = false;
        }

        if (!tiradaExitosa) {
            if (!yaTiroEnEsteTurno) {
                botonTirar.disabled = false;
            }
            return;
        }

        // Finalizar el turno después de mover la ficha.
        try {
            await finalizarTurno();
        } catch (error) {
            console.error("Falló el cambio de turno:", error);
        }
    });
}

// Busca la ficha del jugador y el casillero al que debe avanzar.
function moverFichaEnTablero(idJugador, nuevaPosicion) {
    const ficha = document.querySelector(`.ficha[data-jugador-id="${idJugador}"]`);
    const casilleroDestino = document.querySelector(
        `.casillero[data-casillero="${nuevaPosicion}"] .fichas-en-casillero`,
    );

    // Verifica que existan la ficha y el casillero destino.
    if (!ficha || !casilleroDestino) {
        console.error("No se encontró la ficha o el casillero destino.", {
            idJugador,
            nuevaPosicion,
            fichaEncontrada: Boolean(ficha),
            casilleroEncontrado: Boolean(casilleroDestino),
        });
        return false;
    }
    //Mueve la ficha al nuevo casillero en el HTML.
    casilleroDestino.appendChild(ficha);
    return true;
}

async function animarMovimientoFicha(idJugador, cantidadPasos) {
    const ficha = document.querySelector(`.ficha[data-jugador-id="${idJugador}"]`);

    if (!ficha) {
        console.error("No se encontró la ficha del jugador:", idJugador);
        return;
    }

    // Busca el casillero donde está actualmente la ficha.
    const casilleroActual = ficha.closest(".casillero");

    if (!casilleroActual) {
        console.error("La ficha no está dentro de un casillero.");
        return;
    }

    // Obtiene los casilleros en el orden en que aparecen en el tablero.
    const casilleros = Array.from(document.querySelectorAll(".casillero[data-casillero]"));

    let indiceActual = casilleros.indexOf(casilleroActual);

    if (indiceActual === -1 || casilleros.length === 0) {
        console.error("No se pudo determinar la posición de la ficha.");
        return;
    }

    // Avanza un casillero por vez.
    for (let paso = 0; paso < cantidadPasos; paso++) {
        indiceActual = (indiceActual + 1) % casilleros.length;

        const siguienteCasillero = casilleros[indiceActual];
        const posicionDestino = siguienteCasillero.dataset.casillero;

        // Mueve la ficha al siguiente casillero.
        const movimientoRealizado = moverFichaEnTablero(idJugador, posicionDestino);

        if (!movimientoRealizado) {
            return;
        }

        // Pequeña pausa para que se vea el avance.
        await new Promise((resolve) => setTimeout(resolve, 250));
    }
}

// GESTIÓN DE TURNOS: SINCRONIZACIÓN WEBSOCKET.
conectarWebSocketTurnos(basePath, codigoPartida);
function conectarWebSocketTurnos(base, codigo) {
    if (!codigo || typeof SockJS === "undefined" || typeof Stomp === "undefined") {
        console.error("No se pudo conectar el WebSocket");
        return;
    }
    const socket = new SockJS(`${base}/ws`);
    const stompClient = Stomp.over(socket);
    stompClient.debug = null;
    stompClient.connect(
        {},
        () => {
            console.log("WebSocket conectado a la partida:", codigo);

            //CANAL 1: cambios de turnos y rondas.
            stompClient.subscribe(`/topic/partida/${codigo}/turno`, (mensaje) => {
                const turnoDTO = JSON.parse(mensaje.body);
                actualizarTurnoEnPantalla(turnoDTO);
                // GESTIÓN DE RONDAS: el backend informa la ronda a todos.
                actualizarRondaActual(turnoDTO.rondaActual);
            });

            //CANAL 2: recibe el resultado de la tirada y el movimiento.
            stompClient.subscribe(`/topic/partida/${codigo}`, async (mensaje) => {
                //Esta ruta tiene que coincidir con la que esta controladorPartida. notificadorWebSocket.convertAndSend
                const movimiento = JSON.parse(mensaje.body);
                if (dado1) dado1.textContent = movimiento.dado1;
                if (dado2) dado2.textContent = movimiento.dado2;
                try {
                    const cantidadPasos = Number(movimiento.dado1) + Number(movimiento.dado2);
                    await animarMovimientoFicha(movimiento.idJugador, cantidadPasos);
                    const ficha = document.querySelector(
                        `.ficha[data-jugador-id="${movimiento.idJugador}"]`,
                    );
                    const casillero = ficha?.closest(".casillero");
                    const posicionFinal = casillero ? casillero.dataset.casillero : "desconocida";
                    console.log(
                        "Resultado de la tirada:",
                        "Jugador:",
                        movimiento.idJugador,
                        "| Dados:",
                        movimiento.dado1,
                        "+",
                        movimiento.dado2,
                        "| Total:",
                        cantidadPasos,
                        "| Casillero final:",
                        posicionFinal,
                    );
                } catch (error) {
                    console.error("Error al animar el movimiento:", error);
                }
            });

            // 3. Escuchar la respuesta con las posiciones actuales.
            stompClient.subscribe(`/topic/partida/${codigo}/posiciones`, (mensaje) => {
                const posiciones = JSON.parse(mensaje.body);
                sincronizarPosicionesFichas(posiciones);
            });
            // Primero nos suscribimos al canal y después solicitamos
            // las posiciones actuales al backend.
            stompClient.send(`/app/partida/${codigo}/solicitar-posiciones`, {}, JSON.stringify({}));
        },
        (error) => {
            console.error("Error en la conexión WebSocket:", error);
        },
    );
}

function sincronizarPosicionesFichas(posiciones) {
    if (!Array.isArray(posiciones)) {
        console.error("La respuesta de posiciones no es una lista válida.");
        return;
    }

    posiciones.forEach((jugador) => {
        if (jugador.posicion == null) {
            console.warn("El jugador no tiene una posición definida:", jugador.idJugador);
            return;
        }

        moverFichaEnTablero(jugador.idJugador, jugador.posicion);
    });

    console.log("Posiciones de las fichas sincronizadas:", posiciones);
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
            span.textContent =
                idJugadorActual === usuarioSesionId
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
    //tarea propiedad

    //elementos del panel lateral
    const infoTitulo = document.getElementById("info-titulo");
    const infoPrecio = document.getElementById("info-precio");
    const infoAlquiler = document.getElementById("info-alquiler");
    const infoEstado = document.getElementById("info-estado");
    const btnComprar = document.getElementById("btn-comprar");

    let dineroJugadorActual = 1500; //plata inicial simulada
    const propiedadesJugador = []; //inventario vacío

    //simulacion de respuesta cuando se cae en una propiedad
    // Tu variable que ya tenés
        const propiedadSimulada = {
            nombre: "Avenida Illia",
            precio: 300,
            alquiler: 50,
            tieneDuenio: false,
            esPropiedad: true
        };
        window.propiedadSimulada = propiedadSimulada; // <--- AGREGÁ ESTA LÍNEA

        //Tu función que ya tenés
        function mostrarInfoCasillero(casilleroData) {
           //...(acá adentro está todo tu código que cambia los textos)
        }
        window.mostrarInfoCasillero = mostrarInfoCasillero; // <--- AGREGÁ ESTA LÍNEA

    //esta función se llamará cuando la ficha termine de moverse
    function mostrarInfoCasillero(casilleroData) {
        infoTitulo.textContent = casilleroData.nombre;

        if (casilleroData.esPropiedad) {
            infoPrecio.textContent = "$" + casilleroData.precio;
            infoAlquiler.textContent = "$" + casilleroData.alquiler;

            if (!casilleroData.tieneDuenio) {
                infoEstado.textContent = "Disponible";
                btnComprar.textContent = "Comprar por $" + casilleroData.precio;

                //valida si le alcanza la guita
                if (dineroJugadorActual >= casilleroData.precio) {
                    btnComprar.disabled = false;
                } else {
                    btnComprar.disabled = true;
                    btnComprar.textContent = "Dinero insuficiente";
                }
            } else {
                infoEstado.textContent = "Tiene dueño";
                btnComprar.disabled = true;
                btnComprar.textContent = "Pagar Alquiler";
            }
        } else {

            infoPrecio.textContent = "No disponible";
            infoAlquiler.textContent = "$0";
            infoEstado.textContent = "Lugar público";
            btnComprar.disabled = true;
            btnComprar.textContent = "No adquirible";
        }
    }

    //accion para comprar
    btnComprar.addEventListener("click", () => {
        //descontar
        dineroJugadorActual -= propiedadSimulada.precio;

        //busca el span del dinero en el header y lo actualiza visualmente
        const spanDinero = document.querySelector(".datos-jugador .dinero");
        spanDinero.textContent = "$" + dineroJugadorActual;

        //cambia el estado de la propiedad
        propiedadSimulada.tieneDuenio = true;
        propiedadesJugador.push(propiedadSimulada.nombre);

        //acualiza el panel lateral
        mostrarInfoCasillero(propiedadSimulada);

        //agregacion de historial
        const historial = document.querySelector(".historial__lista");
        const nuevoLi = document.createElement("li");
        nuevoLi.innerHTML = `Compraste: <strong>${propiedadSimulada.nombre}</strong>`;
        historial.appendChild(nuevoLi);
    });

   //conectamos la visualización al botón de tirar dados temporalmente
       if (botonTirar) {
           botonTirar.addEventListener("click", async () => {
               //caudno se tiran los dados aparece la info de la propiedad
               setTimeout(() => {
                   mostrarInfoCasillero(propiedadSimulada);
               }, 1000);
           });
       }
});
}

// Finalizar turno después de avanzar la ficha.
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
