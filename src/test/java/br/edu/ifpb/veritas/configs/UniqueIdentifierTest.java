package br.edu.ifpb.veritas.configs;

import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.models.Subject;
import br.edu.ifpb.veritas.repositories.ProcessRepository;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import br.edu.ifpb.veritas.repositories.StudentRepository;
import br.edu.ifpb.veritas.repositories.SubjectRepository;
import br.edu.ifpb.veritas.services.ProcessService;
import br.edu.ifpb.veritas.services.ProfessorService;
import br.edu.ifpb.veritas.services.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class UniqueIdentifierTest {

    @Autowired
    private StudentService studentService;
    @Autowired
    private ProfessorService professorService;
    @Autowired
    private ProcessService processService;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ProfessorRepository professorRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private ProcessRepository processRepository;

    private Student novoEstudante(String login) {
        Student student = new Student();
        student.setName("Aluno Teste");
        student.setLogin(login);
        student.setPassword("senha123");
        student.setIsActive(true);
        return student;
    }

    private Professor novoProfessor(String login) {
        Professor professor = new Professor();
        professor.setName("Professor Teste");
        professor.setLogin(login);
        professor.setPassword("senha123");
        professor.setCoordinator(false);
        professor.setIsActive(true);
        return professor;
    }

    @Test
    void criacaoConcorrenteDeEstudantesGeraMatriculasDistintas() throws Exception {
        AtomicInteger seq = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Student>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() ->
                    studentService.create(novoEstudante("race-stu-" + System.nanoTime() + "-" + seq.incrementAndGet() + "@test.com"))));
        }
        pool.shutdown();

        List<Student> criados = new ArrayList<>();
        for (Future<Student> future : futures) {
            criados.add(future.get());
        }

        assertNotNull(criados.get(0).getRegister());
        assertNotNull(criados.get(1).getRegister());
        assertFalse(criados.get(0).getRegister().equals(criados.get(1).getRegister()));
        assertTrue(criados.get(0).getRegister().startsWith("STU-"));

        studentRepository.deleteAll(criados);
    }

    @Test
    void criacaoConcorrenteDeProfessoresGeraMatriculasDistintas() throws Exception {
        AtomicInteger seq = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<Professor>> futures = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            futures.add(pool.submit(() ->
                    professorService.create(novoProfessor("race-pro-" + System.nanoTime() + "-" + seq.incrementAndGet() + "@test.com"))));
        }
        pool.shutdown();

        List<Professor> criados = new ArrayList<>();
        for (Future<Professor> future : futures) {
            criados.add(future.get());
        }

        assertNotNull(criados.get(0).getRegister());
        assertNotNull(criados.get(1).getRegister());
        assertFalse(criados.get(0).getRegister().equals(criados.get(1).getRegister()));
        assertTrue(criados.get(0).getRegister().startsWith("PRO-"));

        professorRepository.deleteAll(criados);
    }

    @Test
    void criacaoDeProcessosGeraNumerosUnicos() {
        Student student = studentService.create(novoEstudante("proc-stu-" + System.nanoTime() + "@test.com"));
        Subject subject = new Subject();
        subject.setTitle("Assunto " + System.nanoTime());
        subject.setActive(true);
        subject = subjectRepository.save(subject);

        List<Process> criados = new ArrayList<>();
        try {
            for (int i = 0; i < 5; i++) {
                Process process = new Process();
                process.setTitle("Processo " + i);
                process.setDescription("Descrição do processo " + i);
                process.setCreatedAt(LocalDateTime.now());
                process.setStatus(StatusProcess.WAITING);
                criados.add(processService.createProcess(process, student.getId(), subject.getId()));
            }

            assertTrue(criados.stream().allMatch(p -> p.getNumber() != null));
            assertEquals(5, criados.stream().map(Process::getNumber).distinct().count());
            assertTrue(criados.get(0).getNumber().matches("\\d{4}-\\d{6}"));
        } finally {
            processRepository.deleteAll(criados);
            subjectRepository.delete(subject);
            studentRepository.delete(student);
        }
    }

    @Test
    void matriculaDeEstudanteNaoReusaNumeroAposExclusao() {
        Student primeiro = studentService.create(novoEstudante("delete-stu-" + System.nanoTime() + "@test.com"));
        String matriculaOriginal = primeiro.getRegister();

        studentRepository.delete(primeiro);

        Student segundo = studentService.create(novoEstudante("delete-stu-" + System.nanoTime() + "@test.com"));

        assertNotEquals(matriculaOriginal, segundo.getRegister());

        studentRepository.delete(segundo);
    }
}