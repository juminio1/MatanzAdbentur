
const DIFICULTADES_VALIDAS = ["FACIL", "MEDIO", "DIFICIL"];

export async function solicitarSorteoPartida(basePath, codigoPartida, activacionId, fetchFuncion = fetch) {

    if (!codigoPartida || !activacionId) {
        throw new Error("Faltan los datos de activación de la ruleta.");
    }

    const url = `${basePath}/ruleta/${encodeURIComponent(codigoPartida)}/${encodeURIComponent(activacionId)}/sortear`;

    const respuesta = await fetchFuncion(url, {
        method: "POST",
        credentials: "same-origin",
        headers: { Accept: "application/json" }
    });

    if (!respuesta.ok) {
        throw new Error(`No se pudo sortear la dificultad (HTTP ${respuesta.status}).`);
    }

    const estado = await respuesta.json();

    if (!estado || estado.codigoPartida !== codigoPartida ||
        estado.activacionId !== activacionId ||
        estado.estado !== "SORTEADA" ||
        !DIFICULTADES_VALIDAS.includes(estado.dificultad)) {
        throw new Error("El backend devolvió un resultado de ruleta inválido.");
    }

    return estado;
}

export async function consultarEstadoRuleta(basePath, codigoPartida, fetchFuncion = fetch) {

    if (!codigoPartida) {
        throw new Error("Falta el código de la partida.");
    }

    const url = `${basePath}/ruleta/${encodeURIComponent(codigoPartida)}/estado`;

    const respuesta = await fetchFuncion(url, {
        method: "GET",
        credentials: "same-origin",
        cache: "no-store",
        headers: { Accept: "application/json" }
    });

    // Todavía no existe ninguna activación para esta partida.
    if (respuesta.status === 404) {
        return null;
    }

    if (!respuesta.ok) {
        throw new Error(`No se pudo consultar la ruleta (HTTP ${respuesta.status}).`);
    }

    const estado = await respuesta.json();

    if (!estado || estado.codigoPartida !== codigoPartida ||
        !estado.activacionId || !["ESPERANDO", "SORTEADA"].includes(estado.estado)) {
        throw new Error("El backend devolvió un estado de ruleta inválido.");
    }

    if (estado.estado === "SORTEADA" && !DIFICULTADES_VALIDAS.includes(estado.dificultad)) {
        throw new Error("El backend devolvió una dificultad inválida.");
    }

    return estado;
}

export async function solicitarDificultad(basePath, fetchFuncion = fetch) {

    const respuesta = await fetchFuncion(`${basePath}/ruleta/sortear`, {
        method: "POST"
    });

    if (!respuesta.ok) {
        throw new Error("No se pudo sortear la dificultad");
    }

    const dificultad = (await respuesta.text()).trim();

    if (!["FACIL", "MEDIO", "DIFICIL"].includes(dificultad)) {
        throw new Error("El backend devolvió una dificultad inválida");
    }

    return dificultad;
}

