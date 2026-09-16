package br.edu.ifpb.veritas.configs;

import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RestExceptionHandlerTest.StubErrorConfig.class)
class RestExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration(proxyBeanMethods = false)
    static class StubErrorConfig {
        @Bean
        StubErrorController stubErrorController() {
            return new StubErrorController();
        }
    }

    @RestController
    static class StubErrorController {
        @GetMapping("/stub/internal-error")
        public String internalError() {
            throw new RuntimeException("MENSAGEM_INTERNA_500_NAO_DEVE_VAZAR");
        }

        @GetMapping("/stub/business-error")
        public String businessError() {
            throw new IllegalArgumentException("MENSAGEM_NEGOCIO_400_DEVE_MANTER");
        }
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void erro500NaoVazaMensagemInterna() throws Exception {
        mockMvc.perform(get("/stub/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erro interno inesperado"))
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("MENSAGEM_INTERNA_500_NAO_DEVE_VAZAR"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void erroDeNegocio400MantemMensagem() throws Exception {
        mockMvc.perform(get("/stub/business-error"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("MENSAGEM_NEGOCIO_400_DEVE_MANTER"));
    }
}