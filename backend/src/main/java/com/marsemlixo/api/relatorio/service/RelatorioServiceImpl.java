package com.marsemlixo.api.relatorio.service;

import com.marsemlixo.api.exception.MutiraoNotFoundException;
import com.marsemlixo.api.exception.RelatorioNaoDisponivelException;
import com.marsemlixo.api.mutirao.domain.Mutirao;
import com.marsemlixo.api.mutirao.domain.MutiraoStatus;
import com.marsemlixo.api.mutirao.repository.MutiraoRepository;
import com.marsemlixo.api.relatorio.controller.dto.RelatorioMutiraoResponse;
import com.marsemlixo.api.residuo.domain.RegistroResiduo;
import com.marsemlixo.api.residuo.domain.TipoResiduo;
import com.marsemlixo.api.residuo.repository.RegistroResiduoRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Transactional(readOnly = true)
class RelatorioServiceImpl implements RelatorioService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final MutiraoRepository mutiraoRepository;
    private final RegistroResiduoRepository registroResiduoRepository;

    RelatorioServiceImpl(MutiraoRepository mutiraoRepository,
                         RegistroResiduoRepository registroResiduoRepository) {
        this.mutiraoRepository = mutiraoRepository;
        this.registroResiduoRepository = registroResiduoRepository;
    }

    @Override
    public RelatorioMutiraoResponse gerarResumoPorMutirao(Long mutiraoId) {
        Mutirao mutirao = buscarMutirao(mutiraoId);
        List<RegistroResiduo> registros = buscarRegistros(mutiraoId);
        return montarRelatorio(mutirao, registros);
    }

    @Override
    public RelatorioExcelFile exportarExcelPorMutirao(Long mutiraoId) {
        Mutirao mutirao = buscarMutirao(mutiraoId);
        if (mutirao.getStatus() != MutiraoStatus.CONCLUIDO) {
            throw new RelatorioNaoDisponivelException(mutiraoId, mutirao.getStatus());
        }

        List<RegistroResiduo> registros = buscarRegistros(mutiraoId);
        RelatorioMutiraoResponse relatorio = montarRelatorio(mutirao, registros);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            preencherAbaResumo(workbook.createSheet("Resumo"), relatorio);
            preencherAbaRegistros(workbook.createSheet("Registros"), registros);
            workbook.write(outputStream);

            return new RelatorioExcelFile(
                    "relatorio-mutirao-%d.xlsx".formatted(mutirao.getId()),
                    outputStream.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gerar arquivo Excel do relatório", e);
        }
    }

    private Mutirao buscarMutirao(Long mutiraoId) {
        return mutiraoRepository.findById(mutiraoId)
                .orElseThrow(() -> new MutiraoNotFoundException(mutiraoId));
    }

    private List<RegistroResiduo> buscarRegistros(Long mutiraoId) {
        return registroResiduoRepository.findByMutiraoIdOrderByDataRegistroAsc(mutiraoId);
    }

    private RelatorioMutiraoResponse montarRelatorio(Mutirao mutirao, List<RegistroResiduo> registros) {
        return new RelatorioMutiraoResponse(
                new RelatorioMutiraoResponse.MutiraoResumo(
                        mutirao.getId(),
                        mutirao.getTitulo(),
                        mutirao.getData(),
                        mutirao.getStatus(),
                        new RelatorioMutiraoResponse.AreaResumo(
                                mutirao.getArea().getId(),
                                mutirao.getArea().getNome(),
                                mutirao.getArea().getTipo(),
                                mutirao.getArea().getMunicipio(),
                                mutirao.getArea().getEstado()
                        )
                ),
                montarResumoConsolidado(registros),
                montarTotaisPorTipo(registros)
        );
    }

    private RelatorioMutiraoResponse.ResumoConsolidado montarResumoConsolidado(List<RegistroResiduo> registros) {
        int totalRegistros = registros.size();
        int totalItens = registros.stream()
                .mapToInt(RegistroResiduo::getQuantidade)
                .sum();
        BigDecimal areaTotal = somarAreaTotal(registros);
        int voluntariosDistintos = (int) registros.stream()
                .map(registro -> registro.getVoluntario().getId())
                .distinct()
                .count();
        Instant primeiroRegistro = registros.stream()
                .map(RegistroResiduo::getDataRegistro)
                .min(Comparator.naturalOrder())
                .orElse(null);
        Instant ultimoRegistro = registros.stream()
                .map(RegistroResiduo::getDataRegistro)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new RelatorioMutiraoResponse.ResumoConsolidado(
                totalRegistros,
                totalItens,
                areaTotal,
                voluntariosDistintos,
                primeiroRegistro,
                ultimoRegistro
        );
    }

    private List<RelatorioMutiraoResponse.TotalPorTipo> montarTotaisPorTipo(List<RegistroResiduo> registros) {
        Map<TipoResiduo, List<RegistroResiduo>> agrupados = new TreeMap<>();
        for (RegistroResiduo registro : registros) {
            agrupados.computeIfAbsent(registro.getTipoResiduo(), ignored -> new ArrayList<>())
                    .add(registro);
        }

        List<RelatorioMutiraoResponse.TotalPorTipo> totais = new ArrayList<>();
        for (Map.Entry<TipoResiduo, List<RegistroResiduo>> entry : agrupados.entrySet()) {
            List<RegistroResiduo> registrosDoTipo = entry.getValue();
            totais.add(new RelatorioMutiraoResponse.TotalPorTipo(
                    entry.getKey(),
                    registrosDoTipo.size(),
                    registrosDoTipo.stream().mapToInt(RegistroResiduo::getQuantidade).sum(),
                    somarAreaTotal(registrosDoTipo)
            ));
        }
        return totais;
    }

    private BigDecimal somarAreaTotal(List<RegistroResiduo> registros) {
        return registros.stream()
                .map(RegistroResiduo::getAreaTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private void preencherAbaResumo(Sheet sheet, RelatorioMutiraoResponse relatorio) {
        int rowIndex = 0;
        rowIndex = escreverPar(sheet, rowIndex, "Mutirão ID", relatorio.mutirao().id());
        rowIndex = escreverPar(sheet, rowIndex, "Título", relatorio.mutirao().titulo());
        rowIndex = escreverPar(sheet, rowIndex, "Data", relatorio.mutirao().data());
        rowIndex = escreverPar(sheet, rowIndex, "Status", relatorio.mutirao().status());
        rowIndex = escreverPar(sheet, rowIndex, "Área", relatorio.mutirao().area().nome());
        rowIndex = escreverPar(sheet, rowIndex, "Município", relatorio.mutirao().area().municipio());
        rowIndex = escreverPar(sheet, rowIndex, "Estado", relatorio.mutirao().area().estado());
        rowIndex = escreverPar(sheet, rowIndex, "Total de registros", relatorio.resumo().totalRegistros());
        rowIndex = escreverPar(sheet, rowIndex, "Total de itens", relatorio.resumo().totalItens());
        rowIndex = escreverPar(sheet, rowIndex, "Área total", relatorio.resumo().areaTotal());
        rowIndex = escreverPar(sheet, rowIndex, "Voluntários distintos", relatorio.resumo().voluntariosDistintos());
        rowIndex = escreverPar(sheet, rowIndex, "Primeiro registro em", formatarInstant(relatorio.resumo().primeiroRegistroEm()));
        rowIndex = escreverPar(sheet, rowIndex, "Último registro em", formatarInstant(relatorio.resumo().ultimoRegistroEm()));

        rowIndex++;
        Row header = sheet.createRow(rowIndex++);
        header.createCell(0).setCellValue("Tipo de resíduo");
        header.createCell(1).setCellValue("Total de registros");
        header.createCell(2).setCellValue("Total de itens");
        header.createCell(3).setCellValue("Área total");

        for (RelatorioMutiraoResponse.TotalPorTipo totalPorTipo : relatorio.totaisPorTipo()) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(totalPorTipo.tipoResiduo().name());
            row.createCell(1).setCellValue(totalPorTipo.totalRegistros());
            row.createCell(2).setCellValue(totalPorTipo.totalItens());
            row.createCell(3).setCellValue(totalPorTipo.areaTotal().doubleValue());
        }

        autoSizeColumns(sheet, 4);
    }

    private void preencherAbaRegistros(Sheet sheet, List<RegistroResiduo> registros) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("ID");
        header.createCell(1).setCellValue("Tipo");
        header.createCell(2).setCellValue("Quantidade");
        header.createCell(3).setCellValue("Metragem perpendicular");
        header.createCell(4).setCellValue("Metragem transversal");
        header.createCell(5).setCellValue("Área total");
        header.createCell(6).setCellValue("Longitude");
        header.createCell(7).setCellValue("Latitude");
        header.createCell(8).setCellValue("Voluntário");
        header.createCell(9).setCellValue("Data do registro");
        header.createCell(10).setCellValue("Sincronizado em");
        header.createCell(11).setCellValue("Foto URL");

        int rowIndex = 1;
        for (RegistroResiduo registro : registros) {
            Row row = sheet.createRow(rowIndex++);
            row.createCell(0).setCellValue(registro.getId().toString());
            row.createCell(1).setCellValue(registro.getTipoResiduo().name());
            row.createCell(2).setCellValue(registro.getQuantidade());
            row.createCell(3).setCellValue(registro.getMetragemPerpendicular().doubleValue());
            row.createCell(4).setCellValue(registro.getMetragemTransversal().doubleValue());
            row.createCell(5).setCellValue(registro.getAreaTotal().doubleValue());
            row.createCell(6).setCellValue(registro.getLocalizacao().getX());
            row.createCell(7).setCellValue(registro.getLocalizacao().getY());
            row.createCell(8).setCellValue(registro.getVoluntario().getNome());
            row.createCell(9).setCellValue(formatarInstant(registro.getDataRegistro()));
            row.createCell(10).setCellValue(formatarInstant(registro.getSyncedAt()));
            row.createCell(11).setCellValue(registro.getFotoUrl() != null ? registro.getFotoUrl() : "");
        }

        autoSizeColumns(sheet, 12);
    }

    private int escreverPar(Sheet sheet, int rowIndex, String label, Object value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value != null ? value.toString() : "");
        return rowIndex + 1;
    }

    private void autoSizeColumns(Sheet sheet, int totalColumns) {
        for (int i = 0; i < totalColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String formatarInstant(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_TIME_FORMATTER.format(instant.atOffset(ZoneOffset.UTC));
    }
}
