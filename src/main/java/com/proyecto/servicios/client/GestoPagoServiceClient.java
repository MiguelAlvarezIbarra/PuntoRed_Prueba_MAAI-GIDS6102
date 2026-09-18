package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoService", url = "${gestopago.service.url:https://gestopago.portalventas.net/sistema/service}")
public interface GestoPagoServiceClient {

    @GetMapping("/getProductList.do")
    String getProductList(
            @RequestHeader("Authorization") String authorizationHeader
            // X-API-Key es opcional según la documentación, se puede agregar si el cliente lo requiere
            // @RequestHeader(value = "X-API-Key", required = false) String apiKey
    );
}
