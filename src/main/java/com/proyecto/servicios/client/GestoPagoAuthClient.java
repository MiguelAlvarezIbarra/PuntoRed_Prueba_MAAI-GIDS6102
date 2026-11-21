package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "gestoPagoAuth",
        url = "${gestopago.auth.url}",
        configuration = GestoPagoFeignConfig.class
)
public interface GestoPagoAuthClient {

    @PostMapping("/sistema/app/jwt-gp/authenticate/")
    String authenticate(
            @RequestParam("idDistribuidor") Integer idDistribuidor,
            @RequestParam("codigoDispositivo") String codigoDispositivo,
            @RequestParam("password") String password
    );
}