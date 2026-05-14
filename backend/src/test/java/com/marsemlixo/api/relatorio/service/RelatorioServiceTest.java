package com.marsemlixo.api.relatorio.service;

import com.marsemlixo.api.area.domain.Area;
import com.marsemlixo.api.area.domain.AreaTipo;
import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.auth.domain.VoluntarioRole;
import com.marsemlixo.api.exception.MutiraoNotFoundException;
import com.marsemlixo.api.exception.RelatorioNaoDisponivelException;
import com.marsemlixo.api.mutirao.domain.Mutirao;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.mutirao.repository.MutiraoRepository;
import com.marsemlixo.api.relatorio.controller.dto.RelatorioMutiraoResponse;
import com.marsemlixo.api.residuo.domain.RegistroResiduo;
import com.marsemlixo.api.residuo.domain.TipoResiduo;
import com.marsemlixo.api.residuo.repository.RegistroResiduoRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelatorioServiceTest {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @Mock MutiraoRepository mutiraoRepository;
    @Mock RegistroResiduoRepository registroResiduoRepository;

    @InjectMocks RelatorioServiceImpl service;

    private Mutirao mutiraoConcluido;
    private Area area;
    private Voluntario voluntario1;
    private Voluntario voluntario2;

    @BeforeEach
    void setUp() {
        area = novaArea(10L);
        voluntario1 = novoVoluntario(7L, "Ana Souza");
        voluntario2 = novoVoluntario(8L, "Carlos Lima");
        mutiraoConcluido = novoMutirao(42L, MutiraoStatus.CONCLUIDO, area);
    }

    @Test
    void gerarResumo_comRegistros_agregaTotaisCorretamente() {
        List<RegistroResiduo> registros = List.of(
                novoRegistro(UUID.randomUUID(), mutiraoConcluido, voluntario1, TipoResiduo.PLASTICO, "9.00", 3,
                        Instant.parse("2025-06-14T10:32:11Z")),
                novoRegistro(UUID.randomUUID(), mutiraoConcluido, voluntario2, TipoResiduo.PLASTICO, "4.50", 2,
                        Instant.parse("2025-06-14T10:40:11Z")),
                novoRegistro(UUID.randomUUID(), mutiraoConcluido, voluntario1, TipoResiduo.METAL, "2.00", 1,
                        Instant.parse("2025-06-14T11:00:11Z"))
        );

        when(mutiraoRepository.findById(mutiraoConcluido.getId())).thenReturn(Optional.of(mutiraoConcluido));
        when(registroResiduoRepository.findByMutiraoIdOrderByDataRegistroAsc(mutiraoConcluido.getId()))
                .thenReturn(registros);

        RelatorioMutiraoResponse response = service.gerarResumoPorMutirao(mutiraoConcluido.getId());

        assertThat(response.mutirao().titulo()).isEqualTo("Mutirão Praia do Forte");
        assertThat(response.resumo().totalRegistros()).isEqualTo(3);
        assertThat(response.resumo().totalItens()).isEqualTo(6);
        assertThat(response.resumo().areaTotal()).isEqualByComparingTo("15.50");
        assertThat(response.resumo().voluntariosDistintos()).isEqualTo(2);
        assertThat(response.resumo().primeiroRegistroEm()).isEqualTo(Instant.parse("2025-06-14T10:32:11Z"));
        assertThat(response.resumo().ultimoRegistroEm()).isEqualTo(Instant.parse("2025-06-14T11:00:11Z"));
        assertThat(response.totaisPorTipo()).hasSize(2);
        assertThat(response.totaisPorTipo().getFirst().tipoResiduo()).isEqualTo(TipoResiduo.PLASTICO);
        assertThat(response.totaisPorTipo().getFirst().areaTotal()).isEqualByComparingTo("13.50");
    }

    @Test
    void gerarResumo_semRegistros_retornaTotaisZerados() {
        when(mutiraoRepository.findById(mutiraoConcluido.getId())).thenReturn(Optional.of(mutiraoConcluido));
        when(registroResiduoRepository.findByMutiraoIdOrderByDataRegistroAsc(mutiraoConcluido.getId()))
                .thenReturn(List.of());

        RelatorioMutiraoResponse response = service.gerarResumoPorMutirao(mutiraoConcluido.getId());

        assertThat(response.resumo().totalRegistros()).isZero();
        assertThat(response.resumo().totalItens()).isZero();
        assertThat(response.resumo().areaTotal()).isEqualByComparingTo("0.00");
        assertThat(response.resumo().voluntariosDistintos()).isZero();
        assertThat(response.resumo().primeiroRegistroEm()).isNull();
        assertThat(response.resumo().ultimoRegistroEm()).isNull();
        assertThat(response.totaisPorTipo()).isEmpty();
    }

    @Test
    void exportarExcel_comMutiraoConcluido_geraWorkbookValido() throws Exception {
        List<RegistroResiduo> registros = List.of(
                novoRegistro(UUID.randomUUID(), mutiraoConcluido, voluntario1, TipoResiduo.PLASTICO, "9.00", 3,
                        Instant.parse("2025-06-14T10:32:11Z"))
        );

        when(mutiraoRepository.findById(mutiraoConcluido.getId())).thenReturn(Optional.of(mutiraoConcluido));
        when(registroResiduoRepository.findByMutiraoIdOrderByDataRegistroAsc(mutiraoConcluido.getId()))
                .thenReturn(registros);

        RelatorioExcelFile arquivo = service.exportarExcelPorMutirao(mutiraoConcluido.getId());

        assertThat(arquivo.fileName()).isEqualTo("relatorio-mutirao-42.xlsx");
        assertThat(arquivo.content()).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(arquivo.content()))) {
            assertThat(workbook.getSheet("Resumo")).isNotNull();
            assertThat(workbook.getSheet("Registros")).isNotNull();
            assertThat(workbook.getSheet("Registros").getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo("PLASTICO");
        }
    }

    @Test
    void exportarExcel_comMutiraoNaoConcluido_lancaExcecao() {
        Mutirao mutiraoEmAndamento = novoMutirao(43L, MutiraoStatus.EM_ANDAMENTO, area);
        when(mutiraoRepository.findById(mutiraoEmAndamento.getId())).thenReturn(Optional.of(mutiraoEmAndamento));

        assertThatThrownBy(() -> service.exportarExcelPorMutirao(mutiraoEmAndamento.getId()))
                .isInstanceOf(RelatorioNaoDisponivelException.class);
    }

    @Test
    void gerarResumo_comMutiraoInexistente_lancaExcecao() {
        when(mutiraoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.gerarResumoPorMutirao(999L))
                .isInstanceOf(MutiraoNotFoundException.class);
    }

    private Area novaArea(Long id) {
        Area area = new Area();
        setField(area, "id", id);
        area.setNome("Praia do Forte");
        area.setTipo(AreaTipo.PRAIA);
        area.setMunicipio("Cabo Frio");
        area.setEstado("RJ");
        return area;
    }

    private Mutirao novoMutirao(Long id, MutiraoStatus status, Area area) {
        Mutirao mutirao = new Mutirao();
        setField(mutirao, "id", id);
        mutirao.setTitulo("Mutirão Praia do Forte");
        mutirao.setData(LocalDate.of(2025, 6, 14));
        mutirao.setHoraInicio(LocalTime.of(8, 0));
        mutirao.setHoraFim(LocalTime.of(12, 0));
        mutirao.setStatus(status);
        mutirao.setArea(area);
        return mutirao;
    }

    private Voluntario novoVoluntario(Long id, String nome) {
        Voluntario voluntario = new Voluntario();
        voluntario.setId(id);
        voluntario.setGoogleId("google-" + id);
        voluntario.setEmail("user" + id + "@test.com");
        voluntario.setNome(nome);
        voluntario.setRole(VoluntarioRole.VOLUNTARIO);
        voluntario.setDataCadastro(Instant.now());
        return voluntario;
    }

    private RegistroResiduo novoRegistro(UUID id, Mutirao mutirao, Voluntario voluntario,
                                         TipoResiduo tipoResiduo, String areaTotal, int quantidade,
                                         Instant dataRegistro) {
        RegistroResiduo registro = new RegistroResiduo();
        registro.setId(id);
        registro.setMutirao(mutirao);
        registro.setVoluntario(voluntario);
        registro.setTipoResiduo(tipoResiduo);
        registro.setMetragemPerpendicular(new BigDecimal("2.50"));
        registro.setMetragemTransversal(new BigDecimal("1.20"));
        registro.setQuantidade(quantidade);
        registro.setAreaTotal(new BigDecimal(areaTotal));
        registro.setLocalizacao(GF.createPoint(new Coordinate(-42.0432, -22.8791)));
        registro.setFotoUrl("https://example.com/foto.jpg");
        registro.setDataRegistro(dataRegistro);
        registro.setSyncedAt(dataRegistro.plusSeconds(30));
        return registro;
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
