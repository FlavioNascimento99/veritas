package br.edu.ifpb.veritas.models;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_professors")
@DiscriminatorValue("PROFESSOR")

// Evita recursão infinita ao serializar objetos com referências bidirecionais
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Professor {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Required fields for Postman test
   */

  @NotBlank(message = "Nome do professor é obrigatório")
  @Column(name = "NAME")
  private String name;

  @Column(name = "PHONE_NUMBER")
  private String phoneNumber;
 
  @Column(name = "REGISTER", unique = true)
  private String register;

  @NotBlank(message = "Login é obrigatório")
  @Column(name = "LOGIN")
  private String login;

  @NotBlank(message = "Senha é obrigatória")
  @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
  @Column(name = "PASSWORD")
  private String password;

  @Column(name = "IS_COORDINATOR", nullable = false)
  private Boolean coordinator = false;
  
  @Column(name = "IS_ACTIVE", nullable = false)  
  private Boolean isActive      = true;

  @OneToOne(mappedBy = "courseCoordinator")
  private Course coordinatorAt;

  /**
   * Listagem de Processos anexados ao professor.
   *
   * Nov Substituição de tipagem de ArrayList para List
   * Spring tenta utilizar PersistentBag mas não consegue
   * lidar com listas dinâmicas tão bem quanto listas de
   * tamanho pre-definido.
   */


  /**
   * Log 1: Listagem de Colegiados? Não soa como algo com sentido pra 
   * mim, Professor sim, faz parte de Colegiado, não o contrário.
   * ManyToMany(mappedBy = "members")
   * private List<Collegiate> collegiates;
   */
}
