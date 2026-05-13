package com.marsemlixo.api.residuo.service;

import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.auth.repository.VoluntarioRepository;
import com.marsemlixo.api.exception.MutiraoNaoEmAndamentoException;
import com.marsemlixo.api.exception.MutiraoNotFoundException;
import com.marsemlixo.api.exception.RegistroResiduoNotFoundException;
import com.marsemlixo.api.mutirao.domain.Mutirao;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.mutirao.repository.MutiraoRepository;
import com.marsemlixo.api.residuo.controller.dto.RegistroResiduoCreateRequest;
import com.marsemlixo.api.residuo.controller.dto.RegistroResiduoResponse;
import com.marsemlixo.api.residuo.domain.RegistroResiduo;
import com.marsemlixo.api.residuo.repository.RegistroResiduoRepository;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
class RegistroResiduoServiceImpl implements RegistroResiduoService {

    private final RegistroResiduoRepository registroResiduoRepository;
    private final MutiraoRepository mutiraoRepository;
    private final VoluntarioRepository voluntarioRepository;

    RegistroResiduoServiceImpl(RegistroResiduoRepository registroResiduoRepository,
                               MutiraoRepository mutiraoRepository,
                               VoluntarioRepository voluntarioRepository) {
        this.registroResiduoRepository = registroResiduoRepository;
        this.mutiraoRepository = mutiraoRepository;
        this.voluntarioRepository = voluntarioRepository;
    }

    @Override
    public RegistroResiduoCreateResult criar(RegistroResiduoCreateRequest request, Long voluntarioId) {
        var existente = registroResiduoRepository.findById(request.id());
        if (existente.isPresent()) {
            return new RegistroResiduoCreateResult(toResponse(existente.get()), false);
        }

        Mutirao mutirao = mutiraoRepository.findById(request.mutiraoId())
                .orElseThrow(() -> new MutiraoNotFoundException(request.mutiraoId()));
        validarMutiraoEmAndamento(mutirao);
        validarLocalizacao(request.localizacao());

        Voluntario voluntario = voluntarioRepository.findById(voluntarioId)
                .orElseThrow(() -> new IllegalStateException("Voluntário autenticado não encontrado: " + voluntarioId));

        RegistroResiduo registro = new RegistroResiduo();
        registro.setId(request.id());
        registro.setMutirao(mutirao);
        registro.setVoluntario(voluntario);
        registro.setTipoResiduo(request.tipoResiduo());
        registro.setMetragemPerpendicular(request.metragemPerpendicular().setScale(2, RoundingMode.HALF_UP));
        registro.setMetragemTransversal(request.metragemTransversal().setScale(2, RoundingMode.HALF_UP));
        registro.setQuantidade(request.quantidade());
        registro.setAreaTotal(calcularAreaTotal(
                request.metragemPerpendicular(),
                request.metragemTransversal(),
                request.quantidade()));
        registro.setLocalizacao(request.localizacao());
        registro.setFotoUrl(request.fotoUrl());
        registro.setDataRegistro(request.dataRegistro());
        registro.setSyncedAt(Instant.now());

        return new RegistroResiduoCreateResult(toResponse(registroResiduoRepository.save(registro)), true);
    }

    @Override
    @Transactional(readOnly = true)
    public RegistroResiduoResponse buscarPorId(UUID id) {
        return registroResiduoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RegistroResiduoNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroResiduoResponse> listarPorMutirao(Long mutiraoId) {
        if (!mutiraoRepository.existsById(mutiraoId)) {
            throw new MutiraoNotFoundException(mutiraoId);
        }

        return registroResiduoRepository.findByMutiraoIdOrderByDataRegistroAsc(mutiraoId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validarMutiraoEmAndamento(Mutirao mutirao) {
        if (mutirao.getStatus() != MutiraoStatus.EM_ANDAMENTO) {
            throw new MutiraoNaoEmAndamentoException(mutirao.getId(), mutirao.getStatus());
        }
    }

    private void validarLocalizacao(Point localizacao) {
        if (localizacao == null) {
            throw new IllegalArgumentException("localizacao é obrigatória");
        }
        if (localizacao.getSRID() != 4326) {
            throw new IllegalArgumentException("localizacao deve usar SRID 4326");
        }

        double longitude = localizacao.getX();
        double latitude = localizacao.getY();
        if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("localizacao deve conter latitude/longitude válidas");
        }
    }

    private BigDecimal calcularAreaTotal(BigDecimal perpendicular, BigDecimal transversal, Integer quantidade) {
        return perpendicular
                .multiply(transversal)
                .multiply(BigDecimal.valueOf(quantidade.longValue()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private RegistroResiduoResponse toResponse(RegistroResiduo registro) {
        return new RegistroResiduoResponse(
                registro.getId(),
                registro.getMutirao().getId(),
                new RegistroResiduoResponse.VoluntarioResumo(
                        registro.getVoluntario().getId(),
                        registro.getVoluntario().getNome()),
                registro.getTipoResiduo(),
                registro.getMetragemPerpendicular(),
                registro.getMetragemTransversal(),
                registro.getQuantidade(),
                registro.getAreaTotal(),
                registro.getLocalizacao(),
                registro.getFotoUrl(),
                registro.getDataRegistro(),
                registro.getSyncedAt()
        );
    }
}
