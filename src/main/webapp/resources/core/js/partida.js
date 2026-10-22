const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
const urlParams = new URLSearchParams(window.location.search);
const codigoPartida = urlParams.get("codigo") || window.codigoSala || "";

const dado1 = document.getElementById("dado-1");
const dado2 = document.getElementById("dado-2");
const botonTirar = document.getElementById("btn-tirar");
if (botonTirar) botonTirar.disabled = true;

let estaTirando = false;
let posicionesInicialesSincronizadas = false;

let yaTiroEnEsteTurno = false;

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
        throw new Error(
            `HTTP ${respuesta.status}: ${resultado.error || "No se pudieron tirar los dados."}`,
        );
    }

    return resultado;
}

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

            yaTiroEnEsteTurno = true;

            console.log("Nueva posición:", resultado.nuevaPosicion);
            await procesarMovimiento(resultado);

            tiradaExitosa = true;
        } catch (error) {
            console.error("Error al tirar los dados o mover la ficha:", error);
            if (notaModalInfo)
                notaModalInfo.textContent = error.message || "No se pudo completar la tirada.";
        } finally {
            estaTirando = false;
        }

        if (!tiradaExitosa) {
            actualizarBotonTirada();
            return;
        }
    });
}

function moverFichaEnTablero(idJugador, nuevaPosicion) {
    const ficha = document.querySelector(`.ficha[data-jugador-id="${idJugador}"]`);
    const casilleroDestino = document.querySelector(
        `.casillero[data-casillero="${nuevaPosicion}"] .fichas-en-casillero`,
    );

    if (!ficha || !casilleroDestino) {
        console.error("No se encontró la ficha o el casillero destino.", {
            idJugador,
            nuevaPosicion,
            fichaEncontrada: Boolean(ficha),
            casilleroEncontrado: Boolean(casilleroDestino),
        });
        return false;
    }
    casilleroDestino.appendChild(ficha);
    return true;
}

async function animarMovimientoFicha(idJugador, cantidadPasos, posicionAnterior) {
    const ficha = document.querySelector(`.ficha[data-jugador-id="${idJugador}"]`);

    if (!ficha) {
        console.error("No se encontró la ficha del jugador:", idJugador);
        return;
    }

    let casilleroActual = ficha.closest(".casillero");

    if (posicionAnterior != null && Number.isInteger(Number(posicionAnterior))) {
        const casilleroAnterior = document.querySelector(
            `.casillero[data-casillero="${Number(posicionAnterior)}"]`,
        );
        if (casilleroAnterior && casilleroActual !== casilleroAnterior) {
            moverFichaEnTablero(idJugador, Number(posicionAnterior));
            casilleroActual = casilleroAnterior;
        }
    }

    if (!casilleroActual) {
        console.error("La ficha no está dentro de un casillero.");
        return;
    }

    const casilleros = Array.from(document.querySelectorAll(".casillero[data-casillero]"));

    let indiceActual = casilleros.indexOf(casilleroActual);

    if (indiceActual === -1 || casilleros.length === 0) {
        console.error("No se pudo determinar la posición de la ficha.");
        return;
    }

    for (let paso = 0; paso < cantidadPasos; paso++) {
        indiceActual = (indiceActual + 1) % casilleros.length;

        const siguienteCasillero = casilleros[indiceActual];
        const posicionDestino = siguienteCasillero.dataset.casillero;

        const movimientoRealizado = moverFichaEnTablero(idJugador, posicionDestino);

        if (!movimientoRealizado) {
            return;
        }

        await new Promise((resolve) => setTimeout(resolve, 250));
    }
}

let consultaTurnoEnCurso = false;
let clienteWebSocketPartida = null;
function iniciarSincronizacionTurno(base, codigo) {
    if (!codigo) return;
    actualizarTurnoDesdeServidor(base, codigo);
    window.setInterval(() => actualizarTurnoDesdeServidor(base, codigo), 10000); //2000
}

function iniciarSincronizacionPosiciones() {
    solicitarPosicionesPartida();
    window.setInterval(solicitarPosicionesPartida, 10000); 5000
}

function solicitarPosicionesPartida() {
    if (clienteWebSocketPartida?.connected) {
        clienteWebSocketPartida.send(
            `/app/partida/${codigoPartida}/solicitar-posiciones`,
            {},
            JSON.stringify({}),
        );
    }
}

async function actualizarTurnoDesdeServidor(base, codigo) {
    if (consultaTurnoEnCurso) return;
    consultaTurnoEnCurso = true;
    try {
        const respuesta = await fetch(`${base}/partida/${codigo}/turno`, { cache: "no-store" });
        if (!respuesta.ok) return;
        const turnoDTO = await respuesta.json();
        if (turnoDTO.idJugadorActual == null) return;
        aplicarTurnoDTO(turnoDTO);
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
            clienteWebSocketPartida = stompClient;

            stompClient.subscribe(`/topic/partida/${codigo}/turno`, (mensaje) => {
                const turnoDTO = JSON.parse(mensaje.body);
                aplicarTurnoDTO(turnoDTO);
            });

            stompClient.subscribe(`/topic/partida/${codigo}`, async (mensaje) => {
                const movimiento = JSON.parse(mensaje.body);
                await procesarMovimiento(movimiento);
            });

            stompClient.subscribe(`/topic/partida/${codigo}/posiciones`, (mensaje) => {
                const posiciones = JSON.parse(mensaje.body);
                sincronizarPosicionesFichas(posiciones);
            });
            stompClient.subscribe(`/topic/partida/${codigo}/propiedades`, (mensaje) => {
                const propiedad = JSON.parse(mensaje.body);
                actualizarEtiquetaPrecioPropiedad(propiedad);
                if (propiedadSeleccionada?.posicion === propiedad.posicion)
                    mostrarInfoPropiedad(propiedad);
                if (modalBonoPropiedades?.open) cargarPropiedadesPropias();
                if (propiedad.idVendedor != null && propiedad.dineroVendedor != null) {
                    actualizarDineroJugador(
                        propiedad.idVendedor,
                        propiedad.dineroVendedor,
                        propiedad.bancarrotaVendedor,
                    );
                }

                if (propiedad.idJugadorPropietario != null && propiedad.dineroJugador != null) {
                    actualizarDineroJugador(
                        propiedad.idJugadorPropietario,
                        propiedad.dineroJugador,
                        propiedad.enBancarrota,
                    );
                } else if (propiedad.idJugador != null && propiedad.dineroJugador != null) {
                    actualizarDineroJugador(
                        propiedad.idJugador,
                        propiedad.dineroJugador,
                        propiedad.enBancarrota,
                    );
                }
            });
            stompClient.subscribe(`/topic/partida/${codigo}/dinero`, (mensaje) => {
                const saldo = JSON.parse(mensaje.body);
                actualizarDineroJugador(saldo.idJugador, saldo.dinero, saldo.enBancarrota);
            });
            stompClient.subscribe(`/topic/partida/${codigo}/alquiler`, (mensaje) => {
                const aviso = JSON.parse(mensaje.body);
                guardarAvisoAlquiler(aviso);
            });

            stompClient.subscribe(`/topic/partida/${codigo}/abandono`, async (mensaje) => {
                const datos = JSON.parse(mensaje.body);

                agregarAlHistorial(`${datos.apodo} abandonó la partida.`, datos.idJugador);

                mostrarJugadorAbandonado(datos.idJugador);

                eliminarFichaJugadorAbandonado(datos.idJugador);

                await sincronizarPropiedadesTrasAbandono(datos.idJugador);

                if (
                    datos.victoriaPorAbandono === true &&
                    String(datos.idGanador) === String(window.usuarioSesionId)
                ) {
                    mostrarVictoria(datos);
                }
            });

            // Pido las posiciones después de suscribirme al canal.
            solicitarPosicionesPartida();
        },
        (error) => {
            clienteWebSocketPartida = null;
            console.error("Error en la conexión WebSocket:", error);
            window.setTimeout(() => conectarWebSocketTurnos(base, codigo), 3000);
        },
    );
}

const movimientosEnCurso = new Set();
const movimientosDeJugadoresEnCurso = new Set();
const avisosAlquilerPendientes = new Map();
const estadoPropiedadesConocido = new Map();
let temporizadorAvisoAlquiler;

function actualizarBotonTirada() {
    if (botonTirar) {
        botonTirar.disabled =
            partidaFinalizada ||
            !posicionesInicialesSincronizadas ||
            movimientosDeJugadoresEnCurso.size > 0 ||
            estaTirando ||
            yaTiroEnEsteTurno ||
            window.usuarioEnBancarrota ||
            String(window.turnoActualId) !== String(window.usuarioSesionId);
    }
}

// Espero el final del movimiento para registrar el alquiler.
function guardarAvisoAlquiler(aviso) {
    const idJugador = String(aviso.idJugadorPagador);
    if (!movimientosDeJugadoresEnCurso.has(idJugador)) {
        mostrarAvisoAlquiler(aviso);
        return;
    }
    const avisos = avisosAlquilerPendientes.get(idJugador) || [];
    avisos.push(aviso);
    avisosAlquilerPendientes.set(idJugador, avisos);
}

function mostrarAvisoPartida(mensaje) {
    const elemento = document.getElementById("aviso-alquiler");
    if (!elemento) return;
    elemento.textContent = mensaje;
    elemento.classList.add("aviso-alquiler--visible");
    clearTimeout(temporizadorAvisoAlquiler);
    temporizadorAvisoAlquiler = setTimeout(() => {
        elemento.classList.remove("aviso-alquiler--visible");
    }, 5000);
}

function mostrarAvisoAlquiler(aviso) {
    agregarAlHistorial(
        `${aviso.pagador} pagó $${aviso.monto} de alquiler a ${aviso.propietario} por ${aviso.propiedad}.`,
        aviso.idJugadorPagador,
    );
}

function mostrarAvisosAlquilerDelJugador(idJugador) {
    const avisos = avisosAlquilerPendientes.get(String(idJugador)) || [];
    avisosAlquilerPendientes.delete(String(idJugador));
    avisos.forEach(mostrarAvisoAlquiler);
}

function registrarCaidaEnHistorial(idJugador, posicion) {
    const casillero = document.querySelector(`.casillero[data-casillero="${posicion}"]`);
    const nombreCasillero =
        casillero?.querySelector(".casillero__nombre")?.textContent.trim() ||
        `casillero #${posicion}`;
    const nombreJugador =
        document
            .getElementById(String(idJugador))
            ?.querySelector(".nombre-jugador")
            ?.textContent.trim() || `Jugador ${idJugador}`;
    agregarAlHistorial(`${nombreJugador} cay\u00f3 en ${nombreCasillero}.`, idJugador);
}

async function procesarMovimiento(movimiento) {
    const clave = movimiento.idMovimiento
        ? String(movimiento.idMovimiento)
        : `${movimiento.idJugador}:${movimiento.posicionAnterior}:${movimiento.nuevaPosicion}:${movimiento.dado1}:${movimiento.dado2}`;
    if (movimientosEnCurso.has(clave)) return;
    movimientosEnCurso.add(clave);
    if (dado1) dado1.textContent = movimiento.dado1;
    if (dado2) dado2.textContent = movimiento.dado2;
    try {
        const pasos = Number(movimiento.dado1) + Number(movimiento.dado2);
        movimientosDeJugadoresEnCurso.add(String(movimiento.idJugador));
        actualizarBotonTirada();
        await animarMovimientoFicha(movimiento.idJugador, pasos, movimiento.posicionAnterior);
        const posicionFinal = Number(movimiento.nuevaPosicion);
        if (Number.isInteger(posicionFinal)) {
            moverFichaEnTablero(movimiento.idJugador, posicionFinal);
            registrarCaidaEnHistorial(movimiento.idJugador, posicionFinal);
            await atenderCaidaEnCasillero(movimiento.idJugador, posicionFinal);
        }
    } catch (error) {
        console.error("Error al animar el movimiento:", error);
    } finally {
        movimientosDeJugadoresEnCurso.delete(String(movimiento.idJugador));
        actualizarBotonTirada();
        mostrarAvisosAlquilerDelJugador(movimiento.idJugador);
    }
}

function sincronizarPosicionesFichas(posiciones) {
    if (!Array.isArray(posiciones)) {
        console.error("La respuesta de posiciones no es una lista válida.");
        return;
    }

   
posiciones.forEach((jugador) => {

    const idJugador = String(jugador.idJugador);

    if (!jugadoresAbandonados.has(idJugador)
            && !movimientosDeJugadoresEnCurso.has(idJugador)) {

        if (jugador.posicion != null) {
            moverFichaEnTablero(
                jugador.idJugador,
                jugador.posicion
            );
        }
    }
        moverFichaEnTablero(jugador.idJugador, jugador.posicion);
    });

    posicionesInicialesSincronizadas = true;
    actualizarBotonTirada();
    console.log("Posiciones de las fichas sincronizadas:", posiciones);
}

function actualizarTurnoEnPantalla(turnoDTO) {
    const idJugadorActual = String(turnoDTO.idJugadorActual);
    const usuarioSesionId = String(window.usuarioSesionId);

    if (window.idJugadorTurnoAnterior !== idJugadorActual) {
        yaTiroEnEsteTurno = false;
        window.idJugadorTurnoAnterior = idJugadorActual;
    }

    document.querySelectorAll(".contenedor-turno").forEach((contenedor) => {
        contenedor.innerHTML = "";
    });

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

    document.querySelectorAll(".jugador").forEach((card) => {
        card.classList.remove("turno-activo");
    });

    const cardJugadorActivo = document.getElementById(idJugadorActual);
    if (cardJugadorActivo) {
        cardJugadorActivo.classList.add("turno-activo");
    }

    if (usuarioSesionId === idJugadorActual) {
        desbloquearControles();
    } else {
        bloquearControles();
    }
    if (propiedadSeleccionada) mostrarInfoPropiedad(propiedadSeleccionada);
}

function aplicarTurnoDTO(turnoDTO) {
    if (!turnoDTO || turnoDTO.idJugadorActual == null) return;
    const jugadorAnterior = window.idJugadorTurnoAnterior;
    const cambioDeTurno = String(jugadorAnterior) !== String(turnoDTO.idJugadorActual);
    const idJugadorReloj = String(turnoDTO.idJugadorActual);
    const cambioDeTurnoDelReloj = String(window.idJugadorTurnoCronometro) !== idJugadorReloj;

    if (cambioDeTurno && modalCompra?.open) cerrarModalCompra();
    window.turnoActualId = turnoDTO.idJugadorActual;
    if (cambioDeTurno && modalBonoPropiedades?.open) cargarPropiedadesPropias();
    if (turnoDTO.duracionSegundos == null) {
        prepararRelojSinIniciar();
        window.turnoInicialSincronizado = true;
    } else if (cambioDeTurnoDelReloj || !window.turnoInicialSincronizado) {
        iniciarRelojLocal(turnoDTO.duracionSegundos);
        window.idJugadorTurnoCronometro = idJugadorReloj;
        window.turnoInicialSincronizado = true;
    }
    actualizarTurnoEnPantalla(turnoDTO);
    actualizarRondaActual(turnoDTO.rondaActual);
}

function actualizarRondaActual(rondaActual) {
    const contadorRonda = document.getElementById("ronda-actual");

    if (contadorRonda && rondaActual != null) {
        contadorRonda.textContent = rondaActual;
    }
}

const modalCompra = document.getElementById("modal-compra-propiedad");
const modalInfoPropiedad = document.getElementById("modal-info-propiedad");
const btnModalInfoAccion = document.getElementById("btn-modal-info-accion");
const notaModalInfo = document.getElementById("popup-info-nota");
const modalBonoPropiedades = document.getElementById("modal-bono-propiedades");
const listaPropiedadesPropias = document.getElementById("lista-propiedades-propias");
const mensajeBonoPropiedades = document.getElementById("bono-propiedades-mensaje");
const modalVenderPropiedad = document.getElementById("modal-vender-propiedad");
const nombrePropiedadEnVenta = document.getElementById("vender-propiedad-nombre");
const mensajeOfertaVenta = document.getElementById("oferta-venta-mensaje");
const modalOfertaRecibida = document.getElementById("modal-oferta-recibida");
const mensajeOfertaRecibida = document.getElementById("oferta-recibida-mensaje");
const posicionesPropiedad = new Set([
    1, 2, 4, 5, 6, 7, 9, 11, 12, 13, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 27, 29,
]);
const coloresFicha = ["rojo", "azul", "verde", "amarillo"];
const nivelesAlquiler = [200, 300, 500, 1000, 1500];
const posicionesLocalidades = {
    VILLA_LUZURIAGA: [1, 2],
    SAN_JUSTO: [4, 5, 7],
    ISIDRO_CASANOVA: [9, 11, 13],
    RAFAEL_CASTILLO: [15, 16],
    LAFERRERE: [18, 19],
    CIUDAD_EVITA: [21, 22],
    LA_TABLADA: [23, 25],
    RAMOS_MEJIA: [26, 27, 29],
};
const nombresLocalidades = {
    VILLA_LUZURIAGA: "Villa Luzuriaga",
    SAN_JUSTO: "San Justo",
    ISIDRO_CASANOVA: "Isidro Casanova",
    RAFAEL_CASTILLO: "Rafael Castillo",
    LAFERRERE: "Laferrere",
    CIUDAD_EVITA: "Ciudad Evita",
    LA_TABLADA: "La Tablada",
    RAMOS_MEJIA: "Ramos Mej\u00eda",
};
const coloresLocalidades = {
    VILLA_LUZURIAGA: "#ed5da8",
    SAN_JUSTO: "#42b6e8",
    ISIDRO_CASANOVA: "#ffbd42",
    RAFAEL_CASTILLO: "#91d64b",
    LAFERRERE: "#a17be8",
    CIUDAD_EVITA: "#f16c5e",
    LA_TABLADA: "#38c8b5",
    RAMOS_MEJIA: "#f0df4c",
};
const posicionesParadas = new Set([6, 12, 17, 24]);
let propiedadSeleccionada = null;
let resolviendoDecisionPropiedad = false;
let propiedadesPropiasActuales = [];
let propiedadParaVender = null;
let ofertaActiva = null;

function actualizarEtiquetaPrecioPropiedad(propiedad) {
    const casillero = document.querySelector(`.casillero[data-casillero="${propiedad.posicion}"]`);
    if (!casillero) return;

    const colorLocalidad = coloresLocalidades[propiedad.grupo];
    if (colorLocalidad) {
        casillero.style.backgroundColor = colorLocalidad;
        casillero.dataset.grupoLocalidad = propiedad.grupo;
    } else if (posicionesParadas.has(Number(propiedad.posicion))) {
        casillero.style.backgroundColor = "#fff0c2";
        casillero.dataset.grupoLocalidad = "PARADAS";
    }

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
        ? `Alquiler: $${monto} \u00b7 Propietario: ${propiedad.propietario}`
        : `Precio de compra: $${monto}`;
    etiqueta.setAttribute("aria-label", etiqueta.title);
    const colorPropietario = tieneDueno ? obtenerColorFichaJugador(propiedad.propietarioId) : null;
    if (colorPropietario) {
        etiqueta.dataset.color = colorPropietario;
    } else {
        delete etiqueta.dataset.color;
    }
    registrarCambioPropiedadEnHistorial(propiedad);
}

function registrarCambioPropiedadEnHistorial(propiedad) {
    const posicion = Number(propiedad.posicion);
    const propietarioId = propiedad.propietarioId == null ? null : String(propiedad.propietarioId);
    const nivel = Number(propiedad.nivelMejora ?? 0);
    const anterior = estadoPropiedadesConocido.get(posicion);

    if (anterior && anterior.propietarioId !== propietarioId) {
        if (anterior.propietarioId == null && propietarioId != null) {
            const comprador = obtenerNombreJugadorHistorial(propietarioId);
            agregarAlHistorial(
                `${comprador} compr\u00f3 ${propiedad.nombre} por $${propiedad.precioCompra}.`,
                propietarioId,
            );
        } else if (propietarioId == null) {
            const vendedor = obtenerNombreJugadorHistorial(anterior.propietarioId);
            const precio = Number(propiedad.precioVenta ?? 0).toLocaleString("es-AR");
            agregarAlHistorial(
                `${vendedor} vendi\u00f3 ${propiedad.nombre} al banco por $${precio}.`,
                anterior.propietarioId,
            );
        } else {
            const comprador = obtenerNombreJugadorHistorial(propietarioId);
            const vendedor = obtenerNombreJugadorHistorial(anterior.propietarioId);
            const precio = Number(propiedad.precioVenta ?? 0).toLocaleString("es-AR");
            agregarAlHistorial(
                `${comprador} compr\u00f3 ${propiedad.nombre} a ${vendedor} por $${precio}.`,
                propietarioId,
            );
        }
    } else if (anterior && nivel > anterior.nivel) {
        const propietario = obtenerNombreJugadorHistorial(propietarioId);
        agregarAlHistorial(
            `${propietario} aument\u00f3 el alquiler de ${propiedad.nombre} a $${propiedad.alquiler}.`,
            propietarioId,
        );
    }

    estadoPropiedadesConocido.set(posicion, { propietarioId, nivel });
}

function obtenerNombreJugadorHistorial(idJugador) {
    return (
        document
            .getElementById(String(idJugador))
            ?.querySelector(".nombre-jugador")
            ?.textContent.trim() || `Jugador ${idJugador}`
    );
}

function obtenerColorFichaJugador(idJugador) {
    if (idJugador == null) return null;
    const ficha = document.querySelector(`.ficha[data-jugador-id="${idJugador}"]`);
    return coloresFicha.find((color) => ficha?.classList.contains(`ficha-${color}`)) || null;
}

function pintarLocalidadesTablero() {
    Object.entries(posicionesLocalidades).forEach(([grupo, posiciones]) => {
        posiciones.forEach((posicion) => {
            const casillero = document.querySelector(`.casillero[data-casillero="${posicion}"]`);
            if (!casillero) return;
            casillero.style.backgroundColor = coloresLocalidades[grupo];
            casillero.dataset.grupoLocalidad = grupo;
        });
    });
    posicionesParadas.forEach((posicion) => {
        const casillero = document.querySelector(`.casillero[data-casillero="${posicion}"]`);
        if (!casillero) return;
        casillero.style.backgroundColor = "#fff0c2";
        casillero.dataset.grupoLocalidad = "PARADAS";
    });
}

function mostrarInfoPropiedad(propiedad) {
    propiedadSeleccionada = propiedad;
    actualizarEtiquetaPrecioPropiedad(propiedad);
    const tienePropietario = propiedad.propietarioId != null;
    const descripcion = propiedad.descripcion
        ? `Casillero #${propiedad.posicion} \u00b7 ${propiedad.descripcion}`
        : `Casillero #${propiedad.posicion}`;
    const esPropia =
        !propiedad.disponible && String(propiedad.propietarioId) === String(window.usuarioSesionId);
    const posicionJugador = document
        .querySelector(`.ficha[data-jugador-id="${window.usuarioSesionId}"]`)
        ?.closest(".casillero")?.dataset.casillero;
    const puedeComprar =
        propiedad.disponible &&
        String(posicionJugador) === String(propiedad.posicion) &&
        String(window.turnoActualId) === String(window.usuarioSesionId) &&
        tiempoRestante > 0;
    const textoAccion = puedeComprar
        ? `Comprar por $${propiedad.precioCompra}`
        : esPropia
          ? "Propiedad propia"
          : "No disponible";

    document.getElementById("popup-info-titulo").textContent = propiedad.nombre;
    document.getElementById("popup-info-descripcion").textContent = descripcion;
    document.getElementById("popup-precio-contenedor").hidden = tienePropietario;
    document.getElementById("popup-alquiler-contenedor").hidden = !tienePropietario;
    document.getElementById("popup-info-precio").textContent =
        `$${Number(propiedad.precioCompra).toLocaleString("es-AR")}`;
    document.getElementById("popup-info-alquiler").textContent =
        `$${Number(propiedad.alquiler).toLocaleString("es-AR")}`;
    document.getElementById("popup-info-propietario").textContent =
        propiedad.propietario || "Sin propietario";
    notaModalInfo.textContent = puedeComprar
        ? "Pod\u00e9s comprarla ahora o pasar."
        : esPropia
          ? "Es tu propiedad. Administr\u00e1 el alquiler desde Bono de propiedades."
          : tienePropietario
            ? "El alquiler se cobra automáticamente cuando un rival cae aquí."
            : "Solo podés comprarla al caer en ella durante tu turno.";
    btnModalInfoAccion.disabled = !puedeComprar;
    btnModalInfoAccion.textContent = textoAccion;
}

function actualizarDineroJugador(idJugador, dinero, enBancarrota = false) {
    const tarjeta = document.getElementById(String(idJugador));
    const saldo = tarjeta?.querySelector(".dinero");
    if (saldo) saldo.textContent = `$${Number(dinero).toLocaleString("es-AR")}`;
    if (String(idJugador) === String(window.usuarioSesionId)) {
        window.dineroUsuarioSesion = Number(dinero);
    }
    if (!tarjeta || !enBancarrota) return;

    tarjeta.dataset.bancarrota = "true";
    tarjeta.classList.add("jugador--bancarrota");
    if (!tarjeta.querySelector(".estado-bancarrota")) {
        const etiqueta = document.createElement("span");
        etiqueta.className = "estado-bancarrota";
        etiqueta.textContent = "En quiebra";
        tarjeta.querySelector(".datos-jugador")?.appendChild(etiqueta);
    }
    if (String(idJugador) === String(window.usuarioSesionId) && !window.usuarioEnBancarrota) {
        window.usuarioEnBancarrota = true;
        bloquearControles();
        cerrarModalCompra();
        mostrarAvisoPartida("Llegaste a tu límite de deudas.");
    }
}

async function cargarInfoPropiedad(posicion, abrirPopup = false) {
    const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/propiedades/${posicion}`, {
        cache: "no-store",
    });
    if (respuesta.status === 404) return null;
    if (!respuesta.ok) throw new Error("No se pudo consultar la propiedad");
    const propiedad = await respuesta.json();
    mostrarInfoPropiedad(propiedad);
    if (abrirPopup && !modalInfoPropiedad.open) modalInfoPropiedad.showModal();
    return propiedad;
}

async function comprarPropiedadActual() {
    if (
        !propiedadSeleccionada ||
        !propiedadSeleccionada.disponible ||
        resolviendoDecisionPropiedad ||
        tiempoRestante <= 0 ||
        String(window.turnoActualId) !== String(window.usuarioSesionId)
    )
        return;
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
            throw new Error(
                "No se pudo conectar con el servidor. Comprobá que la aplicación siga ejecutándose.",
            );
        }
        const cuerpoRespuesta = await respuesta.text();
        let datos = {};
        try {
            datos = cuerpoRespuesta ? JSON.parse(cuerpoRespuesta) : {};
        } catch (error) {
            console.error(
                "El endpoint de compra devolvió una respuesta que no es JSON:",
                respuesta.status,
                cuerpoRespuesta,
            );
        }
        if (!respuesta.ok) {
            console.error(
                "El servidor rechazó la compra:",
                respuesta.status,
                datos,
                cuerpoRespuesta,
            );
            if (datos.codigoError === "DINERO_INSUFICIENTE") {
                cerrarModalCompra();
                mostrarAvisoPartida("No tenés guita para comprar esta propiedad.");
                await finalizarTurno();
                return;
            }
            throw new Error(
                datos.error || `El servidor rechazó la compra (HTTP ${respuesta.status}).`,
            );
        }
        mostrarInfoPropiedad(datos);
        cerrarModalCompra();
        if (datos.dineroJugador != null) {
            actualizarDineroJugador(
                datos.idJugadorPropietario ?? window.usuarioSesionId,
                datos.dineroJugador,
                datos.enBancarrota,
            );
        }
        if (datos.idJugadorActual != null) {
            window.turnoActualId = datos.idJugadorActual;
            actualizarTurnoEnPantalla({
                idJugadorActual: datos.idJugadorActual,
                apodoJugadorActual: "",
            });
        }
    } catch (error) {
        notaModalInfo.textContent = error.message || "No se pudo comprar la propiedad.";
        document.getElementById("modal-propiedad-info").textContent = notaModalInfo.textContent;
        if (
            modalCompra?.open &&
            tiempoRestante > 0 &&
            String(window.turnoActualId) === String(window.usuarioSesionId)
        ) {
            document.getElementById("btn-modal-comprar").disabled = false;
            document.getElementById("btn-modal-pasar").disabled = false;
        }
        resolviendoDecisionPropiedad = false;
    }
}

function agregarAlHistorial(texto, idJugador) {
    const item = document.createElement("li");
    item.textContent = texto;
    const color = obtenerColorFichaJugador(idJugador);
    if (color) item.dataset.color = color;
    const lista = document.querySelector(".historial__lista");
    if (!lista) return;
    lista.querySelector(".historial__vacio")?.remove();
    lista.prepend(item);
}

function cerrarModalCompra() {
    document.getElementById("btn-modal-comprar").disabled = true;
    document.getElementById("btn-modal-pasar").disabled = true;
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
        if (!propiedad) {
            return;
        }
    } catch (error) {
        console.error(error);
        notaModalInfo.textContent =
            "No se pudo verificar si la propiedad está disponible para comprar.";
        return;
    }

    if (propiedad.disponible !== true || String(window.turnoActualId) !== String(idJugador)) return;

    const saldoActual =
        document.getElementById(String(idJugador))?.querySelector(".dinero")?.textContent ?? "0";
    const dineroDisponible = Number(saldoActual.replace(/[^\d-]/g, "")) || 0;
    if (dineroDisponible < Number(propiedad.precioCompra)) {
        mostrarAvisoPartida("No tenés guita para comprar esta propiedad.");
        await finalizarTurno().catch((error) => console.error("No se pudo pasar el turno:", error));
        return;
    }

    document.getElementById("modal-propiedad-titulo").textContent = propiedad.nombre;
    document.getElementById("modal-propiedad-info").textContent =
        `Sin dueño · Precio: $${propiedad.precioCompra} · Alquiler: $${propiedad.alquiler}`;
    if (tiempoRestante <= 0) return;
    document.getElementById("btn-modal-comprar").disabled = false;
    document.getElementById("btn-modal-pasar").disabled = false;
    document.getElementById("tiempo-compra").textContent = tiempoRestante;
    modalCompra.showModal();
}

document.querySelectorAll(".casillero[data-casillero]").forEach((casillero) => {
    casillero.addEventListener("click", (event) => {
        if (event.target.closest(".ficha")) return;
        cargarInfoPropiedad(casillero.dataset.casillero, true).catch(console.error);
    });
});
document
    .getElementById("btn-cerrar-info-propiedad")
    ?.addEventListener("click", () => modalInfoPropiedad.close());
modalInfoPropiedad?.addEventListener("click", (event) => {
    if (event.target === modalInfoPropiedad) modalInfoPropiedad.close();
});
btnModalInfoAccion?.addEventListener("click", comprarPropiedadActual);
document.getElementById("btn-modal-comprar")?.addEventListener("click", comprarPropiedadActual);
document.getElementById("btn-modal-pasar")?.addEventListener("click", pasarTurnoPropiedad);

document.getElementById("btn-bono-propiedades")?.addEventListener("click", () => {
    if (!modalBonoPropiedades.open) modalBonoPropiedades.showModal();
    cargarPropiedadesPropias();
});
document
    .getElementById("btn-cerrar-bono-propiedades")
    ?.addEventListener("click", () => modalBonoPropiedades.close());
document
    .getElementById("btn-cerrar-vender-propiedad")
    ?.addEventListener("click", () => modalVenderPropiedad.close());
document
    .getElementById("btn-vender-al-banco")
    ?.addEventListener("click", () => venderPropiedadBono(propiedadParaVender));
document.getElementById("btn-enviar-oferta")?.addEventListener("click", enviarOfertaVenta);
document
    .getElementById("btn-aceptar-oferta")
    ?.addEventListener("click", () => responderOfertaVenta(true));
document
    .getElementById("btn-rechazar-oferta")
    ?.addEventListener("click", () => responderOfertaVenta(false));
modalBonoPropiedades?.addEventListener("click", (event) => {
    if (event.target === modalBonoPropiedades) modalBonoPropiedades.close();
});
modalVenderPropiedad?.addEventListener("click", (event) => {
    if (event.target === modalVenderPropiedad) modalVenderPropiedad.close();
});

async function cargarPropiedadesPropias() {
    if (!listaPropiedadesPropias || !mensajeBonoPropiedades) return;
    mensajeBonoPropiedades.textContent = "Cargando tus propiedades...";
    listaPropiedadesPropias.replaceChildren();
    try {
        const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/propiedades`, {
            cache: "no-store",
            credentials: "same-origin",
        });
        const propiedades = await respuesta.json();
        if (!respuesta.ok)
            throw new Error(propiedades.error || "No se pudieron cargar las propiedades.");
        if (!Array.isArray(propiedades))
            throw new Error("El servidor devolvió una lista de propiedades inválida.");
        if (!propiedades.length) {
            propiedadesPropiasActuales = [];
            mensajeBonoPropiedades.textContent = "No hay propiedades para mostrar.";
            return;
        }
        propiedades.forEach(actualizarEtiquetaPrecioPropiedad);
        propiedadesPropiasActuales = propiedades.filter(
            (propiedad) => String(propiedad.propietarioId) === String(window.usuarioSesionId),
        );
        mensajeBonoPropiedades.textContent = propiedadesPropiasActuales.length
            ? "Para subir alquiler necesit\u00e1s toda la localidad y estar en tu turno."
            : "Todavía no tenés propiedades.";
        propiedadesPropiasActuales.forEach((propiedad) => {
            listaPropiedadesPropias.appendChild(crearTarjetaPropiedad(propiedad));
        });
    } catch (error) {
        mensajeBonoPropiedades.textContent =
            error.message || "No se pudieron cargar tus propiedades.";
    }
}

function crearTarjetaPropiedad(propiedad) {
    const tarjeta = document.createElement("article");
    tarjeta.className = "propiedad-propia";
    tarjeta.dataset.grupo = propiedad.grupo || "";
    tarjeta.style.setProperty(
        "--color-localidad",
        posicionesParadas.has(Number(propiedad.posicion))
            ? "#fff0c2"
            : coloresLocalidades[propiedad.grupo] || "#e2e9f2",
    );
    const titulo = document.createElement("h3");
    titulo.textContent = propiedad.nombre;
    const ubicacion = document.createElement("p");
    const nombreLocalidad = posicionesParadas.has(Number(propiedad.posicion))
        ? "Parada de bondi"
        : nombresLocalidades[propiedad.grupo] || propiedad.descripcion || "Localidad";
    ubicacion.textContent = `${nombreLocalidad} \u00b7 Casillero #${propiedad.posicion}`;
    const datos = document.createElement("dl");
    datos.className = "propiedad-propia__datos";
    agregarDatoPropiedad(datos, "Compra", propiedad.precioCompra);
    agregarDatoPropiedad(datos, "Alquiler base", propiedad.alquilerBase);
    agregarDatoPropiedad(datos, "Alquiler actual", propiedad.alquiler);
    agregarDatoPropiedad(
        datos,
        "Due\u00f1o",
        propiedad.propietarioId == null
            ? "Sin due\u00f1o"
            : String(propiedad.propietarioId) === String(window.usuarioSesionId)
              ? "Vos"
              : propiedad.propietario,
    );
    const esPropia = String(propiedad.propietarioId) === String(window.usuarioSesionId);
    if (!esPropia) {
        tarjeta.append(titulo, ubicacion, datos);
        return tarjeta;
    }
    const nivel = Number(propiedad.nivelMejora ?? 0);
    if (!posicionesParadas.has(Number(propiedad.posicion))) {
        agregarDatoPropiedad(datos, "Nivel", `${nivel} / ${nivelesAlquiler.length}`);
    }
    const acciones = document.createElement("div");
    acciones.className = "propiedad-propia__acciones";
    const botonSubir = document.createElement("button");
    botonSubir.type = "button";
    botonSubir.className = "btn-casillero btn-casillero--secundario";
    const localidadCompleta = tieneLocalidadCompleta(propiedad);
    const esTurnoPropio = String(window.turnoActualId) === String(window.usuarioSesionId);
    const costoMejora = nivelesAlquiler[nivel];
    botonSubir.disabled =
        !localidadCompleta ||
        nivel >= nivelesAlquiler.length ||
        posicionesParadas.has(Number(propiedad.posicion)) ||
        !esTurnoPropio ||
        window.usuarioEnBancarrota ||
        Number(window.dineroUsuarioSesion) < costoMejora;
    botonSubir.textContent = esTurnoPropio ? "Subir alquiler" : "Disponible en tu turno";
    botonSubir.addEventListener("click", () =>
        subirAlquilerPropiedad(propiedad.posicion, botonSubir),
    );
    const botonVender = document.createElement("button");
    botonVender.type = "button";
    botonVender.className = "btn-casillero btn-casillero--principal";
    botonVender.disabled = Boolean(window.usuarioEnBancarrota);
    botonVender.textContent = "Vender";
    botonVender.addEventListener("click", () => abrirOpcionesVenta(propiedad));
    acciones.append(botonSubir, botonVender);
    tarjeta.append(titulo, ubicacion, datos, acciones);
    return tarjeta;
}

function tieneLocalidadCompleta(propiedad) {
    const posiciones = posicionesLocalidades[propiedad.grupo] || [];
    return (
        posiciones.length > 0 &&
        posiciones.every((posicion) =>
            propiedadesPropiasActuales.some(
                (propiedadPropia) => Number(propiedadPropia.posicion) === posicion,
            ),
        )
    );
}

function agregarDatoPropiedad(lista, etiqueta, valor) {
    const fila = document.createElement("div");
    const termino = document.createElement("dt");
    termino.textContent = etiqueta;
    const dato = document.createElement("dd");
    dato.textContent =
        typeof valor === "number" ? `$${Number(valor).toLocaleString("es-AR")}` : valor;
    fila.append(termino, dato);
    lista.appendChild(fila);
}

async function subirAlquilerPropiedad(posicion, boton) {
    boton.disabled = true;
    try {
        const alquilerAnterior = propiedadesPropiasActuales.find(
            (propiedad) => Number(propiedad.posicion) === Number(posicion),
        )?.alquiler;
        const respuesta = await fetch(
            `${basePath}/partida/${codigoPartida}/propiedades/${posicion}/subir-alquiler`,
            { method: "POST", credentials: "same-origin" },
        );
        const propiedad = await respuesta.json();
        if (!respuesta.ok) throw new Error(propiedad.error || "No se pudo subir el alquiler.");
        actualizarEtiquetaPrecioPropiedad(propiedad);
        actualizarDineroJugador(
            propiedad.idJugador ?? window.usuarioSesionId,
            propiedad.dineroJugador,
            propiedad.enBancarrota,
        );
        await cargarPropiedadesPropias();
    } catch (error) {
        mensajeBonoPropiedades.textContent = error.message || "No se pudo subir el alquiler.";
        boton.disabled = false;
    }
}

function abrirOpcionesVenta(propiedad) {
    propiedadParaVender = propiedad;
    nombrePropiedadEnVenta.textContent = `${propiedad.nombre} \u00b7 Alquiler actual: $${propiedad.alquiler}`;
    document.getElementById("btn-vender-al-banco").textContent =
        `Vender al banco por $${Number(propiedad.precioVenta).toLocaleString("es-AR")}`;
    document.getElementById("oferta-precio").value = "";
    document.getElementById("oferta-jugador").value = "";
    mensajeOfertaVenta.textContent = "";
    const selector = document.getElementById("oferta-jugador");
    Array.from(selector.options).forEach((opcion) => {
        if (!opcion.value) return;
        opcion.hidden = opcion.dataset.bancarrota === "true";
        opcion.disabled = opcion.hidden;
    });
    if (!modalVenderPropiedad.open) modalVenderPropiedad.showModal();
}

async function venderPropiedadBono(propiedad) {
    if (!propiedad) return;
    if (!window.confirm(`¿Vender ${propiedad.nombre} al banco por $${propiedad.precioVenta}?`))
        return;
    try {
        const respuesta = await fetch(
            `${basePath}/partida/${codigoPartida}/propiedades/${propiedad.posicion}/vender`,
            { method: "POST", credentials: "same-origin" },
        );
        const datos = await respuesta.json();
        if (!respuesta.ok) throw new Error(datos.error || "No se pudo vender la propiedad.");
        actualizarEtiquetaPrecioPropiedad(datos);
        actualizarDineroJugador(datos.idJugador ?? window.usuarioSesionId, datos.dineroJugador);
        modalVenderPropiedad.close();
        await cargarPropiedadesPropias();
    } catch (error) {
        mensajeBonoPropiedades.textContent = error.message || "No se pudo vender la propiedad.";
    }
}

async function enviarOfertaVenta() {
    if (!propiedadParaVender) return;
    const boton = document.getElementById("btn-enviar-oferta");
    const idJugadorComprador = document.getElementById("oferta-jugador").value;
    const precio = Number(document.getElementById("oferta-precio").value);
    if (!idJugadorComprador || !Number.isInteger(precio) || precio < 0) {
        mensajeOfertaVenta.textContent = "Elegí un jugador e ingresá un precio válido.";
        return;
    }
    boton.disabled = true;
    try {
        const respuesta = await fetch(
            `${basePath}/partida/${codigoPartida}/propiedades/${propiedadParaVender.posicion}/ofertas`,
            {
                method: "POST",
                credentials: "same-origin",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ idJugadorComprador: Number(idJugadorComprador), precio }),
            },
        );
        const datos = await respuesta.json();
        if (!respuesta.ok) throw new Error(datos.error || "No se pudo enviar la oferta.");
        mensajeOfertaVenta.textContent =
            "Oferta enviada. La propiedad se transfiere cuando el otro jugador la acepte.";
        setTimeout(() => {
            if (modalVenderPropiedad.open) modalVenderPropiedad.close();
        }, 1500);
    } catch (error) {
        mensajeOfertaVenta.textContent = error.message || "No se pudo enviar la oferta.";
    } finally {
        boton.disabled = false;
    }
}

async function revisarOfertasPendientes() {
    if (!codigoPartida || modalOfertaRecibida?.open) return;
    try {
        const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/ofertas-pendientes`, {
            cache: "no-store",
            credentials: "same-origin",
        });
        if (!respuesta.ok) return;
        const ofertas = await respuesta.json();
        if (!ofertas.length) return;
        ofertaActiva = ofertas[0];
        document.getElementById("oferta-recibida-detalle").textContent =
            `${ofertaActiva.apodoVendedor} te ofrece ${ofertaActiva.nombrePropiedad} por $${ofertaActiva.precio}.`;
        mensajeOfertaRecibida.textContent = "";
        modalOfertaRecibida.showModal();
    } catch (error) {
        console.warn("No se pudieron revisar las ofertas de propiedades.", error);
    }
}

async function responderOfertaVenta(aceptar) {
    if (!ofertaActiva) return;
    const botones = [
        document.getElementById("btn-aceptar-oferta"),
        document.getElementById("btn-rechazar-oferta"),
    ];
    botones.forEach((boton) => {
        boton.disabled = true;
    });
    try {
        const respuesta = await fetch(
            `${basePath}/partida/${codigoPartida}/ofertas/${ofertaActiva.idOferta}/responder`,
            {
                method: "POST",
                credentials: "same-origin",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ aceptar }),
            },
        );
        const datos = await respuesta.json();
        if (!respuesta.ok) throw new Error(datos.error || "No se pudo responder la oferta.");
        if (aceptar) {
            actualizarEtiquetaPrecioPropiedad(datos);
            actualizarDineroJugador(datos.idJugador, datos.dineroJugador, datos.enBancarrota);
            actualizarDineroJugador(
                datos.idVendedor,
                datos.dineroVendedor,
                datos.bancarrotaVendedor,
            );
        }
        modalOfertaRecibida.close();
        ofertaActiva = null;
        if (modalBonoPropiedades?.open) cargarPropiedadesPropias();
    } catch (error) {
        mensajeOfertaRecibida.textContent = error.message || "No se pudo responder la oferta.";
    } finally {
        botones.forEach((boton) => {
            boton.disabled = false;
        });
    }
}

window.setInterval(revisarOfertasPendientes, 10000); // 2500

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
    aplicarTurnoDTO(turnoDTO);

    console.log("Siguiente jugador:", turnoDTO.apodoJugadorActual);
    console.log("Ronda actual:", turnoDTO.rondaActual);

    return turnoDTO;
}

let intervaloReloj = null;
let tiempoRestante = 30;

function iniciarRelojLocal(segundos) {
    const elementoReloj = document.getElementById("cronometro-nro");
    if (!elementoReloj) return;
    const duracion = Number(segundos);
    tiempoRestante = Number.isFinite(duracion) && duracion >= 0 ? duracion : 30;
    clearInterval(intervaloReloj); // Limpia cualquier reloj anterior
    actualizarTextoReloj(elementoReloj);

    intervaloReloj = setInterval(() => {
        tiempoRestante--;
        if (tiempoRestante < 0) tiempoRestante = 0;
        actualizarTextoReloj(elementoReloj);

        if (tiempoRestante <= 5 && tiempoRestante > 0) {
            elementoReloj.style.color = "#d9534f";
            elementoReloj.style.fontWeight = "bold";
        } else {
            elementoReloj.style.color = "";
            elementoReloj.style.fontWeight = "normal";
        }
        sincronizarTiempoPopupCompra();

        if (tiempoRestante === 0) {
            clearInterval(intervaloReloj);
            bloquearControles();
            cerrarModalCompra();
            if (propiedadSeleccionada) mostrarInfoPropiedad(propiedadSeleccionada);
        }
    }, 1000);
}

function sincronizarTiempoPopupCompra() {
    if (!modalCompra?.open) return;
    const contadorCompra = document.getElementById("tiempo-compra");
    if (contadorCompra) contadorCompra.textContent = String(Math.max(0, tiempoRestante));
}

function actualizarTextoReloj(elemento) {
    if (!elemento) return;
    elemento.innerText = `00:${tiempoRestante.toString().padStart(2, "0")}`;
}

function prepararRelojSinIniciar() {
    const elementoReloj = document.getElementById("cronometro-nro");
    clearInterval(intervaloReloj);
    tiempoRestante = 30;
    actualizarTextoReloj(elementoReloj);
}

function bloquearControles() {
    const btnTirar = document.getElementById("btn-tirar");

    if (btnTirar) {
        btnTirar.disabled = true;
    }

    document.querySelectorAll(".btn-accion").forEach((btn) => {
        if (btn.id !== "btn-bono-propiedades" && btn.id !== "btn-abandonar") {
            btn.disabled = true;
        }
    });
}

function desbloquearControles() {
    if (partidaFinalizada || window.usuarioEnBancarrota) {
        bloquearControles();
    } else {
        if (botonTirar) {
            botonTirar.disabled = false;
        }

        document.querySelectorAll(".btn-accion").forEach((boton) => {
            if (boton.id !== "btn-bono-propiedades" && boton.id !== "btn-abandonar") {
                boton.disabled = false;
            }
        });

        actualizarBotonTirada();
    }
}

// Alineo el historial con el tamaño del tablero.
function alinearHistorialConTablero() {
    const tablero = document.querySelector(".tablero");
    const lateral = document.querySelector(".partida__lateral");
    if (!tablero || !lateral) return;

    const actualizar = () => {
        if (window.matchMedia("(max-width: 780px)").matches) {
            lateral.style.removeProperty("--historial-alto");
            lateral.style.removeProperty("--historial-desplazamiento");
            return;
        }
        const limites = tablero.getBoundingClientRect();
        const desplazamiento = Math.max(0, limites.top - lateral.getBoundingClientRect().top);
        lateral.style.setProperty("--historial-alto", `${limites.height}px`);
        lateral.style.setProperty("--historial-desplazamiento", `${desplazamiento}px`);
    };

    const observador = new ResizeObserver(actualizar);
    observador.observe(tablero);
    observador.observe(document.querySelector(".partida__main"));
    window.addEventListener("resize", actualizar);
    actualizar();
}

window.addEventListener("DOMContentLoaded", () => {
    alinearHistorialConTablero();
    posicionesInicialesSincronizadas = true;
    pintarLocalidadesTablero();
    if (Array.isArray(window.propiedadesIniciales)) {
        window.propiedadesIniciales.forEach(actualizarEtiquetaPrecioPropiedad);
    }
    actualizarBotonTirada();
    conectarWebSocketTurnos(basePath, codigoPartida);
    iniciarSincronizacionTurno(basePath, codigoPartida);
    iniciarSincronizacionPosiciones();
    revisarOfertasPendientes();
    if (window.turnoActualId) {
        const cardActiva = document.getElementById(window.turnoActualId);
        if (cardActiva) {
            cardActiva.classList.add("turno-activo");
        }
    }

    window.turnoActualId = window.turnoActualId || null;
    window.idJugadorTurnoAnterior =
        window.turnoActualId == null ? undefined : String(window.turnoActualId);
    if (!window.turnoInicialSincronizado) prepararRelojSinIniciar();
    if (String(window.turnoActualId) !== String(window.usuarioSesionId)) {
        bloquearControles();
    } else if (window.usuarioEnBancarrota) {
        bloquearControles();
        mostrarAvisoPartida("Llegaste a tu límite de deudas.");
    }
});

//popup de abandonar partida
const botonAbandonar = document.getElementById("btn-abandonar");
const modalAbandonar = document.getElementById("modal-abandonar");
const botonCancelarAbandono = document.getElementById("btn-cancelar-abandono");
const botonConfirmarAbandono = document.getElementById("btn-confirmar-abandono");
const mensajeAbandono = document.getElementById("mensaje-abandono");

let abandonandoPartida = false;

botonAbandonar.addEventListener("click", () => {
    mensajeAbandono.textContent = "";
    modalAbandonar.showModal();
});

botonCancelarAbandono.addEventListener("click", () => {
    modalAbandonar.close();
});

botonConfirmarAbandono.addEventListener("click", confirmarAbandono);

async function confirmarAbandono() {
    if (abandonandoPartida) {
        return;
    }

    abandonandoPartida = true;
    botonConfirmarAbandono.disabled = true;
    botonCancelarAbandono.disabled = true;
    mensajeAbandono.textContent = "";
    botonConfirmarAbandono.textContent = "Abandonando...";

    try {
        const respuesta = await fetch(
            `${basePath}/partida/${encodeURIComponent(codigoPartida)}/abandonar`,
            {
                method: "POST",
                credentials: "same-origin",
                headers: {
                    Accept: "application/json",
                },
            },
        );

        const datos = await respuesta.json().catch(() => ({}));

        if (!respuesta.ok) {
            throw new Error(datos.error || "No se pudo abandonar la partida.");
        }

        window.location.assign(`${basePath}/home`);
    } catch (error) {
        mensajeAbandono.textContent = error.message;
    } finally {
        abandonandoPartida = false;
        botonConfirmarAbandono.disabled = false;
        botonCancelarAbandono.disabled = false;
        botonConfirmarAbandono.textContent = "Sí, abandonar";
    }
}

// Evitar cerrar el popup mientras se procesa el abandono.
modalAbandonar.addEventListener("cancel", (evento) => {
    if (abandonandoPartida) {
        evento.preventDefault();
    }
});

function mostrarJugadorAbandonado(idJugador) {
    const tarjeta = document.getElementById(String(idJugador));

    if (tarjeta && !tarjeta.querySelector(".estado-abandono")) {
        const nombre = tarjeta.querySelector(".nombre-jugador");

        if (nombre) {
            const etiqueta = document.createElement("span");
            etiqueta.className = "estado-abandono";
            etiqueta.textContent = "ABANDONÓ";

            nombre.insertAdjacentElement("afterend", etiqueta);

            tarjeta.classList.remove("turno-activo");
        }
    }
}
const jugadoresAbandonados = new Set();

function eliminarFichaJugadorAbandonado(idJugador) {
    jugadoresAbandonados.add(String(idJugador));

    const ficha = document.querySelector(`.ficha[data-jugador-id="${idJugador}"]`);

    if (ficha) {
        ficha.remove();
    }
}

async function sincronizarPropiedadesTrasAbandono(idJugador) {
    try {
        const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/propiedades`, {
            cache: "no-store",
            credentials: "same-origin",
        });

        if (!respuesta.ok) {
            throw new Error("No se pudieron actualizar las propiedades.");
        }

        const propiedades = await respuesta.json();

        if (!Array.isArray(propiedades)) {
            throw new Error("La lista de propiedades no es válida.");
        }

        propiedades.forEach((propiedad) => {
            const posicion = Number(propiedad.posicion);
            const estadoAnterior = estadoPropiedadesConocido.get(posicion);

            if (
                estadoAnterior?.propietarioId === String(idJugador) &&
                propiedad.propietarioId == null
            ) {
                estadoPropiedadesConocido.delete(posicion);
            }

            actualizarEtiquetaPrecioPropiedad(propiedad);
        });

        if (propiedadSeleccionada) {
            const actualizada = propiedades.find(
                (propiedad) =>
                    Number(propiedad.posicion) === Number(propiedadSeleccionada.posicion),
            );

            if (actualizada) {
                mostrarInfoPropiedad(actualizada);
            }
        }

        if (modalBonoPropiedades?.open) {
            await cargarPropiedadesPropias();
        }
    } catch (error) {
        console.error("Error al sincronizar propiedades tras abandono:", error);
    }
}

let partidaFinalizada = false;

function mostrarVictoria(datos) {
    partidaFinalizada = true;

    clearInterval(intervaloReloj);

    if (botonTirar) {
        botonTirar.disabled = true;
    }

    document.querySelectorAll(".btn-accion").forEach((boton) => {
        boton.disabled = true;
    });

    const mensaje = document.getElementById("mensaje-victoria");

    mensaje.textContent = `${datos.apodo} abandonó la partida. ¡Sos el ganador!`;

    const modal = document.getElementById("modal-victoria");

    if (!modal.open) {
        modal.showModal();
    }
}
