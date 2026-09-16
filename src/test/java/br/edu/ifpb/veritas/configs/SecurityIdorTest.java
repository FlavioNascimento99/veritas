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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityIdorTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ProfessorRepository professorRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private ProcessRepository processRepository;
    @Autowired
    private ProcessService processService;

    private Student createStudent(String login) {
        Student student = new Student();
        student.setName("Aluno " + login);
        student.setLogin(login);
        student.setPassword("senha123");
        student.setIsActive(true);
        return studentRepository.save(student);
    }

    private Professor createProfessor(String login) {
        Professor professor = new Professor();
        professor.setName("Professor " + login);
        professor.setLogin(login);
        professor.setPassword("senha123");
        professor.setCoordinator(false);
        professor.setIsActive(true);
        return professorRepository.save(professor);
    }

    private Subject createSubject() {
        Subject subject = new Subject();
        subject.setTitle("Assunto " + System.nanoTime());
        subject.setActive(true);
        return subjectRepository.save(subject);
    }

    private Process createProcessOwnedBy(Student owner) {
        Process process = new Process();
        process.setTitle("Processo Teste");
        process.setDescription("Descrição do processo");
        process.setCreatedAt(LocalDateTime.now());
        process.setStatus(StatusProcess.WAITING);
        process.setProcessCreator(owner);
        process.setSubject(createSubject());
        return processRepository.save(process);
    }

    private UserDetails studentUser(String login) {
        return User.withUsername(login).password("x").roles("STUDENT").build();
    }

    private UserDetails professorUser(String login) {
        return User.withUsername(login).password("x").roles("PROFESSOR").build();
    }

    private UserDetails adminUser() {
        return User.withUsername("admin-test").password("x").roles("ADMIN").build();
    }

    @Test
    void estudanteListaSeusProcessosMasNaoOsDeOutroEstudante() throws Exception {
        Student studentA = createStudent("aluno-a-" + System.nanoTime() + "@test.com");
        Student studentB = createStudent("aluno-b-" + System.nanoTime() + "@test.com");
        createProcessOwnedBy(studentA);
        createProcessOwnedBy(studentB);

        mockMvc.perform(get("/api/students/" + studentA.getId() + "/processes")
                        .with(user(studentUser(studentA.getLogin()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/students/" + studentB.getId() + "/processes")
                        .with(user(studentUser(studentA.getLogin()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void professorListaSeusProcessosMasNaoOsDeOutroProfessor() throws Exception {
        Professor professorA = createProfessor("prof-a-" + System.nanoTime() + "@test.com");
        Professor professorB = createProfessor("prof-b-" + System.nanoTime() + "@test.com");
        Student student = createStudent("aluno-" + System.nanoTime() + "@test.com");
        Process process = createProcessOwnedBy(student);
        processService.distribute(process.getId(), professorB.getId());

        mockMvc.perform(get("/api/professors/" + professorB.getId() + "/processes")
                        .with(user(professorUser(professorB.getLogin()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/professors/" + professorB.getId() + "/processes")
                        .with(user(professorUser(professorA.getLogin()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminConsultaProcessosDeQualquerEstudante() throws Exception {
        Student student = createStudent("aluno-" + System.nanoTime() + "@test.com");
        createProcessOwnedBy(student);

        mockMvc.perform(get("/api/students/" + student.getId() + "/processes")
                        .with(user(adminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void estudanteSoVeSeusProcessosEmMeuProcessos() throws Exception {
        Student studentA = createStudent("aluno-a-" + System.nanoTime() + "@test.com");
        Student studentB = createStudent("aluno-b-" + System.nanoTime() + "@test.com");
        Process processA = createProcessOwnedBy(studentA);
        Process processB = createProcessOwnedBy(studentB);

        String body = mockMvc.perform(get("/api/processes/my-processes")
                        .with(user(studentUser(studentA.getLogin()))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        org.junit.jupiter.api.Assertions.assertTrue(body.contains("\"id\":" + processA.getId()));
        org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"id\":" + processB.getId()));
    }

    @Test
    void downloadDeDocumentoApenasParaEnvolvidos() throws Exception {
        Student owner = createStudent("dono-" + System.nanoTime() + "@test.com");
        Student stranger = createStudent("estranho-" + System.nanoTime() + "@test.com");
        Professor rapporteur = createProfessor("relator-" + System.nanoTime() + "@test.com");

        Process process = createProcessOwnedBy(owner);
        process.setDocument("conteudo-fake-do-pdf".getBytes(StandardCharsets.UTF_8));
        process.setDocumentFilename("requerimento.pdf");
        processRepository.save(process);

        Long processId = process.getId();

        mockMvc.perform(get("/processes/" + processId + "/download")
                        .with(user(studentUser(owner.getLogin()))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/processes/" + processId + "/download")
                        .with(user(studentUser(stranger.getLogin()))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/processes/" + processId + "/download")
                        .with(user(adminUser())))
                .andExpect(status().isOk());

        processService.distribute(processId, rapporteur.getId());
        mockMvc.perform(get("/processes/" + processId + "/download")
                        .with(user(professorUser(rapporteur.getLogin()))))
                .andExpect(status().isOk());
    }
}