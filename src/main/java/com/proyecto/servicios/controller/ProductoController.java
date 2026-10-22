package com.proyecto.servicios.controller;

import com.proyecto.servicios.dto.response.ApiResponse;
import com.proyecto.servicios.dto.response.ResponseCode;
import com.proyecto.servicios.model.ProductoData;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductoData>>> getProductos() {
        List<ProductoData> productos = productoService.obtenerProductos();

        if (productos.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(ResponseCode.ERROR_SIN_DATOS));
        }

        return ResponseEntity.ok(ApiResponse.ok(productos));
    }

    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Integer>> forceSyncProductos() {
        int total = productoService.sincronizarProductos();
        return ResponseEntity.ok(ApiResponse.ok(total));
    }
}