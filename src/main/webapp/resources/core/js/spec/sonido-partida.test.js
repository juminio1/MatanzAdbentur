import { aplicarSilencio, guardarSilencio, leerSilencio } from "../sonido-partida.js";

function crearAlmacen(inicial = {}) {
    const datos = { ...inicial };
    return {
        getItem: (clave) => (clave in datos ? datos[clave] : null),
        setItem: (clave, valor) => { datos[clave] = String(valor); },
    };
}

function crearBoton() {
    const atributos = {};
    return {
        title: "",
        setAttribute: (nombre, valor) => { atributos[nombre] = valor; },
        getAttribute: (nombre) => atributos[nombre],
    };
}

describe("Botón de sonido de la partida", () => {
    it("dadoQueNoHayPreferenciaGuardadaCuandoSeLeeDaComoResultadoQueSuena", () => {
        expect(leerSilencio(crearAlmacen())).toBeFalse();
    });

    it("dadoQueSeSilencioCuandoSeVuelveALeerDaComoResultadoQueSigueSilenciado", () => {
        const almacen = crearAlmacen();
        guardarSilencio(almacen, true);
        expect(leerSilencio(almacen)).toBeTrue();
        guardarSilencio(almacen, false);
        expect(leerSilencio(almacen)).toBeFalse();
    });

    it("dadoQueElNavegadorBloqueaElAlmacenamientoCuandoSeLeeDaComoResultadoQueSuenaSinErrores", () => {
        const bloqueado = {
            getItem: () => { throw new Error("bloqueado"); },
            setItem: () => { throw new Error("bloqueado"); },
        };
        expect(leerSilencio(bloqueado)).toBeFalse();
        expect(() => guardarSilencio(bloqueado, true)).not.toThrow();
        expect(leerSilencio(null)).toBeFalse();
    });

    it("dadoQueSeSilenciaCuandoSeAplicaDaComoResultadoHowlerMuteadoYElBotonAvisandolo", () => {
        const howler = jasmine.createSpyObj("Howler", ["mute"]);
        const boton = crearBoton();

        aplicarSilencio(true, howler, boton);

        expect(howler.mute).toHaveBeenCalledWith(true);
        expect(boton.getAttribute("aria-pressed")).toBe("true");
        expect(boton.getAttribute("aria-label")).toBe("Activar sonido");
        expect(boton.title).toBe("Activar sonido");
    });

    it("dadoQueSeReactivaCuandoSeAplicaDaComoResultadoHowlerSonandoYElBotonOfreciendoSilenciar", () => {
        const howler = jasmine.createSpyObj("Howler", ["mute"]);
        const boton = crearBoton();

        aplicarSilencio(false, howler, boton);

        expect(howler.mute).toHaveBeenCalledWith(false);
        expect(boton.getAttribute("aria-pressed")).toBe("false");
        expect(boton.getAttribute("aria-label")).toBe("Silenciar sonido");
    });

    it("dadoQueHowlerNoCargoCuandoSeAplicaDaComoResultadoQueNoFalla", () => {
        expect(() => aplicarSilencio(true, undefined, crearBoton())).not.toThrow();
    });
});
