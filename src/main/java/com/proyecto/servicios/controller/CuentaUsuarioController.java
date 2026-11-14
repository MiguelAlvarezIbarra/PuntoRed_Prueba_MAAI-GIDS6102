package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.SeguridadGuard;
import com.proyecto.servicios.config.SeguridadGuard.InfoToken;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.model.CuentaData;
import com.proyecto.servicios.model.ListaCuentasResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.util.Roles;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone las cuentas bancarias propias del usuario autenticado (o todas si es administrador).
 */
@RestController
@RequestMapping("/cuentas")
public class CuentaUsuarioController {

    private static final String ESTATUS_ACTIVA = "ACTIVA";

    private final CuentaRepository cuentaRepository;
    private final SeguridadGuard seguridadGuard;

    public CuentaUsuarioController(CuentaRepository cuentaRepository, SeguridadGuard seguridadGuard) {
        this.cuentaRepository = cuentaRepository;
        this.seguridadGuard = seguridadGuard;
    }

    @GetMapping(value = "/mias", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaCuentasResponse> obtenerMisCuentas(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        InfoToken info = seguridadGuard.exigirToken(authorization);

        List<Cuenta> cuentas = (info.getRol() != null && info.getRol().equals(Roles.ADMIN))
                ? cuentaRepository.findByEstatus(ESTATUS_ACTIVA)
                : cuentaRepository.findByCliente_Id(info.getClienteId()).stream()
                        .filter(c -> ESTATUS_ACTIVA.equalsIgnoreCase(c.getEstatus()))
                        .toList();

        List<CuentaData> data = cuentas.stream().map(this::mapearCuenta).toList();
        ListaCuentasResponse response = new ListaCuentasResponse();
        response.setCodigo(0);
        response.setMensaje("Cuentas activas del usuario consultadas exitosamente");
        response.setCuentas(data);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private CuentaData mapearCuenta(Cuenta cuenta) {
        CuentaData dto = new CuentaData();
        dto.setId(cuenta.getId());
        dto.setNumeroCuenta(cuenta.getNumeroCuenta());
        dto.setSaldo(cuenta.getSaldo());
        dto.setEstatus(cuenta.getEstatus());
        dto.setFechaApertura(cuenta.getFechaApertura());
        if (cuenta.getCliente() != null) {
            dto.setClienteId(cuenta.getCliente().getId());
            dto.setNombreCliente(cuenta.getCliente().getNombre()
                    + " " + cuenta.getCliente().getApellidoPaterno()
                    + " " + cuenta.getCliente().getApellidoMaterno());
        }
        return dto;
    }
}
