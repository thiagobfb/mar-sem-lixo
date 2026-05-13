package com.marsemlixo.api.residuo.controller.dto;

import com.marsemlixo.api.residuo.domain.TipoResiduo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RegistroResiduoCreateRequest(
        @NotNull UUID id,
        @NotNull Long mutiraoId,
        @NotNull TipoResiduo tipoResiduo,
        @NotNull @Positive BigDecimal metragemPerpendicular,
        @NotNull @Positive BigDecimal metragemTransversal,
        @NotNull @Positive Integer quantidade,
        @NotNull Point localizacao,
        String fotoUrl,
        @NotNull Instant dataRegistro
) {}
