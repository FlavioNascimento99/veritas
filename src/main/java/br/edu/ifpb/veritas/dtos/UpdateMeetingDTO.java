package br.edu.ifpb.veritas.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateMeetingDTO {

    @NotBlank(message = "Descrição não pode ser vazia")
    private String description;

    private LocalDateTime scheduledDate;
}