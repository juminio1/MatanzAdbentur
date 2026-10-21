# Organización de las tres capas

Las carpetas agrupan clases por responsabilidad. Una carpeta auxiliar no agrega una capa nueva.

| Capa | Ubicación | Responsabilidad |
| --- | --- | --- |
| Presentación | `presentacion/Controlador*.java` | Recibir peticiones y devolver vistas o respuestas. |
| Presentación | `presentacion/sesion/ResolutorJugadorSesion.java` | Leer la sesión HTTP e identificar al jugador de la partida. |
| Presentación | `presentacion/coordinadores/CoordinadorTurnoPartida.java` | Consultar los servicios y armar el DTO del turno para los controladores. |
| Dominio | `dominio/servicios/ServicioPartida*.java` | Coordinar las operaciones de una partida. |
| Dominio | `dominio/servicios/ServicioInicioPartida*.java` | Validar el inicio y preparar jugadores y estado inicial. |
| Dominio | `dominio/servicios/ServicioTurno*.java` | Resolver orden, penalizaciones y rondas. |
| Infraestructura | `infraestructura/Repositorio*.java` | Consultar y guardar datos. |
| Infraestructura | `infraestructura/temporizadores/GestorTemporizadorPartida.java` | Programar y cancelar tareas; ejecutar la acción de vencimiento dentro de una transacción. |

El resolutor queda en presentación porque conoce `HttpServletRequest` y la sesión del navegador. El coordinador también pertenece a presentación: prepara la respuesta web y delega las reglas a los servicios.

El temporizador no decide quién juega: llama a la acción recibida cuando vence el plazo. Por eso se ubica con los mecanismos técnicos de infraestructura. Sus pruebas están en `src/test/java/com/tallerwebi/infraestructura/temporizadores`.
