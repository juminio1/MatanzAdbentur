# Guía de PostgreSQL y Docker para el equipo

Esta guía explica qué cambió en MatanzAdbentur y cómo ejecutar el proyecto después de reemplazar MySQL por PostgreSQL. Los ejemplos de terminal usan **PowerShell en Windows**. Ejecutar los comandos desde la carpeta del repositorio, donde están `pom.xml` y `docker-compose.yml`.

## 1. ¿Qué es PostgreSQL?

PostgreSQL (también llamado Postgres) es el programa que administra nuestra base de datos: guarda usuarios, salas, partidas y propiedades en tablas relacionadas. SQL es el lenguaje para consultar y modificar esos datos. MySQL y PostgreSQL cumplen ese mismo papel, aunque tienen diferencias de configuración y de sintaxis.

En este proyecto el recorrido es:

```text
Navegador → controladores y servicios Java → repositorios → Hibernate → PostgreSQL
```

Hibernate convierte las operaciones sobre objetos Java en SQL. JDBC es el mecanismo que utiliza Java para conectarse a la base; el driver de PostgreSQL permite comunicarse con ese servidor.

PostgreSQL funciona como un servidor independiente de la aplicación. Por eso primero levantamos la base y después Java. [Referencia oficial](https://www.postgresql.org/docs/17/tutorial-arch.html).

## 2. ¿Qué cambió en el proyecto?

| Parte | Antes | Ahora |
|---|---|---|
| Motor de base de datos | MySQL | PostgreSQL 17 |
| Puerto de la base | 3306 | 5432 |
| Dependencia en `pom.xml` | Conector MySQL | PostgreSQL JDBC 42.7.10 |
| URL JDBC | `jdbc:mysql://...` | `jdbc:postgresql://...` |
| Driver en `HibernateConfig.java` | Driver MySQL | `org.postgresql.Driver` |
| Dialecto de Hibernate | MySQL | PostgreSQL |
| Servicio de Compose | `mysql` | `postgres` |
| Contenedor de base | `tallerwebi-mysql` | `tallerwebi-postgres` |
| Volumen de datos | `mysql_data` | `postgres_data` |

También se adaptaron los scripts: `INSERT IGNORE` pasó a `ON CONFLICT DO NOTHING`, se cambió la sintaxis de modificación de columnas y se reescribió un `UPDATE` con joins. La limpieza de las pruebas de punta a punta ahora se conecta por JDBC, sin invocar MySQL ni Bash.

Las pantallas, las reglas del juego y el registro de usuarios conservan su funcionamiento. Los usuarios nuevos se guardan en PostgreSQL.

**No trasladamos los registros anteriores:** eran datos de prueba. El volumen anterior de MySQL queda separado y no se elimina al iniciar PostgreSQL.

El tablero está definido en el código y las propiedades se crean por partida cuando se necesitan. Los casilleros de eventos de la pantalla no consultan actualmente un catálogo de eventos en la base. No hace falta importar estos datos desde MySQL para conservar el comportamiento actual.

## 3. ¿Para qué usamos Docker?

Docker ejecuta PostgreSQL dentro de un contenedor, sin instalar el servidor PostgreSQL directamente en Windows.

- **Imagen:** la base con la que se construye el contenedor; `DockerfileSQL` utiliza `postgres:17`.
- **Contenedor:** el proceso que ejecuta PostgreSQL.
- **Volumen:** donde quedan guardados los datos, aunque se elimine y vuelva a crear el contenedor.
- **Docker Compose:** lee `docker-compose.yml` y organiza los servicios, conexiones y volúmenes del proyecto.

Tenemos dos servicios: `postgres` (la base) y `jetty-app` (la aplicación Java). Para desarrollar se puede ejecutar solo la base en Docker y Java desde el IDE. [Introducción oficial a Compose](https://docs.docker.com/get-started/docker-concepts/the-basics/what-is-docker-compose/).

## 4. Preparación inicial

Instalar **Docker Desktop**, **JDK 25** y **Maven**. Para Docker Desktop en Windows, seguir la [guía oficial de instalación](https://docs.docker.com/desktop/setup/install/windows-install/), incluyendo los requisitos de WSL 2 o virtualización que correspondan al equipo.

Abrir Docker Desktop y esperar a que el motor esté funcionando. Después verificar:

```powershell
docker version
docker compose version
java -version
mvn -version
```

`docker version` debe mostrar información de Client y Server. Si no puede conectarse al motor, todavía no se puede levantar la base.

Comprobar que exista `.env`. Si no existe, crearlo desde el ejemplo:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Valores locales de referencia:

```dotenv
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tallerwebi
DB_USER=user
DB_PASSWORD=user
```

Son credenciales de desarrollo local. Si se usan otros valores, la aplicación y la base deben coincidir.

## 5. Opción A: base en Docker y Java en el IDE o terminal

Es la opción práctica para desarrollar y depurar el código Java.

### Paso 1: iniciar PostgreSQL

```powershell
docker compose up -d --build postgres
docker compose ps
```

La primera vez puede tardar porque descarga la imagen y crea el volumen. `-d` deja el contenedor ejecutándose en segundo plano. Esperar a que el servicio aparezca como **healthy**.

También se puede consultar su estado:

```powershell
docker compose exec postgres sh -c 'pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

El mensaje esperado incluye `accepting connections`.

### Paso 2: configurar la conexión de Java

Docker Compose lee `.env`, pero **Java y Maven no lo cargan automáticamente**. Para los valores de ejemplo, configurar en la misma terminal:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "tallerwebi"
$env:DB_USER = "user"
$env:DB_PASSWORD = "user"
```

Si `.env` tiene credenciales diferentes, usar esas mismas aquí. Estas variables duran mientras esa terminal esté abierta.

En IntelliJ se pueden agregar las mismas cinco variables en **Run/Debug Configurations → configuración que inicia la aplicación → Environment variables**. Las variables de PowerShell no se aplican a un IDE que ya estaba abierto.

### Paso 3: iniciar la aplicación

```powershell
mvn jetty:run
```

Esperar el inicio de Jetty y abrir [la aplicación local](http://localhost:8080/spring).

Al iniciar Java, Hibernate crea o actualiza las tablas. Después se ejecutan los scripts SQL de inicio. `data.sql` conserva un usuario de prueba predefinido; eso no es una importación de los usuarios viejos. Para verificar el registro, crear usuarios nuevos desde la pantalla.

Para detener Java, presionar `Ctrl+C` en su terminal. Para detener la base sin eliminar sus datos:

```powershell
docker compose stop postgres
```

Para volver a trabajar, abrir Docker Desktop, ejecutar `docker compose up -d postgres` e iniciar Java con las variables de conexión configuradas.

## 6. Opción B: aplicación y base en Docker

Elegir esta opción si se quiere ejecutar ambos servicios en contenedores. No ejecutar simultáneamente Jetty desde el IDE porque ambos usan el puerto 8080.

Primero generar el archivo WAR que utiliza `DockerfileJetty`:

```powershell
mvn clean package
docker compose up -d --build
docker compose ps
```

Compose configura automáticamente las variables de la aplicación y espera a que PostgreSQL esté saludable. Abrir [la aplicación local](http://localhost:8080/spring). Si todavía no responde, revisar:

```powershell
docker compose logs --tail 100 jetty-app
```

Después de cambiar código Java, repetir `mvn clean package` y `docker compose up -d --build` para reconstruir la aplicación.

Para detener y quitar los contenedores y la red del proyecto:

```powershell
docker compose down
```

El volumen conserva los datos. **Agregar `-v` a `down` elimina los volúmenes y los datos de esta base**; no es necesario para detener el proyecto. [Persistencia de volúmenes en Compose](https://docs.docker.com/compose/gettingstarted/).

## 7. ¿Por qué a veces el host es localhost y otras postgres?

| Dónde corre Java | Host de la base | Puerto |
|---|---|---|
| En Windows, mediante Maven o el IDE | `localhost` | `5432` |
| En el contenedor `jetty-app` | `postgres` | `5432` |

Desde Windows se usa el puerto publicado por Docker. Dentro de la red de Compose se usa el nombre del servicio. Dentro del contenedor de Java, `localhost` apuntaría al propio contenedor de Java.

## 8. Comprobar que todo funciona

1. Registrar usuarios nuevos e iniciar sesión.
2. Crear una sala, entrar desde otra sesión de navegador con otro usuario e iniciar una partida.
3. Comprobar movimiento y compra de propiedades.
4. Detener y volver a iniciar la aplicación y la base, sin borrar el volumen.
5. Volver a iniciar sesión con los usuarios creados: deben seguir guardados. La sesión web puede requerir un nuevo login después del reinicio.

Para inspeccionar las tablas sin instalar herramientas adicionales:

```powershell
docker compose exec postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Dentro de `psql`:

```text
\dt
SELECT id, email, username FROM usuario;
\q
```

Las tablas de la aplicación aparecen después de iniciar Java, no solamente al levantar PostgreSQL. También se puede conectar un cliente gráfico como pgAdmin usando host `localhost`, puerto `5432` y los valores de `.env`; su instalación es opcional.

## 9. Pruebas automáticas

```powershell
mvn test
```

La suite habitual conserva HSQLDB en memoria para las pruebas de integración e infraestructura. Pasarla no demuestra por sí solo que PostgreSQL funcione.

La prueba `PostgreSQLTest` usa la configuración real, ejecuta el inicio dos veces y verifica escritura, lectura y generación de IDs. Solo se ejecuta con `RUN_POSTGRES_TESTS=true`. En CI está configurado un PostgreSQL aislado para esta prueba.

Para ejecutarla localmente, crear una base exclusiva de pruebas, por ejemplo `tallerwebi_test` (hacerlo solo la primera vez):

```powershell
docker compose exec postgres sh -c 'createdb -U "$POSTGRES_USER" tallerwebi_test'
```

En una terminal separada de la aplicación, configurar la conexión de pruebas:

```powershell
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:DB_NAME = "tallerwebi_test"
$env:DB_USER = "user"
$env:DB_PASSWORD = "user"
$env:RUN_POSTGRES_TESTS = "true"
mvn '-Dtest=PostgreSQLTest' test
```

Adaptar usuario y contraseña si se cambiaron. Al terminar, cerrar esa terminal para no iniciar después la aplicación con la base de pruebas. Esta prueba actualiza el esquema e inserta el usuario de inicio; sus usuarios temporales se revierten. Las pruebas de punta a punta tienen una limpieza de usuarios propia y también deben usar una base exclusiva de pruebas.

Validación completada: la suite habitual pasó 238 pruebas y la prueba específica contra PostgreSQL real pasó sin errores. También se verificaron manualmente el registro de usuarios, la conservación de los usuarios después de reiniciar la aplicación, la creación de una sala y una partida con jugadores.

## 10. Problemas frecuentes

| Mensaje o problema | Qué revisar |
|---|---|
| No puede conectarse al Docker daemon, error de pipe o motor | Abrir Docker Desktop y esperar a que inicie el motor. |
| Puerto 5432 ocupado | Puede existir otro PostgreSQL local u otro contenedor. Detener el servicio que ocupa el puerto antes de usar esta configuración. |
| Puerto 8080 ocupado | Detener el Jetty anterior o la aplicación del IDE antes de iniciar el contenedor de Java. |
| `FATAL: invalid value for parameter "TimeZone": "America/Buenos_Aires"` | Actualizar el código y reiniciar Jetty. La configuración convierte ese identificador al nombre compatible `America/Argentina/Buenos_Aires`. |
| `Connection refused` | Verificar `docker compose ps`, el estado healthy y host/puerto de Java. |
| `UnknownHostException: postgres` desde el IDE | Usar `DB_HOST=localhost` al ejecutar Java fuera de Docker. |
| `password authentication failed` | Hacer coincidir usuario y contraseña. Cambiar `.env` no modifica automáticamente las credenciales de una base que ya tiene un volumen inicializado. |
| `database ... does not exist` | Confirmar `DB_NAME`. Cambiar esa variable después de crear el volumen no crea otra base automáticamente. |
| No aparecen tablas | Iniciar Java y revisar sus errores: Hibernate crea las tablas al iniciar la aplicación. |
| No aparece el usuario viejo | Es esperado: no se importaron los usuarios de MySQL. Registrar uno nuevo. |
| No encuentra el WAR al construir `jetty-app` | Ejecutar `mvn clean package` antes de `docker compose up -d --build`. |

Comandos para consultar el estado y los mensajes del servidor:

```powershell
docker compose ps
docker compose logs --tail 100 postgres
docker compose logs --tail 100 jetty-app
```

Hibernate utiliza `hbm2ddl.auto=update`: ayuda a crear o actualizar las tablas durante el desarrollo, pero no garantiza resolver cualquier cambio futuro de esquema. Los scripts de `migraciones` actualizan datos o columnas existentes; no trasladan automáticamente una base MySQL a PostgreSQL.
