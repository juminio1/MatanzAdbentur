document.addEventListener("DOMContentLoaded", () => {
    // ==========================================
    // CÓDIGO DE TU COMPAÑERO: DADOS Y TABLERO
    // ==========================================
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

    if (botonTirar) {
        botonTirar.addEventListener("click", async () => {
            try {
                const resultado = await obtenerResultadoDados();
                console.log("Resultado completo:", resultado);
                if (dado1) dado1.textContent = resultado.dado1;
                if (dado2) dado2.textContent = resultado.dado2;
            } catch (error) {
                console.error("Error al tirar los dados:", error);
            }
        });
    }

    // Busca todos los elementos del html que tenga la clase casillero
    const casilleros = document.querySelectorAll(".casillero");
    console.log("Casilleros encontrados:", casilleros.length);
    if (casilleros.length > 0) {
        const ficha = document.createElement("div");
        ficha.classList.add("ficha");
        casilleros[0].appendChild(ficha);
    }

    // ==========================================
    // GESTIÓN DE TURNOS: SINCRONIZACIÓN WEBSOCKET
    // ==========================================
    const basePath = window.location.pathname.startsWith("/spring") ? "/spring" : "";
    const urlParams = new URLSearchParams(window.location.search);
    const codigoPartida = urlParams.get("codigo") || window.codigoSala || "";

    conectarWebSocketTurnos(basePath, codigoPartida);

    function conectarWebSocketTurnos(base, codigo) {
        if (!codigo || typeof SockJS === "undefined" || typeof Stomp === "undefined") {
            return;
        }

        const socket = new SockJS(`${base}/ws`);
        const stompClient = Stomp.over(socket);
        stompClient.debug = null;

        stompClient.connect({}, () => {
            stompClient.subscribe(`/topic/partida/${codigo}/turno`, (mensaje) => {
                const turnoDTO = JSON.parse(mensaje.body);
                actualizarTurnoEnPantalla(turnoDTO);
            });
        });
    }

    function actualizarTurnoEnPantalla(turnoDTO) {
        const idJugadorActual = String(turnoDTO.idJugadorActual);
        const usuarioSesionId = String(window.usuarioSesionId);

        // Limpiar indicadores de turno previos en las tarjetas
        document.querySelectorAll(".contenedor-turno").forEach((contenedor) => {
            contenedor.innerHTML = "";
        });

        // Insertar cartel en el jugador activo
        const cardJugador = document.getElementById(idJugadorActual);
        if (cardJugador) {
            const contenedorTurno = cardJugador.querySelector(".contenedor-turno");
            if (contenedorTurno) {
                const span = document.createElement("span");
                span.className = "indicador-turno";
                span.textContent = (idJugadorActual === usuarioSesionId)
                    ? "Tu turno"
                    : `Turno de ${turnoDTO.apodoJugadorActual}`;
                contenedorTurno.appendChild(span);
            }
        }

        // Bloquear o habilitar el botón de dados según si es el turno del cliente actual
        if (botonTirar && window.usuarioSesionId !== undefined && window.usuarioSesionId !== null) {
            botonTirar.disabled = (usuarioSesionId !== idJugadorActual);
        }
    }
    //tarea propiedad

    //elementos del panel lateral
    const infoTitulo = document.getElementById("info-titulo");
    const infoPrecio = document.getElementById("info-precio");
    const infoAlquiler = document.getElementById("info-alquiler");
    const infoEstado = document.getElementById("info-estado");
    const btnComprar = document.getElementById("btn-comprar");

    let dineroJugadorActual = 1500; //plata inicial simulada
    const propiedadesJugador = []; //inventario vacío

    //simulacion de respuesta cuando se cae en una propiedad
    // Tu variable que ya tenés
        const propiedadSimulada = {
            nombre: "Avenida Illia",
            precio: 300,
            alquiler: 50,
            tieneDuenio: false,
            esPropiedad: true
        };
        window.propiedadSimulada = propiedadSimulada; // <--- AGREGÁ ESTA LÍNEA

        //Tu función que ya tenés
        function mostrarInfoCasillero(casilleroData) {
           //...(acá adentro está todo tu código que cambia los textos)
        }
        window.mostrarInfoCasillero = mostrarInfoCasillero; // <--- AGREGÁ ESTA LÍNEA

    //esta función se llamará cuando la ficha termine de moverse
    function mostrarInfoCasillero(casilleroData) {
        infoTitulo.textContent = casilleroData.nombre;

        if (casilleroData.esPropiedad) {
            infoPrecio.textContent = "$" + casilleroData.precio;
            infoAlquiler.textContent = "$" + casilleroData.alquiler;

            if (!casilleroData.tieneDuenio) {
                infoEstado.textContent = "Disponible";
                btnComprar.textContent = "Comprar por $" + casilleroData.precio;

                //valida si le alcanza la guita
                if (dineroJugadorActual >= casilleroData.precio) {
                    btnComprar.disabled = false;
                } else {
                    btnComprar.disabled = true;
                    btnComprar.textContent = "Dinero insuficiente";
                }
            } else {
                infoEstado.textContent = "Tiene dueño";
                btnComprar.disabled = true;
                btnComprar.textContent = "Pagar Alquiler";
            }
        } else {

            infoPrecio.textContent = "No disponible";
            infoAlquiler.textContent = "$0";
            infoEstado.textContent = "Lugar público";
            btnComprar.disabled = true;
            btnComprar.textContent = "No adquirible";
        }
    }

    //accion para comprar
    btnComprar.addEventListener("click", () => {
        //descontar
        dineroJugadorActual -= propiedadSimulada.precio;

        //busca el span del dinero en el header y lo actualiza visualmente
        const spanDinero = document.querySelector(".datos-jugador .dinero");
        spanDinero.textContent = "$" + dineroJugadorActual;

        //cambia el estado de la propiedad
        propiedadSimulada.tieneDuenio = true;
        propiedadesJugador.push(propiedadSimulada.nombre);

        //acualiza el panel lateral
        mostrarInfoCasillero(propiedadSimulada);

        //agregacion de historial
        const historial = document.querySelector(".historial__lista");
        const nuevoLi = document.createElement("li");
        nuevoLi.innerHTML = `Compraste: <strong>${propiedadSimulada.nombre}</strong>`;
        historial.appendChild(nuevoLi);
    });

   //conectamos la visualización al botón de tirar dados temporalmente
       if (botonTirar) {
           botonTirar.addEventListener("click", async () => {
               //caudno se tiran los dados aparece la info de la propiedad
               setTimeout(() => {
                   mostrarInfoCasillero(propiedadSimulada);
               }, 1000);
           });
       }
});