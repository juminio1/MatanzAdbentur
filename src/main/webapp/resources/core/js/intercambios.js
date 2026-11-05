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
    const jugador = el("intercambio-jugador");
    const ajena = el("intercambio-ajena");
    const pendientes = new Map();
    const enviadas = new Map();
    const descartadas = new Set();
    const resueltas = new Set();
    let actual = null;
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

    function opciones(select, datos, vacio) {
        const valor = select.value;
        select.replaceChildren(...datos.map(dato => new Option(dato.texto, String(dato.valor))));
        if (!datos.length) select.add(new Option(vacio, ""));
        if ([...select.options].some(opcion => opcion.value === valor)) select.value = valor;
        select.disabled = !datos.length;
    }

    function descripcion(propiedad) {
        const nivel = Number(propiedad.nivelMejora ?? 0);
        return `${propiedad.nombre} · Alquiler $${Number(propiedad.alquiler).toLocaleString("es-AR")}${nivel ? ` · Nivel ${nivel}` : ""}`;
    }

    function refrescar() {
        const propiedades = config.propiedades();
        opciones(propia, propiedadesParaIntercambiar(propiedades, config.usuarioId)
            .map(p => ({ valor: p.posicion, texto: descripcion(p) })), "No tenés propiedades para intercambiar");
        const jugadores = [...document.querySelectorAll(".jugador")]
            .filter(card => card.id !== String(config.usuarioId) && card.dataset.bancarrota !== "true"
                && !card.querySelector(".estado-abandono") && card.dataset.abandono !== "true");
        opciones(jugador, jugadores.map(card => ({ valor: card.id,
            texto: card.querySelector(".nombre-jugador")?.textContent || "Jugador" })), "No hay otros jugadores disponibles");
        cambiarJugador();
    }

    function cambiarJugador() {
        opciones(ajena, propiedadesParaIntercambiar(config.propiedades(), jugador.value)
            .map(p => ({ valor: p.posicion, texto: descripcion(p) })), "Este jugador no tiene propiedades");
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
        el("enviar-intercambio").disabled = !habilitado() || enviando || !propia.value || !jugador.value || !ajena.value || tienePropuesta(propia.value);
        document.querySelectorAll("[data-posicion-intercambio]").forEach(boton => {
            const reservada = tienePropuesta(boton.dataset.posicionIntercambio);
            boton.disabled = !habilitado() || reservada;
            boton.textContent = reservada ? "Intercambio pendiente" : "Intercambiar";
            boton.title = reservada ? "Esperá la respuesta o el vencimiento de la propuesta." : "";
        });
        el("aceptar-intercambio").disabled = !habilitado() || respondiendo || !actual;
        el("rechazar-intercambio").disabled = !habilitado() || respondiendo || !actual;
        el("intercambio-resumen").textContent = tienePropuesta(propia.value)
            ? "Esta propiedad ya tiene una propuesta pendiente. Podés elegir otra propiedad desde el selector."
            : propia.value && ajena.value ? "Ofrecés tu propiedad a cambio de la propiedad elegida. El intercambio se realiza sólo si el otro jugador acepta." : "";
    }

    async function abrir(posicion) {
        if (!habilitado()) return;
        el("intercambio-mensaje").textContent = "";
        refrescar();
        if (posicion != null) propia.value = String(posicion);
        actualizarEstado();
        config.abrirModal(modal);
        await config.cargarPropiedades();
        if (modal.open) refrescar();
    }

    async function enviar() {
        if (!habilitado() || enviando || !propia.value || !jugador.value || !ajena.value || tienePropuesta(propia.value)) return;
        enviando = true;
        actualizarEstado();
        try {
            const datos = await json(ruta, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({
                idDestinatario: Number(jugador.value), posicionOfrecida: Number(propia.value), posicionSolicitada: Number(ajena.value),
            }) });
            recibir(datos);
            modal.close();
            config.avisar("Propuesta de intercambio enviada. El otro jugador puede aceptarla o rechazarla.");
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
        el("intercambio-proponente").textContent = `${propuesta.apodoProponente} quiere intercambiar con vos.`;
        pintarRecibida();
        el("intercambio-recibido-mensaje").textContent = "";
        config.abrirModal(recibido);
        if (!recibido.open) actual = null;
        actualizarTiempo();
        actualizarEstado();
    }

    function pintarRecibida() {
        if (!actual) return;
        const propiedades = config.propiedades();
        const ofrecida = propiedades.find(p => Number(p.posicion) === Number(actual.posicionOfrecida));
        const solicitada = propiedades.find(p => Number(p.posicion) === Number(actual.posicionSolicitada));
        el("intercambio-recibis").textContent = ofrecida ? descripcion(ofrecida) : actual.nombreOfrecida;
        el("intercambio-entregas").textContent = solicitada ? descripcion(solicitada) : actual.nombreSolicitada;
    }

    function recibir(datos) {
        if (datos.estado === "pendiente") {
            if (resueltas.has(datos.id)) return;
            if (String(datos.idProponente) === String(config.usuarioId)) enviadas.set(datos.id, datos);
            if (String(datos.idDestinatario) === String(config.usuarioId)) pendientes.set(datos.id, datos);
        } else {
            resueltas.add(datos.id);
            pendientes.delete(datos.id);
            enviadas.delete(datos.id);
            if (actual?.id === datos.id) { cerrarRecibido(); }
            if (String(datos.idProponente) === String(config.usuarioId)) {
                config.avisar(datos.estado === "aceptado" ? "Aceptaron tu intercambio." : "Rechazaron tu intercambio.");
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
        try {
            const datos = await json(ruta);
            pendientes.clear();
            const recibidas = Array.isArray(datos) ? datos : datos.recibidas || [];
            recibidas.filter(p => !resueltas.has(p.id)).forEach(p => pendientes.set(p.id, p));
            enviadas.clear();
            (datos.enviadas || []).filter(p => !resueltas.has(p.id)).forEach(p => enviadas.set(p.id, p));
            actualizarEstado();
            if (actual && !pendientes.has(actual.id) && !respondiendo) { cerrarRecibido(); }
            mostrarSiguiente();
        } catch (error) {
            console.warn("No se pudieron consultar los intercambios.", error);
        } finally {
            consultando = false;
        }
    }

    async function responder(aceptar) {
        if (!actual || respondiendo || !habilitado()) return;
        const propuesta = actual;
        respondiendo = true;
        actualizarEstado();
        try {
            const datos = await json(`${ruta}/${encodeURIComponent(propuesta.id)}/responder`, {
                method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ aceptar }),
            });
            recibir(datos);
            pendientes.delete(propuesta.id);
            cerrarRecibido();
            config.avisar(aceptar ? "Intercambio realizado. Las mejoras se conservaron." : "Intercambio rechazado.");
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
        el("intercambio-tiempo").textContent = `${Math.floor(segundos / 60)}:${String(segundos % 60).padStart(2, "0")}`;
        if (!segundos && !respondiendo) { pendientes.delete(actual.id); cerrarRecibido(); }
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
        cerrandoRecibido = false;
        if (actual) descartadas.add(actual.id);
        actual = null;
    });
    el("aceptar-intercambio").addEventListener("click", () => responder(true));
    el("rechazar-intercambio").addEventListener("click", () => responder(false));
    jugador.addEventListener("change", cambiarJugador);
    propia.addEventListener("change", actualizarEstado);
    ajena.addEventListener("change", actualizarEstado);
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
