package br.edu.ifpb.veritas.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CollegiateEditDTO {
    private Long id;

    @NotBlank(message = "Descrição do colegiado é obrigatória")
    private String description;

    @NotNull(message = "Relator é obrigatório")
    private Long rapporteurId;

    @NotNull(message = "Selecione ao menos um membro")
    @Size(min = 1, message = "Colegiado deve ter ao menos um membro")
    private List<Long> memberIds;

    private List<Long> processIds;
}
