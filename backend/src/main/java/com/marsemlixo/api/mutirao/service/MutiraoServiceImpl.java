package com.marsemlixo.api.mutirao.service;

import com.marsemlixo.api.area.domain.Area;
import com.marsemlixo.api.area.repository.AreaRepository;
import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.auth.repository.VoluntarioRepository;
import com.marsemlixo.api.exception.AreaInativaException;
import com.marsemlixo.api.exception.MutiraoNaoEditavelException;
import com.marsemlixo.api.exception.MutiraoNotFoundException;
import com.marsemlixo.api.exception.TransicaoStatusInvalidaException;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoCreateRequest;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoResponse;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoSummaryResponse;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoUpdateRequest;
import com.marsemlixo.api.mutirao.domain.Mutirao;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.mutirao.repository.MutiraoRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
class MutiraoServiceImpl implements MutiraoService {

    private final MutiraoRepository mutiraoRepository;
    private final AreaRepository areaRepository;
    private final VoluntarioRepository voluntarioRepository;

    MutiraoServiceImpl(MutiraoRepository mutiraoRepository,
                       AreaRepository areaRepository,
                       VoluntarioRepository voluntarioRepository) {
        this.mutiraoRepository = mutiraoRepository;
        this.areaRepository = areaRepository;
        this.voluntarioRepository = voluntarioRepository;
    }

    @Override
    public MutiraoResponse criar(MutiraoCreateRequest request, Long organizadorId) {
        validarHorario(request.horaInicio(), request.horaFim());

        Area area = buscarAreaAtiva(request.areaId());
        Voluntario organizador = voluntarioRepository.findById(organizadorId)
                .orElseThrow(() -> new IllegalStateException("Voluntário autenticado não encontrado: " + organizadorId));

        Mutirao mutirao = new Mutirao();
        mutirao.setTitulo(request.titulo());
        mutirao.setData(request.data());
        mutirao.setHoraInicio(request.horaInicio());
        mutirao.setHoraFim(request.horaFim());
        mutirao.setArea(area);
        mutirao.setOrganizador(organizador);
        mutirao.setObservacoes(request.observacoes());

        return toResponse(mutiraoRepository.save(mutirao));
    }

    @Override
    @Transactional(readOnly = true)
    public MutiraoResponse buscarPorId(Long id) {
        return mutiraoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new MutiraoNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MutiraoSummaryResponse> listar(MutiraoStatus status, Long areaId,
                                               LocalDate dataInicio, LocalDate dataFim,
                                               Pageable pageable) {
        Specification<Mutirao> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (areaId != null) predicates.add(cb.equal(root.get("area").get("id"), areaId));
            if (dataInicio != null) predicates.add(cb.greaterThanOrEqualTo(root.get("data"), dataInicio));
            if (dataFim != null) predicates.add(cb.lessThanOrEqualTo(root.get("data"), dataFim));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return mutiraoRepository.findAll(spec, pageable).map(this::toSummaryResponse);
    }

    @Override
    public MutiraoResponse atualizar(Long id, MutiraoUpdateRequest request) {
        Mutirao mutirao = mutiraoRepository.findById(id)
                .orElseThrow(() -> new MutiraoNotFoundException(id));

        if (mutirao.getStatus() != MutiraoStatus.PLANEJADO) {
            throw new MutiraoNaoEditavelException(mutirao.getStatus());
        }

        validarHorario(request.horaInicio(), request.horaFim());
        Area area = buscarAreaAtiva(request.areaId());

        mutirao.setTitulo(request.titulo());
        mutirao.setData(request.data());
        mutirao.setHoraInicio(request.horaInicio());
        mutirao.setHoraFim(request.horaFim());
        mutirao.setArea(area);
        mutirao.setObservacoes(request.observacoes());

        return toResponse(mutiraoRepository.save(mutirao));
    }

    @Override
    public MutiraoResponse transicionarStatus(Long id, MutiraoStatus novoStatus) {
        Mutirao mutirao = mutiraoRepository.findById(id)
                .orElseThrow(() -> new MutiraoNotFoundException(id));

        validarTransicao(mutirao.getStatus(), novoStatus);

        int rowsUpdated = mutiraoRepository.transicionarStatus(id, mutirao.getStatus(), novoStatus);
        if (rowsUpdated == 0) {
            throw new TransicaoStatusInvalidaException(
                    "Conflito: o status do mutirão foi alterado por outro usuário. Recarregue e tente novamente.");
        }

        return toResponse(mutiraoRepository.findById(id)
                .orElseThrow(() -> new MutiraoNotFoundException(id)));
    }

    private void validarHorario(LocalTime horaInicio, LocalTime horaFim) {
        if (!horaFim.isAfter(horaInicio)) {
            throw new IllegalArgumentException("horaFim deve ser posterior a horaInicio");
        }
    }

    private void validarTransicao(MutiraoStatus atual, MutiraoStatus novo) {
        boolean valida = switch (atual) {
            case PLANEJADO -> novo == MutiraoStatus.EM_ANDAMENTO || novo == MutiraoStatus.CANCELADO;
            case EM_ANDAMENTO -> novo == MutiraoStatus.CONCLUIDO || novo == MutiraoStatus.CANCELADO;
            case CONCLUIDO, CANCELADO -> false;
        };
        if (!valida) {
            throw new TransicaoStatusInvalidaException(atual, novo);
        }
    }

    private Area buscarAreaAtiva(Long areaId) {
        return areaRepository.findById(areaId)
                .filter(Area::isAtiva)
                .orElseThrow(() -> new AreaInativaException(areaId));
    }

    private MutiraoResponse toResponse(Mutirao m) {
        var areaResumo = new MutiraoResponse.AreaResumo(
                m.getArea().getId(),
                m.getArea().getNome(),
                m.getArea().getTipo(),
                m.getArea().getMunicipio());

        var orgResumo = new MutiraoResponse.OrganizadorResumo(
                m.getOrganizador().getId(),
                m.getOrganizador().getNome());

        return new MutiraoResponse(
                m.getId(),
                m.getTitulo(),
                m.getData(),
                m.getHoraInicio(),
                m.getHoraFim(),
                m.getStatus(),
                m.getObservacoes(),
                areaResumo,
                orgResumo);
    }

    private MutiraoSummaryResponse toSummaryResponse(Mutirao m) {
        var areaResumo = new MutiraoSummaryResponse.AreaResumo(
                m.getArea().getId(),
                m.getArea().getNome(),
                m.getArea().getTipo(),
                m.getArea().getMunicipio());

        return new MutiraoSummaryResponse(
                m.getId(),
                m.getTitulo(),
                m.getData(),
                m.getHoraInicio(),
                m.getHoraFim(),
                m.getStatus(),
                areaResumo);
    }
}
