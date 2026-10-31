-- BORRADOR PARA REVISION. NO EJECUTADO. PostgreSQL 17, esquema public.
-- No esta registrado en DatabaseInitializationConfig ni se carga al iniciar Java.
-- Requiere aplicacion detenida, respaldo y revision del inventario del esquema real.
-- Primero revisar SOLO el inventario (hasta el segundo DO), terminando con ROLLBACK.
-- Luego completar v_globales_esperados con el conteo aprobado del inventario.
-- Por defecto termina en ROLLBACK: sustituirlo por COMMIT requiere otra autorizacion.
-- No iniciar la version Java nueva antes de aprobar y aplicar esta migracion.
-- No reutilizar este script si Hibernate ya creo eventoglobal: se detiene para revision.

BEGIN;
SET LOCAL lock_timeout = '5s';
SET LOCAL search_path = public, pg_catalog;

-- INVENTARIO: guardar estos resultados para revisar el esquema y fijar el conteo esperado.
SELECT table_name, column_name, udt_name, character_maximum_length, is_nullable,
       is_identity, identity_generation, column_default
FROM information_schema.columns
WHERE table_schema = 'public'
    AND table_name IN ('evento', 'eventocasillero', 'eventoglobalregistro')
ORDER BY table_name, ordinal_position;

SELECT restriccion.conrelid::regclass AS tabla, restriccion.conname,
       restriccion.contype, restriccion.convalidated,
       pg_get_constraintdef(restriccion.oid) AS definicion,
       EXISTS (
           SELECT 1 FROM pg_catalog.pg_attribute atributo
           WHERE atributo.attrelid = restriccion.conrelid AND atributo.attname = 'dtype'
               AND atributo.attnum = ANY(restriccion.conkey)
       ) AS afecta_dtype
FROM pg_catalog.pg_constraint restriccion
WHERE restriccion.conrelid IN (to_regclass('public.evento'),
    to_regclass('public.eventocasillero'), to_regclass('public.eventoglobalregistro'))
    OR restriccion.confrelid = to_regclass('public.eventoglobalregistro')
ORDER BY tabla, restriccion.conname;

DO $esquema$
DECLARE
    v_tabla text;
BEGIN
    FOREACH v_tabla IN ARRAY ARRAY['evento', 'eventocasillero', 'eventoglobalregistro'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_catalog.pg_class tabla
            JOIN pg_catalog.pg_namespace esquema ON esquema.oid = tabla.relnamespace
            WHERE esquema.nspname = 'public' AND tabla.relname = v_tabla
                AND tabla.relkind = 'r' AND NOT tabla.relispartition
                AND NOT tabla.relrowsecurity
        ) THEN
            RAISE EXCEPTION 'Tabla public.% ausente o con estructura no contemplada', v_tabla;
        END IF;
    END LOOP;

    IF to_regclass('public.eventoglobal') IS NOT NULL
            OR to_regclass('public.migracion_eventos_globales') IS NOT NULL THEN
        RAISE EXCEPTION 'El destino o el mapa ya existen. Revisar; no se sobrescriben';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (VALUES
            ('evento', 'id', ARRAY['int4']),
            ('evento', 'nombre', ARRAY['varchar', 'text']),
            ('evento', 'descripcion', ARRAY['varchar', 'text']),
            ('eventocasillero', 'id', ARRAY['int4']),
            ('eventoglobalregistro', 'id', ARRAY['int8']),
            ('eventoglobalregistro', 'dtype', ARRAY['varchar', 'text']),
            ('eventoglobalregistro', 'descripcion', ARRAY['varchar', 'text']),
            ('eventoglobalregistro', 'tipoefecto', ARRAY['varchar', 'text']),
            ('eventoglobalregistro', 'valor', ARRAY['int4']),
            ('eventoglobalregistro', 'tipoeventoglobal', ARRAY['varchar', 'text'])
        ) esperado(tabla, columna, tipos)
        LEFT JOIN information_schema.columns real
            ON real.table_schema = 'public' AND real.table_name = esperado.tabla
                AND real.column_name = esperado.columna
        WHERE real.column_name IS NULL OR NOT (real.udt_name::text = ANY(esperado.tipos))
    ) THEN
        RAISE EXCEPTION 'Columnas o tipos distintos de los esperados; revisar esquema real';
    END IF;

    -- JOINED sin discriminador en la raiz. No eliminar dtype ni sus CHECK a ciegas.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'evento' AND column_name = 'dtype'
    ) THEN
        RAISE EXCEPTION 'evento tiene dtype: revisar columna y restricciones antes de continuar';
    END IF;
END;
$esquema$;

LOCK TABLE public.evento, public.eventocasillero, public.eventoglobalregistro
    IN ACCESS EXCLUSIVE MODE;

SELECT dtype, tipoeventoglobal, count(*) AS cantidad,
       max(char_length(descripcion)) AS longitud_maxima_descripcion,
       min(id) AS id_minimo, max(id) AS id_maximo
FROM public.eventoglobalregistro
GROUP BY dtype, tipoeventoglobal ORDER BY dtype, tipoeventoglobal;

SELECT count(*) AS globales_esperados_para_revision
FROM public.eventoglobalregistro WHERE dtype = 'EventoGlobal';

-- Migrar todas las filas globales, incluso duplicadas. Las demas quedan archivadas
-- en eventoglobalregistro con todos sus datos; no se convierten en eventos globales.
DO $migracion$
DECLARE
    v_globales_esperados bigint := NULL; -- COMPLETAR con el conteo revisado, NO asumir seis.
    v_globales bigint;
    v_total_origen bigint;
    v_raices bigint;
    v_secuencia regclass;
    v_ultimo bigint;
    v_base bigint;
    v_limite bigint;
    v_longitud_descripcion integer;
    v_longitud_nombre integer;
    v_tabla text;
BEGIN
    SELECT count(*), count(*) FILTER (WHERE dtype = 'EventoGlobal')
        INTO v_total_origen, v_globales FROM public.eventoglobalregistro;
    SELECT count(*) INTO v_raices FROM public.evento;

    IF v_globales_esperados IS NULL OR v_globales_esperados <> v_globales THEN
        RAISE EXCEPTION 'Conteo sin aprobar o distinto: hay % globales; esperado %',
            v_globales, v_globales_esperados;
    END IF;

    -- PK de una sola columna en origen y raiz; nombre conserva su unicidad existente.
    FOREACH v_tabla IN ARRAY ARRAY['evento', 'eventoglobalregistro'] LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_catalog.pg_constraint clave
            JOIN pg_catalog.pg_attribute atributo ON atributo.attrelid = clave.conrelid
                AND atributo.attname = 'id'
            WHERE clave.conrelid = to_regclass('public.' || v_tabla)
                AND clave.contype = 'p' AND clave.conkey = ARRAY[atributo.attnum]
        ) THEN
            RAISE EXCEPTION 'PK inesperada en %', v_tabla;
        END IF;
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_index indice
        JOIN pg_catalog.pg_attribute atributo ON atributo.attrelid = indice.indrelid
            AND atributo.attname = 'nombre'
        WHERE indice.indrelid = 'public.evento'::regclass AND indice.indisunique
            AND indice.indisvalid AND indice.indpred IS NULL AND indice.indexprs IS NULL
            AND indice.indnkeyatts = 1 AND indice.indkey[0] = atributo.attnum
    ) THEN
        RAISE EXCEPTION 'Falta unicidad simple de evento.nombre; revisar sin alterarla';
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_trigger
        WHERE tgrelid IN ('public.evento'::regclass, 'public.eventoglobalregistro'::regclass)
            AND NOT tgisinternal
    ) OR EXISTS (
        SELECT 1 FROM pg_catalog.pg_constraint
        WHERE confrelid = 'public.eventoglobalregistro'::regclass
    ) THEN
        RAISE EXCEPTION 'Hay triggers propios o FK hacia el origen que requieren revision';
    END IF;

    IF EXISTS (
        SELECT 1 FROM pg_catalog.pg_constraint
        WHERE conrelid IN ('public.evento'::regclass, 'public.eventoglobalregistro'::regclass)
            AND NOT convalidated
    ) THEN
        RAISE EXCEPTION 'Hay restricciones sin validar; revisar antes de migrar';
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'evento'
            AND column_name NOT IN ('id', 'nombre', 'descripcion')
            AND is_nullable = 'NO' AND column_default IS NULL AND is_generated = 'NEVER'
    ) THEN
        RAISE EXCEPTION 'La raiz tiene columnas obligatorias adicionales';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.eventoglobalregistro
        WHERE dtype IS NULL OR dtype NOT IN ('EventoGlobal', 'EventoGlobalBase')
    ) THEN
        RAISE EXCEPTION 'Valores dtype desconocidos; revisar datos y CHECK, sin borrarlos';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.eventocasillero casillero
        LEFT JOIN public.evento raiz ON raiz.id = casillero.id
        WHERE raiz.id IS NULL
    ) THEN
        RAISE EXCEPTION 'Hay eventos por casillero sin raiz; no se ocupan sus identificadores';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.eventoglobalregistro WHERE dtype = 'EventoGlobal'
            AND (tipoeventoglobal IS NULL OR tipoeventoglobal NOT IN
                ('INUNDACION', 'ROBO', 'TRAMITES_AFIP', 'PIQUETE', 'FIAT_600', 'ACCIDENTE_SARMIENTO')
                OR tipoefecto IS NULL OR tipoefecto NOT IN
                ('GANAR_PLATA', 'PERDER_PLATA', 'MOVER_CASILLEROS', 'IR_A_CASILLERO',
                 'PERDER_TURNO', 'VOLVER_INICIO'))
    ) THEN
        RAISE EXCEPTION 'Globales con enums nulos o desconocidos; no se modifican sus valores';
    END IF;

    SELECT least(coalesce(character_maximum_length, 255), 255)
        INTO v_longitud_descripcion FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'evento' AND column_name = 'descripcion';
    SELECT coalesce(character_maximum_length, 255)
        INTO v_longitud_nombre FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'evento' AND column_name = 'nombre';

    IF EXISTS (
        SELECT 1 FROM public.eventoglobalregistro WHERE dtype = 'EventoGlobal'
            AND (char_length(descripcion) > v_longitud_descripcion
                OR char_length('__GLOBAL_LEGADO__:' || id::text) > v_longitud_nombre)
    ) THEN
        RAISE EXCEPTION 'Textos exceden el destino/JPA (descripcion %); no se truncan',
            v_longitud_descripcion;
    END IF;

    -- Un nombre tecnico por ID viejo permite conservar filas globales duplicadas.
    IF EXISTS (
        SELECT 1 FROM public.eventoglobalregistro origen
        JOIN public.evento destino ON destino.nombre = '__GLOBAL_LEGADO__:' || origen.id::text
        WHERE origen.dtype = 'EventoGlobal'
    ) THEN
        RAISE EXCEPTION 'Colision de nombres reservados; no se reutilizan raices existentes';
    END IF;

    v_secuencia := pg_get_serial_sequence('public.evento', 'id')::regclass;
    IF v_secuencia IS NULL OR NOT EXISTS (
        SELECT 1 FROM pg_catalog.pg_sequence WHERE seqrelid = v_secuencia
            AND seqincrement = 1 AND NOT seqcycle
    ) THEN
        RAISE EXCEPTION 'Secuencia de evento.id ausente o no contemplada';
    END IF;

    EXECUTE format('SELECT last_value FROM %s', v_secuencia) INTO v_ultimo;
    SELECT greatest(coalesce(max(id), 0), v_ultimo, 0) INTO v_base FROM public.evento;
    SELECT least(seqmax, 2147483647::bigint) INTO v_limite
        FROM pg_catalog.pg_sequence WHERE seqrelid = v_secuencia;
    IF v_globales >= v_limite - v_base THEN
        RAISE EXCEPTION 'No hay espacio Integer para % globales y el siguiente ID', v_globales;
    END IF;

    CREATE TEMP TABLE eg_origen_antes ON COMMIT DROP AS
        SELECT id, to_jsonb(origen) AS fila FROM public.eventoglobalregistro origen;
    CREATE TEMP TABLE eg_raices_antes ON COMMIT DROP AS
        SELECT id, to_jsonb(raiz) AS fila FROM public.evento raiz;
    CREATE TEMP TABLE eg_casilleros_antes ON COMMIT DROP AS
        SELECT id, to_jsonb(casillero) AS fila FROM public.eventocasillero casillero;

    CREATE TABLE public.eventoglobal (
        id integer PRIMARY KEY REFERENCES public.evento(id),
        tipoefecto varchar(255),
        valor integer,
        tipoeventoglobal varchar(255),
        CONSTRAINT eventoglobal_tipoefecto_check CHECK (tipoefecto IN
            ('GANAR_PLATA', 'PERDER_PLATA', 'MOVER_CASILLEROS', 'IR_A_CASILLERO',
             'PERDER_TURNO', 'VOLVER_INICIO')),
        CONSTRAINT eventoglobal_tipoeventoglobal_check CHECK (tipoeventoglobal IN
            ('INUNDACION', 'ROBO', 'TRAMITES_AFIP', 'PIQUETE', 'FIAT_600', 'ACCIDENTE_SARMIENTO'))
    );
    -- Mapa auditable Long -> Integer. No es una entidad Java ni reemplaza al archivo original.
    CREATE TABLE public.migracion_eventos_globales (
        id_anterior bigint PRIMARY KEY REFERENCES public.eventoglobalregistro(id),
        id_nuevo integer NOT NULL UNIQUE
    );

    INSERT INTO public.migracion_eventos_globales(id_anterior, id_nuevo)
    SELECT id, (v_base + row_number() OVER (ORDER BY id))::integer
    FROM public.eventoglobalregistro WHERE dtype = 'EventoGlobal';

    IF EXISTS (
        SELECT 1 FROM public.migracion_eventos_globales mapa
        JOIN public.evento existente ON existente.id = mapa.id_nuevo
    ) OR (SELECT count(*) FROM public.migracion_eventos_globales) <> v_globales THEN
        RAISE EXCEPTION 'Colision de IDs o cantidad incorrecta en el mapa';
    END IF;

    -- RESTART es transaccional. No usar nextval/setval: no revierten con ROLLBACK.
    EXECUTE format('ALTER SEQUENCE %s RESTART WITH %s', v_secuencia, v_base + v_globales + 1);

    INSERT INTO public.evento(id, nombre, descripcion) OVERRIDING SYSTEM VALUE
    SELECT mapa.id_nuevo, '__GLOBAL_LEGADO__:' || origen.id::text, origen.descripcion
    FROM public.eventoglobalregistro origen
    JOIN public.migracion_eventos_globales mapa ON mapa.id_anterior = origen.id;

    INSERT INTO public.eventoglobal(id, tipoefecto, valor, tipoeventoglobal)
    SELECT mapa.id_nuevo, origen.tipoefecto, origen.valor, origen.tipoeventoglobal
    FROM public.eventoglobalregistro origen
    JOIN public.migracion_eventos_globales mapa ON mapa.id_anterior = origen.id;

    ALTER TABLE public.migracion_eventos_globales ADD CONSTRAINT migracion_eventos_globales_destino_fk
        FOREIGN KEY (id_nuevo) REFERENCES public.eventoglobal(id);

    IF (SELECT count(*) FROM public.eventoglobal) <> v_globales
            OR (SELECT count(*) FROM public.evento) <> v_raices + v_globales
            OR (SELECT count(*) FROM public.eventoglobalregistro) <> v_total_origen THEN
        RAISE EXCEPTION 'Conteos finales inconsistentes';
    END IF;

    IF EXISTS (
        SELECT 1 FROM public.migracion_eventos_globales mapa
        JOIN public.eventoglobalregistro origen ON origen.id = mapa.id_anterior
        JOIN public.evento raiz ON raiz.id = mapa.id_nuevo
        JOIN public.eventoglobal global ON global.id = mapa.id_nuevo
        WHERE raiz.descripcion IS DISTINCT FROM origen.descripcion
            OR raiz.nombre IS DISTINCT FROM '__GLOBAL_LEGADO__:' || origen.id::text
            OR global.tipoefecto IS DISTINCT FROM origen.tipoefecto
            OR global.valor IS DISTINCT FROM origen.valor
            OR global.tipoeventoglobal IS DISTINCT FROM origen.tipoeventoglobal
    ) THEN
        RAISE EXCEPTION 'Diferencias de contenido entre origen y destino';
    END IF;

    IF EXISTS (
        SELECT id, fila FROM eg_origen_antes
        EXCEPT ALL SELECT id, to_jsonb(origen) FROM public.eventoglobalregistro origen
    ) OR EXISTS (
        SELECT id, to_jsonb(origen) FROM public.eventoglobalregistro origen
        EXCEPT ALL SELECT id, fila FROM eg_origen_antes
    ) OR EXISTS (
        SELECT id, fila FROM eg_casilleros_antes
        EXCEPT ALL SELECT id, to_jsonb(casillero) FROM public.eventocasillero casillero
    ) OR EXISTS (
        SELECT id, to_jsonb(casillero) FROM public.eventocasillero casillero
        EXCEPT ALL SELECT id, fila FROM eg_casilleros_antes
    ) OR EXISTS (
        SELECT 1 FROM eg_raices_antes anterior
        LEFT JOIN public.evento actual ON actual.id = anterior.id
        WHERE to_jsonb(actual) IS DISTINCT FROM anterior.fila
    ) THEN
        RAISE EXCEPTION 'Los registros originales o los eventos por casillero cambiaron';
    END IF;

    RAISE NOTICE 'Verificado: % globales, % registros originales intactos, IDs nuevos % a %',
        v_globales, v_total_origen, v_base + 1, v_base + v_globales;
END;
$migracion$;

-- Comparaciones para guardar junto con el inventario ANTES del ROLLBACK/COMMIT.
SELECT mapa.id_anterior, mapa.id_nuevo, raiz.nombre,
       raiz.descripcion, global.tipoefecto, global.valor, global.tipoeventoglobal
FROM public.migracion_eventos_globales mapa
JOIN public.evento raiz ON raiz.id = mapa.id_nuevo
JOIN public.eventoglobal global ON global.id = mapa.id_nuevo
ORDER BY mapa.id_anterior;

SELECT origen.tipoeventoglobal, count(*) AS originales,
       (SELECT count(*) FROM public.eventoglobal global
        WHERE global.tipoeventoglobal = origen.tipoeventoglobal) AS migrados
FROM public.eventoglobalregistro origen WHERE origen.dtype = 'EventoGlobal'
GROUP BY origen.tipoeventoglobal ORDER BY origen.tipoeventoglobal;

SELECT (SELECT count(*) FROM eg_origen_antes) AS originales_antes,
       (SELECT count(*) FROM public.eventoglobalregistro) AS originales_despues,
       (SELECT count(*) FROM eg_casilleros_antes) AS casilleros_antes,
       (SELECT count(*) FROM public.eventocasillero) AS casilleros_despues,
       (SELECT count(*) FROM public.migracion_eventos_globales) AS ids_mapeados;

ROLLBACK;
-- COMMIT; -- Solo tras revisar resultados y recibir autorizacion para aplicar.
