package com.marsemlixo.api.residuo.domain;

import com.marsemlixo.api.auth.domain.Voluntario;
import com.marsemlixo.api.mutirao.domain.Mutirao;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registro_residuo")
public class RegistroResiduo {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mutirao_id", nullable = false)
    private Mutirao mutirao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voluntario_id", nullable = false)
    private Voluntario voluntario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_residuo", nullable = false, length = 50)
    private TipoResiduo tipoResiduo;

    @Column(name = "metragem_perpendicular", nullable = false, precision = 8, scale = 2)
    private BigDecimal metragemPerpendicular;

    @Column(name = "metragem_transversal", nullable = false, precision = 8, scale = 2)
    private BigDecimal metragemTransversal;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "area_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaTotal;

    @Column(nullable = false, columnDefinition = "geometry(Point,4326)")
    private Point localizacao;

    @Column(name = "foto_url", columnDefinition = "text")
    private String fotoUrl;

    @Column(name = "data_registro", nullable = false)
    private Instant dataRegistro;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Mutirao getMutirao() {
        return mutirao;
    }

    public void setMutirao(Mutirao mutirao) {
        this.mutirao = mutirao;
    }

    public Voluntario getVoluntario() {
        return voluntario;
    }

    public void setVoluntario(Voluntario voluntario) {
        this.voluntario = voluntario;
    }

    public TipoResiduo getTipoResiduo() {
        return tipoResiduo;
    }

    public void setTipoResiduo(TipoResiduo tipoResiduo) {
        this.tipoResiduo = tipoResiduo;
    }

    public BigDecimal getMetragemPerpendicular() {
        return metragemPerpendicular;
    }

    public void setMetragemPerpendicular(BigDecimal metragemPerpendicular) {
        this.metragemPerpendicular = metragemPerpendicular;
    }

    public BigDecimal getMetragemTransversal() {
        return metragemTransversal;
    }

    public void setMetragemTransversal(BigDecimal metragemTransversal) {
        this.metragemTransversal = metragemTransversal;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getAreaTotal() {
        return areaTotal;
    }

    public void setAreaTotal(BigDecimal areaTotal) {
        this.areaTotal = areaTotal;
    }

    public Point getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(Point localizacao) {
        this.localizacao = localizacao;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Instant getDataRegistro() {
        return dataRegistro;
    }

    public void setDataRegistro(Instant dataRegistro) {
        this.dataRegistro = dataRegistro;
    }

    public Instant getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Instant syncedAt) {
        this.syncedAt = syncedAt;
    }
}
