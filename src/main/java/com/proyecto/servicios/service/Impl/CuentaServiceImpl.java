package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.MensajeError;
import com.proyecto.servicios.model.CuentaAbonoRequest;
import com.proyecto.servicios.model.CuentaData;
import com.proyecto.servicios.model.CuentaResponse;
import com.proyecto.servicios.model.ListaCuentasResponse;
import com.proyecto.servicios.model.SaldoResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Override
    public CuentaResponse obtenerCuentaPorNumero(String numeroCuenta) {
        log.info("Inicio obtenerCuentaPorNumero - numeroCuenta={}", numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(MensajeError.CUENTA_NO_ENCONTRADA));

        CuentaResponse response = new CuentaResponse();
        response.setCuenta(toData(cuenta));
        response.setCodigo(0);
        response.setMensaje("Exito");
        log.info("Fin obtenerCuentaPorNumero - numeroCuenta={}", numeroCuenta);
        return response;
    }

    @Override
    public ListaCuentasResponse obtenerCuentasActivas() {
        List<CuentaData> data = cuentaRepository.findByEstatus("ACTIVA").stream()
                .map(this::toData)
                .collect(Collectors.toList());
        ListaCuentasResponse response = new ListaCuentasResponse();
        response.setCuentas(data);
        response.setCodigo(0);
        response.setMensaje("Exito");
        return response;
    }

    @Override
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        log.info("Inicio consultarSaldo - numeroCuenta={}", numeroCuenta);
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(MensajeError.CUENTA_NO_ENCONTRADA));
        SaldoResponse response = new SaldoResponse();
        response.setNumeroCuenta(cuenta.getNumeroCuenta());
        response.setSaldo(cuenta.getSaldo());
        response.setCodigo(0);
        response.setMensaje("Exito");
        log.info("Fin consultarSaldo - numeroCuenta={}", numeroCuenta);
        return response;
    }

    @Override
    public SaldoResponse abonarSaldo(CuentaAbonoRequest request) {
        log.info("Inicio abonarSaldo - numeroCuenta={}", request.getNumeroCuenta());
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta())
                .orElseThrow(() -> new CuentaNoEncontradaException(MensajeError.CUENTA_NO_ENCONTRADA));

        Cliente cliente = cuenta.getCliente();
        if (cliente == null || Boolean.FALSE.equals(cliente.getActivo()) || !"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ClienteInactivoException(MensajeError.CLIENTE_INACTIVO_MODIFICACION);
        }

        BigDecimal montoLimpio = request.getMonto().stripTrailingZeros();
        if (montoLimpio.scale() < 0) {
            montoLimpio = montoLimpio.setScale(0);
        }

        cuenta.setSaldo(cuenta.getSaldo().add(montoLimpio));
        cuentaRepository.save(cuenta);

        SaldoResponse response = new SaldoResponse();
        response.setNumeroCuenta(cuenta.getNumeroCuenta());
        response.setSaldo(cuenta.getSaldo());
        response.setCodigo(0);
        response.setMensaje("Abono realizado correctamente");
        log.info("Fin abonarSaldo - numeroCuenta={}", request.getNumeroCuenta());
        return response;
    }

    private CuentaData toData(Cuenta cuenta) {
        CuentaData data = new CuentaData();
        data.setId(cuenta.getId());
        data.setClienteId(cuenta.getCliente().getId());
        data.setNumeroCuenta(cuenta.getNumeroCuenta());
        data.setSaldo(cuenta.getSaldo());
        data.setEstatus(cuenta.getEstatus());
        data.setFechaApertura(cuenta.getFechaApertura());
        return data;
    }
}
