import { conectarTriviaPartida } from "./trivia-partida.js?v=7";
import { inicializarChat } from "./chat-partida.js?v=7";
import { crearHistorialEventosGlobales } from "./historial-eventos-globales.js";
import { inicializarIntercambios } from "./intercambios.js?v=8";
import { crearCargaPropiedades } from "./propiedades-vista.js";
import { validarRespuestaDeuda, sincronizarEsperaQuiebra } from "./deuda.js?v=saldo-0";
import { esperarContinuacion } from "./plazo-evento.js";
import { animarPasoFicha, ocultarAvisoPartida, presentarAvisoPartida, reproducirCambioDeSaldo, reproducirAccionPartida, reproducirIntercambioConfirmado, reproducirDadosGlobales, reproducirComisaria, reproducirChatEntrante, reproducirDadosTablero, animarCompraCasillero, animarPagoEntreJugadores, animarPasoPorInicio } from "./efectos-compra.js?v=partida-18";

document.addEventListener("click", evento => {
    if (!evento.isTrusted) return;
    const omitir = "#btn-modal-pasar, #btn-rechazar-oferta, #rechazar-intercambio";
    const boton = evento.target.closest(`#btn-continuar-evento, #global-continuar, #btn-aceptar-oferta, ${omitir}`);
    if (boton && !boton.disabled) reproducirAccionPartida(boton.matches(omitir));
}, { capture: true });
import { aceptarEstadoEvento, puedeTirarEnEvento, seleccionarTurnoMasReciente } from "./eventos-globales.js?v=turno-7";

const registrarEventoGlobalEnHistorial = crearHistorialEventosGlobales(agregarAlHistorial);
let estadoEventoGlobalCliente = null;
let turnoPendienteDuranteEvento = null;
let gestionIntercambios = null;
let chatPartida = null;
let eventoGlobalSincronizado = false;
let peticionEventoGlobal = false;
let mostrandoTiradaGlobal = false;
let consultaEventoGlobalEnCurso = false;
const tarjetasGlobalesMostradas = new Set();
const comprasGlobalesMostradas = new Set();
let compraEventoActual = null;
const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
const urlParams = new URLSearchParams(window.location.search);
const codigoPartida = urlParams.get("codigo") || window.codigoSala || "";

const botonTirar = document.getElementById("btn-tirar");
if (botonTirar) botonTirar.disabled = true;

let estaTirando = false;
let posicionesInicialesSincronizadas = false;
const avisosPartidaPendientes = [];
const quiebrasNotificadas = new Set();

let yaTiroEnEsteTurno = false;

//guarda si se conoce el estado de la deuda, los datos de la deuda y posibles errores
let estadoDeudaLocal = { estado: "desconocido", deuda: null, error: null };
//indica si ya hay una consulta en el servidor.
let consultaDeudaEnCurso = null;
//indica si es necesario volver a consultar la deuda.
let actualizacionDeudaPendiente = false;
//detecta si hubo cambios mientras se consultaba la deuda
let revisionDeudaLocal = 0;

// comprueba que los datos enviados por el servidor sean correctos.
function consultarDeudaPendiente(huboCambio = false) {
    if (!codigoPartida || partidaFinalizada) return Promise.resolve();
    //Comprueba si hubo un cambio en la partida. Ya que la info. anterior de la deuda podria dejar de ser valida.
    if (huboCambio) {
        revisionDeudaLocal++;
        actualizacionDeudaPendiente = true;
        estadoDeudaLocal.estado = "desconocido";
        actualizarAvisoEsperaQuiebra();
        actualizarBotonTirada();
    }
    //si ya existe una consulta en curso, devuelve esa misma, en vez de iniciar otra.
    if (consultaDeudaEnCurso) return consultaDeudaEnCurso;
    actualizacionDeudaPendiente = true;
    consultaDeudaEnCurso = Promise.resolve()
        .then(async () => {
            do {
                actualizacionDeudaPendiente = false;
                const revisionConsultada = revisionDeudaLocal;
                try {
                    //obtiene la deuda pendiente. Realiza la solicitud HTTP.
                    const respuesta = await fetch(
                        `${basePath}/partida/${encodeURIComponent(codigoPartida)}/deuda-pendiente`,
                        {
                            credentials: "same-origin",
                            cache: "no-store",
                            //indica que espera recibir un dato JSON
                            headers: { Accept: "application/json" },
                        },
                    );
                    //Si se devuelve un error en el HTTP, interrumpe el procesamiento y lanza excepcion
                    if (!respuesta.ok)
                        throw new Error(
                            `No se pudo consultar la deuda (HTTP ${respuesta.status}).`,
                        );
                    //valida los datos recibidos
                    const deuda = validarRespuestaDeuda(await respuesta.json());
                    // Un cambio durante la petición exige consultar otra vez antes de habilitar los dados.
                    if (revisionConsultada === revisionDeudaLocal) {
                        estadoDeudaLocal = { estado: "actualizado", deuda, error: null };
                    }
                } catch (error) {
                    if (revisionConsultada === revisionDeudaLocal) {
                        if (estadoDeudaLocal.error?.message !== error.message) {
                            console.warn("No se pudo sincronizar la deuda del jugador.", error);
                        }
                        estadoDeudaLocal = { ...estadoDeudaLocal, estado: "error", error };
                    }
                }
                actualizarBotonTirada();
                actualizarAvisoDeuda();
            } while (actualizacionDeudaPendiente);
        })
        //Libera la consulta y comprueba si quedó otra pendiente
        .finally(() => {
            consultaDeudaEnCurso = null;
            if (actualizacionDeudaPendiente) return consultarDeudaPendiente();
        });
    return consultaDeudaEnCurso;
}

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
        botonTirar.textContent = "Esperando resultado…";
        botonTirar.setAttribute("aria-busy", "true");
        const claveTurnoDeTirada = window.claveTurnoActual;

        try {
            const resultado = await obtenerResultadoDados();

            console.log("Resultado completo:", resultado);

            yaTiroEnEsteTurno = claveTurnoDeTirada === window.claveTurnoActual;

            console.log("Nueva posición:", resultado.nuevaPosicion);
            await procesarMovimiento(resultado);
        } catch (error) {
            console.error("Error al tirar los dados o mover la ficha:", error);
            if (notaModalInfo)
                notaModalInfo.textContent = error.message || "No se pudo completar la tirada.";
        } finally {
            estaTirando = false;
            botonTirar.textContent = "Tirar dados";
            botonTirar.removeAttribute("aria-busy");
            actualizarBotonTirada();
        }
    });
}

function moverFichaEnTablero(idJugador, nuevaPosicion, efecto) {
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
    if (ficha.parentElement === casilleroDestino) return true;
    casilleroDestino.appendChild(ficha);
    if (efecto) animarPasoFicha(ficha, efecto === "llegada", posicionesPropiedad.has(Number(nuevaPosicion)));
    if (efecto === "llegada") {
        // Brillo del color de la ficha alrededor del peón al llegar.
        const colorFicha = getComputedStyle(ficha).getPropertyValue("--color-ficha").trim() || "#ffffff";
        const sombraBase = "drop-shadow(1px 2px 0 rgb(0 0 0 / 45%))";
        ficha.animate(
            [
                { filter: `${sombraBase} drop-shadow(0 0 2px ${colorFicha}) drop-shadow(0 0 6px ${colorFicha})` },
                { filter: `${sombraBase} drop-shadow(0 0 0 transparent) drop-shadow(0 0 0 transparent)` },
            ],
            { duration: 650, easing: "ease-out" },
        );
    }
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

        const efecto = paso === cantidadPasos - 1 ? "llegada" : "paso";
        const movimientoRealizado = moverFichaEnTablero(idJugador, posicionDestino, efecto);

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
    window.setInterval(solicitarPosicionesPartida, 10000);
    5000;
}

function solicitarPosicionesPartida() {
    if (partidaFinalizada) return;
    if (clienteWebSocketPartida?.connected) {
        clienteWebSocketPartida.send(
            `/app/partida/${codigoPartida}/solicitar-posiciones`,
            {},
            JSON.stringify({}),
        );
    }
}

async function actualizarTurnoDesdeServidor(base, codigo) {
    if (partidaFinalizada) return;
    consultarDeudaPendiente();
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
            conectarTriviaPartida({ cliente: stompClient, codigo, base,
                idJugador: window.usuarioSesionId,
                alPremioEntregado: function () { window.location.reload(); },
                estaPreso: function () { return estaPresoEnPantalla(window.usuarioSesionId); },
            });
            consultarResultadosFinales();

            stompClient.subscribe(`/topic/partida/${codigo}/evento-global`, mensaje => {
                aplicarEventoGlobal(JSON.parse(mensaje.body));
            });
            sincronizarEventoGlobal();

            actualizarTurnoDesdeServidor(base, codigo);
            stompClient.subscribe(`/topic/partida/${codigo}/turno`, (mensaje) => {
                const turnoDTO = JSON.parse(mensaje.body);
                aplicarTurnoDTO(turnoDTO);
            });

            // Sincroniza la ruleta entre todos los jugadores de la partida.
            stompClient.subscribe(`/topic/partida/${codigo}/ruleta`, (mensaje) => {
                const estadoRuleta = JSON.parse(mensaje.body);
                document.dispatchEvent(new CustomEvent("ruleta:estado", { detail: estadoRuleta }));
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
                gestionIntercambios?.refrescar();
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
                if (
                    [
                        propiedad.idVendedor,
                        propiedad.idJugadorPropietario,
                        propiedad.idJugador,
                    ].some((id) => id != null && String(id) === String(window.usuarioSesionId))
                ) {
                    consultarDeudaPendiente(true);
                }
            });
            stompClient.subscribe(`/topic/partida/${codigo}/dinero`, (mensaje) => {
                const saldo = JSON.parse(mensaje.body);
                actualizarDineroJugador(saldo.idJugador, saldo.dinero, saldo.enBancarrota);
                actualizarEstadoCarcel(saldo.idJugador, saldo.encarcelado, saldo.intentosCarcel, {
                    tipo: "saldo",
                });
            });
            // Llega cuando a un jugador preso se le acaba el tiempo sin tirar.
            stompClient.subscribe(`/topic/partida/${codigo}/carcel`, (mensaje) => {
                const aviso = JSON.parse(mensaje.body);
                actualizarDineroJugador(aviso.idJugador, aviso.dinero);
                actualizarEstadoCarcel(aviso.idJugador, aviso.encarcelado, aviso.intentosCarcel, {
                    tipo: "tiempo",
                });
            });
            stompClient.subscribe(`/topic/partida/${codigo}/chat`, mensaje => {
                chatPartida?.recibir(JSON.parse(mensaje.body));
            });
            chatPartida?.revisar();
            stompClient.subscribe(`/topic/partida/${codigo}/intercambios`, mensaje => {
                gestionIntercambios?.recibir(JSON.parse(mensaje.body));
            });
            gestionIntercambios?.revisar();
            stompClient.subscribe(`/topic/partida/${codigo}/ofertas`, (mensaje) => {
                const oferta = JSON.parse(mensaje.body);
                if (String(oferta.idComprador) !== String(window.usuarioSesionId)) return;
                if (modalOfertaRecibida?.open) return; // Se muestra al cerrar la oferta actual.
                mostrarOfertaRecibida(oferta);
            });

            // Recupera las ofertas que llegaron antes de conectarse (por ejemplo, al recargar la página).
            revisarOfertasPendientes();
            stompClient.subscribe(`/topic/partida/${codigo}/abandono`, async (mensaje) => {
                const datos = JSON.parse(mensaje.body);
                consultarResultadosFinales();
                if (datos.motivo === "QUIEBRA") {
                    procesarNotificacionQuiebra(datos);
                    await sincronizarPropiedadesTrasAbandono(datos.idJugador);
                    return;
                }

                agregarAlHistorial(`${datos.apodo} abandonó la partida.`, datos.idJugador);

                mostrarJugadorAbandonado(datos.idJugador);

                eliminarFichaJugadorAbandonado(datos.idJugador);

                await sincronizarPropiedadesTrasAbandono(datos.idJugador);

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
const estadoPropiedadesConocido = new Map();
let temporizadorAvisoAlquiler;

function actualizarBotonTirada() {
    if (botonTirar) {
        botonTirar.disabled =
            partidaFinalizada ||
            !eventoGlobalSincronizado || estadoEventoGlobalCliente?.activo ||
            !posicionesInicialesSincronizadas ||
            movimientosDeJugadoresEnCurso.size > 0 ||
            estaTirando ||
            yaTiroEnEsteTurno ||
            window.turnoPausadoPorDeuda ||
            estadoDeudaLocal.estado !== "actualizado" ||
            estadoDeudaLocal.deuda !== null ||
            window.usuarioEnBancarrota ||
            String(window.turnoActualId) !== String(window.usuarioSesionId);
    }
    gestionIntercambios?.actualizarEstado();
    actualizarControlesCarcel();
}

// --- Cárcel ---

const COSTO_FIANZA = 100;
const MAX_INTENTOS_CARCEL = 3;
let pagandoFianza = false;

function estaPresoEnPantalla(idJugador) {
    return document.getElementById(String(idJugador))?.dataset.encarcelado === "true";
}

// Muestra u oculta la etiqueta "Preso" en la tarjeta del jugador.
function pintarEstadoCarcel(tarjeta) {
    const etiqueta = tarjeta?.querySelector(".estado-carcel");
    if (!etiqueta) return;
    const preso = tarjeta.dataset.encarcelado === "true";
    const intentos = Number(tarjeta.dataset.intentosCarcel || 0);
    etiqueta.hidden = !preso;
    etiqueta.textContent = intentos > 0 ? `Preso (${intentos}/${MAX_INTENTOS_CARCEL})` : "Preso";
}

// Guarda el nuevo estado de cárcel de un jugador y deja registro en el historial si cambió.
// "contexto" indica de dónde viene el dato: una tirada ("movimiento") o un aviso de saldo ("saldo").
function actualizarEstadoCarcel(idJugador, encarcelado, intentos, contexto = {}) {
    if (encarcelado == null) return;
    const tarjeta = document.getElementById(String(idJugador));
    if (!tarjeta) return;

    const anterior = {
        encarcelado: tarjeta.dataset.encarcelado === "true",
        intentos: Number(tarjeta.dataset.intentosCarcel || 0),
    };
    const actual = { encarcelado: Boolean(encarcelado), intentos: Number(intentos ?? 0) };

    tarjeta.dataset.encarcelado = String(actual.encarcelado);
    tarjeta.dataset.intentosCarcel = String(actual.intentos);
    pintarEstadoCarcel(tarjeta);
    registrarCambioCarcelEnHistorial(idJugador, anterior, actual, contexto);
    actualizarControlesCarcel();
}

function registrarCambioCarcelEnHistorial(idJugador, anterior, actual, contexto) {
    const nombre = obtenerNombreJugadorHistorial(idJugador);
    const esUsuario = String(idJugador) === String(window.usuarioSesionId);

    if (!anterior.encarcelado && actual.encarcelado) {
        agregarAlHistorial(`${nombre} quedó detenido en la comisaría.`, idJugador);
        if (esUsuario) {
            reproducirComisaria();
            mostrarAvisoPartida(
                "Caíste en la comisaría. En tu próximo turno sacá dobles o pagá $100 de fianza.",
                false, true,
            );
        }
        return;
    }

    if (contexto.tipo === "tiempo") {
        if (actual.encarcelado) {
            agregarAlHistorial(
                `A ${nombre} se le acabó el tiempo estando preso y pierde el intento (${actual.intentos} de ${MAX_INTENTOS_CARCEL}).`,
                idJugador,
            );
        } else {
            agregarAlHistorial(
                `A ${nombre} se le acabó el tiempo en su último intento: le cobran $${COSTO_FIANZA} de fianza y sale en su próximo turno.`,
                idJugador,
            );
        }
        return;
    }

    if (anterior.encarcelado && actual.encarcelado && actual.intentos > anterior.intentos) {
        agregarAlHistorial(
            `${nombre} no sacó dobles y sigue preso (intento ${actual.intentos} de ${MAX_INTENTOS_CARCEL}).`,
            idJugador,
        );
        return;
    }

    if (anterior.encarcelado && !actual.encarcelado) {
        let texto = `${nombre} pagó $${COSTO_FIANZA} de fianza y salió de la comisaría.`;
        if (contexto.tipo === "movimiento" && contexto.dobles) {
            texto = `${nombre} sacó dobles y salió de la comisaría.`;
        } else if (contexto.tipo === "movimiento") {
            texto = `${nombre} no sacó dobles en ${MAX_INTENTOS_CARCEL} intentos: le cobran $${COSTO_FIANZA} de fianza y sale en su próximo turno.`;
        }
        agregarAlHistorial(texto, idJugador);
    }
}

// Muestra el panel de fianza solo si el usuario está preso, es su turno y todavía no tiró.
function actualizarControlesCarcel() {
    const panel = document.getElementById("carcel-turno");
    const boton = document.getElementById("btn-pagar-fianza");
    const texto = document.getElementById("carcel-turno-texto");
    if (!panel || !boton) return;

    const esMiTurno = String(window.turnoActualId) === String(window.usuarioSesionId);
    const mostrar =
        !estadoEventoGlobalCliente?.activo &&
        estaPresoEnPantalla(window.usuarioSesionId) &&
        !window.usuarioEnBancarrota &&
        !partidaFinalizada;
    panel.hidden = !mostrar;
    if (!mostrar) return;

    const alcanza = Number(window.dineroUsuarioSesion ?? 0) >= COSTO_FIANZA;
    const intentos = Number(
        document.getElementById(String(window.usuarioSesionId))?.dataset.intentosCarcel || 0,
    );
    boton.hidden = !esMiTurno || yaTiroEnEsteTurno;
    boton.disabled =
        !alcanza || pagandoFianza || estaTirando || movimientosDeJugadoresEnCurso.size > 0;
    if (texto) {
        const restantes = Math.max(0, MAX_INTENTOS_CARCEL - intentos);
        const detalle = boton.hidden ? `Fianza $${COSTO_FIANZA}.` : "Sacá dobles para salir.";
        texto.textContent = `${restantes} ${restantes === 1 ? "intento restante" : "intentos restantes"} · ${detalle}`;
    }
}

async function pagarFianza() {
    if (pagandoFianza || window.usuarioEnBancarrota || partidaFinalizada) return;
    pagandoFianza = true;
    actualizarControlesCarcel();
    try {
        const respuesta = await fetch(
            `${basePath}/partida/${encodeURIComponent(codigoPartida)}/pagar-fianza`,
            { method: "POST", credentials: "same-origin", headers: { Accept: "application/json" } },
        );
        let datos = {};
        try {
            datos = await respuesta.json();
        } catch (error) {
            datos = {};
        }
        if (!respuesta.ok) {
            mostrarAvisoPartida(datos.error || "No se pudo pagar la fianza.");
            return;
        }
        // El servidor devuelve el saldo exacto (también llega por WebSocket a todos).
        if (datos.dineroJugador != null) {
            actualizarDineroJugador(window.usuarioSesionId, datos.dineroJugador);
        }
        actualizarEstadoCarcel(window.usuarioSesionId, false, 0, { tipo: "saldo" });
        mostrarAvisoPartida(datos.mensaje || "Pagaste la fianza. Ya podés tirar los dados.");
    } catch (error) {
        console.error("Error al pagar la fianza:", error);
        mostrarAvisoPartida("No se pudo pagar la fianza.");
    } finally {
        pagandoFianza = false;
        actualizarControlesCarcel();
    }
}

window.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll(".jugador").forEach(pintarEstadoCarcel);
    document.getElementById("btn-pagar-fianza")?.addEventListener("click", pagarFianza);
    actualizarControlesCarcel();
});

function mostrarAvisoPartida(mensaje, compra = false, silencioso = false) {
    if (!mensaje) return;
    avisosPartidaPendientes.push({ mensaje, compra, silencioso });
    if (!temporizadorAvisoAlquiler) mostrarSiguienteAvisoPartida();
}

function confirmarCompra(nombrePropiedad) {
    mostrarAvisoPartida(nombrePropiedad ? `Compraste ${nombrePropiedad}` : "Compraste la propiedad", true);
}

function mostrarSiguienteAvisoPartida() {
    const aviso = avisosPartidaPendientes.shift();
    if (!aviso) return;
    presentarAvisoPartida(aviso.mensaje, aviso.compra, aviso.silencioso);
    temporizadorAvisoAlquiler = setTimeout(() => {
        ocultarAvisoPartida();
        temporizadorAvisoAlquiler = null;
        mostrarSiguienteAvisoPartida();
    }, aviso.compra ? 2200 : 5000);
}

async function mostrarPopupEvento(idJugador, nombreEvento, descripcionEvento, vencimiento = Date.now() + 30000, exigirTurno = false) {
    if (partidaFinalizada) return;
    const popup =
        String(idJugador) === String(window.usuarioSesionId)
            ? document.getElementById("aviso-evento")
            : null;
    if (!popup) return;

    const botonContinuar = document.getElementById("btn-continuar-evento");
    document.getElementById("aviso-evento-nombre").textContent = nombreEvento;
    document.getElementById("aviso-evento-descripcion").textContent = descripcionEvento;
    popup.setAttribute("aria-hidden", "false");
    popup.classList.add("aviso-evento--visible");
    const continuoEnPlazo = await esperarContinuacion({
        boton: botonContinuar, eventos: document, vencimiento,
        sigueVigente: () => !exigirTurno || String(window.turnoActualId) === String(idJugador),
    });
    popup.classList.remove("aviso-evento--visible");
    popup.setAttribute("aria-hidden", "true");
    if (estadoEventoGlobalCliente?.activo && !debeEsperarPresentacionGlobal()) {
        mostrarEstadoGlobal(estadoEventoGlobalCliente);
    }
    actualizarAvisoDeuda();
    return continuoEnPlazo;
}

function registrarPremioPorInicio(movimiento) {
    const monto = Number(movimiento.dineroPorInicio ?? 0);
    if (monto <= 0) return;

    const nombreJugador =
        document
            .getElementById(String(movimiento.idJugador))
            ?.querySelector(".nombre-jugador")
            ?.textContent.trim() || `Jugador ${movimiento.idJugador}`;
    agregarAlHistorial(
        `${nombreJugador} recibió $${monto} por pasar por Inicio.`,
        movimiento.idJugador,
    );
    animarPasoPorInicio(
        document.querySelector('.casillero[data-casillero="0"]'),
        document.getElementById(String(movimiento.idJugador)),
        monto,
    );
    if (String(movimiento.idJugador) === String(window.usuarioSesionId)) {
        mostrarAvisoPartida(`Recibiste $${monto} por pasar por Inicio.`);
    }
}

function mostrarAvisoAlquiler(aviso, posicion) {
    agregarAlHistorial(
        `${aviso.pagador} pagó $${aviso.monto} de alquiler a ${aviso.propietario} por ${aviso.propiedad}.`,
        aviso.idJugadorPagador,
    );
    animarAlquiler(aviso, posicion);
    if (String(aviso.idJugadorPagador) === String(window.usuarioSesionId)) {
        mostrarAvisoPartida(
            `Se te descontaron $${aviso.monto} de alquiler por ${aviso.propiedad}. Se lo pagaste a ${aviso.propietario}.`,
        );
    }
}

// Monedas que vuelan del que paga el alquiler al dueño de la propiedad.
// El aviso no trae el id del dueño: lo saco de la propiedad donde cayó y, si no está, lo busco por nombre.
function animarAlquiler(aviso, posicion) {
    const tarjetaPagador = document.getElementById(String(aviso.idJugadorPagador));
    const idDueno = catalogoPropiedadesActual.get(Number(posicion))?.propietarioId;
    const tarjetaDueno = (idDueno != null && document.getElementById(String(idDueno)))
        || [...document.querySelectorAll(".jugador")].find(
            (tarjeta) => tarjeta.querySelector(".nombre-jugador")?.textContent.trim() === aviso.propietario,
        );
    animarPagoEntreJugadores(tarjetaPagador, tarjetaDueno, aviso.monto);
}

function aplicarSaldosMovimiento(movimiento) {
    const dobles = Number(movimiento.dado1) === Number(movimiento.dado2);
    (movimiento.saldos ?? []).forEach((saldo) => {
        actualizarDineroJugador(saldo.idJugador, saldo.dinero, saldo.enBancarrota);
        const contexto =
            String(saldo.idJugador) === String(movimiento.idJugador)
                ? { tipo: "movimiento", dobles }
                : { tipo: "saldo" };
        actualizarEstadoCarcel(saldo.idJugador, saldo.encarcelado, saldo.intentosCarcel, contexto);
    });
}

function registrarCaidaEnHistorial(idJugador, posicion) {
    const casillero = document.querySelector(`.casillero[data-casillero="${posicion}"]`);
    const nombreCasillero =
        casillero?.querySelector(".casillero__nombre")?.textContent.replace(/\u00ad/g, "").trim() ||
        `casillero #${posicion}`;
    const nombreJugador =
        document
            .getElementById(String(idJugador))
            ?.querySelector(".nombre-jugador")
            ?.textContent.trim() || `Jugador ${idJugador}`;
    agregarAlHistorial(`${nombreJugador} cay\u00f3 en ${nombreCasillero}.`, idJugador);
}

let numeroTiradaTablero = 0;
let ocultarDadosTablero;

function iniciarAnimacionDadosTablero() {
    const numero = ++numeroTiradaTablero;
    const panel = document.getElementById("dados-tablero");
    const total = document.getElementById("dados-tablero-total");
    const caras = [...panel.querySelectorAll("[data-cara]")];
    const pintarCaras = numeros => caras.forEach((cara, indice) => { cara.dataset.cara = numeros[indice]; });
    const pintarAlAzar = () => pintarCaras(caras.map(() => 1 + Math.floor(Math.random() * 6)));
    clearTimeout(ocultarDadosTablero);
    panel.hidden = false;
    total.textContent = "Tirando…";
    reproducirDadosTablero();

    let giro = [];
    let cambio;
    if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
        pintarAlAzar();
        const sombras = [...panel.querySelectorAll(".dados-tablero__sombra")];
        // Los dados caen desde arriba (grandes, como cerca de la cámara), golpean el tablero y rebotan dos veces.
        // "ease-in" en las bajadas simula la gravedad y "ease-out" en las subidas, el frenado del rebote.
        giro = caras.flatMap((cara, indice) => {
            const lado = indice === 0 ? -1 : 1;
            const demora = indice * 70;
            const caida = cara.animate([
                { transform: `translate(${lado * 46}px, -190px) rotate(${lado * -90}deg) scale(2.2)`, opacity: 0, easing: "ease-in" },
                { opacity: 1, offset: 0.08, easing: "ease-in" },
                { transform: `translate(${lado * 10}px, 0) rotate(${lado * 200}deg) scale(1.12, 0.86)`, offset: 0.46, easing: "ease-out" },
                { transform: `translate(${lado * 5}px, -38px) rotate(${lado * 290}deg) scale(1.06)`, offset: 0.62, easing: "ease-in" },
                { transform: `translate(${lado * 2}px, 0) rotate(${lado * 340}deg) scale(1.06, 0.94)`, offset: 0.76, easing: "ease-out" },
                { transform: `translate(0, -12px) rotate(${lado * 354}deg) scale(1.02)`, offset: 0.86, easing: "ease-in" },
                { transform: `translate(0, 0) rotate(${lado * 360}deg) scale(1)` },
            ], { duration: 950, delay: demora, fill: "backwards" });
            // La sombra queda en el tablero: chica y clara mientras el dado está alto, grande y oscura al tocar.
            const sombra = sombras[indice]?.animate([
                { transform: `translateX(${lado * 46}px) scale(.25)`, opacity: 0, easing: "ease-in" },
                { transform: `translateX(${lado * 10}px) scale(1.05)`, opacity: 1, offset: 0.46, easing: "ease-out" },
                { transform: `translateX(${lado * 5}px) scale(.7)`, opacity: 0.6, offset: 0.62, easing: "ease-in" },
                { transform: `translateX(${lado * 2}px) scale(1)`, opacity: 1, offset: 0.76, easing: "ease-out" },
                { transform: "translateX(0) scale(.88)", opacity: 0.85, offset: 0.86, easing: "ease-in" },
                { transform: "translateX(0) scale(1)", opacity: 1 },
            ], { duration: 950, delay: demora, fill: "backwards" });
            return sombra ? [caida, sombra] : [caida];
        });
        cambio = setInterval(() => {
            if (numero === numeroTiradaTablero) pintarAlAzar();
        }, 75);
    }

    // Las caras sólo cambian durante el rebote, nunca mientras esperamos la red.
    const giroTerminado = Promise.allSettled(giro.map(animacion => animacion.finished))
        .finally(() => clearInterval(cambio));
    return {
        numero, panel, total, pintarCaras,
        giroTerminado,
        detener() {
            clearInterval(cambio);
            giro.forEach(animacion => animacion.cancel());
        },
    };
}

async function mostrarTiradaEnTablero(dadoUno, dadoDos, esTiradaPropia) {
    const valores = [Number(dadoUno), Number(dadoDos)];
    if (!valores.every(valor => Number.isInteger(valor) && valor >= 1 && valor <= 6)) return;

    if (esTiradaPropia && botonTirar) botonTirar.textContent = "Tirando…";
    const tirada = iniciarAnimacionDadosTablero();
    await tirada.giroTerminado;
    tirada.detener();
    if (tirada.numero !== numeroTiradaTablero) return;

    tirada.pintarCaras(valores);
    tirada.total.textContent = `${valores[0]} + ${valores[1]} = ${valores[0] + valores[1]}`;
    ocultarDadosTablero = setTimeout(() => { tirada.panel.hidden = true; }, 1450);
}

async function procesarMovimiento(movimiento) {
    if (partidaFinalizada) return;
    if (movimiento.turnoSiguiente) aplicarTurnoDTO(movimiento.turnoSiguiente);
    const claveTurnoMovimiento = window.claveTurnoActual;
    const vencimientoMovimiento = movimiento.turnoVencido ? Date.now() : Date.now() + tiempoRestante * 1000;
    let permiteCompra = !movimiento.turnoVencido;
    const clave = movimiento.idMovimiento
        ? String(movimiento.idMovimiento)
        : `${movimiento.idJugador}:${movimiento.posicionAnterior}:${movimiento.nuevaPosicion}:${movimiento.dado1}:${movimiento.dado2}`;
    if (movimientosEnCurso.has(clave)) return;
    movimientosEnCurso.add(clave);
    try {
        // Si estaba preso y no salió, el servidor lo deja en el mismo casillero: no se anima nada.
        const seQuedoEnElLugar =
            Number(movimiento.posicionAnterior) === Number(movimiento.nuevaPosicion) &&
            Number(movimiento.posicionCaidaEvento ?? movimiento.nuevaPosicion) ===
                Number(movimiento.posicionAnterior);
        const pasos = seQuedoEnElLugar ? 0 : Number(movimiento.dado1) + Number(movimiento.dado2);
        movimientosDeJugadoresEnCurso.add(String(movimiento.idJugador));
        if (modalDeuda?.open && !declarandoQuiebra) {
            aperturaDeudaPendiente = true;
            modalDeuda.close();
        }
        actualizarBotonTirada();
        if (!movimiento.turnoVencido) {
            const esTiradaPropia = String(movimiento.idJugador) === String(window.usuarioSesionId);
            await mostrarTiradaEnTablero(movimiento.dado1, movimiento.dado2, esTiradaPropia);
        }
        if (partidaFinalizada) return;
        await animarMovimientoFicha(movimiento.idJugador, pasos, movimiento.posicionAnterior);
        if (partidaFinalizada) return;
        const posicionCaida = Number(movimiento.posicionCaidaEvento ?? movimiento.nuevaPosicion);
        if (Number.isInteger(posicionCaida)) {
            moverFichaEnTablero(movimiento.idJugador, posicionCaida, "llegada");
            if (!seQuedoEnElLugar) registrarCaidaEnHistorial(movimiento.idJugador, posicionCaida);
        }
        if (movimiento.nombreEvento && movimiento.descripcionEvento) {
            const nombreJugador =
                document
                    .getElementById(String(movimiento.idJugador))
                    ?.querySelector(".nombre-jugador")
                    ?.textContent.trim() || `Jugador ${movimiento.idJugador}`;
            agregarAlHistorial(
                `${nombreJugador} sacó el evento «${movimiento.nombreEvento}»: ${movimiento.descripcionEvento}`,
                movimiento.idJugador,
            );
            permiteCompra = await mostrarPopupEvento(
                movimiento.idJugador,
                movimiento.nombreEvento,
                movimiento.descripcionEvento,
                vencimientoMovimiento,
                true,
            );
            if (partidaFinalizada) return;
        }
        const posicionFinal = Number(movimiento.nuevaPosicion);
        if (Number.isInteger(posicionFinal)) {
            if (posicionFinal !== posicionCaida) {
                const cantidadCasilleros = document.querySelectorAll(
                    ".casillero[data-casillero]",
                ).length;
                const pasosEvento =
                    (posicionFinal - posicionCaida + cantidadCasilleros) % cantidadCasilleros;
                await animarMovimientoFicha(movimiento.idJugador, pasosEvento, posicionCaida);
                if (partidaFinalizada) return;
                moverFichaEnTablero(movimiento.idJugador, posicionFinal, "llegada");
                registrarCaidaEnHistorial(movimiento.idJugador, posicionFinal);
            }
            aplicarSaldosMovimiento(movimiento);
            registrarPremioPorInicio(movimiento);
            if (movimiento.alquiler) mostrarAvisoAlquiler(movimiento.alquiler, posicionFinal);
            if (permiteCompra && Date.now() < vencimientoMovimiento) {
                await atenderCaidaEnCasillero(movimiento.idJugador, posicionFinal);
            }
        }
        if (String(movimiento.idJugador) === String(window.usuarioSesionId)
                && String(window.turnoActualId) === String(movimiento.idJugador)
                && !movimiento.turnoVencido && claveTurnoMovimiento === window.claveTurnoActual && tiempoRestante > 0
                && !modalCompra?.open && !estadoEventoGlobalCliente?.activo) {
            await consultarDeudaPendiente(true);
            if (estadoDeudaLocal.estado === "actualizado" && !estadoDeudaLocal.deuda
                    && !movimiento.turnoVencido && claveTurnoMovimiento === window.claveTurnoActual && tiempoRestante > 0) {
                await finalizarTurno();
            }
        }
    } catch (error) {
        console.error("Error al animar el movimiento:", error);
    } finally {
        if (String(movimiento.idJugador) === String(window.usuarioSesionId)) {
            consultarDeudaPendiente(true);
        }
        movimientosDeJugadoresEnCurso.delete(String(movimiento.idJugador));
        if (estadoEventoGlobalCliente?.activo) mostrarEstadoGlobal(estadoEventoGlobalCliente);
        actualizarBotonTirada();
        actualizarAvisoDeuda();
    }
}

function sincronizarPosicionesFichas(posiciones) {
    if (!Array.isArray(posiciones)) {
        console.error("La respuesta de posiciones no es una lista válida.");
        return;
    }

    posiciones.forEach((jugador) => {
        const idJugador = String(jugador.idJugador);
        if (jugadoresAbandonados.has(idJugador) || movimientosDeJugadoresEnCurso.has(idJugador))
            return;
        if (jugador.posicion == null) return;
        moverFichaEnTablero(jugador.idJugador, jugador.posicion,
            posicionesInicialesSincronizadas ? "llegada" : undefined);
    });

    posicionesInicialesSincronizadas = true;
    actualizarBotonTirada();
    console.log("Posiciones de las fichas sincronizadas:", posiciones);
}

function actualizarTurnoEnPantalla(turnoDTO) {
    if (partidaFinalizada) return;
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
    if (partidaFinalizada || !turnoDTO || turnoDTO.idJugadorActual == null) return;
    if (estadoEventoGlobalCliente?.activo) {
        turnoPendienteDuranteEvento = seleccionarTurnoMasReciente(turnoPendienteDuranteEvento, turnoDTO);
        return;
    }
    const ronda = Number(turnoDTO.rondaActual ?? 0);
    const finalizados = Number(turnoDTO.turnosFinalizadosEnRonda ?? 0);
    const ultimo = window.ultimoTurnoRecibido;
    const jugadorInicial = window.turnoActualId;
    if (
        ultimo &&
        (ronda < ultimo.ronda || (ronda === ultimo.ronda && finalizados < ultimo.finalizados))
    )
        return;
    window.ultimoTurnoRecibido = { ronda, finalizados };
    const claveTurno = `${turnoDTO.idJugadorActual}:${turnoDTO.rondaActual}:${turnoDTO.turnosFinalizadosEnRonda}`;
    const cambioDeTurno = window.claveTurnoActual !== claveTurno;
    const cambioDeTurnoDelReloj = window.claveTurnoCronometro !== claveTurno;
    if (cambioDeTurno) {
        yaTiroEnEsteTurno = false;
        document.dispatchEvent(new Event("fin-plazo-turno"));
    }
    window.claveTurnoActual = claveTurno;

    if (cambioDeTurno && modalCompra?.open) cerrarModalCompra();
    window.turnoActualId = turnoDTO.idJugadorActual;
    if (cambioDeTurno && (ultimo || String(jugadorInicial) !== String(turnoDTO.idJugadorActual))) {
        consultarDeudaPendiente(true);
    }
    if (cambioDeTurno && modalBonoPropiedades?.open) cargarPropiedadesPropias();
    window.turnoPausadoPorDeuda = Boolean(turnoDTO.pausadoPorDeuda);
    if (window.turnoPausadoPorDeuda) consultarDeudaPendiente(true);
    actualizarAvisoEsperaQuiebra();
    if (window.turnoPausadoPorDeuda) {
        clearInterval(intervaloReloj);
        tiempoRestante = Number(turnoDTO.duracionSegundos ?? tiempoRestante);
        pausarRelojVisual();
    } else if (turnoDTO.duracionSegundos == null) {
        prepararRelojSinIniciar();
        window.turnoInicialSincronizado = true;
    } else {
        iniciarRelojLocal(turnoDTO.duracionSegundos);
        window.claveTurnoCronometro = claveTurno;
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
const notaModalInfo = document.getElementById("popup-info-nota");
const modalBonoPropiedades = document.getElementById("modal-bono-propiedades");
const listaPropiedadesPropias = document.getElementById("lista-propiedades-propias");
const mensajeBonoPropiedades = document.getElementById("bono-propiedades-mensaje");
const modalVenderPropiedad = document.getElementById("modal-vender-propiedad");
const nombrePropiedadEnVenta = document.getElementById("vender-propiedad-nombre");
const botonVentaBanco = document.getElementById("btn-vender-al-banco");
const mensajeVentaBanco = document.getElementById("venta-banco-mensaje");
const mensajeOfertaVenta = document.getElementById("oferta-venta-mensaje");
const modalOfertaRecibida = document.getElementById("modal-oferta-recibida");
const mensajeOfertaRecibida = document.getElementById("oferta-recibida-mensaje");
const modalDeuda = document.getElementById("modal-deuda-pendiente");
const botonDeuda = document.getElementById("btn-deuda-pendiente");
let avisoDeudaPresentado = false;
let aperturaDeudaPendiente = false;
let regresoDesdePropiedadesDeuda = false;
const modalConfirmacionQuiebra = document.getElementById("deuda-confirmacion");
let viendoPropiedadesDuranteEspera = false;
let declarandoQuiebra = false;
let mensajeAccionDeuda = "";

// Actualiza el aviso con datos confirmados, sin borrar la deuda ante errores de consulta.
function actualizarAvisoDeuda() {
    actualizarAvisoEsperaQuiebra();
    if (!modalDeuda || !botonDeuda) return;
    const { estado, deuda } = estadoDeudaLocal;
    if ((estado === "actualizado" && deuda === null) || window.usuarioEnBancarrota || partidaFinalizada) {
        botonDeuda.hidden = true;
        avisoDeudaPresentado = false;
        aperturaDeudaPendiente = false;
        regresoDesdePropiedadesDeuda = false;
        if (!declarandoQuiebra) {
            mensajeAccionDeuda = "";
            if (modalConfirmacionQuiebra.open) modalConfirmacionQuiebra.close();
            if (modalDeuda.open) modalDeuda.close();
        }
        return;
    }
    botonDeuda.hidden = !deuda;
    if (!deuda) return;
    const importe = `$${deuda.montoPendiente.toLocaleString("es-AR")}`;
    botonDeuda.textContent = `Deuda pendiente: ${importe}`;
    document.getElementById("deuda-importe").textContent = importe;
    document.getElementById("deuda-disponible").textContent = `$${Number(window.dineroUsuarioSesion ?? 0).toLocaleString("es-AR")}`;
    document.getElementById("quiebra-mensaje").textContent = mensajeAccionDeuda;
    document.getElementById("deuda-mensaje").textContent = mensajeAccionDeuda ||
        (estado === "error" ? "No se pudo actualizar la deuda. Conservamos la última información disponible." :
            estado === "desconocido" ? "Actualizando la información de la deuda…" : "");
    document.getElementById("btn-cerrar-deuda").disabled = declarandoQuiebra;
    document.getElementById("btn-vender-desde-deuda").disabled = declarandoQuiebra;
    document.getElementById("btn-declarar-quiebra").disabled = declarandoQuiebra || estado !== "actualizado";
    document.getElementById("btn-cancelar-quiebra").disabled = declarandoQuiebra;
    document.getElementById("btn-confirmar-quiebra").disabled = declarandoQuiebra || estado !== "actualizado";
    document.getElementById("btn-confirmar-quiebra").textContent =
        declarandoQuiebra ? "Declarando quiebra…" : "Confirmar quiebra";
    if (!avisoDeudaPresentado) aperturaDeudaPendiente = true;
    abrirAvisoDeuda();
}

// Espera a que termine el movimiento y se cierren las otras ventanas antes de abrir el aviso.
function abrirAvisoDeuda(solicitudManual = false) {
    if (solicitudManual) aperturaDeudaPendiente = true;
    if (!modalDeuda || !aperturaDeudaPendiente || !estadoDeudaLocal.deuda || declarandoQuiebra
            || window.usuarioEnBancarrota || partidaFinalizada || regresoDesdePropiedadesDeuda
            || movimientosDeJugadoresEnCurso.size > 0
            || document.querySelector("dialog[open]:not(#modal-deuda-pendiente)")
            || document.getElementById("aviso-evento")?.classList.contains("aviso-evento--visible")) return;
    if (!solicitudManual && estadoDeudaLocal.estado !== "actualizado") return;
    // El afectado ve primero la consecuencia global (robo, alquiler o tarjeta).
    // La deuda se abre desde «Resolver mi deuda» mientras dura esa presentación.
    if (!solicitudManual && estadoDeudaLocal.deuda
            && (!eventoGlobalSincronizado || estadoEventoGlobalCliente?.activo)) return;
    if (modalDeuda.open) return;
    aperturaDeudaPendiente = false;
    avisoDeudaPresentado = true;
    abrirModalPartida(modalDeuda);
    document.getElementById("btn-cerrar-deuda").focus();
}

// Reutiliza el listado y las ventas actuales, sin interrumpir ventas sucesivas.
function abrirPropiedadesDesdeDeuda() {
    if (declarandoQuiebra || !estadoDeudaLocal.deuda) return;
    regresoDesdePropiedadesDeuda = true;
    aperturaDeudaPendiente = false;
    modalDeuda.close();
    if (!modalBonoPropiedades.open) abrirModalPartida(modalBonoPropiedades);
    cargarPropiedadesPropias();
}

function mostrarConfirmacionQuiebra() {
    if (declarandoQuiebra || estadoDeudaLocal.estado !== "actualizado" || !estadoDeudaLocal.deuda) return;
    mensajeAccionDeuda = "";
    actualizarAvisoDeuda();
    abrirModalPartida(modalConfirmacionQuiebra);
}

// Declara la quiebra por sesión; los turnos y las propiedades siguen a cargo del servidor.
async function confirmarQuiebra() {
    if (declarandoQuiebra || !modalConfirmacionQuiebra.open || estadoDeudaLocal.estado !== "actualizado"
            || !estadoDeudaLocal.deuda || window.usuarioEnBancarrota || partidaFinalizada) return;
    declarandoQuiebra = true;
    mensajeAccionDeuda = "";
    actualizarAvisoDeuda();
    try {
        const respuesta = await fetch(`${basePath}/partida/${encodeURIComponent(codigoPartida)}/declarar-quiebra`, {
            method: "POST", credentials: "same-origin", headers: { Accept: "application/json" },
        });
        const datos = await respuesta.json().catch(() => null);
        if (!respuesta.ok) {
            throw new Error(datos?.error || `No se pudo declarar la quiebra (HTTP ${respuesta.status}).`);
        }
        if (respuesta.redirected || typeof datos?.mensaje !== "string") {
            throw new Error("El servidor no confirmó la declaración. Actualizá la página antes de volver a intentarlo.");
        }
        procesarNotificacionQuiebra({
            idJugador: window.usuarioSesionId,
            apodo: obtenerNombreJugadorHistorial(window.usuarioSesionId),
        });
        declarandoQuiebra = false;
        modalConfirmacionQuiebra.close();
        modalDeuda.close();
        actualizarAvisoDeuda();
    } catch (error) {
        mensajeAccionDeuda = error instanceof TypeError ?
            "No se pudo confirmar la declaración. Comprobá la conexión y actualizá la página antes de volver a intentarlo." :
            error.message || "No se pudo confirmar la declaración. Actualizá la página antes de volver a intentarlo.";
        mostrarAvisoPartida(mensajeAccionDeuda);
    } finally {
        declarandoQuiebra = false;
        actualizarAvisoDeuda();
    }
}

botonDeuda?.addEventListener("click", async () => {
    await consultarDeudaPendiente(true);
    abrirAvisoDeuda(true);
});
document.getElementById("btn-cerrar-deuda")?.addEventListener("click", () => {
    if (!declarandoQuiebra) modalDeuda.close();
});
document.getElementById("btn-vender-desde-deuda")?.addEventListener("click", abrirPropiedadesDesdeDeuda);
document.getElementById("btn-declarar-quiebra")?.addEventListener("click", mostrarConfirmacionQuiebra);
document.getElementById("btn-confirmar-quiebra")?.addEventListener("click", confirmarQuiebra);
document.getElementById("btn-cancelar-quiebra")?.addEventListener("click", () => {
    if (declarandoQuiebra) return;
    modalConfirmacionQuiebra.close();
    mensajeAccionDeuda = "";
    actualizarAvisoDeuda();
});
modalConfirmacionQuiebra.addEventListener("cancel", evento => {
    if (declarandoQuiebra) evento.preventDefault();
});
modalDeuda?.addEventListener("cancel", (evento) => {
    if (declarandoQuiebra) evento.preventDefault();
});
modalBonoPropiedades?.addEventListener("close", () => {
    viendoPropiedadesDuranteEspera = false;
    if (regresoDesdePropiedadesDeuda) {
        regresoDesdePropiedadesDeuda = false;
        aperturaDeudaPendiente = true;
    }
});
document.querySelectorAll("dialog").forEach((modal) => {
    modal.addEventListener("close", actualizarAvisoDeuda);
});
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
const colorParadas = "#fff0c2";
let propiedadSeleccionada = null;
let resolviendoDecisionPropiedad = false;
let propiedadesPropiasActuales = [];
const catalogoPropiedadesActual = new Map((window.propiedadesIniciales || []).map(p => [Number(p.posicion), p]));
let propiedadParaVender = null;
let ofertaActiva = null;

function actualizarEtiquetaPrecioPropiedad(propiedad) {
    const posicionCatalogo = Number(propiedad.posicion);
    catalogoPropiedadesActual.set(posicionCatalogo, { ...catalogoPropiedadesActual.get(posicionCatalogo), ...propiedad });
    const casillero = document.querySelector(`.casillero[data-casillero="${propiedad.posicion}"]`);
    if (!casillero) return;

    const colorLocalidad = coloresLocalidades[propiedad.grupo];
    if (colorLocalidad) {
        casillero.style.setProperty("--color-localidad", colorLocalidad);
        casillero.dataset.grupoLocalidad = propiedad.grupo;
    } else if (posicionesParadas.has(Number(propiedad.posicion))) {
        casillero.style.setProperty("--color-localidad", colorParadas);
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
    casillero.title = `${propiedad.nombre || casillero.querySelector(".casillero__nombre")?.textContent.replace(/\u00ad/g, "").trim()} ·${etiqueta.title}`;
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
            animarCompra(posicion, propietarioId);
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
                propiedad.intercambio ? `${comprador} recibió ${propiedad.nombre} de ${vendedor} en un intercambio.` : `${comprador} compr\u00f3 ${propiedad.nombre} a ${vendedor} por $${precio}.`,
                propietarioId,
            );
            if (!propiedad.intercambio) {
                animarCompra(posicion, propietarioId);
                // Venta entre jugadores: la plata va del comprador al vendedor.
                animarPagoEntreJugadores(
                    document.getElementById(String(propietarioId)),
                    document.getElementById(String(anterior.propietarioId)),
                    propiedad.precioVenta ?? 0,
                );
            }
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

// Sello de "VENDIDO" con el color del comprador sobre el casillero que se acaba de comprar.
function animarCompra(posicion, idComprador) {
    const casillero = document.querySelector(`.casillero[data-casillero="${posicion}"]`);
    animarCompraCasillero(casillero, obtenerColorFichaJugador(idComprador));
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
            casillero.style.setProperty("--color-localidad", coloresLocalidades[grupo]);
            casillero.dataset.grupoLocalidad = grupo;
        });
    });
    posicionesParadas.forEach((posicion) => {
        const casillero = document.querySelector(`.casillero[data-casillero="${posicion}"]`);
        if (!casillero) return;
        casillero.style.setProperty("--color-localidad", colorParadas);
        casillero.dataset.grupoLocalidad = "PARADAS";
    });
}

function mostrarInfoPropiedad(propiedad) {
    propiedadSeleccionada = propiedad;
    actualizarEtiquetaPrecioPropiedad(propiedad);
    const tienePropietario = propiedad.propietarioId != null;
    const esPropia =
        !propiedad.disponible && String(propiedad.propietarioId) === String(window.usuarioSesionId);
    document.getElementById("popup-info-titulo").textContent = propiedad.nombre;
    document.getElementById("popup-info-localidad").textContent = propiedad.descripcion || "";
    document.getElementById("popup-precio-contenedor").hidden = tienePropietario;
    document.getElementById("popup-alquiler-contenedor").hidden = !tienePropietario;
    document.getElementById("popup-info-precio").textContent =
        `$${Number(propiedad.precioCompra).toLocaleString("es-AR")}`;
    document.getElementById("popup-info-alquiler").textContent =
        `$${Number(propiedad.alquiler).toLocaleString("es-AR")}`;
    document.getElementById("popup-info-propietario").textContent =
        propiedad.propietario || "Sin propietario";
    notaModalInfo.textContent = "";
    const acciones = document.getElementById("popup-acciones-propias");
    acciones.hidden = !esPropia;
    if (esPropia) {
        const nivel = Number(propiedad.nivelMejora ?? 0);
        const costo = nivelesAlquiler[nivel];
        const mejorar = document.getElementById("btn-info-mejorar");
        const puedeMejorarse = !posicionesParadas.has(Number(propiedad.posicion)) && costo != null;
        mejorar.textContent = puedeMejorarse ? `Mejorar por $${costo.toLocaleString("es-AR")}` : "Mejora no disponible";
        mejorar.disabled = !puedeMejorarse || !tieneLocalidadCompleta(propiedad)
            || String(window.turnoActualId) !== String(window.usuarioSesionId)
            || yaTiroEnEsteTurno || estaTirando || window.turnoPausadoPorDeuda
            || estadoEventoGlobalCliente?.activo || tiempoRestante <= 0
            || Number(window.dineroUsuarioSesion) < costo || window.usuarioEnBancarrota || partidaFinalizada;
        document.getElementById("btn-info-vender").disabled = Boolean(window.usuarioEnBancarrota || partidaFinalizada);
    }
}

function actualizarDineroJugador(idJugador, dinero, enBancarrota = false) {
    const tarjeta = document.getElementById(String(idJugador));
    const saldo = tarjeta?.querySelector(".dinero");
    if (saldo && dinero != null) saldo.textContent = `$${Number(dinero).toLocaleString("es-AR")}`;
    if (dinero != null && String(idJugador) === String(window.usuarioSesionId)) {
        const saldoAnterior = window.dineroUsuarioSesion;
        window.dineroUsuarioSesion = Number(dinero);
        reproducirCambioDeSaldo(saldoAnterior, window.dineroUsuarioSesion);
        actualizarControlesCarcel();
    }
    if (!enBancarrota) return;

    if (String(idJugador) === String(window.usuarioSesionId)) activarModoEspectador();
    if (!tarjeta) return;

    tarjeta.dataset.bancarrota = "true";
    tarjeta.classList.add("jugador--bancarrota");
    actualizarCompradoresVenta();
    if (!tarjeta.querySelector(".estado-bancarrota")) {
        const etiqueta = document.createElement("span");
        etiqueta.className = "estado-bancarrota";
        etiqueta.textContent = "En quiebra";
        tarjeta.querySelector(".datos-jugador")?.appendChild(etiqueta);
    }
    if (String(idJugador) === String(window.usuarioSesionId)) actualizarAvisoDeuda();
}

async function cargarInfoPropiedad(posicion, abrirPopup = false) {
    const conocida = catalogoPropiedadesActual.get(Number(posicion));
    if (abrirPopup && conocida) {
        propiedadesPropiasActuales = [...catalogoPropiedadesActual.values()].filter(
            p => String(p.propietarioId) === String(window.usuarioSesionId));
        mostrarInfoPropiedad(conocida);
        if (!modalInfoPropiedad.open) abrirModalPartida(modalInfoPropiedad);
    }
    const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/propiedades/${posicion}`, { cache: "no-store" });
    if (respuesta.status === 404) return null;
    if (!respuesta.ok) throw new Error("No se pudo consultar la propiedad");
    const propiedad = await respuesta.json();
    actualizarEtiquetaPrecioPropiedad(propiedad);
    propiedadesPropiasActuales = [...catalogoPropiedadesActual.values()].filter(
        p => String(p.propietarioId) === String(window.usuarioSesionId));
    if (!abrirPopup || !propiedadSeleccionada || Number(propiedadSeleccionada.posicion) === Number(posicion)) {
        mostrarInfoPropiedad(propiedad);
        if (abrirPopup && !conocida && !modalInfoPropiedad.open) abrirModalPartida(modalInfoPropiedad);
    }
    return propiedad;
}

async function comprarPropiedadActual() {
    if (compraEventoActual) return responderCompraDeEvento(true);
    if (
        window.usuarioEnBancarrota || partidaFinalizada ||
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
        if (String(datos.idJugadorPropietario) === String(window.usuarioSesionId)) {
            confirmarCompra(datos.nombre);
        }
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
            !window.usuarioEnBancarrota && !partidaFinalizada &&
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

function pintarDatosCompra(propiedad) {
    document.getElementById("modal-propiedad-titulo").textContent = propiedad.nombre;
    document.getElementById("compra-categoria").textContent = propiedad.descripcion || "";
    document.getElementById("compra-ilustracion").hidden = !/^estaci[oó]n (?:de )?laferrere$/i.test(propiedad.nombre?.trim());
    document.getElementById("modal-propiedad-info").textContent = "";
    document.getElementById("compra-precio").textContent = `$${Number(propiedad.precioCompra).toLocaleString("es-AR")}`;
    document.getElementById("compra-alquiler").textContent = `$${Number(propiedad.alquiler).toLocaleString("es-AR")}`;
}

function cerrarModalCompra() {
    compraEventoActual = null;
    document.getElementById("btn-modal-comprar").disabled = true;
    document.getElementById("btn-modal-pasar").disabled = true;
    if (modalCompra?.open) modalCompra.close();
    resolviendoDecisionPropiedad = false;
}

async function pasarTurnoPropiedad() {
    if (compraEventoActual) return responderCompraDeEvento(false);
    if (resolviendoDecisionPropiedad || window.usuarioEnBancarrota || partidaFinalizada) return;
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

    if (window.usuarioEnBancarrota || partidaFinalizada || propiedad.disponible !== true
            || String(window.turnoActualId) !== String(idJugador)) return;

    const saldoActual =
        document.getElementById(String(idJugador))?.querySelector(".dinero")?.textContent ?? "0";
    const dineroDisponible = Number(saldoActual.replace(/[^\d-]/g, "")) || 0;
    if (dineroDisponible < Number(propiedad.precioCompra)) {
        mostrarAvisoPartida("No tenés guita para comprar esta propiedad.");
        await finalizarTurno().catch(console.error);
        return;
    }

    pintarDatosCompra(propiedad);
    if (tiempoRestante <= 0) return;
    document.getElementById("btn-modal-comprar").disabled = false;
    document.getElementById("btn-modal-pasar").disabled = false;
    abrirModalPartida(modalCompra);
}

document.querySelectorAll(".casillero[data-casillero]").forEach((casillero) => {
    if (posicionesPropiedad.has(Number(casillero.dataset.casillero))) {
        casillero.tabIndex = 0;
        casillero.addEventListener("keydown", evento => {
            if (evento.key !== "Enter" && evento.key !== " ") return;
            evento.preventDefault();
            casillero.click();
        });
    }
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
document.getElementById("btn-modal-comprar")?.addEventListener("click", comprarPropiedadActual);
document.getElementById("btn-modal-pasar")?.addEventListener("click", pasarTurnoPropiedad);

document.getElementById("btn-bono-propiedades")?.addEventListener("click", () => {
    if (!modalBonoPropiedades.open) abrirModalPartida(modalBonoPropiedades);
    cargarPropiedadesPropias();
});
document.getElementById("btn-ayuda-propiedades")?.addEventListener("click", () => {
    abrirModalPartida(document.getElementById("modal-ayuda-propiedades"));
});
document
    .getElementById("btn-cerrar-bono-propiedades")
    ?.addEventListener("click", () => modalBonoPropiedades.close());
document
    .getElementById("btn-cerrar-vender-propiedad")
    ?.addEventListener("click", () => modalVenderPropiedad.close());
botonVentaBanco?.addEventListener("click", () => venderPropiedadBono(propiedadParaVender));
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

const cargarListaPropiedades = crearCargaPropiedades(async () => {
    if (!listaPropiedadesPropias || !mensajeBonoPropiedades) return null;
    if (!catalogoPropiedadesActual.size) mensajeBonoPropiedades.textContent = "Cargando tus propiedades...";
    const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/propiedades`, {
        cache: "no-store", credentials: "same-origin",
    });
    const propiedades = await respuesta.json();
    if (!respuesta.ok) throw new Error(propiedades.error || "No se pudieron cargar las propiedades.");
    if (!Array.isArray(propiedades)) throw new Error("El servidor devolvió una lista de propiedades inválida.");
    return propiedades;
}, pintarListaPropiedades, error => {
    mensajeBonoPropiedades.textContent = error.message || "No se pudieron cargar tus propiedades.";
});

function pintarListaPropiedades(propiedades) {
    propiedades.forEach(actualizarEtiquetaPrecioPropiedad);
    propiedadesPropiasActuales = propiedades.filter(
        propiedad => String(propiedad.propietarioId) === String(window.usuarioSesionId));
    mensajeBonoPropiedades.textContent = propiedadesPropiasActuales.length
        ? ""
        : "Todavía no tenés propiedades.";
    pintarTarjetasAgrupadas(modalBonoPropiedades, listaPropiedadesPropias, crearTarjetaPropiedad);
    if (modalInfoPropiedad.open && propiedadSeleccionada) {
        const actual = propiedades.find(p => Number(p.posicion) === Number(propiedadSeleccionada.posicion));
        if (actual) mostrarInfoPropiedad(actual);
    }
}

function pintarTarjetasAgrupadas(modal, lista, crearTarjeta) {
    const grupos = new Map();
    for (const propiedad of propiedadesPropiasActuales) {
        const esParada = posicionesParadas.has(Number(propiedad.posicion));
        const localidad = esParada ? "PARADAS" : propiedad.grupo;
        if (!grupos.has(localidad)) {
            const grupo = document.createElement("div");
            grupo.className = "grupo-propiedades-propias";
            const color = esParada ? colorParadas : coloresLocalidades[localidad];
            if (color) grupo.style.setProperty("--color-propiedad", color);
            grupos.set(localidad, grupo);
        }
        grupos.get(localidad).append(crearTarjeta(propiedad));
    }
    const columnasModal = Math.min(3, Math.max(1, ...Array.from(
        grupos.values(), grupo => grupo.childElementCount,
    )));
    modal.style.setProperty("--columnas-modal", columnasModal);
    lista.replaceChildren(...grupos.values());
}

async function cargarPropiedadesPropias() {
    if (catalogoPropiedadesActual.size) pintarListaPropiedades([...catalogoPropiedadesActual.values()]);
    return cargarListaPropiedades();
}

function crearTarjetaPropiedad(propiedad, seleccionar, soloResumen = false) {
    const tarjeta = document.createElement("article");
    tarjeta.className = "propiedad-propia";
    const encabezado = document.createElement("div");
    encabezado.className = "modal-compra__titulo";
    const titulo = document.createElement("h3");
    titulo.textContent = propiedad.nombre;
    const ubicacion = document.createElement("span");
    ubicacion.className = "modal-compra__categoria";
    const esParada = posicionesParadas.has(Number(propiedad.posicion));
    ubicacion.textContent = esParada
        ? "Parada de bondi"
        : nombresLocalidades[propiedad.grupo] || propiedad.descripcion || "";
    encabezado.append(titulo, ubicacion);
    const datos = document.createElement("dl");
    datos.className = "propiedad-propia__datos";
    if (propiedad.alquiler != null) agregarDatoPropiedad(datos, "Alquiler", propiedad.alquiler);
    const nivel = Number(propiedad.nivelMejora ?? 0);
    if (!esParada && propiedad.nivelMejora != null) agregarDatoPropiedad(datos, "Nivel de mejora", `${nivel} / ${nivelesAlquiler.length}`);
    tarjeta.append(encabezado);
    if (datos.childElementCount) tarjeta.append(datos);
    if (soloResumen) return tarjeta;
    if (seleccionar) {
        const boton = document.createElement("button");
        boton.type = "button";
        boton.className = "venta-tarjeta-seleccion";
        boton.setAttribute("aria-label", `Elegir ${propiedad.nombre}`);
        boton.addEventListener("click", () => seleccionar(propiedad));
        tarjeta.append(boton);
        return tarjeta;
    }
    const acciones = document.createElement("div");
    acciones.className = "propiedad-propia__acciones";
    const botonSubir = document.createElement("button");
    botonSubir.type = "button";
    botonSubir.className = "btn-casillero btn-casillero--principal";
    const localidadCompleta = tieneLocalidadCompleta(propiedad);
    const esTurnoPropio = String(window.turnoActualId) === String(window.usuarioSesionId);
    const costoMejora = nivelesAlquiler[nivel];
    const puedeMejorarse = !esParada && costoMejora != null;
    botonSubir.disabled =
        partidaFinalizada ||
        !localidadCompleta ||
        !puedeMejorarse ||
        !esTurnoPropio || yaTiroEnEsteTurno || estaTirando ||
        window.usuarioEnBancarrota ||
        Number(window.dineroUsuarioSesion) < costoMejora;
    botonSubir.textContent =
        esTurnoPropio && puedeMejorarse
            ? `Mejorar $${costoMejora.toLocaleString("es-AR")}`
            : "Mejorar";
    botonSubir.addEventListener("click", () =>
        subirAlquilerPropiedad(propiedad.posicion, botonSubir),
    );
    const botonVender = document.createElement("button");
    botonVender.type = "button";
    botonVender.className = "btn-casillero btn-casillero--secundario";
    botonVender.disabled = Boolean(window.usuarioEnBancarrota || partidaFinalizada);
    botonVender.textContent = "Vender";
    botonVender.addEventListener("click", () => abrirOpcionesVenta(propiedad));
    const botonIntercambiar = document.createElement("button");
    botonIntercambiar.type = "button";
    botonIntercambiar.className = "btn-casillero btn-casillero--secundario";
    botonIntercambiar.textContent = "Intercambiar";
    botonIntercambiar.dataset.posicionIntercambio = String(propiedad.posicion);
    const propuestaPendiente = gestionIntercambios?.tienePropuesta(propiedad.posicion);
    botonIntercambiar.disabled = Boolean(window.usuarioEnBancarrota || partidaFinalizada || propuestaPendiente);
    if (propuestaPendiente) botonIntercambiar.textContent = "Intercambio pendiente";
    botonIntercambiar.addEventListener("click", () => gestionIntercambios?.abrir(propiedad.posicion));
    if (!esParada) acciones.append(botonSubir);
    acciones.append(botonVender, botonIntercambiar);
    tarjeta.append(acciones);
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

async function subirAlquilerPropiedad(posicion, boton, mensaje = mensajeBonoPropiedades) {
    if (window.usuarioEnBancarrota || partidaFinalizada) return;
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
        if (modalInfoPropiedad.open && Number(propiedadSeleccionada?.posicion) === Number(posicion)) mostrarInfoPropiedad(propiedad);
        mostrarAvisoPartida("Aumentaste el alquiler de la propiedad.");
    } catch (error) {
        boton.disabled = Boolean(window.usuarioEnBancarrota || partidaFinalizada);
        if (modalInfoPropiedad.open && Number(propiedadSeleccionada?.posicion) === Number(posicion)) {
            mostrarInfoPropiedad(propiedadSeleccionada);
        }
        mensaje.textContent = error.message || "No se pudo subir el alquiler.";
    }
}

function abrirOpcionesVenta(propiedad) {
    if (window.usuarioEnBancarrota || partidaFinalizada) return;
    seleccionarPropiedadVenta(propiedad);
    mensajeVentaBanco.textContent = "";
    mensajeOfertaVenta.textContent = "";
    document.getElementById("oferta-precio").value = "";
    mostrarDestinoVenta();
    abrirModalPartida(modalVenderPropiedad);
}

function seleccionarPropiedadVenta(propiedad) {
    propiedadParaVender = propiedad;
    nombrePropiedadEnVenta.textContent = propiedad.nombre;
    document.getElementById("btn-elegir-propiedad-venta").textContent = `Propiedad: ${propiedad.nombre} · Cambiar`;
    document.getElementById("vender-propiedad-categoria").textContent = propiedad.descripcion || "";
    document.getElementById("venta-banco-importe").textContent =
        `$${Number(propiedad.precioVenta).toLocaleString("es-AR")}`;
}

function mostrarDestinoVenta(destino) {
    nombrePropiedadEnVenta.parentElement.hidden = destino === "jugador";
    document.getElementById("venta-destino").hidden = Boolean(destino);
    document.getElementById("venta-panel-banco").hidden = destino !== "banco";
    document.getElementById("venta-panel-jugador").hidden = destino !== "jugador";
    document.getElementById("btn-volver-venta").hidden = !destino;
    document.getElementById("vender-propiedad-titulo").textContent = {
        banco: "Vender al banco", jugador: "Vender a un jugador",
    }[destino] || "¿A quién querés vender?";
    if (destino !== "jugador") return;

    pintarJugadoresParaElegir(document.getElementById("oferta-jugadores"), "oferta-jugador");
}

function jugadorPuedeComprar(jugador) {
    return jugador && jugador.id !== String(window.usuarioSesionId) &&
        jugador.dataset.bancarrota !== "true" && jugador.dataset.abandono !== "true" &&
        !jugador.matches(".jugador--bancarrota") &&
        !jugador.querySelector(".estado-bancarrota, .estado-abandono") &&
        !jugadoresAbandonados.has(jugador.id);
}

function actualizarCompradoresVenta() {
    if (!modalVenderPropiedad?.open || document.getElementById("venta-panel-jugador").hidden) return;
    const lista = document.getElementById("oferta-jugadores");
    pintarJugadoresParaElegir(lista, "oferta-jugador", lista.querySelector("input:checked")?.value);
}

function pintarJugadoresParaElegir(lista, nombreCampo, seleccionado) {
    lista.replaceChildren();
    for (const jugador of document.querySelectorAll(".jugadores > .jugador")) {
        if (!jugadorPuedeComprar(jugador)) continue;
        const opcion = document.createElement("label");
        opcion.className = "venta-comprador";
        opcion.style.setProperty("--color-comprador", getComputedStyle(jugador).getPropertyValue("--color-ficha"));
        const radio = document.createElement("input");
        radio.type = "radio";
        radio.name = nombreCampo;
        radio.value = jugador.id;
        radio.checked = jugador.id === seleccionado;
        const avatar = jugador.querySelector(".avatar-jugador").cloneNode();
        avatar.alt = "";
        const nombre = document.createElement("span");
        nombre.textContent = jugador.querySelector(".nombre-jugador").textContent;
        opcion.append(radio, avatar, nombre);
        lista.append(opcion);
    }
    if (!lista.childElementCount) lista.textContent = "No hay otros jugadores disponibles.";
}

document.querySelectorAll("[data-destino-venta]").forEach(boton => {
    boton.addEventListener("click", () => mostrarDestinoVenta(boton.dataset.destinoVenta));
});
document.getElementById("btn-volver-venta").addEventListener("click", () => mostrarDestinoVenta());
document.getElementById("btn-elegir-propiedad-venta").addEventListener("click", () =>
    abrirSelectorPropiedades(modalVenderPropiedad, seleccionarPropiedadVenta));

async function abrirSelectorPropiedades(origen, seleccionar) {
    const modal = document.getElementById("modal-elegir-propiedad-venta");
    const lista = document.getElementById("lista-propiedades-venta");
    await cargarPropiedadesPropias();
    if (!origen.open || modal.open) return;
    pintarTarjetasAgrupadas(modal, lista, propiedad => crearTarjetaPropiedad(propiedad, elegida => {
        seleccionar(elegida);
        modal.close();
    }));
    if (!lista.childElementCount) lista.textContent = "Todavía no tenés propiedades.";
    abrirModalPartida(modal);
    if (!modal.open) return;
    const cerrarSelector = () => modal.close();
    origen.addEventListener("close", cerrarSelector, { once: true });
    modal.addEventListener("close", () => {
        origen.removeEventListener("close", cerrarSelector);
        if (!origen.open || !origen.matches("#modal-intercambio, #modal-intercambio-recibido") ||
            window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
        origen.querySelector(".intercambio-contenido").animate([
            { opacity: 0, translate: "0 8px" },
            { opacity: 1, translate: "0 0" },
        ], { duration: 200, easing: "ease-out" });
    }, { once: true });
}

let ultimoTurnoSolicitadoPorPagoDeuda = null;

// Solicita una sola vez el cierre del mismo turno propio cuando la venta terminó de pagar la deuda.
async function finalizarTurnoTrasPagarDeuda(claveTurnoVenta) {
    if (estadoEventoGlobalCliente?.activo || !claveTurnoVenta || claveTurnoVenta !== window.claveTurnoActual
            || ultimoTurnoSolicitadoPorPagoDeuda === claveTurnoVenta
            || window.usuarioSesionId == null
            || String(window.turnoActualId) !== String(window.usuarioSesionId)
            || tiempoRestante <= 0 || partidaFinalizada || window.usuarioEnBancarrota || declarandoQuiebra
            || estadoDeudaLocal.estado !== "actualizado" || estadoDeudaLocal.deuda !== null) return;
    // Reservamos el envío antes de esperar la respuesta para evitar solicitudes simultáneas.
    ultimoTurnoSolicitadoPorPagoDeuda = claveTurnoVenta;
    // Conserva el turno tras pagar la deuda.
}

async function venderPropiedadBono(propiedad) {
    if (!propiedad || botonVentaBanco.disabled || window.usuarioEnBancarrota || partidaFinalizada) return;
    botonVentaBanco.disabled = true;
    mensajeVentaBanco.textContent = "";
    // El listado abierto desde deuda conserva ese contexto mientras se actualiza la consulta.
    const ventaConDeudaConocida = estadoDeudaLocal.deuda !== null
        && (estadoDeudaLocal.estado === "actualizado"
            || (estadoDeudaLocal.estado === "desconocido" && regresoDesdePropiedadesDeuda));
    const claveTurnoVenta = window.usuarioSesionId != null
            && String(window.turnoActualId) === String(window.usuarioSesionId) && tiempoRestante > 0
        ? window.claveTurnoActual : null;
    try {
        const respuesta = await fetch(
            `${basePath}/partida/${codigoPartida}/propiedades/${propiedad.posicion}/vender`,
            { method: "POST", credentials: "same-origin" },
        );
        const datos = await respuesta.json();
        if (!respuesta.ok) throw new Error(datos.error || "No se pudo vender la propiedad.");
        await consultarDeudaPendiente(true);
        actualizarEtiquetaPrecioPropiedad(datos);
        actualizarDineroJugador(datos.idJugador ?? window.usuarioSesionId, datos.dineroJugador);
        modalVenderPropiedad.close();
        mostrarAvisoPartida(`Vendiste ${propiedad.nombre} al banco.`);
        try {
            if (ventaConDeudaConocida) await finalizarTurnoTrasPagarDeuda(claveTurnoVenta);
        } finally {
            await cargarPropiedadesPropias();
        }
    } catch (error) {
        const mensaje = modalVenderPropiedad.open ? mensajeVentaBanco : mensajeBonoPropiedades;
        mensaje.textContent = error.message || "No se pudo vender la propiedad.";
    } finally {
        botonVentaBanco.disabled = false;
    }
}

async function leerRespuestaJson(respuesta) {
    let datos;
    try {
        datos = JSON.parse(await respuesta.text());
    } catch (error) {
        throw new Error(`El servidor no pudo completar la solicitud (HTTP ${respuesta.status}).`);
    }
    if (!respuesta.ok) throw new Error(datos.error || `El servidor rechazó la solicitud (HTTP ${respuesta.status}).`);
    return datos;
}

async function enviarOfertaVenta() {
    if (!propiedadParaVender || window.usuarioEnBancarrota || partidaFinalizada) return;
    const boton = document.getElementById("btn-enviar-oferta");
    const idJugadorComprador = document.querySelector('[name="oferta-jugador"]:checked')?.value;
    if (idJugadorComprador && !jugadorPuedeComprar(document.getElementById(idJugadorComprador))) {
        actualizarCompradoresVenta();
        mensajeOfertaVenta.textContent = "Ese jugador ya no está disponible. Elegí otro.";
        return;
    }
    const precio = document.getElementById("oferta-precio").valueAsNumber;
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
        await leerRespuestaJson(respuesta);
        modalVenderPropiedad.close();
        mostrarAvisoPartida("Oferta enviada. La propiedad se transfiere cuando el otro jugador la acepte.");
    } catch (error) {
        mensajeOfertaVenta.textContent = error.message || "No se pudo enviar la oferta.";
    } finally {
        boton.disabled = Boolean(window.usuarioEnBancarrota || partidaFinalizada);
    }
}

async function revisarOfertasPendientes() {
    if (!codigoPartida || modalOfertaRecibida?.open || window.usuarioEnBancarrota || partidaFinalizada) return;
    try {
        const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/ofertas-pendientes`, {
            cache: "no-store",
            credentials: "same-origin",
        });
        if (!respuesta.ok) return;
        const ofertas = await respuesta.json();
        if (!ofertas.length || modalOfertaRecibida?.open) return;
        mostrarOfertaRecibida(ofertas[0]);
    } catch (error) {
        console.warn("No se pudieron revisar las ofertas de propiedades.", error);
    }
}

function pintarTarjetaOferta(oferta) {
    const propiedad = catalogoPropiedadesActual.get(Number(oferta.posicion)) ||
        { posicion: oferta.posicion, nombre: oferta.nombrePropiedad };
    const tarjeta = crearTarjetaResumenPropiedad(propiedad);
    const datos = tarjeta.querySelector("dl") || document.createElement("dl");
    datos.className = "propiedad-propia__datos";
    agregarDatoPropiedad(datos, "Precio de la oferta", oferta.precio);
    tarjeta.append(datos);
    document.getElementById("oferta-recibida-tarjeta").replaceChildren(tarjeta);
}

function mostrarOfertaRecibida(oferta) {
    if (window.usuarioEnBancarrota || partidaFinalizada) return;
    ofertaActiva = oferta;
    document.getElementById("oferta-recibida-detalle").textContent =
        `${oferta.apodoVendedor} te ofrece esta propiedad.`;
    pintarTarjetaOferta(oferta);
    mensajeOfertaRecibida.textContent = "";
    abrirModalPartida(modalOfertaRecibida);
    if (!catalogoPropiedadesActual.has(Number(oferta.posicion))) {
        cargarPropiedadesPropias().then(() => {
            if (ofertaActiva === oferta && modalOfertaRecibida.open) pintarTarjetaOferta(oferta);
        });
    }
}

async function responderOfertaVenta(aceptar) {
    if (!ofertaActiva || window.usuarioEnBancarrota || partidaFinalizada) return;
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
        const datos = await leerRespuestaJson(respuesta);
        if (aceptar) {
            consultarDeudaPendiente(true);
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
        revisarOfertasPendientes(); // Muestra la siguiente oferta, si llegó otra mientras tanto.
    } catch (error) {
        mensajeOfertaRecibida.textContent = error.message || "No se pudo responder la oferta.";
    } finally {
        botones.forEach((boton) => {
            boton.disabled = Boolean(window.usuarioEnBancarrota || partidaFinalizada);
        });
    }
}

// Si ya hay un pedido para avanzar el turno en curso, se reutiliza en lugar de mandar otro.
let finalizacionTurnoEnCurso = null;

function finalizarTurno() {
    if (finalizacionTurnoEnCurso) return finalizacionTurnoEnCurso;
    finalizacionTurnoEnCurso = enviarFinalizacionTurno().finally(() => {
        finalizacionTurnoEnCurso = null;
    });
    return finalizacionTurnoEnCurso;
}

async function enviarFinalizacionTurno() {
    if (window.usuarioEnBancarrota || partidaFinalizada || estadoEventoGlobalCliente?.activo) return;
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
let tiempoRestante = 60;

function iniciarRelojLocal(segundos) {
    if (estadoEventoGlobalCliente?.activo) return;
    const elementoReloj = document.getElementById("cronometro-nro");
    if (!elementoReloj) return;
    const duracion = Number(segundos);
    let segundosTotalesRestantes = Number.isFinite(duracion) && duracion >= 0 ? duracion : 60;
    tiempoRestante = segundosTotalesRestantes;
    clearInterval(intervaloReloj); // Limpia cualquier reloj anterior
    actualizarTextoReloj(elementoReloj);

    intervaloReloj = setInterval(() => {
        segundosTotalesRestantes = Math.max(0, segundosTotalesRestantes - 1);
        tiempoRestante = segundosTotalesRestantes;
        actualizarTextoReloj(elementoReloj);

        if (tiempoRestante === 0) {
            document.dispatchEvent(new Event("fin-plazo-turno"));
            clearInterval(intervaloReloj);
            bloquearControles();
            cerrarModalCompra();
            if (propiedadSeleccionada) mostrarInfoPropiedad(propiedadSeleccionada);
        }
    }, 1000);
}

// El servidor conserva las tiradas, los plazos y las consecuencias. La UI sólo los representa.
async function sincronizarEventoGlobal() {
    if (!codigoPartida || partidaFinalizada || peticionEventoGlobal || consultaEventoGlobalEnCurso) return;
    consultaEventoGlobalEnCurso = true;
    try {
        const respuesta = await fetch(`${basePath}/partida/${encodeURIComponent(codigoPartida)}/evento-global`, {
            cache: "no-store", credentials: "same-origin",
        });
        if (!respuesta.ok) throw new Error(`No se pudo consultar el evento (${respuesta.status}).`);
        aplicarEventoGlobal(await respuesta.json());
    } catch (error) {
        console.warn("No se pudo sincronizar el evento global.", error);
    } finally {
        consultaEventoGlobalEnCurso = false;
    }
}

function debeEsperarPresentacionGlobal() {
    return movimientosDeJugadoresEnCurso.size > 0
        || document.getElementById("aviso-evento")?.classList.contains("aviso-evento--visible");
}

function aplicarEventoGlobal(estado) {
    if (partidaFinalizada || !aceptarEstadoEvento(estadoEventoGlobalCliente, estado)) return;
    registrarEventoGlobalEnHistorial(estado);
    const estabaActivo = estadoEventoGlobalCliente?.activo;
    const empezoPausaPorDeuda = estado.deudaPendiente && !estadoEventoGlobalCliente?.deudaPendiente;
    estadoEventoGlobalCliente = estado;
    if (empezoPausaPorDeuda) consultarDeudaPendiente(true);
    actualizarAvisoEsperaQuiebra();
    eventoGlobalSincronizado = true;
    // El backend ya pausó la partida. El popup espera a que termine el evento del casillero.
    mostrarEstadoGlobal(estado, estabaActivo);
}

function mostrarEstadoGlobal(estado, estabaActivo = estado.activo) {
    const panel = document.getElementById("aviso-global");
    if (!panel) {
        console.error("Falta #aviso-global en partida.html; comprobá que el HTML actualizado esté cargado.");
        return;
    }
    const mostrar = estado.activo && !debeEsperarPresentacionGlobal();
    const seEstaMostrando = panel.classList.contains("aviso-evento--visible");
    if (mostrar && !seEstaMostrando) {
        document.querySelectorAll("dialog[open]").forEach(modal => {
            if (modal.id === "modal-deuda-pendiente") {
                modal.close();
                aperturaDeudaPendiente = true;
                avisoDeudaPresentado = false;
            } else if (!esModalPermitidoDuranteEvento(modal)) {
                modal.close();
            }
        });
    }
    panel.classList.toggle("aviso-evento--visible", mostrar);
    panel.setAttribute("aria-hidden", String(!mostrar));
    if (estado.activo) {
        clearInterval(intervaloReloj);
        pausarRelojVisual();
        if (modalCompra?.open) cerrarModalCompra();
        if (!mostrandoTiradaGlobal) pintarEventoGlobal(estado);
        if (mostrar) {
            estado.jugadores.forEach(jugador => {
                actualizarDineroJugador(jugador.id, jugador.dinero);
                actualizarEstadoCarcel(jugador.id, jugador.encarcelado, jugador.intentosCarcel);
                if (jugador.activo) moverFichaEnTablero(jugador.id, jugador.posicion,
                    estado.resuelto && posicionesInicialesSincronizadas ? "llegada" : undefined);
            });
        }
        posicionesInicialesSincronizadas = true;
        actualizarControlesEventoGlobal();
        actualizarTiempoEventoGlobal();
        consultarDeudaPendiente();
    } else if (estabaActivo) {
        window.ultimoTurnoRecibido = { ronda: estado.rondaActual, finalizados: estado.turnosFinalizados };
        window.turnoActualId = estado.idJugadorActual;
        window.turnoInicialSincronizado = false;
        const jugadorActual = (estado.jugadores ?? []).find(j => String(j.id) === String(estado.idJugadorActual));
        if (estado.idJugadorActual != null) actualizarTurnoEnPantalla({
            idJugadorActual: estado.idJugadorActual, apodoJugadorActual: jugadorActual?.apodo || "",
        });
        const pendiente = turnoPendienteDuranteEvento;
        turnoPendienteDuranteEvento = null;
        if (pendiente) aplicarTurnoDTO(pendiente);
        consultarDeudaPendiente(true);
        actualizarTurnoDesdeServidor(basePath, codigoPartida);
        solicitarPosicionesPartida();
    }
    actualizarBotonTirada();
    if (!estado.activo) mostrarTarjetaGlobal(estado).then(() => mostrarCompraDeEvento(estado));
}

async function mostrarTarjetaGlobal(estado) {
    if (!estado.tarjeta || String(estado.seleccionado) !== String(window.usuarioSesionId)
            || debeEsperarPresentacionGlobal() || partidaFinalizada) return;
    const clave = `tarjeta-global:${codigoPartida}:${estado.ronda}:${estado.seleccionado}`;
    if (tarjetasGlobalesMostradas.has(clave)) return;
    try {
        if (sessionStorage.getItem(clave)) return;
    } catch (error) {
        console.debug("La tarjeta se recordará durante esta sesión de la página.", error);
    }
    tarjetasGlobalesMostradas.add(clave);
    const panel = document.getElementById("aviso-global");
    panel.classList.remove("aviso-evento--visible");
    panel.setAttribute("aria-hidden", "true");
    await mostrarPopupEvento(estado.seleccionado, estado.tarjeta.nombre, estado.tarjeta.descripcion);
    try {
        sessionStorage.setItem(clave, "vista");
    } catch (error) {
        console.debug("No se pudo guardar la presentación de la tarjeta.", error);
    }
}

async function continuarEventoGlobal() {
    const estado = estadoEventoGlobalCliente;
    if (peticionEventoGlobal || !estado?.activo || !estado.resuelto || debeEsperarPresentacionGlobal()) return;
    await mostrarTarjetaGlobal(estado);
    if (estadoEventoGlobalCliente?.activo && estadoEventoGlobalCliente.ronda === estado.ronda) {
        enviarAccionEventoGlobal("continuar");
    }
}

async function mostrarCompraDeEvento(estado) {
    if (estado.activo || estado.compraPendiente == null || partidaFinalizada || window.usuarioEnBancarrota
            || String(estado.seleccionado) !== String(window.usuarioSesionId)
            || debeEsperarPresentacionGlobal() || document.querySelector("dialog[open]")) return;
    const clave = `${codigoPartida}:${estado.ronda}:${estado.seleccionado}`;
    if (comprasGlobalesMostradas.has(clave)) return;
    comprasGlobalesMostradas.add(clave);
    try {
        const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/propiedades/${estado.compraPendiente}`, {
            cache: "no-store", credentials: "same-origin",
        });
        const propiedad = await leerRespuestaJson(respuesta);
        if (estadoEventoGlobalCliente?.activo || debeEsperarPresentacionGlobal()
                || document.querySelector("dialog[open]")) {
            comprasGlobalesMostradas.delete(clave);
        } else if (propiedad.disponible && !partidaFinalizada) {
            compraEventoActual = { ronda: estado.ronda, posicion: estado.compraPendiente };
            pintarDatosCompra(propiedad);
            document.getElementById("btn-modal-comprar").disabled = false;
            document.getElementById("btn-modal-pasar").disabled = false;
            abrirModalPartida(modalCompra);
        }
    } catch (error) {
        comprasGlobalesMostradas.delete(clave);
        console.warn("No se pudo consultar la oportunidad de compra.", error);
    }
}

async function responderCompraDeEvento(aceptar) {
    if (!compraEventoActual || estadoEventoGlobalCliente?.activo || partidaFinalizada) return;
    const botones = [document.getElementById("btn-modal-comprar"), document.getElementById("btn-modal-pasar")];
    if (botones.some(boton => boton.disabled)) return;
    botones.forEach(boton => { boton.disabled = true; });
    try {
        const respuesta = await fetch(`${basePath}/partida/${codigoPartida}/compra-evento`, {
            method: "POST", credentials: "same-origin", headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ ronda: compraEventoActual.ronda, aceptar }),
        });
        const datos = await leerRespuestaJson(respuesta);
        if (aceptar && datos.estado !== "rechazada") {
            actualizarEtiquetaPrecioPropiedad(datos);
            actualizarDineroJugador(datos.idJugador, datos.dineroJugador);
            confirmarCompra(datos.nombre);
        }
        cerrarModalCompra();
        compraEventoActual = null;
        sincronizarEventoGlobal();
    } catch (error) {
        document.getElementById("modal-propiedad-info").textContent = error.message;
    } finally {
        botones.forEach(boton => { boton.disabled = false; });
    }
}

// Agregar este helper cerca de las funciones de eventos globales.
function escribirTextoGlobal(id, texto) {
    const elemento = document.getElementById(id);
    if (elemento) elemento.textContent = texto ?? "";
}

const textosEventoGlobal = {
    TRAMITES_AFIP: ["Trámites en AFIP", "Hay una fila enorme. Quien saque el número más alto avanza hasta el jugador más adelantado.", "Ganó"],
    INUNDACION: ["Inundación", "Se inundó el barrio. Quien saque el número más alto retrocede tres casilleros.", "Perdió"],
    PIQUETE: ["Piquete", "Cortaron el paso. Quien saque el número más bajo vuelve a Inicio.", "Perdió"],
    ROBO: ["Robo"],
    FIAT_600: ["Fiat 600"],
    ACCIDENTE_SARMIENTO: ["Accidente en el Sarmiento"],
};
function pintarCarasDadosGlobales(uno, dos) {
    [uno, dos].forEach((cara, indice) => {
        const dado = document.getElementById(`global-dado-${indice + 1}`);
        dado.dataset.cara = cara;
        dado.textContent = cara;
    });
}

function pintarDadosGlobales(tirada, mensaje) {
    const uno = Math.floor(tirada / 10);
    const dos = tirada % 10;
    pintarCarasDadosGlobales(uno || 1, dos || 1);
    escribirTextoGlobal("global-etapa", tirada == null ? mensaje : `Sacaste ${uno + dos}`);
}

function pintarEventoGlobal(estado) {
    const [nombre, descripcion, resultado] = textosEventoGlobal[estado.nombre] || [];
    const titulo = nombre || (estado.nombre ?? "Evento global").replaceAll("_", " ");
    const vistaResultados = estado.resuelto && estado.requiereDados;
    document.getElementById("aviso-global").classList.toggle("aviso-global--resultados", vistaResultados);
    escribirTextoGlobal("global-nombre", titulo);
    document.getElementById("global-pantalla-tirada").hidden = estado.resuelto;
    document.getElementById("global-pantalla-resultado").hidden = !estado.resuelto;
    escribirTextoGlobal("global-mensaje", estado.deudaPendiente ? "Hay una deuda pendiente." : "");
    if (estado.resuelto) {
        const seleccionado = estado.jugadores.find(j => String(j.id) === String(estado.seleccionado));
        const descripcionResuelta = (estado.descripcion ?? "").replace("[nombre del jugador]", seleccionado?.apodo || "Un jugador");
        const consecuencia = estado.consecuencia || descripcionResuelta;
        const resumen = estado.tarjeta?.nombre
            ? consecuencia.replace(` Sacó la tarjeta «${estado.tarjeta.nombre}».`, "")
            : consecuencia;
        escribirTextoGlobal("global-descripcion-resultado", estado.requiereDados || resumen === descripcionResuelta ? "" : descripcionResuelta);
        const premioAnterior = estado.nombre === "FIAT_600" && resumen.match(/, (recibió \$[\d.,]+) y ahora tiene/);
        escribirTextoGlobal("global-consecuencia", premioAnterior && seleccionado
            ? `${seleccionado.apodo} ${premioAnterior[1]}.` : resumen);
        pintarResultadosGlobales(estado, resultado);
        return;
    }

    escribirTextoGlobal("global-descripcion", descripcion || estado.descripcion || "");
    document.getElementById("global-dados").hidden = !estado.requiereDados;
    const habilitado = (estado.habilitados ?? []).some(id => String(id) === String(window.usuarioSesionId));
    const mensaje = estado.etapa > 1 ? (habilitado ? "Desempate · Volvé a tirar" : "Desempate en curso") : "";
    pintarDadosGlobales(estado.tiradas?.[`${estado.etapa}:${window.usuarioSesionId}`], mensaje);
}

function pintarResultadosGlobales(estado, resultado) {
    const lista = document.getElementById("global-tiradas");
    lista.hidden = !estado.requiereDados;
    lista.replaceChildren();
    if (lista.hidden) return;

    for (const jugador of estado.jugadores) {
        const tiradas = Array.from({ length: estado.etapa }, (_, indice) =>
            estado.tiradas?.[`${indice + 1}:${jugador.id}`]).filter(tirada => tirada != null);
        if (!jugador.activo && !tiradas.length) continue;

        const fila = document.createElement("li");
        const nombre = document.createElement("span");
        nombre.textContent = jugador.apodo;
        const puntaje = document.createElement("strong");
        puntaje.textContent = tiradas.length
            ? tiradas.map(tirada => Math.floor(tirada / 10) + tirada % 10).join(" → ")
            : "Sin tirada";
        if (String(estado.seleccionado) === String(jugador.id)) {
            fila.classList.add("aviso-global__tirada--seleccionada");
            const etiqueta = document.createElement("small");
            etiqueta.textContent = resultado || "Seleccionado";
            fila.append(nombre, etiqueta, puntaje);
        } else {
            fila.append(nombre, puntaje);
        }
        lista.append(fila);
    }
}

function actualizarControlesEventoGlobal() {
    const estado = estadoEventoGlobalCliente;
    if (!estado?.activo) return;
    const jugador = window.usuarioSesionId;
    const activo = estado.jugadores.some(participante => String(participante.id) === String(jugador) && participante.activo);
    const tirar = document.getElementById("global-tirar");
    if (mostrandoTiradaGlobal) {
        tirar.disabled = true;
        return;
    }
    const habilitado = (estado.habilitados ?? []).some(id => String(id) === String(jugador));
    const tirada = estado.tiradas?.[`${estado.etapa}:${jugador}`];
    tirar.hidden = estado.resuelto || !estado.requiereDados || !activo || !habilitado;
    tirar.textContent = tirada == null ? "Tirar dados" : "Esperando al resto…";
    tirar.disabled = !puedeTirarEnEvento(estado, jugador) || peticionEventoGlobal;
    const continuar = document.getElementById("global-continuar");
    continuar.hidden = !estado.resuelto;
    const confirmado = estado.confirmados.some(id => String(id) === String(jugador));
    continuar.disabled = !activo || peticionEventoGlobal || confirmado;
    continuar.textContent = confirmado ? "Esperando al resto…" : "Continuar partida";
    document.getElementById("global-deuda").hidden = !estadoDeudaLocal.deuda;
}

function actualizarTiempoEventoGlobal() {
    if (!estadoEventoGlobalCliente?.activo || partidaFinalizada) return;
    const segundos = Math.max(0,
        Math.ceil((Number(estadoEventoGlobalCliente.vence) - Date.now()) / 1000));
    const tiempo = Number.isFinite(segundos) ? segundos : 0;
    escribirTextoGlobal("global-tiempo", `${Math.floor(tiempo / 60)}:${String(tiempo % 60).padStart(2, "0")}`);
    document.querySelector(".aviso-global__reloj").classList.toggle("aviso-global__reloj--urgente", tiempo <= 5);
    actualizarControlesEventoGlobal();
}

async function enviarAccionEventoGlobal(accion) {
    if (peticionEventoGlobal || !estadoEventoGlobalCliente?.activo) return;
    const estado = estadoEventoGlobalCliente;
    peticionEventoGlobal = true;
    const dados = document.getElementById("global-dados");
    let giro;
    let cambioCaras;
    if (accion === "tirar") {
        mostrandoTiradaGlobal = true;
        dados.classList.add("aviso-global__dados--girando");
        escribirTextoGlobal("global-etapa", "Tirando…");
        reproducirDadosGlobales();
        if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
            const caraAlAzar = () => 1 + Math.floor(Math.random() * 6);
            cambioCaras = setInterval(() => pintarCarasDadosGlobales(caraAlAzar(), caraAlAzar()), 75);
        }
        giro = new Promise(resolve => setTimeout(resolve, 480));
    }
    actualizarControlesEventoGlobal();
    try {
        const parametros = new URLSearchParams({ ronda: estado.ronda, etapa: estado.etapa });
        const respuesta = await fetch(`${basePath}/partida/${encodeURIComponent(codigoPartida)}/evento-global/${accion}?${parametros}`, {
            method: "POST", credentials: "same-origin",
        });
        const datos = await respuesta.json();
        if (!respuesta.ok) throw new Error(datos.error || "No se pudo completar la acción del evento.");
        aplicarEventoGlobal(datos);
        if (giro) {
            await giro;
            clearInterval(cambioCaras);
            dados.classList.remove("aviso-global__dados--girando");
            const vigente = estadoEventoGlobalCliente;
            const tirada = vigente?.ronda === estado.ronda
                ? vigente.tiradas?.[`${estado.etapa}:${window.usuarioSesionId}`] : null;
            if (tirada != null && vigente.activo) {
                pintarDadosGlobales(tirada);
                if (vigente.resuelto || vigente.etapa !== estado.etapa) {
                    await new Promise(resolve => setTimeout(resolve, 650));
                }
            }
            if (estadoEventoGlobalCliente?.activo) pintarEventoGlobal(estadoEventoGlobalCliente);
        }
    } catch (error) {
        if (giro && estadoEventoGlobalCliente?.activo) pintarEventoGlobal(estadoEventoGlobalCliente);
        escribirTextoGlobal("global-mensaje", error.message);
    } finally {
        if (giro) {
            clearInterval(cambioCaras);
            dados.classList.remove("aviso-global__dados--girando");
            mostrandoTiradaGlobal = false;
        }
        peticionEventoGlobal = false;
        actualizarControlesEventoGlobal();
        sincronizarEventoGlobal();
    }
}

document.getElementById("global-tirar").addEventListener("click", () => enviarAccionEventoGlobal("tirar"));
document.getElementById("global-continuar").addEventListener("click", continuarEventoGlobal);
modalCompra.addEventListener("cancel", evento => {
    evento.preventDefault();
    pasarTurnoPropiedad();
});
document.getElementById("global-deuda").addEventListener("click", async () => {
    await consultarDeudaPendiente(true);
    if (estadoDeudaLocal.deuda) abrirAvisoDeuda(true);
});
document.addEventListener("click", evento => {
    if (!estadoEventoGlobalCliente?.activo || partidaFinalizada) return;
    if (debeEsperarPresentacionGlobal()
            && evento.target.closest("#aviso-evento, #btn-continuar-evento")) return;
    if (evento.target.closest("#aviso-global, #modal-deuda-pendiente, #deuda-confirmacion, #modal-bono-propiedades, #modal-vender-propiedad, #modal-elegir-propiedad-venta, #modal-abandonar, #btn-abandonar, #modal-victoria")) return;
    if (evento.target.closest("#modal-oferta-recibida, #btn-bono-propiedades, #btn-ayuda-propiedades, #modal-ayuda-propiedades, #btn-deuda-pendiente, #aviso-espera-quiebra, #modal-intercambio, #modal-intercambio-recibido, #chat-partida, [data-abrir-chat], #btn-sonido")) return;
    evento.preventDefault();
    evento.stopImmediatePropagation();
}, true);
window.addEventListener("DOMContentLoaded", () => {
    sincronizarEventoGlobal();
    consultarDeudaPendiente();
    window.setInterval(sincronizarEventoGlobal, 3000);
    window.setInterval(actualizarTiempoEventoGlobal, 250);
});

function pausarRelojVisual() {
    const elemento = document.getElementById("cronometro-nro");
    if (!elemento) return;
    elemento.textContent = "Pausa";
    elemento.closest(".cronometro").dataset.estado = "pausa";
}

function actualizarTextoReloj(elemento) {
    if (!elemento) return;
    elemento.closest(".cronometro").dataset.estado = tiempoRestante <= 5 ? "urgente" : "normal";
    elemento.innerText = `${Math.floor(tiempoRestante / 60).toString().padStart(2, "0")}:${(tiempoRestante % 60).toString().padStart(2, "0")}`;
}

function prepararRelojSinIniciar() {
    const elementoReloj = document.getElementById("cronometro-nro");
    clearInterval(intervaloReloj);
    tiempoRestante = 60;
    actualizarTextoReloj(elementoReloj);
}

function bloquearControles() {
    const btnTirar = document.getElementById("btn-tirar");

    if (btnTirar) {
        btnTirar.disabled = true;
    }

    document.querySelectorAll(".btn-accion").forEach((btn) => {
        if (!["btn-bono-propiedades", "btn-abandonar"].includes(btn.id)) {
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
            if (!["btn-bono-propiedades", "btn-abandonar"].includes(boton.id)) {
                boton.disabled = false;
            }
        });

        actualizarBotonTirada();
    }
}

// Alineo el historial con el tamaño del tablero.
// En pantalla ancha los botones van debajo del historial, así que le resto su alto para que la columna termine donde termina el tablero.
function alinearHistorialConTablero() {
    const tablero = document.querySelector(".tablero");
    const lateral = document.querySelector(".partida__lateral");
    const acciones = document.querySelector(".partida__pie .acciones");
    if (!tablero || !lateral) return;
    const actualizar = () => {
        if (window.matchMedia("(max-width: 1250px)").matches) {
            lateral.style.removeProperty("--historial-alto");
            lateral.style.removeProperty("--historial-desplazamiento");
            return;
        }
        // El historial puede empezar más arriba que el tablero (en el borde de la mesa), pero termina a su misma altura.
        const limites = tablero.getBoundingClientRect();
        const inicio = lateral.getBoundingClientRect().top;
        const estiloPie = getComputedStyle(lateral.parentElement);
        const separacion = parseFloat(estiloPie.rowGap) || 0;
        const reservaChat = parseFloat(estiloPie.paddingBottom) || 0;
        const altoAcciones = acciones ? acciones.offsetHeight + separacion : 0;
        lateral.style.setProperty("--historial-alto", `${Math.max(0, limites.bottom - inicio - altoAcciones - reservaChat)}px`);
        lateral.style.setProperty("--historial-desplazamiento", "0px");
    };
    const observador = new ResizeObserver(actualizar);
    observador.observe(tablero);
    observador.observe(document.querySelector(".partida__main"));
    if (acciones) observador.observe(acciones);
    window.addEventListener("resize", actualizar);
    actualizar();
}

window.addEventListener("DOMContentLoaded", () => {
    alinearHistorialConTablero();
    consultarResultadosFinales();
    window.setInterval(consultarResultadosFinales, 10000);
    posicionesInicialesSincronizadas = true;
    if (window.turnoActualId != null) {
        const jugador = document.getElementById(String(window.turnoActualId));
        actualizarTurnoEnPantalla({ idJugadorActual: window.turnoActualId,
            apodoJugadorActual: jugador?.querySelector(".nombre-jugador")?.textContent || "" });
    }
    pintarLocalidadesTablero();
    if (Array.isArray(window.propiedadesIniciales)) {
        window.propiedadesIniciales.forEach(actualizarEtiquetaPrecioPropiedad);
    }
    actualizarBotonTirada();
    conectarWebSocketTurnos(basePath, codigoPartida);
    if (window.usuarioEnBancarrota) activarModoEspectador();
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
    if (partidaFinalizada) return;
    mensajeAbandono.textContent = "";
    abrirModalPartida(modalAbandonar);
});

botonCancelarAbandono.addEventListener("click", () => {
    modalAbandonar.close();
});

botonConfirmarAbandono.addEventListener("click", confirmarAbandono);

async function confirmarAbandono() {
    if (abandonandoPartida || partidaFinalizada) {
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

        if (!partidaFinalizada) window.location.assign(`${basePath}/home`);
    } catch (error) {
        mensajeAbandono.textContent = error.message;
    } finally {
        abandonandoPartida = false;
        botonConfirmarAbandono.disabled = partidaFinalizada;
        botonCancelarAbandono.disabled = partidaFinalizada;
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
    if (tarjeta) tarjeta.dataset.abandono = "true";

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
    actualizarCompradoresVenta();

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

// Mantiene el tablero y sus suscripciones; bloquea solamente las acciones locales.
function activarModoEspectador() {
    const yaEraEspectador = window.usuarioEnBancarrota;
    window.usuarioEnBancarrota = true;
    bloquearControles();
    cerrarModalCompra();
    if (modalVenderPropiedad?.open) modalVenderPropiedad.close();
    if (modalOfertaRecibida?.open) modalOfertaRecibida.close();
    ofertaActiva = null;
    document.querySelectorAll(
        ".propiedad-propia__acciones button, #btn-vender-al-banco, #btn-enviar-oferta, #btn-aceptar-oferta, #btn-rechazar-oferta",
    ).forEach((boton) => { boton.disabled = true; });
    actualizarBotonTirada();
    actualizarAvisoDeuda();
    if (!yaEraEspectador) mostrarAvisoPartida("Estás en quiebra. Podés seguir observando la partida.");
}

// Atiende la quiebra confirmada sin ejecutar las acciones ni los mensajes de abandono.
function procesarNotificacionQuiebra(datos) {
    eliminarFichaJugadorAbandonado(datos.idJugador);
    if (!quiebrasNotificadas.has(String(datos.idJugador))) {
        quiebrasNotificadas.add(String(datos.idJugador));
        agregarAlHistorial(`${datos.apodo} se declaró en quiebra.`, datos.idJugador);
        actualizarDineroJugador(datos.idJugador, null, true);
        const tarjeta = document.getElementById(String(datos.idJugador));
        tarjeta?.classList.remove("turno-activo");
        tarjeta?.querySelector(".contenedor-turno")?.replaceChildren();
    }
    if (datos.partidaFinalizada === true) {
        detenerPartidaEnPantalla();
        consultarResultadosFinales();
    }
}

// Detiene la presentación y evita que respuestas anteriores reactiven el turno.
function detenerPartidaEnPantalla() {
    document.getElementById("btn-continuar-evento")?.click();
    partidaFinalizada = true;
    actualizarAvisoEsperaQuiebra();
    document.querySelectorAll("dialog[open]:not(#modal-victoria)").forEach(modal => modal.close());
    document.getElementById("aviso-evento")?.classList.remove("aviso-evento--visible");
    document.getElementById("aviso-evento")?.setAttribute("aria-hidden", "true");
    document.getElementById("aviso-global")?.classList.remove("aviso-evento--visible");
    document.getElementById("aviso-global")?.setAttribute("aria-hidden", "true");
    bloquearAccionesFinalizadas();
    if (modalOfertaRecibida?.open) modalOfertaRecibida.close();
    if (modalVenderPropiedad?.open) modalVenderPropiedad.close();
    ofertaActiva = null;
    cerrarModalCompra();
    document.querySelectorAll(".propiedad-propia__acciones button, #btn-aceptar-oferta, #btn-rechazar-oferta")
        .forEach((boton) => { boton.disabled = true; });
    actualizarAvisoDeuda();

    clearInterval(intervaloReloj);
    window.turnoActualId = null;
    document.querySelectorAll(".contenedor-turno").forEach((contenedor) => contenedor.replaceChildren());
    document.querySelectorAll(".jugador.turno-activo").forEach((tarjeta) => tarjeta.classList.remove("turno-activo"));

    if (botonTirar) {
        botonTirar.disabled = true;
    }

    document.querySelectorAll(".btn-accion").forEach((boton) => {
        boton.disabled = true;
    });

}

let consultaResultadosEnCurso = null;
let resultadosFinalesMostrados = false;

function esModalPermitidoDuranteEvento(modal) {
    return ["modal-deuda-pendiente", "deuda-confirmacion", "modal-bono-propiedades", "modal-ayuda-propiedades", "modal-vender-propiedad", "modal-elegir-propiedad-venta", "modal-abandonar", "modal-oferta-recibida", "modal-intercambio", "modal-intercambio-recibido", "chat-telefono"].includes(modal.id);
}

function abrirModalPartida(modal) {
    if (estadoEventoGlobalCliente?.activo && !esModalPermitidoDuranteEvento(modal)) return;
    if (partidaFinalizada || modal.open) return;
    const anterior = [...document.querySelectorAll("dialog[open]:not(.dialogo-en-pausa)")].at(-1);
    modal.showModal();
    if (anterior) {
        anterior.classList.add("dialogo-en-pausa");
        modal.addEventListener("close", () => anterior.classList.remove("dialogo-en-pausa"), { once: true });
    }
}

function bloquearAccionesFinalizadas() {
    if (!partidaFinalizada) return;
    document.querySelectorAll("button").forEach(boton => {
        if (!boton.closest("#modal-victoria, #chat-partida, [data-abrir-chat], #btn-sonido") && !boton.disabled) boton.disabled = true;
    });
}

const observadorFinalizacion = new MutationObserver(bloquearAccionesFinalizadas);
observadorFinalizacion.observe(document.body, {
    subtree: true, childList: true, attributes: true, attributeFilter: ["disabled"],
});
window.addEventListener("pagehide", () => observadorFinalizacion.disconnect());

document.addEventListener("click", evento => {
    if (partidaFinalizada && !evento.target.closest("#modal-victoria, #chat-partida, [data-abrir-chat], #btn-sonido")) {
        evento.preventDefault();
        evento.stopImmediatePropagation();
    }
}, true);

document.getElementById("modal-victoria").addEventListener("cancel", evento => evento.preventDefault());

function consultarResultadosFinales() {
    if (!codigoPartida || resultadosFinalesMostrados) return Promise.resolve();
    if (consultaResultadosEnCurso) return consultaResultadosEnCurso;
    consultaResultadosEnCurso = (async () => {
        try {
            const respuesta = await fetch(
                `${basePath}/partida/${encodeURIComponent(codigoPartida)}/resultados-finales`,
                { cache: "no-store", credentials: "same-origin" },
            );
            if (!respuesta.ok) return;
            const datos = await respuesta.json();
            if (datos.estado !== "FINALIZADA" || !Array.isArray(datos.resultados)) return;
            detenerPartidaEnPantalla();
            mostrarResultadosFinales(datos);
            resultadosFinalesMostrados = true;
        } catch {
            return;
        } finally {
            consultaResultadosEnCurso = null;
        }
    })();
    return consultaResultadosEnCurso;
}

function mostrarResultadosFinales(datos) {
    const mensaje = document.getElementById("mensaje-victoria");
    mensaje.textContent = datos.motivo || "";
    mensaje.hidden = !datos.motivo;
    const ganadores = datos.resultados.filter(resultado => resultado.ganador === true);
    document.getElementById("ganadores-partida").textContent =
        `${ganadores.length === 1 ? "Ganador" : "Ganadores"}: ${ganadores.map(resultado => resultado.apodo).join(" · ")}`;
    const ranking = document.getElementById("ranking-final");
    ranking.replaceChildren();
    datos.resultados.forEach((resultado, indice) => {
        const fila = document.createElement("li");
        if (resultado.ganador === true) fila.classList.add("resultado-ganador");
        const detalle = document.createElement("details");
        const resumen = document.createElement("summary");
        const estado = { QUEBRADO: "En quiebra", ABANDONADO: "Abandonó" }[resultado.estadoParticipante];
        resumen.textContent = `${indice + 1}. ${resultado.apodo}${estado ? ` · ${estado}` : ""}${resultado.ganador === true ? " · GANADOR" : ""}`;
        const lista = document.createElement("dl");
        const campos = [
            ["Dinero final", resultado.dineroFinal, true],
            ["Cantidad de propiedades", resultado.cantidadPropiedades, false],
            ["Cantidad de mejoras", resultado.cantidadMejoras, false],
            ["Valor de propiedades incluyendo mejoras", resultado.valorPropiedades, true],
            ["Deuda pendiente", resultado.deudaPendiente, true],
            ["Patrimonio total", resultado.patrimonioTotal, true],
        ];
        campos.forEach(([nombre, valor, dinero]) => {
            const campo = document.createElement("div");
            const etiqueta = document.createElement("dt");
            etiqueta.textContent = nombre;
            const dato = document.createElement("dd");
            dato.textContent = valor == null ? "No disponible"
                : `${dinero ? "$" : ""}${Number(valor).toLocaleString("es-AR")}`;
            campo.append(etiqueta, dato);
            lista.append(campo);
        });
        detalle.append(resumen, lista);
        fila.append(detalle);
        ranking.append(fila);
    });
    const modal = document.getElementById("modal-victoria");
    if (!modal.open) modal.showModal();
}


function actualizarAvisoEsperaQuiebra() {
    sincronizarEsperaQuiebra(document.getElementById("aviso-espera-quiebra"), {
        pausado: !document.getElementById("chat-telefono")?.open && !viendoPropiedadesDuranteEspera && (window.turnoPausadoPorDeuda || estadoEventoGlobalCliente?.deudaPendiente),
        estadoDeuda: estadoDeudaLocal,
        finalizada: partidaFinalizada,
    });
}

document.getElementById("aviso-espera-quiebra")?.addEventListener("cancel", evento => evento.preventDefault());

document.getElementById("espera-ver-propiedades")?.addEventListener("click", () => {
    viendoPropiedadesDuranteEspera = true;
    actualizarAvisoEsperaQuiebra();
    abrirModalPartida(modalBonoPropiedades);
    if (!modalBonoPropiedades.open) {
        viendoPropiedadesDuranteEspera = false;
        actualizarAvisoEsperaQuiebra();
        return;
    }
    cargarPropiedadesPropias();
});

document.getElementById("btn-info-mejorar")?.addEventListener("click", () => {
    if (propiedadSeleccionada) subirAlquilerPropiedad(propiedadSeleccionada.posicion,
        document.getElementById("btn-info-mejorar"), notaModalInfo);
});
document.getElementById("btn-info-vender")?.addEventListener("click", () => {
    if (!propiedadSeleccionada) return;
    const propiedad = propiedadSeleccionada;
    modalInfoPropiedad.close();
    abrirOpcionesVenta(propiedad);
});

function crearTarjetaResumenPropiedad(propiedad, titulo) {
    const tarjeta = crearTarjetaPropiedad(propiedad, null, true);
    tarjeta.style.setProperty("--color-propiedad", posicionesParadas.has(Number(propiedad.posicion))
        ? colorParadas : coloresLocalidades[propiedad.grupo] || "#c3b9d2");
    if (titulo) {
        const etiqueta = document.createElement("p");
        etiqueta.className = "intercambio-etiqueta";
        etiqueta.textContent = titulo;
        tarjeta.prepend(etiqueta);
    }
    return tarjeta;
}

gestionIntercambios = inicializarIntercambios({
    basePath, codigo: codigoPartida, usuarioId: window.usuarioSesionId,
    propiedades: () => [...catalogoPropiedadesActual.values()],
    cargarPropiedades: cargarPropiedadesPropias, abrirModal: abrirModalPartida,
    elegirPropiedad: abrirSelectorPropiedades,
    reproducirConfirmacion: reproducirIntercambioConfirmado,
    pintarJugadores: pintarJugadoresParaElegir,
    crearTarjetaResumen: crearTarjetaResumenPropiedad,
    finalizada: () => partidaFinalizada, espectador: () => Boolean(window.usuarioEnBancarrota),
    avisar: mostrarAvisoPartida, aplicarPropiedad: propiedad => {
        actualizarEtiquetaPrecioPropiedad(propiedad);
        if (Number(propiedadSeleccionada?.posicion) === Number(propiedad.posicion)) mostrarInfoPropiedad(propiedad);
    },
});

chatPartida = inicializarChat({
    basePath, codigo: codigoPartida, usuarioId: window.usuarioSesionId, notificar: reproducirChatEntrante, alCambiarVisibilidad: actualizarAvisoEsperaQuiebra,
});
