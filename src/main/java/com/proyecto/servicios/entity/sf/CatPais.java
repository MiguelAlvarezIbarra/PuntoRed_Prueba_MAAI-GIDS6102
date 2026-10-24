package com.proyecto.servicios.entity.sf;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cat_paises")
@Getter
@Setter
@NoArgsConstructor
public class CatPais {

    @Id
    @Column(name = "nombre", columnDefinition = "TEXT", nullable = false)
    private String nombre;
}
