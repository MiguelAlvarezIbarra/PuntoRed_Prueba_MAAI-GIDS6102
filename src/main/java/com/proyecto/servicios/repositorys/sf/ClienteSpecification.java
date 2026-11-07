package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.model.ClienteSearchRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Busqueda dinamica de clientes mediante Criteria API de JPA.
 * - Solo se agrega un filtro por cada campo que venga con valor.
 * - Todos los filtros activos se combinan con el operador AND.
 * - Busquedas de texto utilizan coincidencia parcial (LIKE '%valor%') insensible a mayusculas/minusculas.
 * - Permite rangos de fechas de registro y filtros por estatus activo.
 */
public final class ClienteSpecification {

    private ClienteSpecification() {
    }

    public static Specification<Cliente> buscar(ClienteSearchRequest request, Integer clienteIdRestringido) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();

            if (clienteIdRestringido != null) {
                filtros.add(cb.equal(root.get("id"), clienteIdRestringido));
            }

            if (request != null) {
                agregarFiltrosTexto(request, root, cb, filtros);
                agregarFiltroCuenta(request, root, query, cb, filtros);
                agregarFiltrosFechaYEstatus(request, root, cb, filtros);
            }

            return cb.and(filtros.toArray(new Predicate[0]));
        };
    }

    private static void agregarFiltrosTexto(
            ClienteSearchRequest request,
            Root<Cliente> root,
            CriteriaBuilder cb,
            List<Predicate> filtros) {

        if (tieneValor(request.getNombre())) {
            Expression<String> nombreCompleto = cb.concat(
                    cb.concat(root.get("nombre"), " "),
                    cb.coalesce(root.get("segundoNombre"), ""));
            filtros.add(contiene(cb, nombreCompleto, request.getNombre()));
        }

        agregarSiTieneValor(filtros, cb, root, "rfc", request.getRfc());
        agregarSiTieneValor(filtros, cb, root, "curp", request.getCurp());
        agregarSiTieneValor(filtros, cb, root, "correo", request.getCorreo());
    }

    private static void agregarSiTieneValor(
            List<Predicate> filtros,
            CriteriaBuilder cb,
            Root<Cliente> root,
            String atributo,
            String valor) {

        if (tieneValor(valor)) {
            filtros.add(contiene(cb, root.get(atributo), valor));
        }
    }

    private static void agregarFiltroCuenta(
            ClienteSearchRequest request,
            Root<Cliente> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            List<Predicate> filtros) {

        if (tieneValor(request.getNumeroCuenta())) {
            Subquery<Integer> sub = query.subquery(Integer.class);
            Root<Cuenta> cu = sub.from(Cuenta.class);
            sub.select(cu.get("cliente").get("id"))
                    .where(contiene(cb, cu.get("numeroCuenta"), request.getNumeroCuenta()));
            filtros.add(root.get("id").in(sub));
        }
    }

    private static void agregarFiltrosFechaYEstatus(
            ClienteSearchRequest request,
            Root<Cliente> root,
            CriteriaBuilder cb,
            List<Predicate> filtros) {

        if (request.getFechaDesde() != null) {
            LocalDateTime desdeDT = request.getFechaDesde().atStartOfDay();
            filtros.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desdeDT));
        }

        if (request.getFechaHasta() != null) {
            LocalDateTime hastaDT = request.getFechaHasta().atTime(23, 59, 59);
            filtros.add(cb.lessThanOrEqualTo(root.get("fechaRegistro"), hastaDT));
        }

        if (request.getActivo() != null) {
            filtros.add(cb.equal(root.get("activo"), request.getActivo()));
        }
    }

    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static Predicate contiene(CriteriaBuilder cb, Expression<String> campo, String valor) {
        return cb.like(cb.lower(campo), "%" + valor.trim().toLowerCase() + "%");
    }
}
