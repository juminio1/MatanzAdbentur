const confirmar = new Audio(new URL("../audio/accion-confirmar.wav", import.meta.url));
const volver = new Audio(new URL("../audio/accion-pasar.wav", import.meta.url));
confirmar.volume = volver.volume = 0.25;

function sonar(audio) {
    audio.currentTime = 0;
    audio.play().catch(() => {});
}

document.addEventListener("click", evento => {
    if (evento.target.closest("#btnVolver, #modalJugar [data-bs-dismiss]")) sonar(volver);
    else if (evento.target.closest(".hero .btn-play, #btnIrAUnirse, #btn-register, .registro-footer a")) sonar(confirmar);
});

document.addEventListener("submit", evento => {
    if (evento.target.closest(".login-card, .registro-container, #modalJugar")) sonar(confirmar);
}, true);
