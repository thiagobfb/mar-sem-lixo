package com.marsemlixo.api.area.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marsemlixo.api.area.domain.AreaTipo;
import org.locationtech.jts.geom.Polygon;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AreaResponse(
        UUID id,
        String nome,
        AreaTipo tipo,
        String municipio,
        String estado,
        Polygon poligono,
        boolean ativa
) {
}
