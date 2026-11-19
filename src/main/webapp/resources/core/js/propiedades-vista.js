// Agrupa solicitudes y descarta la respuesta si cambió el catálogo mientras se consultaba.
export function crearCargaPropiedades(obtener, aplicar, informarError) {
    let revision = 0;
    let pendiente = null;
    return (catalogoCambio = false) => {
        if (catalogoCambio) revision++;
        if (pendiente) return pendiente;
        pendiente = Promise.resolve().then(async () => {
            let actual;
            do {
                actual = revision;
                try {
                    const propiedades = await obtener();
                    if (actual !== revision || propiedades == null) continue;
                    const unicas = [...new Map(propiedades.map(p => [Number(p.posicion), p])).values()];
                    aplicar(unicas);
                } catch (error) {
                    if (actual === revision) informarError(error);
                }
            } while (actual !== revision);
        }).finally(() => { pendiente = null; });
        return pendiente;
    };
}
