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
