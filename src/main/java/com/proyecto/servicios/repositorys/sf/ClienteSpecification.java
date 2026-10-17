package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.model.ClienteSearchRequest;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
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

            // Restriccion de seguridad para usuarios normales (solo ven su propio registro)
            if (clienteIdRestringido != null) {
                filtros.add(cb.equal(root.get("id"), clienteIdRestringido));
            }

            if (request != null) {
                if (tieneValor(request.getNombre())) {
                    Expression<String> nombreCompleto = cb.concat(
                            cb.concat(root.get("nombre"), " "),
                            cb.coalesce(root.get("segundoNombre"), ""));
                    filtros.add(contiene(cb, nombreCompleto, request.getNombre()));
                }

                if (tieneValor(request.getRfc())) {
                    filtros.add(contiene(cb, root.get("rfc"), request.getRfc()));
                }

                if (tieneValor(request.getCurp())) {
                    filtros.add(contiene(cb, root.get("curp"), request.getCurp()));
                }

                if (tieneValor(request.getCorreo())) {
                    filtros.add(contiene(cb, root.get("correo"), request.getCorreo()));
                }

                if (tieneValor(request.getNumeroCuenta())) {
                    Subquery<Integer> sub = query.subquery(Integer.class);
                    Root<Cuenta> cu = sub.from(Cuenta.class);
                    sub.select(cu.get("cliente").get("id"))
                            .where(contiene(cb, cu.get("numeroCuenta"), request.getNumeroCuenta()));
                    filtros.add(root.get("id").in(sub));
                }

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

            return cb.and(filtros.toArray(new Predicate[0]));
        };
    }

    private static boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static Predicate contiene(CriteriaBuilder cb, Expression<String> campo, String valor) {
        return cb.like(cb.lower(campo), "%" + valor.trim().toLowerCase() + "%");
    }
}
