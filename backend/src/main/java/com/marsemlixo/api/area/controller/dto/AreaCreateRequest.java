package com.marsemlixo.api.area.controller.dto;

import com.marsemlixo.api.area.domain.AreaTipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.locationtech.jts.geom.Polygon;

public record AreaCreateRequest(
        @NotBlank String nome,
        @NotNull AreaTipo tipo,
        @NotBlank String municipio,
        @NotBlank @Size(min = 2, max = 2) String estado,
        @NotNull Polygon poligono
) {}
