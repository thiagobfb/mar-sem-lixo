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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AreaServiceTest {

    @Mock AreaRepository areaRepository;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks AreaServiceImpl service;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private Polygon poligonoValido;

    @BeforeEach
    void setUp() {
        Coordinate[] coords = {
            new Coordinate(-42.0, -22.0),
            new Coordinate(-42.1, -22.0),
            new Coordinate(-42.1, -22.1),
            new Coordinate(-42.0, -22.1),
            new Coordinate(-42.0, -22.0)
        };
        LinearRing ring = GF.createLinearRing(coords);
        poligonoValido = GF.createPolygon(ring);
    }

    @Test
    void criar_comDadosValidos_retornaAreaResponse() {
        when(areaRepository.existsByNomeAndMunicipio(any(), any())).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any())).thenReturn(true);

        Area areaSalva = areaComId(UUID.randomUUID());
        when(areaRepository.save(any())).thenReturn(areaSalva);

        var request = new AreaCreateRequest("Praia do Forte", AreaTipo.PRAIA, "Cabo Frio", "RJ", poligonoValido);

        AreaResponse response = service.criar(request);

        assertThat(response).isNotNull();
        assertThat(response.nome()).isEqualTo("Praia do Forte");
        verify(areaRepository).save(any());
    }

    @Test
    void criar_comNomeDuplicado_lancaExcecao() {
        when(areaRepository.existsByNomeAndMunicipio("Praia do Forte", "Cabo Frio")).thenReturn(true);

        var request = new AreaCreateRequest("Praia do Forte", AreaTipo.PRAIA, "Cabo Frio", "RJ", poligonoValido);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(NomeDuplicadoException.class);
        verify(areaRepository, never()).save(any());
    }

    @Test
    void criar_comPoligonoInvalido_lancaExcecao() {
        when(areaRepository.existsByNomeAndMunicipio(any(), any())).thenReturn(false);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class), any())).thenReturn(false);

        var request = new AreaCreateRequest("Praia do Forte", AreaTipo.PRAIA, "Cabo Frio", "RJ", poligonoValido);

        assertThatThrownBy(() -> service.criar(request))
                .isInstanceOf(PoligonoInvalidoException.class);
        verify(areaRepository, never()).save(any());
    }

    @Test
    void buscarPorId_existente_retornaAreaResponse() {
        UUID id = UUID.randomUUID();
        when(areaRepository.findById(id)).thenReturn(Optional.of(areaComId(id)));

        AreaResponse response = service.buscarPorId(id);

        assertThat(response.id()).isEqualTo(id);
    }

    @Test
    void buscarPorId_inexistente_lancaExcecao() {
        UUID id = UUID.randomUUID();
        when(areaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(AreaNotFoundException.class);
    }

    @Test
    void inativar_existente_marcaComoInativa() {
        UUID id = UUID.randomUUID();
        Area area = areaComId(id);
        area.setAtiva(true);
        when(areaRepository.findById(id)).thenReturn(Optional.of(area));

        service.inativar(id);

        assertThat(area.isAtiva()).isFalse();
        verify(areaRepository).save(area);
    }

    @Test
    void inativar_inexistente_lancaExcecao() {
        UUID id = UUID.randomUUID();
        when(areaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.inativar(id))
                .isInstanceOf(AreaNotFoundException.class);
    }

    @Test
    void atualizar_comNomeDuplicado_lancaExcecao() {
        UUID id = UUID.randomUUID();
        Area area = areaComId(id);
        when(areaRepository.findById(id)).thenReturn(Optional.of(area));
        when(areaRepository.existsByNomeAndMunicipioAndIdNot("Outro Nome", "Cabo Frio", id))
                .thenReturn(true);

        var request = new AreaUpdateRequest("Outro Nome", null, null, null, null);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(NomeDuplicadoException.class);
    }

    private Area areaComId(UUID id) {
        Area area = new Area();
        var idField = setFieldById(area, id);
        area.setNome("Praia do Forte");
        area.setTipo(AreaTipo.PRAIA);
        area.setMunicipio("Cabo Frio");
        area.setEstado("RJ");
        area.setPoligono(poligonoValido);
        return area;
    }

    // Seta o campo id via reflection (campo gerado pelo JPA)
    private Object setFieldById(Area area, UUID id) {
        try {
            var field = Area.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(area, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
