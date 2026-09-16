package br.edu.ifpb.veritas.configs;

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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityMassAssignmentTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ProfessorRepository professorRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private CollegiateRepository collegiateRepository;
    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private ProcessRepository processRepository;

    private Student createStudent(String login) {
        Student student = new Student();
        student.setName("Aluno " + login);
        student.setLogin(login);
        student.setPassword("senha123");
        student.setIsActive(true);
        student.setRegister("STU-2026-0999");
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

    private Collegiate createCollegiate(int memberCount) {
        List<Professor> members = new ArrayList<>();
        for (int i = 0; i < memberCount; i++) {
            members.add(createProfessor("colega-" + System.nanoTime() + "-" + i + "@test.com"));
        }
        Collegiate collegiate = new Collegiate();
        collegiate.setDescription("Colegiado teste");
        collegiate.setCreatedAt(LocalDateTime.now());
        collegiate.setCollegiateMemberList(members);
        return collegiateRepository.save(collegiate);
    }

    private UserDetails userWithRoles(String username, String... roles) {
        return User.withUsername(username).password("x").roles(roles).build();
    }

    @Test
    void criacaoDeProcessoIgnoraStatusNumeroEVotoInformadosPeloCliente() throws Exception {
        Student student = createStudent("aluno-" + System.nanoTime() + "@test.com");
        Subject subject = createSubject();

        String body =
                "{\"title\":\"Meu processo\", \"description\":\"D\", " +
                "\"id\": 999, \"status\": \"APPROVED\", " +
                "\"number\": \"XX-00000\", \"rapporteurVote\": \"DEFERIMENTO\", " +
                "\"createdAt\": \"2020-01-01T00:00:00\"}";

        String response = mockMvc.perform(post("/api/processes")
                        .param("subjectId", String.valueOf(subject.getId()))
                        .contentType("application/json")
                        .content(body)
                        .with(user(userWithRoles(student.getLogin(), "STUDENT"))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        org.junit.jupiter.api.Assertions.assertTrue(response.contains("\"status\":\"WAITING\""));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("XX-00000"));
        Long createdId = extractProcessId(response);
        Process saved = processRepository.findById(createdId).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(StatusProcess.WAITING, saved.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(saved.getRapporteurVote());
        org.junit.jupiter.api.Assertions.assertNotEquals("XX-00000", saved.getNumber());
    }

    @Test
    void criacaoDeReuniaoSubstituiParticipantesInformadosPelosMembrosDoColegiado() throws Exception {
        Collegiate collegiate = createCollegiate(3);
        Professor outsider = createProfessor("fora-" + System.nanoTime() + "@test.com");

        String body =
                "{\"collegiateId\": " + collegiate.getId() + ", \"description\": \"Reunião teste\", " +
                "\"status\": \"FINALIZADA\", \"active\": true, " +
                "\"participants\": [{\"id\":" + outsider.getId() + "}]}";

        String response = mockMvc.perform(post("/api/meetings")
                        .contentType("application/json")
                        .content(body)
                        .with(user(userWithRoles("coord-teste", "ADMIN"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        org.junit.jupiter.api.Assertions.assertFalse(response.contains("\"status\":\"FINALIZADA\""));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("\"status\":\"DISPONIVEL\""));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("\"" + outsider.getId() + "\""));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("\"active\":false"));
    }

    @Test
    void atualizacaoDeReuniaoNaoPermiteAlterarStatusNemPauta() throws Exception {
        Collegiate collegiate = createCollegiate(3);
        Meeting meeting = new Meeting();
        meeting.setDescription("Reunião original");
        meeting.setCollegiate(collegiate);
        meeting.setStatus(MeetingStatus.DISPONIVEL);
        meeting.setActive(false);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setParticipants(new ArrayList<>(collegiate.getCollegiateMemberList()));
        meeting = meetingRepository.save(meeting);

        String body =
                "{\"description\": \"Reunião editada\", \"scheduledDate\": \"2026-10-01T10:00:00\", " +
                "\"status\": \"FINALIZADA\", \"active\": true, \"id\": 888}";

        String response = mockMvc.perform(put("/api/meetings/" + meeting.getId())
                        .contentType("application/json")
                        .content(body)
                        .with(user(userWithRoles("coord-teste", "COORDINATOR"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        org.junit.jupiter.api.Assertions.assertTrue(response.contains("Reunião editada"));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("\"status\":\"DISPONIVEL\""));
        org.junit.jupiter.api.Assertions.assertTrue(response.contains("\"active\":false"));
        org.junit.jupiter.api.Assertions.assertFalse(response.contains("\"id\":888"));
    }

    @Test
    void atualizacaoDeEstudanteNaoAlteraMatriculaNemEstadoCadastral() throws Exception {
        Student student = createStudent("aluno-" + System.nanoTime() + "@test.com");

        String body =
                "{\"name\": \"Nome Novo\", \"login\": \"" + student.getLogin() + "\", " +
                "\"register\": \"STU-0000\", \"isActive\": false}";

        mockMvc.perform(put("/api/students/" + student.getId())
                        .contentType("application/json")
                        .content(body)
                        .with(user(userWithRoles("admin-teste", "ADMIN"))))
                .andExpect(status().isOk());

        Student reloaded = studentRepository.findById(student.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Nome Novo", reloaded.getName());
        org.junit.jupiter.api.Assertions.assertEquals("STU-2026-0999", reloaded.getRegister());
        org.junit.jupiter.api.Assertions.assertTrue(reloaded.getIsActive());
    }

    @Test
    void atualizacaoDeProfessorNaoAlteraCoordenadoria() throws Exception {
        Professor professor = createProfessor("prof-" + System.nanoTime() + "@test.com");
        professor.setCoordinator(true);
        professorRepository.save(professor);

        String body =
                "{\"name\": \"Nome Novo\", \"login\": \"" + professor.getLogin() + "\", " +
                "\"coordinator\": false}";

        mockMvc.perform(put("/api/professors/" + professor.getId())
                        .contentType("application/json")
                        .content(body)
                        .with(user(userWithRoles("admin-teste", "ADMIN"))))
                .andExpect(status().isOk());

        Professor reloaded = professorRepository.findById(professor.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals("Nome Novo", reloaded.getName());
        org.junit.jupiter.api.Assertions.assertTrue(reloaded.getCoordinator());
    }

    private Long extractProcessId(String json) {
        String marker = "\"id\":";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf(",", start);
        if (end == -1) {
            end = json.indexOf("}", start);
        }
        return Long.parseLong(json.substring(start, end).trim());
    }
}