package com.marsemlixo.api.residuo.service;

import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.auth.domain.VoluntarioRole;
import com.marsemlixo.api.auth.repository.VoluntarioRepository;
import com.marsemlixo.api.exception.MutiraoNaoEmAndamentoException;
import com.marsemlixo.api.exception.MutiraoNotFoundException;
import com.marsemlixo.api.mutirao.domain.Mutirao;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.mutirao.repository.MutiraoRepository;
import com.marsemlixo.api.residuo.controller.dto.RegistroResiduoCreateRequest;
import com.marsemlixo.api.residuo.domain.RegistroResiduo;
import com.marsemlixo.api.residuo.domain.TipoResiduo;
import com.marsemlixo.api.residuo.repository.RegistroResiduoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistroResiduoServiceTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock RegistroResiduoRepository registroResiduoRepository;
    @Mock MutiraoRepository mutiraoRepository;
    @Mock VoluntarioRepository voluntarioRepository;

    @InjectMocks RegistroResiduoServiceImpl service;

    private Mutirao mutiraoEmAndamento;
    private Voluntario voluntario;
    private Point localizacao;

    @BeforeEach
    void setUp() {
        voluntario = novoVoluntario(7L);
        mutiraoEmAndamento = novoMutirao(42L, MutiraoStatus.EM_ANDAMENTO);
        localizacao = GF.createPoint(new Coordinate(-42.0432, -22.8791));
    }

    @Test
    void criar_comDadosValidos_calculaAreaTotalERetornaCreated() {
        UUID id = UUID.randomUUID();
        RegistroResiduo salvo = novoRegistro(id, mutiraoEmAndamento, voluntario);

        when(registroResiduoRepository.findById(id)).thenReturn(Optional.empty());
        when(mutiraoRepository.findById(mutiraoEmAndamento.getId())).thenReturn(Optional.of(mutiraoEmAndamento));
        when(voluntarioRepository.findById(voluntario.getId())).thenReturn(Optional.of(voluntario));
        when(registroResiduoRepository.save(any())).thenReturn(salvo);

        RegistroResiduoCreateResult result = service.criar(novoRequest(id), voluntario.getId());

        assertThat(result.created()).isTrue();
        assertThat(result.response().areaTotal()).isEqualByComparingTo("9.00");
    }

    @Test
    void criar_comMesmoId_retornaExistenteSemDuplicar() {
        UUID id = UUID.randomUUID();
        RegistroResiduo existente = novoRegistro(id, mutiraoEmAndamento, voluntario);

        when(registroResiduoRepository.findById(id)).thenReturn(Optional.of(existente));

        RegistroResiduoCreateResult result = service.criar(novoRequest(id), voluntario.getId());

        assertThat(result.created()).isFalse();
        verify(registroResiduoRepository, never()).save(any());
        verify(mutiraoRepository, never()).findById(any());
    }

    @Test
    void criar_comMutiraoPlanejado_lancaExcecao() {
        UUID id = UUID.randomUUID();
        Mutirao mutiraoPlanejado = novoMutirao(42L, MutiraoStatus.PLANEJADO);

        when(registroResiduoRepository.findById(id)).thenReturn(Optional.empty());
        when(mutiraoRepository.findById(mutiraoPlanejado.getId())).thenReturn(Optional.of(mutiraoPlanejado));

        assertThatThrownBy(() -> service.criar(novoRequest(id), voluntario.getId()))
                .isInstanceOf(MutiraoNaoEmAndamentoException.class);
    }

    @Test
    void criar_comMutiraoInexistente_lancaExcecao() {
        UUID id = UUID.randomUUID();

        when(registroResiduoRepository.findById(id)).thenReturn(Optional.empty());
        when(mutiraoRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(novoRequest(id), voluntario.getId()))
                .isInstanceOf(MutiraoNotFoundException.class);
    }

    @Test
    void listarPorMutirao_existente_retornaRegistros() {
        UUID id = UUID.randomUUID();
        RegistroResiduo registro = novoRegistro(id, mutiraoEmAndamento, voluntario);

        when(mutiraoRepository.existsById(mutiraoEmAndamento.getId())).thenReturn(true);
        when(registroResiduoRepository.findByMutiraoIdOrderByDataRegistroAsc(mutiraoEmAndamento.getId()))
                .thenReturn(List.of(registro));

        var response = service.listarPorMutirao(mutiraoEmAndamento.getId());

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().id()).isEqualTo(id);
    }

    private RegistroResiduoCreateRequest novoRequest(UUID id) {
        return new RegistroResiduoCreateRequest(
                id,
                42L,
                TipoResiduo.PLASTICO,
                new BigDecimal("2.50"),
                new BigDecimal("1.20"),
                3,
                localizacao,
                "https://example.com/foto.jpg",
                Instant.parse("2025-06-14T10:32:11Z")
        );
    }

    private RegistroResiduo novoRegistro(UUID id, Mutirao mutirao, Voluntario voluntario) {
        RegistroResiduo registro = new RegistroResiduo();
        registro.setId(id);
        registro.setMutirao(mutirao);
        registro.setVoluntario(voluntario);
        registro.setTipoResiduo(TipoResiduo.PLASTICO);
        registro.setMetragemPerpendicular(new BigDecimal("2.50"));
        registro.setMetragemTransversal(new BigDecimal("1.20"));
        registro.setQuantidade(3);
        registro.setAreaTotal(new BigDecimal("9.00"));
        registro.setLocalizacao(localizacao);
        registro.setFotoUrl("https://example.com/foto.jpg");
        registro.setDataRegistro(Instant.parse("2025-06-14T10:32:11Z"));
        registro.setSyncedAt(Instant.parse("2025-06-14T10:33:02Z"));
        return registro;
    }

    private Mutirao novoMutirao(Long id, MutiraoStatus status) {
        Mutirao mutirao = new Mutirao();
        setField(mutirao, "id", id);
        mutirao.setTitulo("Mutirão Teste");
        mutirao.setData(LocalDate.now());
        mutirao.setHoraInicio(LocalTime.of(8, 0));
        mutirao.setHoraFim(LocalTime.of(12, 0));
        mutirao.setStatus(status);
        return mutirao;
    }

    private Voluntario novoVoluntario(Long id) {
        Voluntario v = new Voluntario();
        v.setId(id);
        v.setGoogleId("google-" + id);
        v.setEmail("user" + id + "@test.com");
        v.setNome("Usuário " + id);
        v.setRole(VoluntarioRole.VOLUNTARIO);
        v.setDataCadastro(Instant.now());
        return v;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
