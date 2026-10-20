-- Amplía la columna antes de convertir los nombres anteriores, sin eliminar registros.
ALTER TABLE SalaDeEspera_fichasOcupadas
    MODIFY COLUMN fichasOcupadas VARCHAR(20) NULL;

UPDATE SalaDeEspera_fichasOcupadas
SET fichasOcupadas = CASE fichasOcupadas
    WHEN 'ROJA' THEN 'ROJO'
    WHEN 'AMARILLA' THEN 'AMARILLO'
    ELSE fichasOcupadas
END
WHERE fichasOcupadas IN ('ROJA', 'AMARILLA');
