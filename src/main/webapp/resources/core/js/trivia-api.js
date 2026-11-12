// Lecturas del controlador existente. Un 404 indica que no hay trivia.
export function crearApiTrivia({ base = "", codigo, solicitar = fetch } = {}) {
    const ruta = base + "/partida/" + encodeURIComponent(codigo) + "/trivia";
    async function consultar(sufijo) {
        const respuesta = await solicitar(ruta + sufijo, {
            cache: "no-store", credentials: "same-origin",
        });
        if (respuesta.status === 404) return null;
        if (!respuesta.ok) throw new Error("HTTP " + respuesta.status);
        return respuesta.json();
    }
    function obtenerEstado() { return consultar(""); }
    function obtenerPregunta() { return consultar("/pregunta"); }
    async function enviarRespuesta(id, opcion) {
        const respuesta = await solicitar(ruta + "/respuesta", {
            method: "POST", credentials: "same-origin",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ id, opcion }),
        });
        if (!respuesta.ok) throw new Error("HTTP " + respuesta.status);
        return respuesta.json();
    }
    async function obtenerPropiedades() {
        const respuesta = await solicitar(base + "/partida/" + encodeURIComponent(codigo) + "/propiedades", {
            cache: "no-store", credentials: "same-origin",
        });
        if (!respuesta.ok) throw new Error("HTTP " + respuesta.status);
        return respuesta.json();
    }
    async function elegirPremio(eleccion) {
        const respuesta = await solicitar(ruta + "/premio", {
            method: "POST", credentials: "same-origin",
            headers: { "Content-Type": "application/json" }, body: JSON.stringify(eleccion),
        });
        if (!respuesta.ok) throw new Error("HTTP " + respuesta.status);
        return respuesta.json();
    }
    return { obtenerEstado, obtenerPregunta, enviarRespuesta, obtenerPropiedades, elegirPremio };
}
