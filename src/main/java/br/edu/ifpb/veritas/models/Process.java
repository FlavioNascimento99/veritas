package br.edu.ifpb.veritas.models;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.ifpb.veritas.configs.DecisionTypeConverter;
import br.edu.ifpb.veritas.enums.DecisionType;
import br.edu.ifpb.veritas.enums.ProcessRapporteurStatus;
import br.edu.ifpb.veritas.enums.StatusProcess;
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
@Table(name = "TB_PROCESSES")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Process {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "TITLE")
    private String title;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "NUMBER")
    private String number;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "DISTRIBUTED_AT")
    private LocalDateTime distributedAt;

    @Column(name = "SOLVED_AT")
    private LocalDateTime solvedAt;

    @Column(name = "PROCESS_STATUS")
    @Enumerated(EnumType.STRING)
    private StatusProcess status;

    @Column(name = "RAPPORTEUR_VOTE")
    @Convert(converter = DecisionTypeConverter.class)
    private DecisionType rapporteurVote;

    @Column(name = "PROCESS_ORDER")
    private String processOrder;

    @ManyToOne
    @JoinColumn(name = "INTERESTED_STUDENT_ID", nullable = false)
    private Student processCreator;

    @ManyToOne
    @JoinColumn(name = "PROCESS_SUBJECT_ID", nullable = false)
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "PROFESSOR_RAPPORTEUR_ID", nullable = true)
    private Professor processRapporteur;

    @OneToMany(mappedBy = "process", cascade = CascadeType.ALL)
    private List<Vote> processVoteList;

    /**
     * Justificativa escrita pelo relator ao votar (REQFUNC 5)
     */
    @Column(name = "RAPPORTEUR_JUSTIFICATION", columnDefinition = "TEXT")
    private String rapporteurJustification;

    /**
     * REQFUNC 16: Armazena o PDF do requerimento enviado pelo aluno
     */
    @Column(name = "DOCUMENT", columnDefinition = "BYTEA")
    private byte[] document;

    @Column(name = "DOCUMENT_FILENAME")
    private String documentFilename;

    @Column(name = "DOCUMENT_UPLOAD_DATE")
    private LocalDateTime documentUploadDate;

    /**
     * REQFUNC 14: Associação com uma reunião (ManyToOne).
     * Um processo pode estar em uma única reunião apenas.
     * Garante que um processo adicionado a uma reunião não será adicionado a outra.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = true)
    private Meeting meeting;

    /**
     * Status do voto do relator (PENDING, VOTED, ABSTAINED)
     * Diferente de rapporteurVote que é a decisão (DEFERIMENTO/INDEFERIMENTO)
     */
    @Column(name = "RAPPORTEUR_STATUS")
    @Enumerated(EnumType.STRING)
    private ProcessRapporteurStatus rapporteurStatus;

    /**
     * Status atual do processo no fluxo
     */
}