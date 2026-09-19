package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoServiceClient;
import com.proyecto.servicios.entity.Producto;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.repositorys.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private GestoPagoServiceClient gestoPagoServiceClient;
    @Mock
    private GestoPagoTokenService gestoPagoTokenService;
    @Mock
    private ProductoRepository productoRepository;

    private ProductoService productoService;
    private GestoPagoToken tokenActivo;

    private static final String XML_UN_PRODUCTO =
            "<?xml version='1.0' encoding='UTF-8'?>" +
            "<RESPONSE><PRODUCTOS><producto " +
                    "idProducto=\"101\" " +
                    "idServicio=\"5\" " +
                    "idCatTipoServicio=\"2\" " +
                    "tipoFront=\"1\" " +
                    "tipoReferencia=\"a\" " +
                    "precio=\"150.00\">" +
                    "Recarga Telcel" +
                    "</producto></PRODUCTOS>" +
                    "<MENSAJE><CODIGO>01</CODIGO><TEXTO>OK</TEXTO></MENSAJE>" +
                    "</RESPONSE>";

    private static final String XML_RESPUESTA_VACIA =
            "<?xml version='1.0' encoding='UTF-8'?>" +
            "<RESPONSE><MENSAJE><CODIGO>99</CODIGO><TEXTO>Sin productos</TEXTO></MENSAJE></RESPONSE>";

    @BeforeEach
    void setUp() {
        productoService = new ProductoService(gestoPagoServiceClient, gestoPagoTokenService, productoRepository);
        tokenActivo = new GestoPagoToken();
        tokenActivo.setToken("token-valido");
    }

    @Test
    void sincronizarProductos_guardaProductoCuandoTodoSaleBien() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoServiceClient.getProductList("Bearer token-valido")).thenReturn(XML_UN_PRODUCTO);
        when(productoRepository.findByIdProductoAndIdServicio(101, 5)).thenReturn(Optional.empty());

        productoService.sincronizarProductos();

        verify(productoRepository).saveAll(argThat((Iterable<Producto> lista) -> {
            Producto p = lista.iterator().next();
            return p.getIdProducto().equals(101);
        }));
        verify(gestoPagoTokenService, never()).renovarToken();
    }

    @Test
    void sincronizarProductos_intentaRenovarTokenCuandoNoHayActivo() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.empty());
        // Después de renovar, sigue sin encontrar token
        doNothing().when(gestoPagoTokenService).renovarToken();

        productoService.sincronizarProductos();

        verify(gestoPagoTokenService).renovarToken();
        // No se debe llamar al servicio de productos porque no hay token
        verify(gestoPagoServiceClient, never()).getProductList(any());
    }

    @Test
    void sincronizarProductos_noFallaCuandoRespuestaEsVacia() {
        when(gestoPagoTokenService.obtenerTokenActual()).thenReturn(Optional.of(tokenActivo));
        when(gestoPagoServiceClient.getProductList("Bearer token-valido")).thenReturn(XML_RESPUESTA_VACIA);

        // No debería lanzar excepción
        productoService.sincronizarProductos();

        verify(productoRepository, never()).saveAll(any());
    }

    @Test
    void obtenerTodosLosProductos_retornaListaDesdeRepositorio() {
        Producto p = Producto.builder().idProducto(1).idServicio(1).nombreProducto("Test").nombreServicio("Test").build();
        when(productoRepository.findAll()).thenReturn(List.of(p));

        List<Producto> resultado = productoService.obtenerTodosLosProductos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombreProducto()).isEqualTo("Test");
    }
}