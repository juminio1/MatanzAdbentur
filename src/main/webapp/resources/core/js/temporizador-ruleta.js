export function iniciarTemporizadorRuleta(segundos, girar) {

    return setTimeout(() => {
        girar();
    }, segundos * 1000);
}
