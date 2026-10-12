// DADOS

const dado1 = document.getElementById("dado-1");
const dado2 = document.getElementById("dado-2");
const botonTirar = document.getElementById("btn-tirar");

// Une el front con el backend. Conecta con controladorPartida metodo tirar dados.
async function obtenerResultadoDados() {
    const respuesta = await fetch("http://localhost:8080/spring/partida/tirar-dados", {
        method: "POST",
    });
    if (!respuesta.ok) {
        throw new Error("No se pudieron tirar los dados");
    }
    return await respuesta.json();
}

botonTirar.addEventListener("click", async () => {
    const resultado = await obtenerResultadoDados();
    console.log("Resultado completo:", resultado);
    dado1.textContent = resultado.dado1;
    dado2.textContent = resultado.dado2;
});

//busca todos los elementos del html que tenga la clase casillero
const casilleros = document.querySelectorAll(".casillero");
console.log("Casilleros encontrados:", casilleros.length);
const ficha = document.createElement("div");
ficha.classList.add("ficha");
casilleros[0].appendChild(ficha);
