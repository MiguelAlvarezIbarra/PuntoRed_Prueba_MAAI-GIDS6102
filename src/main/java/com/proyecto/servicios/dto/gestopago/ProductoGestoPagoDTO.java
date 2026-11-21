package com.proyecto.servicios.dto.gestopago;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.json.JSONObject;

@Getter
@Setter
@NoArgsConstructor
public class ProductoGestoPagoDTO {

    private static final String LOGO_BASE_URL = "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/";

    private Integer idProducto;
    private Integer idServicio;
    private Integer idCatTipoServicio;
    private Integer tipoFront;
    private String tipoReferencia;
    private String precio;
    private String nombreProducto;
    private String nombreServicio;
    private String descripcion;
    private String logoUrl;
    private String categoria;
    private String tipoOperacion;

    public static ProductoGestoPagoDTO fromJson(JSONObject prodJson) {
        ProductoGestoPagoDTO dto = new ProductoGestoPagoDTO();
        int idProd = prodJson.optInt("idProducto");
        int idServ = prodJson.optInt("idServicio");
        int idCat = prodJson.optInt("idCatTipoServicio");
        int tipoFront = prodJson.optInt("tipoFront");

        dto.setIdProducto(idProd);
        dto.setIdServicio(idServ);
        dto.setIdCatTipoServicio(idCat);
        dto.setTipoFront(tipoFront);
        dto.setTipoReferencia(prodJson.optString("tipoReferencia", null));
        dto.setPrecio(prodJson.optString("precio", null));

        String attrProducto = prodJson.optString("producto", "").trim();
        String textContent = prodJson.optString("content", "").trim();
        String nombreProducto = !attrProducto.isEmpty()
                ? attrProducto
                : (!textContent.isEmpty() ? textContent : "Desconocido");
        dto.setNombreProducto(nombreProducto);

        String attrServicio = prodJson.optString("servicio", "").trim();
        dto.setNombreServicio(!attrServicio.isEmpty() ? attrServicio : nombreProducto);

        String legend = prodJson.optString("legend", "").trim();
        dto.setDescripcion(!legend.isEmpty() ? legend : null);
        dto.setLogoUrl(idServ > 0 ? LOGO_BASE_URL + idServ + ".png" : null);
        dto.setCategoria(resolverCategoria(idCat));
        dto.setTipoOperacion(resolverTipoOperacion(tipoFront, idCat));

        return dto;
    }

    private static String resolverCategoria(int idCatTipoServicio) {
        return switch (idCatTipoServicio) {
            case 1 -> "Movistar";
            case 2 -> "Telcel";
            case 3 -> "AT&T";
            case 4 -> "Unefon";
            case 5 -> "Pago de Servicios";
            case 6 -> "Nextel";
            case 7 -> "Pago de Impuestos / Gobierno";
            case 8 -> "Productos Financieros";
            case 9 -> "Productos por Catalogo";
            case 10 -> "Servicios de Prepago";
            case 11 -> "Entretenimiento";
            case 12 -> "Virgin";
            case 13 -> "Otras Recargas";
            case 14 -> "Venta de Seguros";
            case 15 -> "Pago Derechos de Agua";
            case 16 -> "Juegos y Sorteos";
            case 17 -> "MazTiempo";
            case 18 -> "Autopistas y Transportes";
            case 19 -> "Internet de Prepago";
            default -> "Servicios Mexico";
        };
    }

    private static String resolverTipoOperacion(int tipoFront, int idCatTipoServicio) {
        if (idCatTipoServicio == 10 || idCatTipoServicio == 11) {
            return "PIN";
        }
        return tipoFront == 1 ? "RECARGA" : "RECAUDO";
    }
}
