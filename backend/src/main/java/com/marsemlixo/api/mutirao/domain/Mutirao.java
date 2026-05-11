package com.marsemlixo.api.mutirao.domain;

import com.marsemlixo.api.area.domain.Area;
import com.marsemlixo.api.auth.domain.Voluntario;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "mutirao")
public class Mutirao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizador_id", nullable = false)
    private Voluntario organizador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MutiraoStatus status = MutiraoStatus.PLANEJADO;

    @Column(columnDefinition = "text")
    private String observacoes;

    public Long getId() { return id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public Voluntario getOrganizador() { return organizador; }
    public void setOrganizador(Voluntario organizador) { this.organizador = organizador; }

    public MutiraoStatus getStatus() { return status; }
    public void setStatus(MutiraoStatus status) { this.status = status; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
}
