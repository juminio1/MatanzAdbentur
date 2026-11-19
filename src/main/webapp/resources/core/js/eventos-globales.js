// El panel decide cuándo llamar: un resultado puede seguir siendo un evento activo.
export function crearPresentadorSonidoEventoGlobal(codigo, reproducir, obtenerAlmacen) {
    const presentados = new Set();
    let ultimaRonda = 0;
    return estado => {
        if (!estado?.activo || !Number.isSafeInteger(estado.ronda)
                || estado.ronda <= 0 || estado.ronda < ultimaRonda) return;
        ultimaRonda = estado.ronda;
        const clave = `sonido-evento-global:${codigo}:${estado.ronda}:${estado.nombre}`;
        if (presentados.has(clave)) return;
        presentados.add(clave);
        try {
            const almacen = obtenerAlmacen();
            if (almacen?.getItem(clave)) return;
            almacen?.setItem(clave, "presentado");
        } catch {
            // Sin almacenamiento, el Set evita repeticiones durante esta carga.
        }
        reproducir(estado.nombre);
    };
}

// Las revisiones pertenecen a la partida y crecen también cuando se cierra el evento.
export function aceptarEstadoEvento(actual, siguiente) {
    return !!siguiente && Number.isSafeInteger(siguiente.revision)
        && (!actual || siguiente.revision >= actual.revision);
}

export function puedeTirarEnEvento(estado, jugador) {
    return !!estado?.activo && !estado.resuelto
        && estado.habilitados.some(id => String(id) === String(jugador))
        && estado.tiradas[`${estado.etapa}:${jugador}`] == null;
}

// Durante un evento se conserva el turno más reciente aunque llegue una consulta vieja después.
export function seleccionarTurnoMasReciente(actual, siguiente) {
    if (!siguiente || siguiente.idJugadorActual == null) return actual;
    if (!actual) return siguiente;
    const rondaActual = Number(actual.rondaActual ?? 0);
    const rondaNueva = Number(siguiente.rondaActual ?? 0);
    if (rondaNueva < rondaActual) return actual;
    if (rondaNueva === rondaActual && Number(siguiente.turnosFinalizadosEnRonda ?? 0)
            < Number(actual.turnosFinalizadosEnRonda ?? 0)) return actual;
    return siguiente;
}
