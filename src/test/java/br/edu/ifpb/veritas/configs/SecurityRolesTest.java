package br.edu.ifpb.veritas.configs;

import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRolesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProfessorRepository professorRepository;

    @Test
    void anonimoRedirecionadoParaLoginEmApi() throws Exception {
        mockMvc.perform(get("/api/collegiates"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void estudanteNaoAcessaAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void estudanteNaoDistribuiProcesso() throws Exception {
        mockMvc.perform(patch("/api/processes/1/distribute")
                        .contentType("application/json")
                        .content("{\"professorId\": 1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void estudanteNaoConsultaProcessosDeProfessor() throws Exception {
        mockMvc.perform(get("/api/processes/designated-to-me").param("professorId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void professorConsultaProcessosDesignados() throws Exception {
        String login = "relator-" + System.nanoTime() + "@test.com";
        Professor professor = new Professor();
        professor.setName("Relator Teste");
        professor.setLogin(login);
        professor.setPassword("123456");
        professor.setCoordinator(false);
        professor.setIsActive(true);
        professorRepository.save(professor);

        mockMvc.perform(get("/api/processes/designated-to-me")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(org.springframework.security.core.userdetails.User
                                        .withUsername(login).password("x").roles("PROFESSOR").build())))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAcessaAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PROFESSOR")
    void professorNaoCriaReuniao() throws Exception {
        mockMvc.perform(post("/api/meetings")
                        .contentType("application/json")
                        .content("{\"collegiateId\": 999999, \"description\": \"Reunião teste\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void coordenadorChegaAoHandlerDeCriacaoDeReuniao() throws Exception {
        mockMvc.perform(post("/api/meetings")
                        .contentType("application/json")
                        .content("{\"id\": 1, \"description\": \"Reunião teste\", "
                                + "\"collegiate\": {\"id\": 999999}, \"status\": \"DISPONIVEL\"}"))
                .andExpect(result ->
                        org.junit.jupiter.api.Assertions.assertNotEquals(403, result.getResponse().getStatus()));
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void coordenadorNaoAcessaAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoListaMeusProcessosDeEstudante() throws Exception {
        mockMvc.perform(get("/api/processes/my-processes").param("studentId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void estudanteNaoAcessaPaginaAdmin() throws Exception {
        mockMvc.perform(get("/admin/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAcessaPaginaAdmin() throws Exception {
        mockMvc.perform(get("/admin/students"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void estudanteNaoFazAssignDeProcessoViaWeb() throws Exception {
        mockMvc.perform(post("/dashboard/assign")
                        .param("processId", "1")
                        .param("professorId", "1")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "COORDINATOR")
    void coordenadorCriaAdminViaApiNegado() throws Exception {
        mockMvc.perform(post("/api/admin")
                        .contentType("application/json")
                        .content("{\"name\":\"X\",\"login\":\"x@x.com\",\"password\":\"123456\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCriaAdminViaApiPermitido() throws Exception {
        mockMvc.perform(post("/api/admin")
                        .contentType("application/json")
                        .content("{\"name\":\"Admin Teste\",\"login\":\"adm-rbac-"
                                + System.nanoTime()
                                + "@test.com\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminNaoCriaProcessoComoEstudante() throws Exception {
        mockMvc.perform(post("/api/processes")
                        .param("studentId", "1")
                        .param("subjectId", "1")
                        .contentType("application/json")
                        .content("{\"id\": 1, \"title\": \"Título\", \"description\": \"Descrição\", "
                                + "\"createdAt\": \"2026-09-16T10:00:00\", \"status\": \"WAITING\", "
                                + "\"processCreator\": {\"id\": 2}, \"subject\": {\"id\": 3}}"))
                .andExpect(status().isForbidden());
    }
}