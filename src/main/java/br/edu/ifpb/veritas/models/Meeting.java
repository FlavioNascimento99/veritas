/**
 * Classe de Reunião
 *
 *
 * Responsável por gerar uma Ata (Documentação referente à si mesmo)
 * que possuirá Colegiado e uma Lista de Processos.
 *
 * questionamentos? deverá possuir mais alguma prop?
 */

package br.edu.ifpb.veritas.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.edu.ifpb.veritas.enums.MeetingStatus;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_meetings")

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Meeting {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Descrição/Título da reunião para identificação
   */
  @Column(name = "description")
  private String description;

  // Pertence a um Colegiado
  @ManyToOne
  @JoinColumn(name = "collegiate_id")
  private Collegiate collegiate; // 1 colegiado ao qual a reunião pertence

  // Lista de Processos em pauta na reunião (carregados via relação ManyToOne do Process)
  @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Process> processes; // 0..* processos em pauta

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  // REQFUNC: Data e hora em que a reunião foi aberta/iniciada
  @Column(name = "opened_at")
  private LocalDateTime openedAt;

  // REQFUNC 9: Data e hora em que a reunião está agendada para acontecer
  @Column(name = "scheduled_date")
  private LocalDateTime scheduledDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private MeetingStatus status;

  // Acredito que a ata foi adiada foi adiada
  // para a próxima etapa do projeto

  // REQFUNC 09: Lista de professores escalados para a reunião
  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
  @JoinTable(
          name = "meeting_participants",
          joinColumns = @JoinColumn(name = "meeting_id"),
          inverseJoinColumns = @JoinColumn(name = "professor_id")
    )
  private List<Professor> participants = new ArrayList<>();

  // REQFUNC 10: Indica se a reunião está atualmente em andamento
  @Column(name = "is_active")
  private boolean active = false;

  /**
   * Ata da reunião (PDF)
   */
  @Column(name = "meeting_document", columnDefinition = "bytea")
  private byte[] meetingDocument;
}
