package com.marsemlixo.api.mutirao.service;

import com.marsemlixo.api.area.domain.Area;
import com.marsemlixo.api.area.domain.AreaTipo;
import com.marsemlixo.api.area.repository.AreaRepository;
import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.auth.domain.VoluntarioRole;
import com.marsemlixo.api.auth.repository.VoluntarioRepository;
import com.marsemlixo.api.exception.AreaInativaException;
import com.marsemlixo.api.exception.MutiraoNaoEditavelException;
import com.marsemlixo.api.exception.MutiraoNotFoundException;
import com.marsemlixo.api.exception.TransicaoStatusInvalidaException;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoCreateRequest;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoResponse;
import com.marsemlixo.api.mutirao.controller.dto.MutiraoUpdateRequest;
import com.marsemlixo.api.mutirao.domain.Mutirao;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.mutirao.repository.MutiraoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MutiraoServiceTest {

    @Mock MutiraoRepository mutiraoRepository;
    @Mock AreaRepository areaRepository;
    @Mock VoluntarioRepository voluntarioRepository;

    @InjectMocks MutiraoServiceImpl service;

    private Area areaAtiva;
    private Voluntario organizador;
    private Long organizadorId;

    @BeforeEach
    void setUp() {
        areaAtiva = novaArea(1L, true);
        organizadorId = 1L;
        organizador = novoVoluntario(organizadorId);
    }

    // ---- criar ----

    @Test
    void criar_comDadosValidos_retornaMutiraoResponse() {
        when(areaRepository.findById(areaAtiva.getId())).thenReturn(Optional.of(areaAtiva));
        when(voluntarioRepository.findById(organizadorId)).thenReturn(Optional.of(organizador));

        Mutirao salvo = novoMutirao(2L, areaAtiva, organizador, MutiraoStatus.PLANEJADO);
        when(mutiraoRepository.save(any())).thenReturn(salvo);

        var request = new MutiraoCreateRequest(
                "Mutirão Praia do Forte",
                LocalDate.now().plusDays(7),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                areaAtiva.getId(),
                null);

        MutiraoResponse response = service.criar(request, organizadorId);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(MutiraoStatus.PLANEJADO);
        verify(mutiraoRepository).save(any());
    }

    @Test
    void criar_comHoraFimAnteriorHoraInicio_lancaExcecao() {
        var request = new MutiraoCreateRequest(
                "Título",
                LocalDate.now().plusDays(1),
                LocalTime.of(12, 0),
                LocalTime.of(8, 0),
                999999L,
                null);

        assertThatThrownBy(() -> service.criar(request, organizadorId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("horaFim");
        verify(mutiraoRepository, never()).save(any());
    }

    @Test
    void criar_comHoraFimIgualHoraInicio_lancaExcecao() {
        var request = new MutiraoCreateRequest(
                "Título",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(8, 0),
                999999L,
                null);

        assertThatThrownBy(() -> service.criar(request, organizadorId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criar_comAreaInexistente_lancaExcecao() {
        Long areaId = 999999L;
        when(areaRepository.findById(areaId)).thenReturn(Optional.empty());

        var request = new MutiraoCreateRequest(
                "Título",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                areaId,
                null);

        assertThatThrownBy(() -> service.criar(request, organizadorId))
                .isInstanceOf(AreaInativaException.class);
        verify(mutiraoRepository, never()).save(any());
    }

    @Test
    void criar_comAreaInativa_lancaExcecao() {
        Area areaInativa = novaArea(2L, false);
        when(areaRepository.findById(areaInativa.getId())).thenReturn(Optional.of(areaInativa));

        var request = new MutiraoCreateRequest(
                "Título",
                LocalDate.now().plusDays(1),
                LocalTime.of(8, 0),
                LocalTime.of(12, 0),
                areaInativa.getId(),
                null);

        assertThatThrownBy(() -> service.criar(request, organizadorId))
                .isInstanceOf(AreaInativaException.class);
        verify(mutiraoRepository, never()).save(any());
    }

    // ---- transicionarStatus ----

    @Test
    void transicionarStatus_planejadoParaEmAndamento_retornaAtualizado() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.PLANEJADO);
        Mutirao atualizado = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.EM_ANDAMENTO);

        when(mutiraoRepository.findById(id))
                .thenReturn(Optional.of(mutirao))
                .thenReturn(Optional.of(atualizado));
        when(mutiraoRepository.transicionarStatus(id, MutiraoStatus.PLANEJADO, MutiraoStatus.EM_ANDAMENTO))
                .thenReturn(1);

        MutiraoResponse response = service.transicionarStatus(id, MutiraoStatus.EM_ANDAMENTO);

        assertThat(response.status()).isEqualTo(MutiraoStatus.EM_ANDAMENTO);
    }

    @Test
    void transicionarStatus_emAndamentoParaConcluido_retornaAtualizado() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.EM_ANDAMENTO);
        Mutirao atualizado = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.CONCLUIDO);

        when(mutiraoRepository.findById(id))
                .thenReturn(Optional.of(mutirao))
                .thenReturn(Optional.of(atualizado));
        when(mutiraoRepository.transicionarStatus(id, MutiraoStatus.EM_ANDAMENTO, MutiraoStatus.CONCLUIDO))
                .thenReturn(1);

        MutiraoResponse response = service.transicionarStatus(id, MutiraoStatus.CONCLUIDO);

        assertThat(response.status()).isEqualTo(MutiraoStatus.CONCLUIDO);
    }

    @Test
    void transicionarStatus_planejadoParaCancelado_retornaAtualizado() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.PLANEJADO);
        Mutirao atualizado = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.CANCELADO);

        when(mutiraoRepository.findById(id))
                .thenReturn(Optional.of(mutirao))
                .thenReturn(Optional.of(atualizado));
        when(mutiraoRepository.transicionarStatus(id, MutiraoStatus.PLANEJADO, MutiraoStatus.CANCELADO))
                .thenReturn(1);

        MutiraoResponse response = service.transicionarStatus(id, MutiraoStatus.CANCELADO);

        assertThat(response.status()).isEqualTo(MutiraoStatus.CANCELADO);
    }

    @Test
    void transicionarStatus_emAndamentoParaCancelado_retornaAtualizado() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.EM_ANDAMENTO);
        Mutirao atualizado = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.CANCELADO);

        when(mutiraoRepository.findById(id))
                .thenReturn(Optional.of(mutirao))
                .thenReturn(Optional.of(atualizado));
        when(mutiraoRepository.transicionarStatus(id, MutiraoStatus.EM_ANDAMENTO, MutiraoStatus.CANCELADO))
                .thenReturn(1);

        MutiraoResponse response = service.transicionarStatus(id, MutiraoStatus.CANCELADO);

        assertThat(response.status()).isEqualTo(MutiraoStatus.CANCELADO);
    }

    @Test
    void transicionarStatus_planejadoParaConcluido_lancaExcecao() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.PLANEJADO);
        when(mutiraoRepository.findById(id)).thenReturn(Optional.of(mutirao));

        assertThatThrownBy(() -> service.transicionarStatus(id, MutiraoStatus.CONCLUIDO))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void transicionarStatus_concluidoParaQualquer_lancaExcecao() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.CONCLUIDO);
        when(mutiraoRepository.findById(id)).thenReturn(Optional.of(mutirao));

        assertThatThrownBy(() -> service.transicionarStatus(id, MutiraoStatus.PLANEJADO))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void transicionarStatus_canceladoParaQualquer_lancaExcecao() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.CANCELADO);
        when(mutiraoRepository.findById(id)).thenReturn(Optional.of(mutirao));

        assertThatThrownBy(() -> service.transicionarStatus(id, MutiraoStatus.PLANEJADO))
                .isInstanceOf(TransicaoStatusInvalidaException.class);
    }

    @Test
    void transicionarStatus_conflitoConcorrente_lancaExcecao() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.PLANEJADO);
        when(mutiraoRepository.findById(id)).thenReturn(Optional.of(mutirao));
        when(mutiraoRepository.transicionarStatus(any(), any(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.transicionarStatus(id, MutiraoStatus.EM_ANDAMENTO))
                .isInstanceOf(TransicaoStatusInvalidaException.class)
                .hasMessageContaining("Conflito");
    }

    // ---- atualizar ----

    @Test
    void atualizar_comStatusPlanejado_retornaAtualizado() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.PLANEJADO);
        when(mutiraoRepository.findById(id)).thenReturn(Optional.of(mutirao));
        when(areaRepository.findById(areaAtiva.getId())).thenReturn(Optional.of(areaAtiva));
        when(mutiraoRepository.save(any())).thenReturn(mutirao);

        var request = new MutiraoUpdateRequest(
                "Novo Título",
                LocalDate.now().plusDays(10),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                areaAtiva.getId(),
                "Obs atualizada");

        MutiraoResponse response = service.atualizar(id, request);

        assertThat(response).isNotNull();
        verify(mutiraoRepository).save(any());
    }

    @Test
    void atualizar_comStatusEmAndamento_lancaExcecao() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.EM_ANDAMENTO);
        when(mutiraoRepository.findById(id)).thenReturn(Optional.of(mutirao));

        var request = new MutiraoUpdateRequest(
                "Novo Título",
                LocalDate.now().plusDays(10),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                areaAtiva.getId(),
                null);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(MutiraoNaoEditavelException.class);
        verify(mutiraoRepository, never()).save(any());
    }

    @Test
    void atualizar_comStatusConcluido_lancaExcecao() {
        Long id = 1L;
        Mutirao mutirao = novoMutirao(id, areaAtiva, organizador, MutiraoStatus.CONCLUIDO);
        when(mutiraoRepository.findById(id)).thenReturn(Optional.of(mutirao));

        var request = new MutiraoUpdateRequest(
                "Novo Título",
                LocalDate.now().plusDays(10),
                LocalTime.of(9, 0),
                LocalTime.of(13, 0),
                areaAtiva.getId(),
                null);

        assertThatThrownBy(() -> service.atualizar(id, request))
                .isInstanceOf(MutiraoNaoEditavelException.class);
    }

    // ---- buscarPorId ----

    @Test
    void buscarPorId_inexistente_lancaExcecao() {
        Long id = 1L;
        when(mutiraoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(MutiraoNotFoundException.class);
    }

    // ---- helpers ----

    private Area novaArea(Long id, boolean ativa) {
        Area area = new Area();
        setId(area, Area.class, id);
        area.setNome("Praia do Forte");
        area.setTipo(AreaTipo.PRAIA);
        area.setMunicipio("Cabo Frio");
        area.setEstado("RJ");
        area.setAtiva(ativa);
        return area;
    }

    private Voluntario novoVoluntario(Long id) {
        Voluntario v = new Voluntario();
        v.setId(id);
        v.setNome("Coordenador Teste");
        v.setEmail("coord@test.com");
        v.setGoogleId("google-id-test");
        v.setRole(VoluntarioRole.COORDENADOR);
        v.setDataCadastro(Instant.now());
        return v;
    }

    private Mutirao novoMutirao(Long id, Area area, Voluntario org, MutiraoStatus status) {
        Mutirao m = new Mutirao();
        setId(m, Mutirao.class, id);
        m.setTitulo("Mutirão Teste");
        m.setData(LocalDate.now().plusDays(7));
        m.setHoraInicio(LocalTime.of(8, 0));
        m.setHoraFim(LocalTime.of(12, 0));
        m.setArea(area);
        m.setOrganizador(org);
        m.setStatus(status);
        return m;
    }

    private <T> void setId(T obj, Class<T> clazz, Long id) {
        try {
            var field = clazz.getDeclaredField("id");
            field.setAccessible(true);
            field.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
