package com.proyecto.servicios.model.puntored;

import lombok.Data;

import java.util.Map;

/**
 * Comprobante digital (tirilla) devuelto por Puntored H2H V2 o reconstruido desde el historial.
 */
@Data
public class TicketData {
    private String encabezado;
    private String titulo;
    private Map<String, Object> cuerpo;
    private String piePagina;
    private String logoUrl;
}
