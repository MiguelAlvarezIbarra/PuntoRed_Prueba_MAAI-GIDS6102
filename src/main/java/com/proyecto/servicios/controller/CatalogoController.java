package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.puntored.CatalogoBusquedaRequest;
import com.proyecto.servicios.model.puntored.CatalogoServiciosResponse;
import com.proyecto.servicios.service.PuntoredCatalogoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST del catalogo dinamico de productos y servicios PuntoRed.
 * Respeta las reglas arquitectonicas (sin @PathVariable ni @RequestParam).
 */
@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final PuntoredCatalogoService catalogoService;

    public CatalogoController(PuntoredCatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogoServiciosResponse> obtenerCatalogoCompleto() {
        return new ResponseEntity<>(catalogoService.consultarCatalogo(new CatalogoBusquedaRequest()), HttpStatus.OK);
    }

    @PostMapping(value = "/buscar", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CatalogoServiciosResponse> buscarCatalogo(
            @RequestBody(required = false) CatalogoBusquedaRequest request) {
        CatalogoBusquedaRequest filtro = request != null ? request : new CatalogoBusquedaRequest();
        return new ResponseEntity<>(catalogoService.consultarCatalogo(filtro), HttpStatus.OK);
    }
}
