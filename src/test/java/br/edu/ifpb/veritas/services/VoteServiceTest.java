package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.enums.DecisionType;
import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.enums.VoteType;
import br.edu.ifpb.veritas.models.Collegiate;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.models.Subject;
import br.edu.ifpb.veritas.repositories.CollegiateRepository;
import br.edu.ifpb.veritas.repositories.MeetingRepository;
import br.edu.ifpb.veritas.repositories.ProcessRepository;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import br.edu.ifpb.veritas.repositories.StudentRepository;
import br.edu.ifpb.veritas.repositories.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regra de julgamento (README seção 2): se a maioria dos membros vota igual ao relator,
 * o resultado é a decisão do relator; se a maioria diverge, o resultado é o oposto;
 * em caso de empate, prevalece a decisão do relator.
 */
@SpringBootTest
@Transactional
class VoteServiceTest {

    @Autowired
    private VoteService voteService;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private CollegiateRepository collegiateRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    private Professor createProfessor(String login) {
        Professor professor = new Professor();
        professor.setName("Professor " + login);
        professor.setLogin(login);
        professor.setPassword("senha123");
        professor.setCoordinator(false);
        professor.setIsActive(true);
        return professorRepository.save(professor);
    }

    private Process createProcessWithMeeting(Professor rapporteur, List<Professor> members) {
        Student student = new Student();
        student.setName("Aluno Teste");
        student.setLogin("aluno-" + System.nanoTime());
        student.setPassword("senha123");
        student = studentRepository.save(student);

        Subject subject = new Subject();
        subject.setTitle("Assunto " + System.nanoTime());
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        Collegiate collegiate = new Collegiate();
        collegiate.setDescription("Colegiado Teste");
        collegiate.setCreatedAt(LocalDateTime.now());
        collegiate = collegiateRepository.save(collegiate);

        Meeting meeting = new Meeting();
        meeting.setDescription("Reunião Teste");
        meeting.setCollegiate(collegiate);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setScheduledDate(LocalDateTime.now());
        meeting.setStatus(MeetingStatus.EM_ANDAMENTO);
        meeting.setActive(true);
        meeting.setParticipants(new ArrayList<>(members));
        meeting.setProcesses(new ArrayList<>());
        meeting = meetingRepository.save(meeting);

        Process process = new Process();
        process.setTitle("Processo Teste");
        process.setDescription("Descrição do processo de teste");
        process.setCreatedAt(LocalDateTime.now());
        process.setStatus(StatusProcess.UNDER_ANALISYS);
        process.setProcessCreator(student);
        process.setSubject(subject);
        process.setProcessRapporteur(rapporteur);
        process.setMeeting(meeting);
        return processRepository.save(process);
    }

    @Test
    void relatorDefereEMaioriaConcordaMantemDeferimento() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member1 = createProfessor("membro1-" + System.nanoTime());
        Professor member2 = createProfessor("membro2-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member1, member2));

        voteService.registerRapporteurDecision(process.getId(), rapporteur.getId(),
                DecisionType.DEFERIMENTO, "Justificativa do relator");
        voteService.registerProfessorVote(process.getId(), member1.getId(), VoteType.DEFERIDO, "");
        voteService.registerProfessorVote(process.getId(), member2.getId(), VoteType.DEFERIDO, "");

        assertEquals(DecisionType.DEFERIMENTO, voteService.calculateResult(process.getId()));
    }

    @Test
    void relatorIndefereEMaioriaConcordaMantemIndeferimento() {
        // Caso que expõe o bug: quando o relator INDEFERE e a maioria vota igual a ele
        // (INDEFERIDO), o resultado deve permanecer INDEFERIMENTO. A implementação antiga
        // contava votos DEFERIDO como "concorda com o relator" incondicionalmente, o que
        // invertia esse resultado para DEFERIMENTO.
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member1 = createProfessor("membro1-" + System.nanoTime());
        Professor member2 = createProfessor("membro2-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member1, member2));

        voteService.registerRapporteurDecision(process.getId(), rapporteur.getId(),
                DecisionType.INDEFERIMENTO, "Justificativa do relator");
        voteService.registerProfessorVote(process.getId(), member1.getId(), VoteType.INDEFERIDO, "");
        voteService.registerProfessorVote(process.getId(), member2.getId(), VoteType.INDEFERIDO, "");

        assertEquals(DecisionType.INDEFERIMENTO, voteService.calculateResult(process.getId()));
    }

    @Test
    void relatorDefereEMaioriaDivergeInverteParaIndeferimento() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member1 = createProfessor("membro1-" + System.nanoTime());
        Professor member2 = createProfessor("membro2-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member1, member2));

        voteService.registerRapporteurDecision(process.getId(), rapporteur.getId(),
                DecisionType.DEFERIMENTO, "Justificativa do relator");
        voteService.registerProfessorVote(process.getId(), member1.getId(), VoteType.INDEFERIDO, "");
        voteService.registerProfessorVote(process.getId(), member2.getId(), VoteType.INDEFERIDO, "");

        assertEquals(DecisionType.INDEFERIMENTO, voteService.calculateResult(process.getId()));
    }

    @Test
    void empatePrevaleceDecisaoDoRelator() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member1 = createProfessor("membro1-" + System.nanoTime());
        Professor member2 = createProfessor("membro2-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member1, member2));

        voteService.registerRapporteurDecision(process.getId(), rapporteur.getId(),
                DecisionType.DEFERIMENTO, "Justificativa do relator");
        voteService.registerProfessorVote(process.getId(), member1.getId(), VoteType.DEFERIDO, "");
        voteService.registerProfessorVote(process.getId(), member2.getId(), VoteType.INDEFERIDO, "");

        assertEquals(DecisionType.DEFERIMENTO, voteService.calculateResult(process.getId()));
    }

    @Test
    void membroNaoPodeVotarDuasVezesNoMesmoProcesso() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member = createProfessor("membro-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member));

        voteService.registerRapporteurDecision(process.getId(), rapporteur.getId(),
                DecisionType.DEFERIMENTO, "Justificativa do relator");
        voteService.registerProfessorVote(process.getId(), member.getId(), VoteType.DEFERIDO, "");

        assertThrows(IllegalStateException.class, () ->
                voteService.registerProfessorVote(process.getId(), member.getId(), VoteType.INDEFERIDO, ""));
    }

    @Test
    void relatorNaoPodeVotarComoMembro() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member = createProfessor("membro-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(rapporteur, member));

        voteService.registerRapporteurDecision(process.getId(), rapporteur.getId(),
                DecisionType.DEFERIMENTO, "Justificativa do relator");

        assertThrows(IllegalStateException.class, () ->
                voteService.registerProfessorVote(process.getId(), rapporteur.getId(), VoteType.DEFERIDO, ""));
    }

    @Test
    void naoPodeVotarEmProcessoJaFinalizado() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member = createProfessor("membro-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member));
        process.setStatus(StatusProcess.APPROVED);
        process = processRepository.save(process);

        Long processId = process.getId();
        assertThrows(IllegalStateException.class, () ->
                voteService.registerProfessorVote(processId, member.getId(), VoteType.DEFERIDO, ""));
    }

    @Test
    void naoPodeVotarEmReuniaoNaoAtiva() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member = createProfessor("membro-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member));

        voteService.registerRapporteurDecision(process.getId(), rapporteur.getId(),
                DecisionType.DEFERIMENTO, "Justificativa do relator");

        Meeting meeting = process.getMeeting();
        meeting.setActive(false);
        meeting.setStatus(MeetingStatus.FINALIZADA);
        meetingRepository.save(meeting);

        assertThrows(IllegalStateException.class, () ->
                voteService.registerProfessorVote(process.getId(), member.getId(), VoteType.DEFERIDO, ""));
    }

    @Test
    void naoPodeApregoarProcessoSemVotoDoRelator() {
        Professor rapporteur = createProfessor("relator-" + System.nanoTime());
        Professor member = createProfessor("membro-" + System.nanoTime());

        Process process = createProcessWithMeeting(rapporteur, List.of(member));
        Long processId = process.getId();

        assertThrows(IllegalStateException.class, () -> voteService.announceProcess(processId));
    }
}
