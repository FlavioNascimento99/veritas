package br.edu.ifpb.veritas.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_ADMINISTRATOR")
@DiscriminatorValue("ADMINISTRATOR")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Administrator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome do administrador é obrigatório")
    @Column(name = "TB_ADMIN_NAME")
    private String name;

    @Column(name = "TB_ADMIN_PHONE_NUMBER")
    private String phoneNumber;

    @Column(name = "TB_ADMIN_REGISTER")
    private String register;

    @NotBlank(message = "Login é obrigatório")
    @Column(name = "TB_ADMIN_LOGIN")
    private String login;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
    @Column(name = "TB_ADMIN_PASSWORD")
    private String password;

    /**
     * Admin isActive full true.
     */
    private Boolean isActive = true;
}
