package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "gestoPagoService", url = "${gestopago.service.url:https://gestopago.portalventas.net/sistema/service}")
public interface GestoPagoServiceClient {

    @GetMapping("/getProductList.do")
    String getProductList(
            @RequestHeader("Authorization") String authorizationHeader
    );

    @PostMapping(value = "/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody String formBody
    );
}
