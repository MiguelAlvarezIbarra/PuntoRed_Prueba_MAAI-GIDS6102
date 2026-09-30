package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.ClienteRequest;
import com.proyecto.servicios.model.ClienteResponse;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ListaClientesResponse;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    // ---------- Endpoints minimos ----------

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> crearCliente(@Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.creaCliente(request), HttpStatus.CREATED);
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaClientesResponse> obtenerClientes() {
        return new ResponseEntity<>(clienteService.obtenerClientes(), HttpStatus.OK);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerClientePorId(@PathVariable Integer id) {
        return new ResponseEntity<>(clienteService.obtenerClientePorId(id), HttpStatus.OK);
    }

    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(@PathVariable Integer id,
                                                             @Valid @RequestBody ClienteRequest request) {
        return new ResponseEntity<>(clienteService.actualizaCliente(id, request), HttpStatus.OK);
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> eliminarCliente(@PathVariable Integer id) {
        return new ResponseEntity<>(clienteService.eliminaCliente(id), HttpStatus.OK);
    }

    // ---------- Consultas solicitadas ----------

    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerPorCurp(@PathVariable String curp) {
        return new ResponseEntity<>(clienteService.obtenerClientePorCurp(curp), HttpStatus.OK);
    }

    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerPorRfc(@PathVariable String rfc) {
        return new ResponseEntity<>(clienteService.obtenerClientePorRfc(rfc), HttpStatus.OK);
    }

    @GetMapping(value = "/correo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerPorCorreo(@RequestParam String correo) {
        return new ResponseEntity<>(clienteService.obtenerClientePorCorreo(correo), HttpStatus.OK);
    }

    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> obtenerPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return new ResponseEntity<>(clienteService.obtenerClientePorNumeroCuenta(numeroCuenta), HttpStatus.OK);
    }

    @GetMapping(value = "/activos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaClientesResponse> obtenerActivos() {
        return new ResponseEntity<>(clienteService.obtenerClientesActivos(), HttpStatus.OK);
    }

    @GetMapping(value = "/rango-fechas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaClientesResponse> obtenerPorRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return new ResponseEntity<>(clienteService.obtenerClientesPorRangoFechas(desde, hasta), HttpStatus.OK);
    }
}
