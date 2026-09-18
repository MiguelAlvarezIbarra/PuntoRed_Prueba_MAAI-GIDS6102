CREATE SEQUENCE productos_seq START WITH 1 INCREMENT BY 50;

CREATE TABLE productos (
    id BIGINT NOT NULL,
    id_producto INT NOT NULL,
    id_servicio INT NOT NULL,
    id_cat_tipo_servicio INT,
    nombre_producto VARCHAR(255) NOT NULL,
    nombre_servicio VARCHAR(255) NOT NULL,
    tipo_front INT,
    tipo_referencia VARCHAR(10),
    precio VARCHAR(50),
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_productos PRIMARY KEY (id)
);

CREATE INDEX idx_productos_id_producto ON productos (id_producto);
CREATE INDEX idx_productos_id_servicio ON productos (id_servicio);
