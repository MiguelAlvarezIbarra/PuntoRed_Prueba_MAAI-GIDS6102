package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Busqueda dinamica de clientes. Solo se agrega un filtro por cada campo que
 * venga con valor; todos se combinan con AND y buscan por coincidencia parcial
 * (LIKE '%valor%') sin distinguir mayusculas/minusculas.
 */
public final class ClienteSpecification {

    private ClienteSpecification() {
    }

    public static Specification<Cliente> buscar(String nombre, String rfc, String curp,
                                                String cuenta, String correo) {
        return (root, query, cb) -> {
            List<Predicate> filtros = new ArrayList<>();

            if (tieneValor(nombre)) {
                // nombre + segundo nombre, para que "Ana Sofia" encuentre a quien tenga ambos
                Expression<String> nombreCompleto = cb.concat(
                        cb.concat(root.get("nombre"), " "),
                        cb.coalesce(root.get("segundoNombre"), ""));
                filtros.add(contiene(cb, nombreCompleto, nombre));
            }
            if (tieneValor(rfc)) {
                filtros.add(contiene(cb, root.get("rfc"), rfc));
            }
            if (tieneValor(curp)) {
                filtros.add(contiene(cb, root.get("curp"), curp));
            }
            if (tieneValor(correo)) {
                filtros.add(contiene(cb, root.get("correo"), correo));
            }
            if (tieneValor(cuenta)) {
                // Subquery para no duplicar clientes con varias cuentas
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<Cuenta> cu = sub.from(Cuenta.class);
                sub.select(cu.get("cliente").get("id"))
                        .where(contiene(cb, cu.get("numeroCuenta"), cuenta));
                filtros.add(root.get("id").in(sub));
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
