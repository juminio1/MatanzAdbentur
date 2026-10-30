# Arquitectura de MatanzAdbentur

El proyecto conserva las tres capas del curso: presentación, dominio e infraestructura.
Las entidades contienen datos, relaciones JPA, constructores y accesores. Las reglas se
implementan en servicios de dominio; no se agregó una capa de aplicación.

## Responsabilidades

| Componente | Responsabilidad |
| --- | --- |
| Controladores | Recibir parámetros y sesión, traducir errores y devolver respuestas. |
| ControladorPartida | Tablero, turnos, tirada de dados y abandono. |
| ControladorPropiedadesPartida | Consulta, compra, venta al banco y mejora. |
| ControladorOfertaPropiedad | Creación, consulta y respuesta de ofertas entre jugadores. |
| MapeadorMovimiento / MapeadorPropiedad | Construir la representación para el cliente. |
| ServicioJugada | Coordinar la tirada, movimiento, casillero, deuda y continuación del turno en una transacción. |
| ServicioPartida | Ciclo de vida, consultas de partida y coordinación del cambio de turno. |
| ServicioInicioPartida | Preparar participantes y estado inicial. |
| ServicioTurno | Orden, rondas y elección del siguiente jugador. |
| ServicioMovimiento | Posiciones y premio por cruzar inicio. |
| ServicioEconomia | Validar importes y consultar, acreditar o debitar saldo. |
| ServicioPropiedad | Compra, venta, transferencia, mejoras y cálculo/pago de alquiler. |
| ServicioOfertaPropiedad | Ofertas de venta entre jugadores: creación, vencimiento (5 min) y respuesta. Las ofertas se guardan en memoria y se avisan por WebSocket en `/topic/partida/{codigo}/ofertas`. |
| ServicioGestionQuiebra | Deudas pendientes y declaración de quiebra. |
| ServicioEventoCasillero | Selección y aplicación de efectos. |
| ServicioParticipacion | Participación y condición activa del jugador. |
| CatalogoTablero | Posiciones, grupos y valores iniciales del tablero. |
| Infraestructura | Persistencia, temporizadores y ejecución de efectos después del commit. |

## Flujo de una tirada

```text
ControladorPartida
  -> ServicioJugada (transacción)
       -> ServicioPartida: cargar partida y consultar turno
       -> ServicioDado: generar tirada
       -> ServicioMovimiento -> ServicioEconomia
       -> ServicioEventoCasillero -> movimiento/economía/quiebra
       -> ServicioPropiedad / ServicioGestionQuiebra
       -> ServicioPartida: persistir y avanzar turno cuando corresponda
  -> MapeadorMovimiento
  -> respuesta HTTP / notificación WebSocket
```

Propiedades consulta directamente su repositorio y ServicioTurno. Eventos usa movimiento
y economía. Ninguno depende de ServicioPartida para operaciones de saldo, evitando el
acoplamiento de vuelta hacia el coordinador. Los resultados de dados, posiciones y jugadas
pertenecen al dominio y no importan clases de presentación.

## Dinero, transacciones y notificaciones

ServicioEconomia no declara una quiebra por alcanzar un saldo arbitrario. Los pagos que
no pueden cubrirse se resuelven mediante deuda y ServicioGestionQuiebra, que publica los
eventos correspondientes. Así se elimina la segunda regla de quiebra que antes existía en
ServicioPartida al llegar a -2000. Economía no persiste: el servicio que coordina la operación
guarda los cambios dentro de su transacción.

El premio por inicio se calcula en ServicioMovimiento, también para eventos.
ServicioEventoCasillero devuelve el premio acreditado en ResultadoEvento; ServicioJugada
lo acumula sin recalcularlo. El precio de venta al banco se calcula en ServicioPropiedad. El DTO no
acredita dinero ni decide el premio; sólo conserva el cálculo de duración de la animación.

ServicioJugada revierte todos sus cambios cuando falla un paso. Los cambios de temporizador
originados al avanzar turno y los avisos de turno/movimiento se ejecutan después del commit.
Los observadores que cambian el estado del juego se ejecutan dentro de la transacción;
la notificación de quiebra conserva su ejecución posterior a la confirmación.

## Criterios para próximas modificaciones

Las acciones de compra, tirada y avance manual identifican al jugador usando el nombre
autenticado de la sesión, que se copia al apodo al crear los participantes. No comparan
el identificador de Usuario con el de Jugador. En la tirada se comprueba además que el
identificador recibido corresponda a ese participante.

Jugador conserva el dato persistido `tiroEnTurno`, sin reglas en la entidad.
ServicioJugada comprueba y marca la tirada dentro de su transacción; ServicioPartida
lo restablece al comenzar el siguiente turno y ServicioTurno al inicializar participantes.
Este control impide repetir solicitudes sucesivas; la protección frente a operaciones
simultáneas queda pendiente de la revisión de concurrencia.

- Una regla compartida tiene un responsable único.
- Usar validaciones y salidas tempranas para evitar anidamiento.
- Mantener los efectos de eventos en el mapa de aplicadores mientras sean simples.
- Agregar estrategias separadas sólo si un efecto adquiere reglas propias complejas.
- Evitar servicios que sólo envuelvan cada getter o setter.
- Probar comportamiento: saldos, pertenencia a la partida, turnos, deuda y rollback.

Validación del proyecto: `mvn test`. Incluye los controles de calidad configurados en el POM.
Los tests de integración de efectos usan Hibernate/HSQLDB y verifican persistencia y rollback.
