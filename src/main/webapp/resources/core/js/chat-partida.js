export function combinarMensajes(actuales, nuevos) {
    const mensajes = new Map(actuales.map(m => [String(m.id), m]));
    nuevos.forEach(m => { if (m?.id != null) mensajes.set(String(m.id), m); });
    return [...mensajes.values()].sort((a, b) => Number(a.id) - Number(b.id)).slice(-100);
}

export function inicializarChat({ basePath, codigo, usuarioId, notificar, alCambiarVisibilidad }) {
    const el = id => document.getElementById(id);
    const telefono = el("chat-telefono");
    const lista = el("chat-mensajes");
    const campo = el("chat-texto");
    const enviar = el("chat-enviar");
    const estado = el("chat-estado");
    const ruta = `${basePath}/partida/${encodeURIComponent(codigo)}/chat`;
    let mensajes = [];
    let noLeidos = 0;
    let cargado = false;
    let consultando = false;
    let enviando = false;

    function colorearJugador(elemento, id) {
        const jugador = document.getElementById(String(id));
        const color = jugador ? getComputedStyle(jugador).getPropertyValue("--color-ficha").trim() : "";
        elemento.style.setProperty("--chat-color", color || "#aac1cf");
    }

    function avatar(id, apodo) {
        const contenedor = document.createElement("span");
        contenedor.className = "chat-avatar";
        const original = document.getElementById(String(id))?.querySelector(".avatar-jugador");
        if (original?.src) {
            const imagen = document.createElement("img");
            imagen.src = original.src;
            imagen.alt = "";
            contenedor.append(imagen);
        } else contenedor.textContent = (apodo || "?").trim().slice(0, 1).toUpperCase();
        return contenedor;
    }

    function actualizarContador() {
        document.querySelectorAll("[data-chat-contador]").forEach(contador => {
            contador.textContent = noLeidos > 99 ? "99+" : String(noLeidos);
            contador.hidden = noLeidos === 0;
        });
        document.querySelectorAll("[data-abrir-chat]").forEach(boton => {
            boton.setAttribute("aria-expanded", String(telefono.open));
            boton.setAttribute("aria-label", noLeidos ? `Abrir chat, ${noLeidos} ${noLeidos === 1 ? "mensaje nuevo" : "mensajes nuevos"}` : "Abrir chat de la partida");
        });
    }

    function recibir(nuevos, avisar = true) {
        const conocidos = new Set(mensajes.map(m => String(m.id)));
        const combinados = combinarMensajes(mensajes, nuevos);
        const agregados = combinados.filter(m => !conocidos.has(String(m.id)));
        if (!agregados.length) return;
        mensajes = combinados;
        const entrantes = agregados.filter(m => String(m.idJugador) !== String(usuarioId)).length;
        if (avisar && entrantes) {
            if (!telefono.open || document.hidden) noLeidos += entrantes;
            notificar?.();
            if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
                document.querySelectorAll("[data-abrir-chat]").forEach(boton => {
                    boton.animate([
                        { transform: "translateX(0)" }, { transform: "translateX(-4px) rotate(-5deg)" },
                        { transform: "translateX(4px) rotate(5deg)" }, { transform: "translateX(-3px) rotate(-3deg)" },
                        { transform: "translateX(3px) rotate(3deg)" }, { transform: "translateX(0)" },
                    ], { duration: 380 });
                });
            }
        }
        const alFinal = lista.scrollHeight - lista.scrollTop - lista.clientHeight < 60;
        lista.replaceChildren(...mensajes.map(mensaje => {
            const fila = document.createElement("li");
            fila.className = "chat-mensaje";
            colorearJugador(fila, mensaje.idJugador);
            if (String(mensaje.idJugador) === String(usuarioId)) fila.classList.add("chat-mensaje--propio");
            const autor = document.createElement("strong");
            autor.textContent = mensaje.apodo;
            const texto = document.createElement("p");
            texto.textContent = mensaje.texto;
            const hora = document.createElement("time");
            const fecha = new Date(mensaje.fecha);
            hora.dateTime = fecha.toISOString();
            hora.textContent = fecha.toLocaleTimeString("es-AR", { hour: "2-digit", minute: "2-digit" });
            const burbuja = document.createElement("div");
            burbuja.className = "chat-burbuja";
            const encabezado = document.createElement("div");
            encabezado.className = "chat-meta";
            encabezado.append(autor, hora);
            burbuja.append(encabezado, texto);
            fila.append(avatar(mensaje.idJugador, mensaje.apodo), burbuja);
            return fila;
        }));
        if (alFinal || agregados.some(m => String(m.idJugador) === String(usuarioId))) lista.scrollTop = lista.scrollHeight;
        actualizarContador();
    }

    async function json(opciones = {}) {
        const respuesta = await fetch(ruta, { credentials: "same-origin", cache: "no-store", ...opciones });
        if (!respuesta.ok) {
            const error = await respuesta.json().catch(() => ({}));
            throw new Error(error.error || "No se pudo conectar con el chat. Volvé a intentar.");
        }
        return respuesta.json();
    }

    async function revisar() {
        if (consultando || !codigo) return;
        consultando = true;
        try {
            recibir(await json(), cargado);
            cargado = true;
            if (!enviando) estado.textContent = "";
        } catch (error) { if (!enviando) estado.textContent = error.message; }
        finally { consultando = false; }
    }

    function abrir() {
        if (!telefono.open) telefono.showModal();
        alCambiarVisibilidad?.();
        noLeidos = 0;
        actualizarContador();
        lista.scrollTop = lista.scrollHeight;
        campo.focus();
        revisar();
    }

    function cerrar() { telefono.close(); }
    document.querySelectorAll("[data-abrir-chat]").forEach(boton => boton.addEventListener("click", abrir));
    el("chat-guardar").addEventListener("click", cerrar);
    telefono.addEventListener("close", () => {
        actualizarContador();
        alCambiarVisibilidad?.();
    });
    telefono.addEventListener("click", evento => { if (evento.target === telefono) cerrar(); });
    el("chat-formulario").addEventListener("submit", async evento => {
        evento.preventDefault();
        const texto = campo.value.trim();
        if (enviando || !texto || texto.length > 500) return;
        enviando = true;
        enviar.disabled = true;
        estado.textContent = "";
        try {
            recibir([await json({ method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ texto }) })]);
            campo.value = "";
            estado.textContent = "";
        } catch (error) { estado.textContent = error.message; }
        finally { enviando = false; enviar.disabled = false; campo.focus(); }
    });
    campo.addEventListener("keydown", evento => {
        if (evento.key === "Enter" && !evento.shiftKey && !evento.isComposing) {
            evento.preventDefault();
            el("chat-formulario").requestSubmit();
        }
    });
    document.addEventListener("visibilitychange", () => {
        if (!document.hidden && telefono.open) { noLeidos = 0; actualizarContador(); }
    });
    const consulta = window.setInterval(revisar, 5000);
    window.addEventListener("pagehide", () => clearInterval(consulta));
    queueMicrotask(revisar);
    return { recibir: mensaje => recibir([mensaje]), revisar };
}
