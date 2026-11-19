import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
import vm from "node:vm";

test("una actualización atrasada o repetida no borra una ficha confirmada", () => {
    const script = readFileSync(new URL("../../main/webapp/resources/core/js/sala-de-espera.js", import.meta.url), "utf8");
    const suscripciones = new Map();
    const cliente = {
        connect: (_, listo) => listo(),
        subscribe: (tema, recibir) => suscripciones.set(tema, recibir),
    };
    const contexto = vm.createContext({
        document: {
            addEventListener() {},
            getElementById: () => ({ dataset: { revision: "4", usuarioId: "2" } }),
        },
        Stomp: { over: () => cliente },
        SockJS: class {},
    });
    vm.runInContext(script, contexto);
    const listasDibujadas = [];
    const fichasDibujadas = [];
    contexto.actualizarJugadores = sala => listasDibujadas.push(sala.revision);
    contexto.actualizarDisponibilidadFichas = () => {
        fichasDibujadas.push(vm.runInContext("miFichaSeleccionada", contexto));
    };
    vm.runInContext('miNombreUsuario = "Bruno"; conectarWebSocket("SALA", "/ws")', contexto);
    const recibir = suscripciones.get("/topic/sala/SALA");
    function entregar(revision, fichasAsignadas) {
        recibir({ body: JSON.stringify({ revision, fichasAsignadas }) });
    }
    entregar(3, {}); // Anterior incluso a la página inicial.
    entregar(6, { Ana: "ROJO", Bruno: "AZUL" });
    entregar(5, { Ana: "ROJO" });
    entregar(6, { Ana: "ROJO", Bruno: "AZUL" });
    entregar(7, { Ana: "ROJO", Bruno: "VERDE" });

    assert.deepEqual(listasDibujadas, [6, 7]);
    assert.deepEqual(fichasDibujadas, ["AZUL", "VERDE"]);
});
