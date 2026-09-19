package com.proyecto.servicios.dto.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {

    private final int codigo;
    private final String mensaje;
    private final T data;

    private ApiResponse(int codigo, String mensaje, T data) {
        this.codigo = codigo;
        this.mensaje = mensaje;
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ResponseCode.OK.getCodigo(), ResponseCode.OK.getMensaje(), data);
    }

    public static <T> ApiResponse<T> error(ResponseCode code) {
        return new ApiResponse<>(code.getCodigo(), code.getMensaje(), null);
    }

    public static <T> ApiResponse<T> error(ResponseCode code, String mensajeDetalle) {
        return new ApiResponse<>(code.getCodigo(), mensajeDetalle, null);
    }
}