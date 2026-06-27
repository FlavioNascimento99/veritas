/**
 * O Model Collegiate se trata de, um grupo de Professor's 
 * que estarão dentro de uma determinada Meeting, decidindo 
 * se, o Vote do Rapporteur é deferível ou não;
 */

package br.edu.ifpb.veritas.models;

import java.time.LocalDateTime;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_COLLEGIATE")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Collegiate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;


  /**
   * Propriedades de criação e modificação.
   * 
   * LocalDateTime Structure
   */
  @Column(name = "CREATED_AT")
  private LocalDateTime createdAt;

  @Column(name = "CLOSED_AT")
  private LocalDateTime closedAt;

  /**
   * Breve descritivo do Colegiado.
  */
  @NotBlank(message = "Descrição do colegiado é obrigatória")
  @Column(name = "DESCRIPTION")
  private String description;

  /**
   * Rapporteur é o Professor/Relator do Processo.
   * Criada uma tabela com o ID do Professor em questão.
  */
  @ManyToOne
  @JoinColumn(name = "PROFESSOR_ID")
  private Professor rapporteur;
  
  /**
   * Log 1: Estou escervendo essa propriedade com a ideia de, Colegiado, nada mais é 
   * que, o grupo 
   */
  @ManyToMany
  @JoinTable(
    name                  = "MEMBERS", 
      joinColumns         = @JoinColumn(name = "COLLEGIATE_ID"), 
      inverseJoinColumns  = @JoinColumn(name = "PROFESSOR_ID")
  )
  private List<Professor> collegiateMemberList;

  /**
   * Relacionamento reverso: Reuniões deste colegiado
   * Crítico para persistência - permite cascade delete e save
   */
  @OneToMany(mappedBy = "collegiate", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<Meeting> meetings = new ArrayList<>();
  
  
  /**
   * Relacionamento com aluno representante
   * 
  @OneToOne
  @JoinColumn(name = "COLLEGIATE_REP_STUDENT_ID", nullable = true)
  private Student representativeStudent;
   */

  /**
   * Log 1: Listagem de processos sob análise pelo colegiado.
   *  mappedBy refere ao atributo de mesmo nome em Process
   *  Colegiado não deveria ter uma lista de processos,
   *  é a reunião que os julga
   * 
   * 
    @OneToMany(mappedBy="collegiate")
    private List<Process> processesToJudgeList;
   *
   */
  
}