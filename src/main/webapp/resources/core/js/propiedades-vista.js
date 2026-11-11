// Aplica únicamente la última consulta iniciada, reemplazando la lista de una sola vez.
export function crearCargaPropiedades(obtener, aplicar, informarError) {
    let revision = 0;
    return async () => {
        const actual = ++revision;
        try {
            const propiedades = await obtener();
            if (actual !== revision || propiedades == null) return;
            const unicas = [...new Map(propiedades.map(p => [Number(p.posicion), p])).values()];
            aplicar(unicas);
        } catch (error) {
            if (actual === revision) informarError(error);
        }
    };
}
