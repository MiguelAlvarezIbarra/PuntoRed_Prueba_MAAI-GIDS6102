package com.proyecto.servicios.service;

import com.proyecto.servicios.dto.gestopago.ProductoGestoPagoDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogo oficial-compatible de GestoPago / PuntoRed Mexico en MXN utilizado
 * unicamente como respaldo automatico cuando el servidor oficial de GestoPago
 * rechaza la IP de origen (CODIGO 05 / Origen Valido) o no responde.
 *
 * Todos los idServicio y URLs de logotipo corresponden a imagenes PNG reales
 * publicadas en https://gestopago.portalventas.net/sistema/images/gestopago/servicios/{idServicio}.png.
 */
public final class GestoPagoMxCatalogoFallback {

    private static final String LOGO_BASE = "https://gestopago.portalventas.net/sistema/images/gestopago/servicios/";

    private GestoPagoMxCatalogoFallback() {
    }

    public static List<ProductoGestoPagoDTO> construirCatalogoMexico() {
        List<ProductoGestoPagoDTO> lista = new ArrayList<>();

        // 1. AT&T Recargas (idServicio=115, idCatTipoServicio=3)
        agregarRecargas(lista, 115, 3, "AT&T", "AT&T", 3001,
                new String[]{"30.00", "50.00", "100.00", "150.00", "200.00", "300.00", "500.00"},
                "Recarga de tiempo aire AT&T Mexico.");

        // 2. Telcel Recargas (idServicio=133, idCatTipoServicio=2; incluye idProducto=582 del sandbox)
        lista.add(crearItem(582, 133, 2, 1, "a", "20.00", "Telcel $20", "Telcel",
                "Recarga directa de tiempo aire Telcel Mexico.", "Telcel", "RECARGA"));
        agregarRecargas(lista, 133, 2, "Telcel", "Telcel", 2001,
                new String[]{"30.00", "50.00", "100.00", "150.00", "200.00", "300.00", "500.00"},
                "Recarga directa de tiempo aire Telcel Mexico.");

        // 3. Movistar Recargas (idServicio=87, idCatTipoServicio=1)
        agregarRecargas(lista, 87, 1, "Movistar", "Movistar", 1001,
                new String[]{"30.00", "50.00", "100.00", "150.00", "200.00", "300.00"},
                "Recarga directa de tiempo aire Movistar Mexico.");

        // 4. Unefon Recargas (idServicio=116, idCatTipoServicio=4; incluye idProducto=308 del sandbox)
        lista.add(crearItem(308, 116, 4, 1, "a", "30.00", "Unefon $30", "Unefon",
                "Recarga directa de tiempo aire Unefon Mexico.", "Unefon", "RECARGA"));
        agregarRecargas(lista, 116, 4, "Unefon", "Unefon", 4001,
                new String[]{"50.00", "100.00", "150.00", "200.00"},
                "Recarga directa de tiempo aire Unefon Mexico.");

        // 5. Virgin Mobile Mexico (idServicio=104, idCatTipoServicio=12)
        agregarRecargas(lista, 104, 12, "Virgin Mobile", "Virgin", 1201,
                new String[]{"50.00", "100.00", "150.00", "200.00"},
                "Recarga directa Virgin Mobile Mexico.");

        // 6. Oui Movil (idServicio=78, idCatTipoServicio=13)
        agregarRecargas(lista, 78, 13, "Oui Movil", "Otras Recargas", 1301,
                new String[]{"50.00", "100.00", "200.00"},
                "Recarga directa Oui Movil Mexico.");

        // 7. Paquetes AT&T Armalo / Planes (idServicio=108; incluye idProducto=272 del sandbox)
        lista.add(crearItem(272, 108, 3, 1, "a", "200.00", "AT&T Paquete $200 (30 dias)", "AT&T Paquetes",
                "Llamadas y SMS ilimitados + redes sociales y datos 30 dias.", "Paquetes - AT&T", "PAQUETE"));
        lista.add(crearItem(3101, 108, 3, 1, "a", "50.00", "AT&T Paquete $50 (7 dias)", "AT&T Paquetes",
                "Minutos y SMS ilimitados + redes sociales 7 dias.", "Paquetes - AT&T", "PAQUETE"));
        lista.add(crearItem(3102, 108, 3, 1, "a", "100.00", "AT&T Paquete $100 (14 dias)", "AT&T Paquetes",
                "Minutos y SMS ilimitados + 1.5 GB + redes sociales 14 dias.", "Paquetes - AT&T", "PAQUETE"));
        lista.add(crearItem(3103, 108, 3, 1, "a", "150.00", "AT&T Paquete $150 (25 dias)", "AT&T Paquetes",
                "Minutos y SMS ilimitados + 2.5 GB + redes sociales 25 dias.", "Paquetes - AT&T", "PAQUETE"));

        // 8. Paquetes Telcel Amigo Sin Limite (idServicio=159) e Internet Amigo (idServicio=160)
        lista.add(crearItem(2101, 159, 2, 1, "a", "50.00", "Amigo Sin Limite $50 (7 dias)", "Telcel Paquetes",
                "Minutos y SMS ilimitados + 500 MB + redes sociales 7 dias.", "Paquetes - Telcel", "PAQUETE"));
        lista.add(crearItem(2102, 159, 2, 1, "a", "100.00", "Amigo Sin Limite $100 (15 dias)", "Telcel Paquetes",
                "Minutos y SMS ilimitados + 1.3 GB + redes sociales 15 dias.", "Paquetes - Telcel", "PAQUETE"));
        lista.add(crearItem(2103, 159, 2, 1, "a", "150.00", "Amigo Sin Limite $150 (26 dias)", "Telcel Paquetes",
                "Minutos y SMS ilimitados + 2 GB + redes sociales 26 dias.", "Paquetes - Telcel", "PAQUETE"));
        lista.add(crearItem(2104, 159, 2, 1, "a", "200.00", "Amigo Sin Limite $200 (30 dias)", "Telcel Paquetes",
                "Minutos y SMS ilimitados + 3 GB + redes sociales 30 dias.", "Paquetes - Telcel", "PAQUETE"));
        lista.add(crearItem(2105, 159, 2, 1, "a", "300.00", "Amigo Sin Limite $300 (30 dias)", "Telcel Paquetes",
                "Minutos y SMS ilimitados + 4.5 GB + redes sociales 30 dias.", "Paquetes - Telcel", "PAQUETE"));
        lista.add(crearItem(2201, 160, 19, 1, "a", "50.00", "Internet Amigo $50 (7 dias)", "Telcel Internet",
                "Paquete de navegacion movil Telcel 7 dias.", "Paquetes - Internet", "PAQUETE"));
        lista.add(crearItem(2202, 160, 19, 1, "a", "100.00", "Internet Amigo $100 (15 dias)", "Telcel Internet",
                "Paquete de navegacion movil Telcel 15 dias.", "Paquetes - Internet", "PAQUETE"));

        // 9. Paquetes Movistar (idServicio=124) y CFE Internet para Todos (idServicio=153)
        lista.add(crearItem(1101, 124, 1, 1, "a", "50.00", "Movistar Paquete $50 (7 dias)", "Movistar Paquetes",
                "Llamadas ilimitadas + datos y redes sociales 7 dias.", "Paquetes - Movistar", "PAQUETE"));
        lista.add(crearItem(1102, 124, 1, 1, "a", "100.00", "Movistar Paquete $100 (15 dias)", "Movistar Paquetes",
                "Llamadas ilimitadas + 2 GB y redes sociales 15 dias.", "Paquetes - Movistar", "PAQUETE"));
        lista.add(crearItem(1103, 124, 1, 1, "a", "200.00", "Movistar Paquete $200 (30 dias)", "Movistar Paquetes",
                "Llamadas ilimitadas + 4 GB y redes sociales 30 dias.", "Paquetes - Movistar", "PAQUETE"));
        lista.add(crearItem(1531, 153, 19, 1, "a", "50.00", "CFE Internet Paquete $50", "CFE Telecomunicaciones",
                "Paquete de telefonia e internet CFE para Todos.", "Paquetes - Internet", "PAQUETE"));
        lista.add(crearItem(1532, 153, 19, 1, "a", "100.00", "CFE Internet Paquete $100", "CFE Telecomunicaciones",
                "Paquete mensual basico CFE Internet para Todos.", "Paquetes - Internet", "PAQUETE"));
        lista.add(crearItem(1533, 153, 19, 1, "a", "200.00", "CFE Internet Paquete $200", "CFE Telecomunicaciones",
                "Paquete mensual amplio CFE Internet para Todos.", "Paquetes - Internet", "PAQUETE"));

        // 10. Pago de Servicios / Recaudos Mexico (tipoFront=4 o 2)
        lista.add(crearItem(597, 166, 5, 4, "c", "0.00", "CFE Recibo de Luz", "CFE",
                "Pago de recibo de energia electrica CFE. Ingresa numero de servicio (12 digitos) o codigo de barras.",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(271, 107, 5, 4, "b", "0.00", "Telmex Recibo Telefonico", "Telmex",
                "Pago de linea telefonica e internet Infinitum Telmex (10 digitos).",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(205, 76, 5, 4, "b", "0.00", "izzi Telecomunicaciones", "izzi",
                "Pago de cuenta izzi internet, TV y telefonia.",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(220, 85, 5, 4, "b", "0.00", "Totalplay Internet y TV", "Totalplay",
                "Pago de recibo Totalplay mediante numero de cuenta (10 digitos).",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(601, 167, 5, 4, "b", "0.00", "Megacable", "Megacable",
                "Pago de servicio Megacable con numero de suscriptor.",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(602, 168, 5, 4, "b", "0.00", "Sky Television", "Sky",
                "Pago de suscripcion Sky y VeTV con numero de cuenta.",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(191, 61, 5, 4, "b", "0.00", "Dish Mexico", "Dish",
                "Pago de servicio de television Dish con numero de referencia.",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(194, 64, 5, 2, "c", "0.00", "Naturgy Gas Natural", "Naturgy",
                "Pago de recibo de gas natural Naturgy Mexico.",
                "Pago de Servicios", "RECAUDO"));
        lista.add(crearItem(185, 56, 15, 2, "c", "0.00", "AGUAKAN Agua Cancun", "AGUAKAN",
                "Pago de servicio de agua potable AGUAKAN.",
                "Pago Derechos de Agua", "RECAUDO"));
        lista.add(crearItem(207, 77, 15, 2, "c", "0.00", "SIAPA Agua Guadalajara", "SIAPA",
                "Pago de servicio de agua potable SIAPA.",
                "Pago Derechos de Agua", "RECAUDO"));
        lista.add(crearItem(190, 60, 15, 2, "c", "0.00", "CMAPAS Agua Salamanca", "CMAPAS",
                "Pago de servicio de agua potable CMAPAS Guanajuato.",
                "Pago Derechos de Agua", "RECAUDO"));
        lista.add(crearItem(196, 66, 15, 2, "c", "0.00", "JUMAPA Agua Celaya", "JUMAPA",
                "Pago de servicio de agua potable JUMAPA Guanajuato.",
                "Pago Derechos de Agua", "RECAUDO"));
        lista.add(crearItem(180, 53, 7, 2, "b", "0.00", "INFONAVIT Mensualidad", "INFONAVIT",
                "Abono o mensualidad de credito INFONAVIT con numero de credito (10 digitos).",
                "Pago de Impuestos / Gobierno", "RECAUDO"));
        lista.add(crearItem(594, 164, 8, 2, "b", "0.00", "Coppel Abono a Cuenta", "Coppel",
                "Abono a credito o tarjeta departamental Coppel.",
                "Productos Financieros", "RECAUDO"));
        lista.add(crearItem(593, 163, 8, 2, "b", "0.00", "Mercado Pago Deposito", "Mercado Pago",
                "Deposito a cuenta o referencia Mercado Pago Mexico.",
                "Productos Financieros", "RECAUDO"));
        lista.add(crearItem(140, 38, 18, 2, "b", "0.00", "TELEVIA Recarga TAG", "TELEVIA",
                "Recarga de saldo para TAG de autopistas TELEVIA.",
                "Autopistas y Transportes", "RECAUDO"));
        lista.add(crearItem(584, 154, 18, 2, "b", "0.00", "PASE Recarga TAG", "PASE",
                "Recarga de saldo para TAG de casetas PASE.",
                "Autopistas y Transportes", "RECAUDO"));
        lista.add(crearItem(209, 79, 9, 2, "b", "0.00", "Avon Pago de Pedido", "Avon",
                "Pago de pedido de representante Avon Mexico.",
                "Productos por Catalogo", "RECAUDO"));

        // 11. Pines Digitales y Tarjetas de Regalo Mexico (idCatTipoServicio=10/11, precios fijos MXN)
        lista.add(crearItem(200, 71, 10, 1, "a", "100.00", "Amazon $100", "Amazon",
                "Tarjeta de regalo digital Amazon.com.mx. Canjea tu PIN en www.amazon.com.mx/gc/redeem/.",
                "Pines - Tiendas", "PIN"));
        lista.add(crearItem(7102, 71, 10, 1, "a", "300.00", "Amazon $300", "Amazon",
                "Tarjeta de regalo digital Amazon.com.mx. Canjea tu PIN en www.amazon.com.mx/gc/redeem/.",
                "Pines - Tiendas", "PIN"));
        lista.add(crearItem(7103, 71, 10, 1, "a", "500.00", "Amazon $500", "Amazon",
                "Tarjeta de regalo digital Amazon.com.mx. Canjea tu PIN en www.amazon.com.mx/gc/redeem/.",
                "Pines - Tiendas", "PIN"));
        lista.add(crearItem(7029, 178, 11, 1, "a", "150.00", "Netflix $150", "Netflix",
                "PIN digital Netflix Mexico. Ingresa tu codigo en www.netflix.com/codigo.",
                "Pines - Entretenimiento", "PIN"));
        lista.add(crearItem(7030, 178, 11, 1, "a", "300.00", "Netflix $300", "Netflix",
                "PIN digital Netflix Mexico. Ingresa tu codigo en www.netflix.com/codigo.",
                "Pines - Entretenimiento", "PIN"));
        lista.add(crearItem(7031, 178, 11, 1, "a", "500.00", "Netflix $500", "Netflix",
                "PIN digital Netflix Mexico. Ingresa tu codigo en www.netflix.com/codigo.",
                "Pines - Entretenimiento", "PIN"));
        lista.add(crearItem(1441, 144, 11, 1, "a", "150.00", "Xbox Game Pass $150", "Xbox Game Pass",
                "Codigo digital Xbox Game Pass Mexico. Canjea en redeem.microsoft.com.",
                "Pines - Gaming", "PIN"));
        lista.add(crearItem(1442, 144, 11, 1, "a", "300.00", "Xbox Game Pass $300", "Xbox Game Pass",
                "Codigo digital Xbox Game Pass Mexico. Canjea en redeem.microsoft.com.",
                "Pines - Gaming", "PIN"));
        lista.add(crearItem(1431, 143, 11, 1, "a", "200.00", "PlayStation Store $200", "PlayStation",
                "Tarjeta digital PlayStation Store Mexico.",
                "Pines - Gaming", "PIN"));
        lista.add(crearItem(1432, 143, 11, 1, "a", "400.00", "PlayStation Store $400", "PlayStation",
                "Tarjeta digital PlayStation Store Mexico.",
                "Pines - Gaming", "PIN"));
        lista.add(crearItem(1411, 141, 11, 1, "a", "200.00", "Nintendo eShop $200", "Nintendo eShop",
                "Tarjeta de saldo digital Nintendo eShop Mexico.",
                "Pines - Gaming", "PIN"));
        lista.add(crearItem(1471, 147, 11, 1, "a", "200.00", "Roblox $200", "Roblox",
                "Tarjeta digital Robux para Roblox Mexico.",
                "Pines - Gaming", "PIN"));
        lista.add(crearItem(1341, 134, 11, 1, "a", "200.00", "Apple / iTunes $200", "Apple iTunes",
                "Tarjeta digital App Store & iTunes Mexico.",
                "Pines - Entretenimiento", "PIN"));
        lista.add(crearItem(1421, 142, 11, 1, "a", "115.00", "Deezer Premium $115", "Deezer",
                "PIN de suscripcion mensual Deezer Premium Mexico.",
                "Pines - Entretenimiento", "PIN"));
        lista.add(crearItem(1511, 151, 11, 1, "a", "200.00", "Cinepolis $200", "Cinepolis",
                "Folio electronico Cinepolis Mexico.",
                "Pines - Entretenimiento", "PIN"));
        lista.add(crearItem(1291, 129, 10, 1, "a", "150.00", "Rappi Tarjeta $150", "Rappi",
                "Codigo de saldo prepago Rappi Mexico.",
                "Pines - Tiendas", "PIN"));

        return lista;
    }

    private static void agregarRecargas(
            List<ProductoGestoPagoDTO> destino,
            int idServicio,
            int idCatTipoServicio,
            String nombreServicio,
            String categoria,
            int idProductoInicial,
            String[] montos,
            String descripcion) {
        int idProd = idProductoInicial;
        for (String monto : montos) {
            String etiquetaMonto = monto.endsWith(".00") ? monto.substring(0, monto.length() - 3) : monto;
            destino.add(crearItem(
                    idProd++,
                    idServicio,
                    idCatTipoServicio,
                    1,
                    "a",
                    monto,
                    nombreServicio + " $" + etiquetaMonto,
                    nombreServicio,
                    descripcion,
                    categoria,
                    "RECARGA"
            ));
        }
    }

    private static ProductoGestoPagoDTO crearItem(
            int idProducto,
            int idServicio,
            int idCatTipoServicio,
            int tipoFront,
            String tipoReferencia,
            String precio,
            String nombreProducto,
            String nombreServicio,
            String descripcion,
            String categoria,
            String tipoOperacion) {
        ProductoGestoPagoDTO dto = new ProductoGestoPagoDTO();
        dto.setIdProducto(idProducto);
        dto.setIdServicio(idServicio);
        dto.setIdCatTipoServicio(idCatTipoServicio);
        dto.setTipoFront(tipoFront);
        dto.setTipoReferencia(tipoReferencia);
        dto.setPrecio(precio);
        dto.setNombreProducto(nombreProducto);
        dto.setNombreServicio(nombreServicio);
        dto.setDescripcion(descripcion);
        dto.setLogoUrl(LOGO_BASE + idServicio + ".png");
        dto.setCategoria(categoria);
        dto.setTipoOperacion(tipoOperacion);
        return dto;
    }
}
