package br.edu.ifpb.veritas.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateMeetingDTO {

    @NotNull(message = "Colegiado é obrigatório")
    private Long collegiateId;

    private String description;

    private LocalDateTime scheduledDate;

    private List<Long> processIds;
}