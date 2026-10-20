-- Conserva las propiedades y sus dueños; actualiza únicamente los nombres anteriores.
UPDATE Propiedad
SET nombre = CASE nombre
    WHEN 'Villa Palito' THEN 'San Justo Shopping'
    WHEN 'Villa San Pete' THEN 'Plaza Hipólito Yrigoyen'
END
WHERE nombre IN ('Villa Palito', 'Villa San Pete');

UPDATE Casillero
SET nombre = CASE posicion
    WHEN 6 THEN 'Colectivo 242'
    WHEN 12 THEN 'Colectivo 96'
    WHEN 17 THEN 'Colectivo 621'
    WHEN 24 THEN 'Colectivo 620'
END,
descripcion = 'Parada de bondi'
WHERE posicion IN (6, 12, 17, 24);

UPDATE Propiedad p
SET nombre = c.nombre, grupo = NULL
FROM CasilleroPropiedad cp
JOIN Casillero c ON c.id = cp.id
WHERE cp.propiedad_id = p.id AND c.posicion IN (6, 12, 17, 24);

UPDATE Casillero
SET nombre = 'EVENTOS'
WHERE nombre IN ('EVENTO 1', 'EVENTO 2', 'EVENTO 3', 'EVENTO 4', 'EVENTO 5');

UPDATE Casillero
SET nombre = 'COMISARÍA'
WHERE nombre IN ('Cárcel / Comisaría', 'Cárcel', 'Carcel');

UPDATE Casillero
SET nombre = CASE nombre
    WHEN 'Villa Palito' THEN 'San Justo Shopping'
    WHEN 'Villa San Pete' THEN 'Plaza Hipólito Yrigoyen'
END
WHERE nombre IN ('Villa Palito', 'Villa San Pete');
