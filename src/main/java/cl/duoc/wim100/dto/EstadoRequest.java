package cl.duoc.wim100.dto;

import cl.duoc.wim100.model.EstadoPostulante;
import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "El estado es obligatorio") EstadoPostulante estado) {}
