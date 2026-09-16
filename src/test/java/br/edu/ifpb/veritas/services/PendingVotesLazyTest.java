package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.enums.DecisionType;
import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.enums.StatusProcess;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cobrir o fluxo real de reunião/voto após Meeting.participants virar LAZY —
 * a lazy collection de participantes deve carregar normalmente dentro da
 * transação que executa findPendingVotesByProfessor.
 */
@SpringBootTest
@Transactional
class PendingVotesLazyTest {

    @Autowired
    private ProcessService processService;

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

    @Test
    void participantesCarregadosViaTransacaoDoServicoSemEager() {
        Professor rapporteur = new Professor();
        rapporteur.setName("Relator Lazy");
        rapporteur.setLogin("relator-lazy-" + System.nanoTime());
        rapporteur.setPassword("senha123");
        rapporteur.setCoordinator(false);
        rapporteur.setIsActive(true);
        rapporteur = professorRepository.save(rapporteur);

        Professor member = new Professor();
        member.setName("Membro Lazy");
        member.setLogin("membro-lazy-" + System.nanoTime());
        member.setPassword("senha123");
        member.setCoordinator(false);
        member.setIsActive(true);
        member = professorRepository.save(member);

        Student student = new Student();
        student.setName("Aluno Lazy");
        student.setLogin("aluno-lazy-" + System.nanoTime());
        student.setPassword("senha123");
        student = studentRepository.save(student);

        Subject subject = new Subject();
        subject.setTitle("Assunto Lazy " + System.nanoTime());
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        Collegiate collegiate = new Collegiate();
        collegiate.setDescription("Colegiado Lazy");
        collegiate.setCreatedAt(LocalDateTime.now());
        collegiate = collegiateRepository.save(collegiate);

        Meeting meeting = new Meeting();
        meeting.setDescription("Reunião Lazy");
        meeting.setCollegiate(collegiate);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setScheduledDate(LocalDateTime.now());
        meeting.setStatus(MeetingStatus.EM_ANDAMENTO);
        meeting.setActive(true);
        meeting.setParticipants(new ArrayList<>(List.of(member)));
        meeting.setProcesses(new ArrayList<>());
        meeting = meetingRepository.save(meeting);

        Process process = new Process();
        process.setTitle("Processo Lazy");
        process.setDescription("Descrição do processo lazy");
        process.setCreatedAt(LocalDateTime.now());
        process.setStatus(StatusProcess.UNDER_ANALISYS);
        process.setProcessCreator(student);
        process.setSubject(subject);
        process.setProcessRapporteur(rapporteur);
        process.setRapporteurVote(DecisionType.DEFERIMENTO);
        process.setMeeting(meeting);
        process = processRepository.save(process);

        final Long processId = process.getId();
        List<Process> pending = processService.findPendingVotesByProfessor(member.getId());

        assertTrue(pending.stream().anyMatch(p -> p.getId().equals(processId)));
        assertEquals(1, pending.size());
    }
}