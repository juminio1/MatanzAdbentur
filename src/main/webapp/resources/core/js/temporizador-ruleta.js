
export function calcularSegundosRestantes(venceEn, ahora = Date.now()) {

    const vencimiento = Number(venceEn);

    if (!Number.isFinite(vencimiento) || vencimiento <= 0) {
        return 0;
    }

    return Math.max(0, Math.ceil((vencimiento - ahora) / 1000));
}

export function iniciarTemporizadorRuleta(segundos, girar) {

    // Se conserva para mantener compatibilidad con los tests anteriores.
    // La ruleta sincronizada no utiliza este temporizador para sortear.
    return setTimeout(() => {
        girar();
    }, segundos * 1000);
}
