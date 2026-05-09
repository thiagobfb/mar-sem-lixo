package com.marsemlixo.api.area.controller.dto;

import com.marsemlixo.api.area.domain.AreaTipo;
import jakarta.validation.constraints.Size;
import org.locationtech.jts.geom.Polygon;

public record AreaUpdateRequest(
        @Size(min = 1) String nome,
        AreaTipo tipo,
        @Size(min = 1) String municipio,
        @Size(min = 2, max = 2) String estado,
        Polygon poligono
) {}
