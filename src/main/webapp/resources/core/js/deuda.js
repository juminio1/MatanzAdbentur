export function validarRespuestaDeuda(datos) {
    //Comprueba que el servidor haya enviado los datos y q tieneDeudaPendeinte sea un valor booleano.
    if (!datos || typeof datos.tieneDeudaPendiente !== "boolean") {
        throw new Error("La respuesta de deuda no es válida.");
    }
    //verifica si el jugador NO tiene deuda
    if (!datos.tieneDeudaPendiente) {
        if (datos.montoPendiente !== null || datos.motivo !== null || datos.acreedor !== null) {
            throw new Error("La respuesta sin deuda está incompleta o no es válida.");
        }
        return null; //no hay deuda pendiente
    }
    //verifica los datos de la deuda. Monto valido y mayor a 0, que el motivo sea un texto y q no este vacio.
    if (
        !Number.isSafeInteger(datos.montoPendiente) ||
        datos.montoPendiente <= 0 ||
        typeof datos.motivo !== "string" ||
        !datos.motivo.trim()
    ) {
        throw new Error("Los datos de la deuda pendiente no son válidos.");
    }
    const acreedor = datos.acreedor;
    //verifica los datos del acreedor. Si existe a quien le debe dinero, comprueba un id valido y q el apodo no este vacio.
    if (
        acreedor !== null &&
        (!acreedor ||
            !Number.isSafeInteger(acreedor.idJugador) ||
            acreedor.idJugador <= 0 ||
            typeof acreedor.apodo !== "string" ||
            !acreedor.apodo.trim())
    ) {
        throw new Error("Los datos del acreedor no son válidos.");
    }
    //Devuelve la info. de la deuda ya validada.
    return {
        montoPendiente: datos.montoPendiente,
        motivo: datos.motivo,
        acreedor:
            acreedor === null ? null : { idJugador: acreedor.idJugador, apodo: acreedor.apodo },
    };
}

// consulta al servidor si el jugador tiene una deuda pendiente y actualiza el estado local.

// La pausa es global, pero este cartel solo corresponde a quienes no tienen deuda propia.
// Un estado desconocido o fallido nunca debe tapar las acciones para resolverla.
export function sincronizarEsperaQuiebra(aviso, { pausado, estadoDeuda, finalizada }) {
    if (!aviso) return;
    const mostrar = Boolean(pausado && estadoDeuda.estado === "actualizado"
        && estadoDeuda.deuda === null && !finalizada);
    if (mostrar && !aviso.open) aviso.showModal();
    if (!mostrar && aviso.open) aviso.close();
}
