const btnJugarAhora = document.getElementById("btnJugarAhora");
const modalPartida = document.getElementById("modalPartida");
const cerrarModal = document.getElementById("cerrarModal");

btnJugarAhora.addEventListener("click", () => {
    modalPartida.classList.add("activo");
});

cerrarModal.addEventListener("click", () => {
    modalPartida.classList.remove("activo");
});

modalPartida.addEventListener("click", (event) => {
    if (event.target === modalPartida) {
        modalPartida.classList.remove("activo");
    }
});