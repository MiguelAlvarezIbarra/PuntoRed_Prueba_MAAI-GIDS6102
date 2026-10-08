CREATE TABLE cat_paises (
    nombre TEXT PRIMARY KEY
);

CREATE TABLE cat_nacionalidades (
    nombre TEXT PRIMARY KEY
);

INSERT INTO cat_paises (nombre) VALUES 
('Antigua y Barbuda'), ('Argentina'), ('Bahamas'), ('Barbados'), ('Belice'), 
('Bolivia'), ('Brasil'), ('Canadá'), ('Chile'), ('Colombia'), 
('Costa Rica'), ('Cuba'), ('Dominica'), ('Ecuador'), ('El Salvador'), 
('Estados Unidos'), ('Granada'), ('Guatemala'), ('Guyana'), ('Haití'), 
('Honduras'), ('Jamaica'), ('México'), ('Nicaragua'), ('Panamá'), 
('Paraguay'), ('Perú'), ('República Dominicana'), ('San Cristóbal y Nieves'), 
('San Vicente y las Granadinas'), ('Santa Lucía'), ('Surinam'), 
('Trinidad y Tobago'), ('Uruguay'), ('Venezuela');

INSERT INTO cat_nacionalidades (nombre) VALUES 
('Antiguano'), ('Argentino'), ('Bahameño'), ('Barbadense'), ('Beliceño'), 
('Boliviano'), ('Brasileño'), ('Canadiense'), ('Chileno'), ('Colombiano'), 
('Costarricense'), ('Cubano'), ('Dominiqués'), ('Ecuatoriano'), ('Salvadoreño'), 
('Estadounidense'), ('Granadino'), ('Guatemalteco'), ('Guyanés'), ('Haitiano'), 
('Hondureño'), ('Jamaicano'), ('Mexicano'), ('Nicaragüense'), ('Panameño'), 
('Paraguayo'), ('Peruano'), ('Dominicano'), ('Sancristobaleño'), 
('Sanvicentino'), ('Santalucense'), ('Surinamés'), 
('Trinitense'), ('Uruguayo'), ('Venezolano');

-- Limpiar los datos existentes para que no rompan la llave foránea
UPDATE clientes SET nacionalidad = 'Mexicano' WHERE nacionalidad NOT IN (SELECT nombre FROM cat_nacionalidades);
UPDATE domicilios SET pais = 'México' WHERE pais NOT IN (SELECT nombre FROM cat_paises);

ALTER TABLE clientes ADD CONSTRAINT fk_clientes_nacionalidad FOREIGN KEY (nacionalidad) REFERENCES cat_nacionalidades(nombre);
ALTER TABLE domicilios ADD CONSTRAINT fk_domicilios_pais FOREIGN KEY (pais) REFERENCES cat_paises(nombre);
