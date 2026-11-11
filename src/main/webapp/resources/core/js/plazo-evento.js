// El plazo de la presentación es independiente de las pausas y del reloj del siguiente turno.
export function esperarContinuacion({ boton, eventos, vencimiento, sigueVigente = () => true,
    ahora = Date.now, programar = setTimeout, cancelar = clearTimeout }) {
    return new Promise(resolve => {
        let temporizador;
        const terminar = aceptar => {
            cancelar(temporizador);
            boton.removeEventListener("click", continuar);
            eventos.removeEventListener("fin-plazo-turno", vencer);
            resolve(aceptar && ahora() < vencimiento && sigueVigente());
        };
        const continuar = () => terminar(true);
        const vencer = () => terminar(false);
        boton.addEventListener("click", continuar);
        eventos.addEventListener("fin-plazo-turno", vencer);
        temporizador = programar(vencer, Math.max(0, vencimiento - ahora()));
        if (ahora() >= vencimiento || !sigueVigente()) vencer();
    });
}
