// Recibe tanto las notificaciones como las consultas HTTP del mismo evento.
export function crearHistorialEventosGlobales(registrar) {
    const rondas = new Map();
    let ultimaRevision = -1;

    return estado => {
        if (!estado || !Number.isSafeInteger(estado.revision) || estado.revision < ultimaRevision) return;
        ultimaRevision = estado.revision;
        if (!(estado.ronda > 0) || !estado.nombre) return;
        let ronda = rondas.get(estado.ronda);
        if (!ronda) {
            ronda = { registrados: new Set(), pausado: false };
            rondas.set(estado.ronda, ronda);
        }
        const nombre = estado.nombre.replaceAll("_", " ");
        const jugadores = estado.jugadores ?? [];
        const jugador = id => jugadores.find(j => String(j.id) === String(id))?.apodo || `Jugador ${id}`;
        const anotar = (clave, texto, id) => {
            if (ronda.registrados.has(clave)) return;
            ronda.registrados.add(clave);
            registrar(texto, id);
        };
        const descripcion = (estado.descripcion || "").replaceAll("[nombre del jugador]",
            estado.seleccionado ? jugador(estado.seleccionado) : "un jugador");
        anotar("inicio", `Evento global «${nombre}» (ronda ${estado.ronda}).${descripcion ? ` ${descripcion}` : ""}`);

        const tiradas = Object.entries(estado.tiradas ?? {}).map(([clave, valor]) => {
            const [etapa, id] = clave.split(":").map(Number);
            return { clave, etapa, id, valor };
        }).filter(t => Number.isSafeInteger(t.etapa) && t.etapa > 0 && Number.isSafeInteger(t.id)
            && Number.isInteger(t.valor) && Math.floor(t.valor / 10) >= 1 && Math.floor(t.valor / 10) <= 6
            && t.valor % 10 >= 1 && t.valor % 10 <= 6)
            .sort((a, b) => a.etapa - b.etapa || a.id - b.id);
        const desempate = etapa => {
            if (etapa > 1) anotar(`etapa:${etapa}`, `«${nombre}»: desempate ${etapa - 1}; vuelven a tirar los jugadores empatados.`);
        };
        tiradas.forEach(tirada => {
            desempate(tirada.etapa);
            const dado1 = Math.floor(tirada.valor / 10);
            const dado2 = tirada.valor % 10;
            anotar(`tirada:${tirada.clave}`, `${jugador(tirada.id)} sacó ${dado1} y ${dado2} (${dado1 + dado2}) en «${nombre}»${tirada.etapa > 1 ? `, desempate ${tirada.etapa - 1}` : ""}.`, tirada.id);
        });
        desempate(Number(estado.etapa));
        if (estado.resuelto && estado.consecuencia?.trim()) {
            anotar("resultado", `Resultado de «${nombre}»: ${estado.consecuencia.trim()}`, estado.seleccionado);
        }
        // La consecuencia ya incluye la tarjeta de casillero, si el movimiento provocó una.
        if (estado.deudaPendiente) {
            anotar("deuda", `«${nombre}»: la partida queda en pausa hasta resolver la deuda pendiente.`);
        } else if (ronda.pausado && estado.activo) {
            anotar("deuda-resuelta", `«${nombre}»: la deuda fue resuelta; el evento puede continuar.`);
        }
        ronda.pausado = Boolean(estado.deudaPendiente);
        if (!estado.activo) anotar("fin", `Finalizó el evento global «${nombre}».`);
    };
}
