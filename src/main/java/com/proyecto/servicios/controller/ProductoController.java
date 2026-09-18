package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.Producto;
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
    public ResponseEntity<List<Producto>> getProductos() {
        List<Producto> productos = productoService.obtenerTodosLosProductos();
        return ResponseEntity.ok(productos);
    }

    @PostMapping("/sync")
    public ResponseEntity<String> forceSyncProductos() {
        try {
            int total = productoService.sincronizarProductos();
            return ResponseEntity.ok("Sincronización exitosa. Productos procesados: " + total);
        } catch (Exception e) {
            log.error("Error forzando sincronización de productos: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Falló la sincronización: " + e.getMessage());
        }
    }
}