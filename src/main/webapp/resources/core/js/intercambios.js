export function propiedadesParaIntercambiar(propiedades, propietario) {
    return propiedades.filter(p => p.propietarioId != null && String(p.propietarioId) === String(propietario));
}

export function tieneIntercambioPendiente(propuestas, posicion, ahora = Date.now()) {
    return propuestas.some(p => Number(p.posicionOfrecida) === Number(posicion) && p.vence > ahora);
}

export function inicializarIntercambios(config) {
    const el = id => document.getElementById(id);
    const modal = el("modal-intercambio");
    const recibido = el("modal-intercambio-recibido");
    const propia = el("intercambio-propia");
    const jugadores = el("intercambio-jugadores");
    const contrapropiedad = el("intercambio-contrapropiedad");
    const jugadorElegido = () => jugadores.querySelector("input:checked")?.value;
    const pendientes = new Map();
    const enviadas = new Map();
    const descartadas = new Set();
    const resueltas = new Set();
    let actual = null;
    let posicionPropia = null;
    let posicionContrapropiedad = null;
    let revision = 0;
    let enviando = false;
    let respondiendo = false;
    let consultando = false;
    let cerrandoRecibido = false;
    const habilitado = () => !config.finalizada() && !config.espectador();
    const ruta = `${config.basePath}/partida/${config.codigo}/intercambios`;

    async function json(url, opciones) {
        const respuesta = await fetch(url, { credentials: "same-origin", cache: "no-store", ...opciones });
        const datos = await respuesta.json();
        if (!respuesta.ok) throw new Error(datos.error || "No se pudo completar el intercambio.");
        return datos;
    }

    function refrescar() {
        config.pintarJugadores(jugadores, "intercambio-jugador", jugadorElegido());
        pintarPropia();
    }

    function pintarPropia() {
        const elegida = propiedadesParaIntercambiar(config.propiedades(), config.usuarioId)
            .find(p => Number(p.posicion) === Number(posicionPropia) && posicionPropia != null);
        el("intercambio-propia-texto").textContent = elegida ? `Ofrecés: ${elegida.nombre} · Cambiar` : "Elegí tu propiedad";
        actualizarEstado();
    }

    function tienePropuesta(posicion) {
        return tieneIntercambioPendiente([...enviadas.values()], posicion);
    }

    function actualizarEstado() {
        if (!habilitado()) {
            if (modal.open) modal.close();
            if (recibido.open) cerrarRecibido();
        }
        el("enviar-intercambio").disabled = !habilitado() || enviando || posicionPropia == null ||
            !jugadorElegido() || tienePropuesta(posicionPropia) ||
            !propiedadesParaIntercambiar(config.propiedades(), config.usuarioId)
                .some(p => Number(p.posicion) === Number(posicionPropia));
        document.querySelectorAll("[data-posicion-intercambio]").forEach(boton => {
            const reservada = tienePropuesta(boton.dataset.posicionIntercambio);
            boton.disabled = !habilitado() || reservada;
            boton.textContent = reservada ? "Intercambio pendiente" : "Intercambiar";
            boton.title = reservada ? "Esperá la respuesta o el vencimiento de la propuesta." : "";
        });
        el("aceptar-intercambio").disabled = !habilitado() || respondiendo || !actual || resueltas.has(actual.id) ||
            (debeElegirContrapropiedad() && posicionContrapropiedad == null);
        el("rechazar-intercambio").disabled = !habilitado() || respondiendo || !actual || resueltas.has(actual.id);
        el("intercambio-resumen").textContent = posicionPropia != null && tienePropuesta(posicionPropia)
            ? "Esta propiedad ya tiene una propuesta pendiente. Podés elegir otra propiedad desde el selector."
            : "";
    }

    function debeElegirContrapropiedad() {
        return actual?.requiereConfirmacion && !actual.confirmacionFinal;
    }

    async function abrir(posicion) {
        if (!habilitado()) return;
        el("intercambio-mensaje").textContent = "";
        posicionPropia = posicion ?? null;
        jugadores.replaceChildren();
        refrescar();
        config.abrirModal(modal);
        await config.cargarPropiedades();
        if (modal.open) refrescar();
    }

    async function enviar() {
        if (el("enviar-intercambio").disabled) return;
        enviando = true;
        actualizarEstado();
        try {
            const datos = await json(ruta, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({
                idDestinatario: Number(jugadorElegido()), posicionOfrecida: Number(posicionPropia),
            }) });
            recibir(datos);
            modal.close();
            config.avisar("Propuesta enviada. El otro jugador puede elegir qué darte a cambio.");
        } catch (error) {
            el("intercambio-mensaje").textContent = error.message;
        } finally {
            enviando = false;
            actualizarEstado();
        }
    }

    function mostrarSiguiente() {
        if (!habilitado() || actual || respondiendo || cerrandoRecibido || document.querySelector("dialog[open]")
                || document.querySelector(".aviso-evento--visible")) return;
        const propuesta = [...pendientes.values()].find(p => p.vence > Date.now() && !descartadas.has(p.id));
        if (!propuesta) return;
        actual = propuesta;
        posicionContrapropiedad = null;
        el("intercambio-recibido-titulo").textContent = propuesta.confirmacionFinal
            ? "Confirmá el intercambio" : "Te proponen un intercambio";
        el("intercambio-proponente").textContent = propuesta.confirmacionFinal
            ? "Revisá las dos propiedades antes de confirmar."
            : `${propuesta.apodoProponente} quiere intercambiar con vos.`;
        contrapropiedad.hidden = !debeElegirContrapropiedad();
        contrapropiedad.textContent = "Elegí qué propiedad dar";
        recibido.classList.toggle("intercambio-confirmacion", Boolean(propuesta.confirmacionFinal));
        pintarRecibida();
        el("intercambio-recibido-mensaje").textContent = "";
        config.abrirModal(recibido);
        if (!recibido.open) actual = null;
        actualizarTiempo();
        actualizarEstado();
    }

    function pintarRecibida() {
        if (!actual || recibido.classList.contains("intercambio-confirmado")) return;
        const propiedades = config.propiedades();
        const ofrecida = propiedades.find(p => Number(p.posicion) === Number(actual.posicionOfrecida));
        const posicion = actual.posicionSolicitada ?? posicionContrapropiedad;
        const solicitada = posicion == null ? null : propiedades.find(p => Number(p.posicion) === Number(posicion));
        const tarjetas = el("intercambio-tarjetas");
        tarjetas.replaceChildren(config.crearTarjetaResumen(
            ofrecida || { posicion: actual.posicionOfrecida, nombre: actual.nombreOfrecida },
            actual.confirmacionFinal ? "Entregás" : "Recibís",
        ));
        if (actual.confirmacionFinal) {
            const flecha = document.createElement("span");
            flecha.className = "intercambio-flecha";
            flecha.setAttribute("aria-hidden", "true");
            flecha.textContent = "⇄";
            tarjetas.append(flecha);
        }
        if (solicitada || actual.nombreSolicitada) tarjetas.append(config.crearTarjetaResumen(
            solicitada || { posicion, nombre: actual.nombreSolicitada },
            actual.confirmacionFinal ? "Recibís" : "Entregás",
        ));
    }

    function recibir(datos) {
        revision++;
        if (datos.estado === "pendiente") {
            if (resueltas.has(datos.id)) return;
            const idUsuario = String(config.usuarioId);
            const anterior = pendientes.get(datos.id) || enviadas.get(datos.id);
            if (anterior?.confirmacionFinal && !datos.confirmacionFinal) return;
            if (String(datos.idProponente) === idUsuario) enviadas.set(datos.id, datos);
            const idRespuesta = datos.confirmacionFinal ? datos.idProponente : datos.idDestinatario;
            if (String(idRespuesta) === idUsuario) pendientes.set(datos.id, datos);
            else {
                pendientes.delete(datos.id);
                if (actual?.id === datos.id) cerrarRecibido();
            }
        } else {
            if (resueltas.has(datos.id)) return;
            resueltas.add(datos.id);
            pendientes.delete(datos.id);
            enviadas.delete(datos.id);
            if (actual?.id === datos.id) {
                if (datos.estado === "aceptado" && actual.confirmacionFinal) {
                    recibido.classList.add("intercambio-confirmado");
                    const tarjetas = el("intercambio-tarjetas").querySelectorAll(".propiedad-propia");
                    const animaciones = [...tarjetas].flatMap(tarjeta => tarjeta.getAnimations());
                    Promise.allSettled(animaciones.map(animacion => animacion.finished)).then(() => {
                        if (actual?.id === datos.id) cerrarRecibido();
                    });
                } else {
                    cerrarRecibido();
                }
            }
            if ([datos.idProponente, datos.idDestinatario].some(id => String(id) === String(config.usuarioId))) {
                if (datos.estado === "aceptado") {
                    config.reproducirConfirmacion();
                    config.avisar("Intercambio realizado. Las mejoras se conservaron.", false, true);
                } else {
                    config.avisar("Intercambio rechazado.");
                }
            }
            if (datos.estado === "aceptado") {
                (datos.propiedades || []).forEach(config.aplicarPropiedad);
                if (modal.open) refrescar();
            }
        }
        actualizarEstado();
        mostrarSiguiente();
    }

    async function revisar() {
        if (!habilitado() || consultando) return;
        consultando = true;
        const revisionConsultada = revision;
        try {
            const datos = await json(ruta);
            if (revisionConsultada !== revision) return;
            pendientes.clear();
            const recibidas = Array.isArray(datos) ? datos : datos.recibidas || [];
            recibidas.filter(p => !resueltas.has(p.id)).forEach(p => pendientes.set(p.id, p));
            enviadas.clear();
            (datos.enviadas || []).filter(p => !resueltas.has(p.id)).forEach(p => enviadas.set(p.id, p));
            actualizarEstado();
            if (actual && !pendientes.has(actual.id) && !respondiendo && !resueltas.has(actual.id)) cerrarRecibido();
            mostrarSiguiente();
        } catch (error) {
            console.warn("No se pudieron consultar los intercambios.", error);
        } finally {
            consultando = false;
        }
    }

    async function responder(aceptar) {
        if (!actual || respondiendo || !habilitado() || resueltas.has(actual.id)) return;
        const propuesta = actual;
        const elegir = aceptar && debeElegirContrapropiedad();
        if (elegir && posicionContrapropiedad == null) return;
        respondiendo = true;
        actualizarEstado();
        try {
            const datos = await json(`${ruta}/${encodeURIComponent(propuesta.id)}/responder`, {
                method: "POST", headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ aceptar, posicionElegida: elegir ? posicionContrapropiedad : null }),
            });
            recibir(datos);
            pendientes.delete(propuesta.id);
            if (datos.estado !== "aceptado") cerrarRecibido();
            if (elegir) config.avisar("Elección enviada. Falta la confirmación del otro jugador.");
            await config.cargarPropiedades();
        } catch (error) {
            el("intercambio-recibido-mensaje").textContent = error.message;
        } finally {
            respondiendo = false;
            actualizarEstado();
            revisar();
        }
    }

    function actualizarTiempo() {
        actualizarEstado();
        if (!actual) return;
        const segundos = Math.max(0, Math.ceil((actual.vence - Date.now()) / 1000));
        el("intercambio-reloj").classList.toggle("intercambio-reloj--urgente", segundos <= 20);
        el("intercambio-tiempo").textContent = `${Math.floor(segundos / 60)}:${String(segundos % 60).padStart(2, "0")}`;
        if (!segundos && !respondiendo && !resueltas.has(actual.id)) { pendientes.delete(actual.id); cerrarRecibido(); }
    }

    function cerrarRecibido() {
        actual = null;
        if (recibido.open) {
            cerrandoRecibido = true;
            recibido.close();
        }
    }

    el("enviar-intercambio").addEventListener("click", enviar);
    el("cerrar-intercambio").addEventListener("click", () => modal.close());
    el("cerrar-intercambio-recibido").addEventListener("click", () => recibido.close());
    recibido.addEventListener("close", () => {
        recibido.classList.remove("intercambio-confirmado");
        cerrandoRecibido = false;
        if (actual) descartadas.add(actual.id);
        actual = null;
    });
    el("aceptar-intercambio").addEventListener("click", () => responder(true));
    el("rechazar-intercambio").addEventListener("click", () => responder(false));
    jugadores.addEventListener("change", actualizarEstado);
    propia.addEventListener("click", () => config.elegirPropiedad(modal, propiedad => {
        posicionPropia = propiedad.posicion;
        pintarPropia();
    }));
    contrapropiedad.addEventListener("click", () => config.elegirPropiedad(recibido, propiedad => {
        if (!debeElegirContrapropiedad()) return;
        posicionContrapropiedad = propiedad.posicion;
        contrapropiedad.textContent = `Entregás: ${propiedad.nombre} · Cambiar`;
        pintarRecibida();
        actualizarEstado();
    }));
    document.addEventListener("close", () => queueMicrotask(mostrarSiguiente), true);
    const consulta = window.setInterval(revisar, 5000);
    const reloj = window.setInterval(actualizarTiempo, 1000);
    window.addEventListener("pagehide", () => { clearInterval(consulta); clearInterval(reloj); });
    actualizarEstado();
    queueMicrotask(revisar);
    return { abrir, recibir, revisar, actualizarEstado, tienePropuesta, refrescar: () => {
        if (modal.open) refrescar();
        if (recibido.open) pintarRecibida();
    } };
}
