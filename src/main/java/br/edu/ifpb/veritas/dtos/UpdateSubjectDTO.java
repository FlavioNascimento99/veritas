package br.edu.ifpb.veritas.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSubjectDTO {

    @NotBlank(message = "Título do assunto é obrigatório")
    private String title;

    private String description;
}