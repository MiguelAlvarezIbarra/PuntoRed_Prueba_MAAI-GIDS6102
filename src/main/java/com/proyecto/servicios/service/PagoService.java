package com.proyecto.servicios.service;

import com.proyecto.servicios.model.PagoRequest;
import com.proyecto.servicios.model.PagoResponse;
import com.proyecto.servicios.model.Pin2faResponse;
import com.proyecto.servicios.model.SolicitudPin2faRequest;

public interface PagoService {

    Pin2faResponse enviarPin2fa(SolicitudPin2faRequest request, String authorizationHeader);

    PagoResponse realizarPago(PagoRequest request, String authorizationHeader);
}
