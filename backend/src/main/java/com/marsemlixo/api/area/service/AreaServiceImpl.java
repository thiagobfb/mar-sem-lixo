package com.marsemlixo.api.area.service;

import com.marsemlixo.api.area.controller.dto.AreaCreateRequest;
import com.marsemlixo.api.area.controller.dto.AreaResponse;
import com.marsemlixo.api.area.controller.dto.AreaUpdateRequest;
import com.marsemlixo.api.area.domain.Area;
import com.marsemlixo.api.area.domain.AreaTipo;
import com.marsemlixo.api.area.repository.AreaRepository;
import com.marsemlixo.api.exception.AreaNotFoundException;
import com.marsemlixo.api.exception.NomeDuplicadoException;
import com.marsemlixo.api.exception.PoligonoInvalidoException;
import jakarta.persistence.criteria.Predicate;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;
    private final JdbcTemplate jdbcTemplate;

    AreaServiceImpl(AreaRepository areaRepository, JdbcTemplate jdbcTemplate) {
        this.areaRepository = areaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AreaResponse criar(AreaCreateRequest request) {
        if (areaRepository.existsByNomeAndMunicipio(request.nome(), request.municipio())) {
            throw new NomeDuplicadoException(request.nome(), request.municipio());
        }
        validarPoligono(request.poligono());

        Area area = new Area();
        area.setNome(request.nome());
        area.setTipo(request.tipo());
        area.setMunicipio(request.municipio());
        area.setEstado(request.estado().toUpperCase());
        area.setPoligono(request.poligono());

        return toResponse(areaRepository.save(area));
    }

    @Override
    @Transactional(readOnly = true)
    public AreaResponse buscarPorId(UUID id) {
        return areaRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new AreaNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AreaResponse> listar(Boolean ativa, AreaTipo tipo, String municipio, String estado, boolean incluirPoligono) {
        Specification<Area> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ativa != null) predicates.add(cb.equal(root.get("ativa"), ativa));
            if (tipo != null) predicates.add(cb.equal(root.get("tipo"), tipo));
            if (municipio != null) predicates.add(cb.equal(cb.lower(root.get("municipio")), municipio.toLowerCase()));
            if (estado != null) predicates.add(cb.equal(cb.lower(root.get("estado")), estado.toLowerCase()));
            query.orderBy(cb.asc(root.get("nome")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return areaRepository.findAll(spec).stream()
                .map(incluirPoligono ? this::toResponse : this::toResponseSemPoligono)
                .toList();
    }

    @Override
    public AreaResponse atualizar(UUID id, AreaUpdateRequest request) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new AreaNotFoundException(id));

        if (request.nome() != null) {
            if (areaRepository.existsByNomeAndMunicipioAndIdNot(
                    request.nome(),
                    request.municipio() != null ? request.municipio() : area.getMunicipio(),
                    id)) {
                throw new NomeDuplicadoException(request.nome(),
                        request.municipio() != null ? request.municipio() : area.getMunicipio());
            }
            area.setNome(request.nome());
        }
        if (request.tipo() != null) area.setTipo(request.tipo());
        if (request.municipio() != null) area.setMunicipio(request.municipio());
        if (request.estado() != null) area.setEstado(request.estado().toUpperCase());
        if (request.poligono() != null) {
            validarPoligono(request.poligono());
            area.setPoligono(request.poligono());
        }

        return toResponse(areaRepository.save(area));
    }

    @Override
    public void inativar(UUID id) {
        Area area = areaRepository.findById(id)
                .orElseThrow(() -> new AreaNotFoundException(id));
        area.setAtiva(false);
        areaRepository.save(area);
    }

    private AreaResponse toResponse(Area area) {
        return new AreaResponse(area.getId(), area.getNome(), area.getTipo(),
                area.getMunicipio(), area.getEstado(), area.getPoligono(), area.isAtiva());
    }

    private AreaResponse toResponseSemPoligono(Area area) {
        return new AreaResponse(area.getId(), area.getNome(), area.getTipo(),
                area.getMunicipio(), area.getEstado(), null, area.isAtiva());
    }

    private void validarPoligono(Polygon poligono) {
        String wkt = new WKTWriter().write(poligono);
        Boolean valido = jdbcTemplate.queryForObject(
                "SELECT ST_IsValid(ST_GeomFromText(?, 4326))", Boolean.class, wkt);
        if (!Boolean.TRUE.equals(valido)) {
            throw new PoligonoInvalidoException("self-intersection ou geometria mal formada");
        }
    }
}
