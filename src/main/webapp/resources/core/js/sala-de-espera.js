function conectarWebSocket(codigoSala) {

    const socket = new SockJS("/ws"); 

    const stompClient = Stomp.over(socket); 

    stompClient.connect({}, function () {

        console.log("Conectado al WebSocket");

        stompClient.subscribe("/topic/sala/" + codigoSala, function (mensaje) { 

            const salaActualizada = JSON.parse(mensaje.body);

            console.log("Sala actualizada:", salaActualizada);

        });

    });
   document.addEventListener("DOMContentLoaded", function () {

    const datosSala = document.getElementById("datos-sala");

    const codigoSala = datosSala.dataset.codigo;

    conectarWebSocket(codigoSala);

});
}