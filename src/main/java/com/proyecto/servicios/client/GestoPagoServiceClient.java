package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "gestoPagoService",
        url = "${gestopago.service.url:https://gestopago.portalventas.net/sistema/service}",
        configuration = GestoPagoFeignConfig.class
)
public interface GestoPagoServiceClient {

    @GetMapping("/sendEcho.do")
    String sendEcho(
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/validateMe.do")
    String validateMe(
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/getBalance.do")
    String getBalance(
            @RequestHeader("Authorization") String authorizationHeader
    );

    @GetMapping("/getProductList.do")
    String getProductList(
            @RequestHeader("Authorization") String authorizationHeader
    );

    @PostMapping(value = "/verifyReference.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String verifyReference(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody String formBody
    );

    @PostMapping(value = "/sendTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody String formBody
    );

    @PostMapping(value = "/confirmTx.do", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String confirmTx(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody String formBody
    );
}
