package br.edu.ifpb.veritas.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_STUDENTS")
@DiscriminatorValue("STUDENT")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Student {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @NotBlank(message = "Nome do estudante é obrigatório")
  @Column(name = "NAME")
  private String name;

  @Column(name = "PHONE_NUMBER")
  private String phoneNumber;
  /**
   * Log 1: Já não vejo mais necessidade na implementação de Matrícula
   * dentro da estidade estudante, visto que não vai ser necessário nenhum
   * tipo de busca ou filtro de forma obrigatória dentro da aplicação.
   */
  @Column(name = "REGISTER")
  private String register;

  /**
   * Log 1: Seria interessante a implementação de uma regra para criação de 
   * login dentro do sistema, seja baseado em @ ou primeiro e último nome do 
   * usuário.
   */
  @NotBlank(message = "Login é obrigatório")
  @Column(name = "LOGIN")
  private String login;

  @NotBlank(message = "Senha é obrigatória")
  @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
  @Column(name = "PASSWORD")
  private String password;

  @Column(name = "IS_ACTIVE")
  private Boolean isActive = true;


  /**
   * Log 1: Essa propriedade se trata do lado B de uma relação bi-direcional entre 
   * Aluno e seu Curso.
   */
  @ManyToOne
  @JoinColumn(name = "CURRENT_COURSE")
  private Course currentCourse;

  /**
   * Log 1: Listagem de Processos emitidos pelo Estudante em questão.
   */
  @OneToMany(mappedBy = "processCreator", cascade = CascadeType.ALL)
  private List<Process> processList; 
  
  /**
   * Log 1: Ainda não definido exatamente como deverá ser feito a implementação
   * do representante do Colegiado. Inicialmente, penso na NÃO NECESSIDADE de um, 
   * mas sim em algo como um Listener do Processo.
   * 
   * Papel do Listener do Processo irá se resumir a um usuário do tipo Estudante, que
   * a partir de um Observer. 
   * 
   * Consultar: https://refactoring.guru/design-patterns/observer
  @OneToOne(mappedBy = "representativeStudent")
  private Collegiate representativeCollegiate;
   */
}