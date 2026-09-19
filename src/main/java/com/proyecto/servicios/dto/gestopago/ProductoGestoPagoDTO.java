package com.proyecto.servicios.dto.gestopago;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
@NoArgsConstructor
public class ProductoGestoPagoDTO {

    private Integer idProducto;
    private Integer idServicio;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private String tipoReferencia;
    private String precio;
    private String nombreProducto;
    private String nombreServicio;

    public static ProductoGestoPagoDTO fromJson(JSONObject prodJson) {
        ProductoGestoPagoDTO dto = new ProductoGestoPagoDTO();
        dto.setIdProducto(prodJson.optInt("idProducto"));
        dto.setIdServicio(prodJson.optInt("idServicio"));
        dto.setIdCatTipoServicio(prodJson.optInt("idCatTipoServicio"));
        dto.setTipoFront(prodJson.optInt("tipoFront"));
        dto.setTipoReferencia(prodJson.optString("tipoReferencia", null));
        dto.setPrecio(prodJson.optString("precio", null));

        String nombreProducto = prodJson.optString("content", "Desconocido");
        dto.setNombreProducto(nombreProducto);
        dto.setNombreServicio(prodJson.optString("servicio", nombreProducto));

        return dto;
    }
}