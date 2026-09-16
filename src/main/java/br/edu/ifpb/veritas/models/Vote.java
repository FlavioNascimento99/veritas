package br.edu.ifpb.veritas.models;

import br.edu.ifpb.veritas.enums.VoteType;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "TB_VOTES")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Vote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "vote_type")
  private VoteType voteType;

  // Ausentes (away = true) não têm voteType — posicionamento sem direção
  @AssertTrue(message = "Tipo de voto é obrigatório para votos de membros presentes")
  public boolean isVoteTypePresentWhenPresent() {
    return Boolean.TRUE.equals(away) || voteType != null;
  }
  
  /**
   * Log 1: Define se o mesmo (Professor membro do Colegiado) 
   * encontrou-se ausente da reunião para definir destino do 
   * Processo.
   */
  private Boolean away = false;


  // O columnDefinition é para permitir textos longos (que imagino que seria o caso, visto que é uma justificativa de um voto)
  @Column(name = "justification", columnDefinition = "TEXT")
  private String justification;

  // Registra a data e hora em que o voto foi realizado
  @Column(name = "voted_at")
  private LocalDateTime votedAt;

  /**
   * Log 1: Haverá N votos para um Processo, logo referencia 
   * a este segundo.
   */
  @NotNull(message = "Processo é obrigatório")
  @ManyToOne
  @JoinColumn(name = "PROCESS_ID", nullable = true)
  private Process process;


  /**
   * Um professor pode votar em MÚLTIPLOS processos (um voto por processo).
   * - Muitos votos podem pertencer a um professor (em processos diferentes)
   * - Cada voto pertence a um único professor
   */
  @NotNull(message = "Professor é obrigatório")
  @ManyToOne
  @JoinColumn(name = "PROFESSOR_ID", nullable = false)
  private Professor professor;

}
