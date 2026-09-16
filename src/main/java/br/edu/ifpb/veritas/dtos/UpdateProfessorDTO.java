package br.edu.ifpb.veritas.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProfessorDTO {

    @NotBlank(message = "Nome do professor é obrigatório")
    private String name;

    private String phoneNumber;

    @NotBlank(message = "Login é obrigatório")
    private String login;

    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    private String password;
}