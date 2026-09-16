package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.models.Subject;
import br.edu.ifpb.veritas.repositories.ProcessRepository;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import br.edu.ifpb.veritas.repositories.StudentRepository;
import br.edu.ifpb.veritas.repositories.SubjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProcessServiceTest {

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

    private Process createWaitingProcess() {
        Student student = new Student();
        student.setName("Aluno Teste");
        student.setLogin("aluno-" + System.nanoTime());
        student.setPassword("senha123");
        student = studentRepository.save(student);

        Subject subject = new Subject();
        subject.setTitle("Assunto " + System.nanoTime());
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        Process process = new Process();
        process.setTitle("Processo Teste");
        process.setDescription("Descrição do processo de teste");
        process.setCreatedAt(LocalDateTime.now());
        process.setStatus(StatusProcess.WAITING);
        process.setProcessCreator(student);
        process.setSubject(subject);
        return processRepository.save(process);
    }

    private Professor createProfessor() {
        Professor professor = new Professor();
        professor.setName("Professor Teste");
        professor.setLogin("professor-" + System.nanoTime());
        professor.setPassword("senha123");
        professor.setCoordinator(false);
        professor.setIsActive(true);
        return professorRepository.save(professor);
    }

    @Test
    void distribuirProcessoEmEsperaDesignaRelatorEMudaStatus() {
        Process process = createWaitingProcess();
        Professor professor = createProfessor();

        Process distributed = processService.distribute(process.getId(), professor.getId());

        assertEquals(StatusProcess.UNDER_ANALISYS, distributed.getStatus());
        assertEquals(professor.getId(), distributed.getProcessRapporteur().getId());
    }

    @Test
    void naoPodeRedistribuirProcessoJaEmAnalise() {
        Process process = createWaitingProcess();
        Professor professor = createProfessor();
        Professor otherProfessor = createProfessor();

        processService.distribute(process.getId(), professor.getId());
        Long processId = process.getId();

        assertThrows(IllegalStateException.class, () ->
                processService.distribute(processId, otherProfessor.getId()));
    }

    @Test
    void naoPodeDistribuirProcessoJaFinalizado() {
        Process process = createWaitingProcess();
        process.setStatus(StatusProcess.APPROVED);
        process = processRepository.save(process);
        Long processId = process.getId();

        Professor professor = createProfessor();

        assertThrows(IllegalStateException.class, () ->
                processService.distribute(processId, professor.getId()));
    }

    private Process prepareForUpload() {
        Student student = new Student();
        student.setName("Aluno Teste");
        student.setLogin("aluno-" + System.nanoTime());
        student.setPassword("senha123");
        student = studentRepository.save(student);

        Subject subject = new Subject();
        subject.setTitle("Assunto " + System.nanoTime());
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        Process process = new Process();
        process.setTitle("Processo Teste");
        process.setDescription("Descrição do processo de teste");
        return processService.createProcess(process, student.getId(), subject.getId(), null);
    }

    @Test
    void uploadComExtensaoPdfMasConteudoZIPERejeitado() {
        Process process = prepareForUpload();

        MockMultipartFile fakePdf = new MockMultipartFile(
                "documentFile", "documento.pdf", "application/pdf",
                new byte[]{0x50, 0x4B, 0x03, 0x04, 0x00, 0x00});

        assertThrows(IllegalArgumentException.class, () ->
                processService.uploadDocument(process.getId(),
                        process.getProcessCreator().getId(), fakePdf));
    }

    @Test
    void uploadDePdfValidoEaceitoELeNomeEsanitizado() {
        Process process = prepareForUpload();

        MockMultipartFile pdf = new MockMultipartFile(
                "documentFile", "relatorio.pdf", "application/pdf",
                "%PDF-1.7 fake content".getBytes(StandardCharsets.UTF_8));

        Process updated = processService.uploadDocument(process.getId(),
                process.getProcessCreator().getId(), pdf);

        assertEquals("relatorio.pdf", updated.getDocumentFilename());
        assertEquals("%PDF-1.7 fake content", new String(updated.getDocument()));
    }

    @Test
    void nomeComQuebraDeLinhaESaneadoAntesDeServir() {
        Process process = prepareForUpload();

        String maliciousName = "relatorio\r\nContent-Disposition: injected.pdf";
        MockMultipartFile pdf = new MockMultipartFile(
                "documentFile", maliciousName, "application/pdf",
                "%PDF-1.7 fake content".getBytes(StandardCharsets.UTF_8));

        Process updated = processService.uploadDocument(process.getId(),
                process.getProcessCreator().getId(), pdf);

        assertEquals("relatorioContent-Disposition: injected.pdf", updated.getDocumentFilename());
        assertEquals(updated.getDocumentFilename(), ProcessService.sanitizeFilename(maliciousName));
    }

    @Test
    void uploadDeConteudoNaoPdfForadoContentTypePdfERejeitado() {
        Process process = prepareForUpload();

        MultipartFile notPdf = new MockMultipartFile(
                "documentFile", "notapdf.pdf", "application/pdf",
                "<html>não é pdf</html>".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () ->
                processService.uploadDocument(process.getId(),
                        process.getProcessCreator().getId(), notPdf));
    }
}
