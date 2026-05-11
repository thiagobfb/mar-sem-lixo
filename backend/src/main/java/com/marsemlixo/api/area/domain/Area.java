package com.marsemlixo.api.area.domain;

import jakarta.persistence.*;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(
    name = "area",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_area_nome_municipio",
        columnNames = {"nome", "municipio"}
    )
)
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AreaTipo tipo;

    @Column(nullable = false)
    private String municipio;

    @Column(nullable = false, length = 2)
    private String estado;

    @Column(nullable = false, columnDefinition = "GEOMETRY(Polygon, 4326)")
    private Polygon poligono;

    @Column(nullable = false)
    private boolean ativa = true;

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public AreaTipo getTipo() { return tipo; }
    public void setTipo(AreaTipo tipo) { this.tipo = tipo; }
    public String getMunicipio() { return municipio; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Polygon getPoligono() { return poligono; }
    public void setPoligono(Polygon poligono) { this.poligono = poligono; }
    public boolean isAtiva() { return ativa; }
    public void setAtiva(boolean ativa) { this.ativa = ativa; }
}
