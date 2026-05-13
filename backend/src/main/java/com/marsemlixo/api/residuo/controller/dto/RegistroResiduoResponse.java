package com.marsemlixo.api.residuo.controller.dto;

import com.marsemlixo.api.residuo.domain.TipoResiduo;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegistroResiduoResponse(
        UUID id,
        Long mutiraoId,
        VoluntarioResumo voluntario,
        TipoResiduo tipoResiduo,
        BigDecimal metragemPerpendicular,
        BigDecimal metragemTransversal,
        Integer quantidade,
        BigDecimal areaTotal,
        Point localizacao,
        String fotoUrl,
        Instant dataRegistro,
        Instant syncedAt
) {
    public record VoluntarioResumo(Long id, String nome) {}
}
