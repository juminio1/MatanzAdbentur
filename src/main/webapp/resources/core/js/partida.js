// Determina la ruta base y construye la URL correcta.
const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
const urlParams = new URLSearchParams(window.location.search);
const codigoPartida = urlParams.get("codigo") || window.codigoSala || "";

// Obtiene los elementos del HTML por su ID.
const dado1 = document.getElementById("dado-1");
const dado2 = document.getElementById("dado-2");
const botonTirar = document.getElementById("btn-tirar");
if (botonTirar && window.usuarioSesionId != null
    && String(window.usuarioSesionId) === String(window.turnoActualId)) {
    botonTirar.disabled = false;
}

// Evita enviar varias peticiones simultáneas.
let estaTirando = false;

// Registra si el jugador actual ya tiró en este turno.
let yaTiroEnEsteTurno = false;

// Une el frontend con el backend de dados.
async function obtenerResultadoDados() {
    const idJugador = window.usuarioSesionId;
    if (idJugador == null || !codigoPartida) {
        throw new Error("Faltan los datos de la partida o del jugador para tirar los dados.");
    }

    const parametros = new URLSearchParams({
        codigoUnico: String(codigoPartida),
        idJugador: String(idJugador),
    });
    const respuesta = await fetch(`${basePath}/partida/tirar-dados?${parametros}`, {
        method: "POST",
        credentials: "same-origin",
        headers: { Accept: "application/json" },
    });
    const cuerpo = await respuesta.text();
    let resultado;
    try {
        resultado = JSON.parse(cuerpo);
    } catch (error) {
        const detalle = cuerpo.trim().startsWith("<")
            ? "El servidor devolvió una página en lugar del resultado de los dados."
            : "El servidor respondió con datos que no son JSON.";
        throw new Error(`HTTP ${respuesta.status}: ${detalle}`);
    }

    if (!respuesta.ok) {
        throw new Error(`HTTP ${respuesta.status}: ${resultado.error || "No se pudieron tirar los dados."}`);
    }

    return resultado;
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
            await procesarMovimiento(resultado);

            tiradaExitosa = true;
        } catch (error) {
            console.error("Error al tirar los dados o mover la ficha:", error);
            if (infoNota) infoNota.textContent = error.message || "No se pudo completar la tirada.";
        } finally {
            estaTirando = false;
        }

        if (!tiradaExitosa) {
            if (!yaTiroEnEsteTurno) {
                botonTirar.disabled = false;
            }
            return;
        }

        // El turno termina cuando se resuelve la compra, se pasa o vence el plazo.
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
iniciarSincronizacionTurno(basePath, codigoPartida);

let consultaTurnoEnCurso = false;
function iniciarSincronizacionTurno(base, codigo) {
    if (!codigo) return;
    actualizarTurnoDesdeServidor(base, codigo);
    window.setInterval(() => actualizarTurnoDesdeServidor(base, codigo), 2000);
}

async function actualizarTurnoDesdeServidor(base, codigo) {
    if (consultaTurnoEnCurso) return;
    consultaTurnoEnCurso = true;
    try {
        const respuesta = await fetch(`${base}/partida/${codigo}/turno`, { cache: "no-store" });
        if (!respuesta.ok) return;
        const turnoDTO = await respuesta.json();
        if (turnoDTO.idJugadorActual == null) return;
        window.turnoActualId = turnoDTO.idJugadorActual;
        actualizarTurnoEnPantalla(turnoDTO);
        actualizarRondaActual(turnoDTO.rondaActual);
    } catch (error) {
        console.warn("No se pudo sincronizar el turno con el servidor.", error);
    } finally {
        consultaTurnoEnCurso = false;
    }
}

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
                window.turnoActualId = turnoDTO.idJugadorActual;
                actualizarTurnoEnPantalla(turnoDTO);
                // GESTIÓN DE RONDAS: el backend informa la ronda a todos.
                actualizarRondaActual(turnoDTO.rondaActual);
            });

            //CANAL 2: recibe el resultado de la tirada y el movimiento.
            stompClient.subscribe(`/topic/partida/${codigo}`, async (mensaje) => {
                //Esta ruta tiene que coincidir con la que esta controladorPartida. notificadorWebSocket.convertAndSend
                const movimiento = JSON.parse(mensaje.body);
                await procesarMovimiento(movimiento);
            });

            // 3. Escuchar la respuesta con las posiciones actuales.
            stompClient.subscribe(`/topic/partida/${codigo}/posiciones`, (mensaje) => {
                const posiciones = JSON.parse(mensaje.body);
                sincronizarPosicionesFichas(posiciones);
            });
            stompClient.subscribe(`/topic/partida/${codigo}/propiedades`, (mensaje) => {
                const propiedad = JSON.parse(mensaje.body);
                actualizarEtiquetaPrecioPropiedad(propiedad);
                if (propiedadSeleccionada?.posicion === propiedad.posicion) mostrarInfoPropiedad(propiedad);
                if (propiedad.idJugadorPropietario != null && propiedad.dineroJugador != null) {
                    actualizarDineroJugador(propiedad.idJugadorPropietario, propiedad.dineroJugador);
                }
            });
            stompClient.subscribe(`/topic/partida/${codigo}/dinero`, (mensaje) => {
                const saldo = JSON.parse(mensaje.body);
                actualizarDineroJugador(saldo.idJugador, saldo.dinero);
            });
            stompClient.subscribe(`/topic/partida/${codigo}/alquiler`, (mensaje) => {
                const aviso = JSON.parse(mensaje.body);
                guardarAvisoAlquiler(aviso);
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

const movimientosEnCurso = new Map();
const avisosAlquilerPendientes = new Map();
let temporizadorAvisoAlquiler;

function guardarAvisoAlquiler(aviso) {
    if (String(aviso.idJugadorPagador) !== String(window.usuarioSesionId)) return;
    const idJugador = String(aviso.idJugadorPagador);
    const avisos = avisosAlquilerPendientes.get(idJugador) || [];
    avisos.push(aviso);
    avisosAlquilerPendientes.set(idJugador, avisos);
}

function mostrarAvisoAlquiler(aviso) {
    const elemento = document.getElementById("aviso-alquiler");
    if (!elemento) return;
    elemento.textContent = `Se te descontaron $${aviso.monto} de alquiler por ${aviso.propiedad}. Se lo pagaste a ${aviso.propietario}.`;
    elemento.classList.add("aviso-alquiler--visible");
    clearTimeout(temporizadorAvisoAlquiler);
    temporizadorAvisoAlquiler = setTimeout(() => {
        elemento.classList.remove("aviso-alquiler--visible");
    }, 5000);
}

function mostrarAvisosAlquilerDelJugador(idJugador) {
    const avisos = avisosAlquilerPendientes.get(String(idJugador)) || [];
    avisosAlquilerPendientes.delete(String(idJugador));
    avisos.forEach(mostrarAvisoAlquiler);
}

async function procesarMovimiento(movimiento) {
    const clave = `${movimiento.idJugador}:${movimiento.nuevaPosicion}:${movimiento.dado1}:${movimiento.dado2}`;
    if (movimientosEnCurso.has(clave)) return;
    movimientosEnCurso.set(clave, true);
    setTimeout(() => movimientosEnCurso.delete(clave), 5000);
    if (dado1) dado1.textContent = movimiento.dado1;
    if (dado2) dado2.textContent = movimiento.dado2;
    try {
        const pasos = Number(movimiento.dado1) + Number(movimiento.dado2);
        await animarMovimientoFicha(movimiento.idJugador, pasos);
        const posicionFinal = Number(movimiento.nuevaPosicion);
        if (Number.isInteger(posicionFinal)) {
            moverFichaEnTablero(movimiento.idJugador, posicionFinal);
            await atenderCaidaEnCasillero(movimiento.idJugador, posicionFinal);
        }
    } catch (error) {
        console.error("Error al animar el movimiento:", error);
    } finally {
        mostrarAvisosAlquilerDelJugador(movimiento.idJugador);
    }
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
    if (propiedadSeleccionada) mostrarInfoPropiedad(propiedadSeleccionada);
}

// GESTIÓN DE RONDAS: solo se actualiza la visualización.
function actualizarRondaActual(rondaActual) {
    const contadorRonda = document.getElementById("ronda-actual");

    if (contadorRonda && rondaActual != null) {
        contadorRonda.textContent = rondaActual;
    }
}

const infoTitulo = document.getElementById("info-titulo");
const infoSub = document.getElementById("info-sub");
const infoPrecio = document.getElementById("info-precio");
const infoAlquiler = document.getElementById("info-alquiler");
const infoEstado = document.getElementById("info-estado");
const infoNota = document.getElementById("info-nota");
const btnComprar = document.getElementById("btn-comprar");
const modalCompra = document.getElementById("modal-compra-propiedad");
const modalInfoPropiedad = document.getElementById("modal-info-propiedad");
const btnModalInfoAccion = document.getElementById("btn-modal-info-accion");
const posicionesPropiedad = new Set([1, 2, 4, 5, 6, 7, 9, 11, 12, 13, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27, 29]);
const coloresFicha = ["rojo", "azul", "verde", "amarillo"];
let propiedadSeleccionada = null;
let temporizadorCompra = null;
let resolviendoDecisionPropiedad = false;

function actualizarEtiquetaPrecioPropiedad(propiedad) {
    const casillero = document.querySelector(`.casillero[data-casillero="${propiedad.posicion}"]`);
    if (!casillero) return;

    casillero.classList.add("casillero--precio-superior");

    let etiqueta = casillero.querySelector(".casillero__precio");
    if (!etiqueta) {
        etiqueta = document.createElement("span");
        etiqueta.className = "casillero__precio";
        casillero.appendChild(etiqueta);
    }

    const tieneDueno = propiedad.disponible === false && propiedad.propietarioId != null;
    const monto = tieneDueno ? propiedad.alquiler : propiedad.precioCompra;
    if (monto == null) return;

    etiqueta.textContent = `$${Number(monto).toLocaleString("es-AR")}`;
    etiqueta.title = tieneDueno
        ? `Alquiler: $${monto} · Propietario: ${propiedad.propietario}`
        : `Precio de compra: $${monto}`;
    etiqueta.setAttribute("aria-label", etiqueta.title);
    const colorPropietario = tieneDueno ? obtenerColorFichaJugador(propiedad.propietarioId) : null;
    if (colorPropietario) {
        etiqueta.dataset.color = colorPropietario;
    } else {
        delete etiqueta.dataset.color;
    }
}

function obtenerColorFichaJugador(idJugador) {
    if (idJugador == null) return null;
    const ficha = document.querySelector(`.ficha[data-jugador-id="${idJugador}"]`);
    return coloresFicha.find((color) => ficha?.classList.contains(`ficha-${color}`)) || null;
}

async function cargarEtiquetasPrecioTablero() {
    try {
        const propiedades = await Promise.all(Array.from(posicionesPropiedad, async (posicion) => {
            const respuesta = await fetch(
                `${basePath}/partida/${codigoPartida}/propiedades/${posicion}`,
                { cache: "no-store" },
            );
            if (!respuesta.ok) return null;
            return respuesta.json();
        }));
        propiedades.filter(Boolean).forEach(actualizarEtiquetaPrecioPropiedad);
    } catch (error) {
        console.error(error);
    }
}

function mostrarInfoPropiedad(propiedad) {
    propiedadSeleccionada = propiedad;
    actualizarEtiquetaPrecioPropiedad(propiedad);
    const tienePropietario = propiedad.propietarioId != null;
    infoTitulo.textContent = propiedad.nombre;
    infoSub.textContent = propiedad.descripcion
        ? `Casillero #${propiedad.posicion} · ${propiedad.descripcion}`
        : `Casillero #${propiedad.posicion}`;
    infoPrecio.textContent = `$${propiedad.precioCompra}`;
    infoAlquiler.textContent = `$${propiedad.alquiler}`;
    infoEstado.textContent = propiedad.disponible ? "Sin dueño" : `Pertenece a ${propiedad.propietario}`;
    const posicionJugador = document.querySelector(`.ficha[data-jugador-id="${window.usuarioSesionId}"]`)
        ?.closest(".casillero")?.dataset.casillero;
    const puedeComprar = propiedad.disponible
        && String(posicionJugador) === String(propiedad.posicion)
        && String(window.turnoActualId) === String(window.usuarioSesionId);
    const esPropia = !propiedad.disponible
        && String(propiedad.propietarioId) === String(window.usuarioSesionId);

    if (propiedad.disponible) {
        infoNota.textContent = puedeComprar
            ? "Podés comprarla ahora o pasar."
            : "Propiedad disponible. Solo se puede comprar al caer en ella durante tu turno.";
    } else if (esPropia) {
        infoNota.textContent = `Es tuya. Podés venderla al banco por $${propiedad.precioVenta ?? Math.floor(propiedad.precioCompra / 2)}.`;
    } else {
        infoNota.textContent = "El alquiler se cobra automáticamente cuando un rival cae aquí.";
    }

    btnComprar.disabled = !puedeComprar && !esPropia;
    btnComprar.dataset.accionPropiedad = puedeComprar ? "comprar" : (esPropia ? "vender" : "");
    btnComprar.textContent = puedeComprar
        ? `Comprar por $${propiedad.precioCompra}`
        : (esPropia ? `Vender por $${propiedad.precioVenta ?? Math.floor(propiedad.precioCompra / 2)}` : "No disponible");

    document.getElementById("popup-info-titulo").textContent = propiedad.nombre;
    document.getElementById("popup-info-descripcion").textContent = infoSub.textContent;
    document.getElementById("popup-precio-contenedor").hidden = tienePropietario;
    document.getElementById("popup-alquiler-contenedor").hidden = !tienePropietario;
    document.getElementById("popup-info-precio").textContent = `$${Number(propiedad.precioCompra).toLocaleString("es-AR")}`;
    document.getElementById("popup-info-alquiler").textContent = `$${Number(propiedad.alquiler).toLocaleString("es-AR")}`;
    document.getElementById("popup-info-propietario").textContent = propiedad.propietario || "Sin propietario";
    btnModalInfoAccion.disabled = btnComprar.disabled;
    btnModalInfoAccion.textContent = btnComprar.textContent;
}

function actualizarDineroJugador(idJugador, dinero) {
    const saldo = document.getElementById(String(idJugador))?.querySelector(".dinero");
    if (saldo) saldo.textContent = `$${Number(dinero).toLocaleString("es-AR")}`;
}

async function cargarInfoPropiedad(posicion, abrirPopup = false) {
    const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/propiedades/${posicion}`, { cache: "no-store" });
    if (respuesta.status === 404) return null;
    if (!respuesta.ok) throw new Error("No se pudo consultar la propiedad");
    const propiedad = await respuesta.json();
    mostrarInfoPropiedad(propiedad);
    if (abrirPopup && !modalInfoPropiedad.open) modalInfoPropiedad.showModal();
    return propiedad;
}

async function comprarPropiedadActual() {
    if (!propiedadSeleccionada || !propiedadSeleccionada.disponible || resolviendoDecisionPropiedad) return;
    resolviendoDecisionPropiedad = true;
    document.getElementById("btn-modal-comprar").disabled = true;
    document.getElementById("btn-modal-pasar").disabled = true;
    try {
        const urlCompra = `${basePath}/partida/${codigoPartida}/propiedades/${propiedadSeleccionada.posicion}/comprar`;
        let respuesta;
        try {
            respuesta = await fetch(urlCompra, { method: "POST", credentials: "same-origin" });
        } catch (error) {
            console.error("No se pudo conectar con el endpoint de compra:", urlCompra, error);
            throw new Error("No se pudo conectar con el servidor. Comprobá que la aplicación siga ejecutándose.");
        }
        const cuerpoRespuesta = await respuesta.text();
        let datos = {};
        try {
            datos = cuerpoRespuesta ? JSON.parse(cuerpoRespuesta) : {};
        } catch (error) {
            console.error("El endpoint de compra devolvió una respuesta que no es JSON:", respuesta.status, cuerpoRespuesta);
        }
        if (!respuesta.ok) {
            console.error("El servidor rechazó la compra:", respuesta.status, datos, cuerpoRespuesta);
            throw new Error(datos.error || `El servidor rechazó la compra (HTTP ${respuesta.status}).`);
        }
        mostrarInfoPropiedad(datos);
        cerrarModalCompra();
        agregarAlHistorial(`Compraste: ${datos.nombre}`);
        if (datos.dineroJugador != null) actualizarDineroJugador(datos.idJugadorPropietario ?? window.usuarioSesionId, datos.dineroJugador);
        if (datos.idJugadorActual != null) {
            window.turnoActualId = datos.idJugadorActual;
            actualizarTurnoEnPantalla({ idJugadorActual: datos.idJugadorActual, apodoJugadorActual: "" });
        }
    } catch (error) {
        infoNota.textContent = error.message || "No se pudo comprar la propiedad.";
        document.getElementById("modal-propiedad-info").textContent = infoNota.textContent;
        document.getElementById("btn-modal-comprar").disabled = false;
        document.getElementById("btn-modal-pasar").disabled = false;
        resolviendoDecisionPropiedad = false;
    }
}

async function venderPropiedadActual() {
    if (!propiedadSeleccionada || btnComprar.dataset.accionPropiedad !== "vender" || resolviendoDecisionPropiedad) return;
    const propiedad = propiedadSeleccionada;
    const precioVenta = Number(propiedad.precioVenta ?? Math.floor(propiedad.precioCompra / 2));
    if (!window.confirm(`¿Vender ${propiedad.nombre} al banco por $${precioVenta}?`)) return;

    resolviendoDecisionPropiedad = true;
    btnComprar.disabled = true;
    try {
        const respuesta = await fetch(
            `${basePath}/partida/${codigoPartida}/propiedades/${propiedad.posicion}/vender`,
            { method: "POST", credentials: "same-origin" },
        );
        const datos = await respuesta.json();
        if (!respuesta.ok) throw new Error(datos.error || "No se pudo vender la propiedad.");

        mostrarInfoPropiedad(datos);
        actualizarDineroJugador(datos.idJugador ?? window.usuarioSesionId, datos.dineroJugador);
        agregarAlHistorial(`Vendiste al banco: ${datos.nombre} por $${datos.precioVenta}`);
    } catch (error) {
        infoNota.textContent = error.message || "No se pudo vender la propiedad.";
        btnComprar.disabled = false;
    } finally {
        resolviendoDecisionPropiedad = false;
    }
}

function agregarAlHistorial(texto) {
    const item = document.createElement("li");
    item.textContent = texto;
    document.querySelector(".historial__lista")?.prepend(item);
}

function cerrarModalCompra() {
    if (temporizadorCompra) clearInterval(temporizadorCompra);
    temporizadorCompra = null;
    if (modalCompra?.open) modalCompra.close();
    resolviendoDecisionPropiedad = false;
}

async function pasarTurnoPropiedad() {
    if (resolviendoDecisionPropiedad) return;
    resolviendoDecisionPropiedad = true;
    cerrarModalCompra();
    await finalizarTurno().catch(console.error);
}

async function atenderCaidaEnCasillero(idJugador, posicion) {
    if (String(idJugador) !== String(window.usuarioSesionId)) return;
    if (!posicionesPropiedad.has(Number(posicion))) return;
    let propiedad;
    try {
        propiedad = await cargarInfoPropiedad(posicion);
        // Los casilleros especiales no tienen ficha de propiedad: no abren modal.
        // El backend avanza el turno automáticamente en esos casilleros.
        if (!propiedad) {
            return;
        }
        agregarAlHistorial(`Caíste en: ${propiedad.nombre}`);
    } catch (error) {
        console.error(error);
        infoNota.textContent = "No se pudo verificar si la propiedad está disponible para comprar.";
        return;
    }

    // El diálogo de compra se abre solo tras confirmar que está libre y que
    // quien cayó sigue siendo el jugador del turno en esta sesión.
    if (propiedad.disponible !== true || String(window.turnoActualId) !== String(idJugador)) return;

    document.getElementById("modal-propiedad-titulo").textContent = propiedad.nombre;
    document.getElementById("modal-propiedad-info").textContent =
        `Sin dueño · Precio: $${propiedad.precioCompra} · Alquiler: $${propiedad.alquiler}`;
    document.getElementById("btn-modal-comprar").disabled = false;
    document.getElementById("btn-modal-pasar").disabled = false;
    let segundos = 30;
    document.getElementById("tiempo-compra").textContent = segundos;
    modalCompra.showModal();
    temporizadorCompra = setInterval(() => {
        segundos -= 1;
        document.getElementById("tiempo-compra").textContent = segundos;
        if (segundos <= 0) pasarTurnoPropiedad();
    }, 1000);
}

document.querySelectorAll(".casillero[data-casillero]").forEach((casillero) => {
    casillero.addEventListener("click", (event) => {
        if (event.target.closest(".ficha")) return;
        cargarInfoPropiedad(casillero.dataset.casillero, true).catch(console.error);
    });
});
document.getElementById("btn-info-casillero")?.addEventListener("click", () => {
    const posicion = document.querySelector(`.ficha[data-jugador-id="${window.usuarioSesionId}"]`)?.closest(".casillero")?.dataset.casillero;
    if (posicion != null) cargarInfoPropiedad(posicion, true).catch(console.error);
});
document.getElementById("btn-cerrar-info-propiedad")?.addEventListener("click", () => modalInfoPropiedad.close());
modalInfoPropiedad?.addEventListener("click", (event) => {
    if (event.target === modalInfoPropiedad) modalInfoPropiedad.close();
});
btnModalInfoAccion?.addEventListener("click", () => btnComprar.click());
document.getElementById("btn-modal-comprar")?.addEventListener("click", comprarPropiedadActual);
document.getElementById("btn-modal-pasar")?.addEventListener("click", pasarTurnoPropiedad);
btnComprar?.addEventListener("click", () => {
    if (btnComprar.dataset.accionPropiedad === "comprar") comprarPropiedadActual();
    if (btnComprar.dataset.accionPropiedad === "vender") venderPropiedadActual();
});

cargarEtiquetasPrecioTablero();

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
    window.turnoActualId = turnoDTO.idJugadorActual;
    actualizarTurnoEnPantalla(turnoDTO);
    actualizarRondaActual(turnoDTO.rondaActual);

    console.log("Siguiente jugador:", turnoDTO.apodoJugadorActual);
    console.log("Ronda actual:", turnoDTO.rondaActual);

    return turnoDTO;
}
