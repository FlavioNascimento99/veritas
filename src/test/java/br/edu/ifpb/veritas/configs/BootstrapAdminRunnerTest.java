package br.edu.ifpb.veritas.configs;

import br.edu.ifpb.veritas.models.Administrator;
import br.edu.ifpb.veritas.services.AdminService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminRunnerTest {

    @Mock
    private AdminService adminService;
    @Mock
    private Environment environment;
    @InjectMocks
    private BootstrapAdminRunner runner;

    @Test
    void semEnvVarsNaoCriaAdmin() throws Exception {
        when(environment.getProperty("ADMIN_BOOTSTRAP_EMAIL")).thenReturn(null);
        when(environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD")).thenReturn(null);
        when(environment.getProperty("ADMIN_BOOTSTRAP_NAME", "Administrador")).thenReturn("Administrador");

        runner.run();

        verify(adminService, never()).create(any());
        verifyNoInteractions(adminService);
    }

    @Test
    void emailDefinidoMasSemSenhaNaoCriaAdmin() throws Exception {
        when(environment.getProperty("ADMIN_BOOTSTRAP_EMAIL")).thenReturn("admin@test.com");
        when(environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD")).thenReturn(null);
        when(environment.getProperty("ADMIN_BOOTSTRAP_NAME", "Administrador")).thenReturn("Administrador");

        runner.run();

        verify(adminService, never()).create(any());
    }

    @Test
    void senhaCurtaNaoCriaAdmin() throws Exception {
        when(environment.getProperty("ADMIN_BOOTSTRAP_EMAIL")).thenReturn("admin@test.com");
        when(environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD")).thenReturn("12345");
        when(environment.getProperty("ADMIN_BOOTSTRAP_NAME", "Administrador")).thenReturn("Administrador");

        runner.run();

        verify(adminService, never()).create(any());
    }

    @Test
    void adminJaExistenteNaoCriaNovo() throws Exception {
        when(environment.getProperty("ADMIN_BOOTSTRAP_EMAIL")).thenReturn("admin@test.com");
        when(environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD")).thenReturn("123456");
        when(environment.getProperty("ADMIN_BOOTSTRAP_NAME", "Administrador")).thenReturn("Administrador");
        when(adminService.findByLogin("admin@test.com")).thenReturn(Optional.of(new Administrator()));

        runner.run();

        verify(adminService, never()).create(any());
    }

    @Test
    void criaAdminComSucesso() throws Exception {
        when(environment.getProperty("ADMIN_BOOTSTRAP_EMAIL")).thenReturn("admin@test.com");
        when(environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD")).thenReturn("123456");
        when(environment.getProperty("ADMIN_BOOTSTRAP_NAME", "Administrador")).thenReturn("Admin Sobrenome");
        when(adminService.findByLogin("admin@test.com")).thenReturn(Optional.empty());

        runner.run();

        verify(adminService).create(argThat(a ->
                "admin@test.com".equals(a.getLogin())
                        && "Admin Sobrenome".equals(a.getName())
                        && "123456".equals(a.getPassword())
        ));
    }

    @Test
    void criaAdminComNomeDefault() throws Exception {
        when(environment.getProperty("ADMIN_BOOTSTRAP_EMAIL")).thenReturn("admin@test.com");
        when(environment.getProperty("ADMIN_BOOTSTRAP_PASSWORD")).thenReturn("123456");
        when(environment.getProperty("ADMIN_BOOTSTRAP_NAME", "Administrador")).thenReturn("Administrador");
        when(adminService.findByLogin("admin@test.com")).thenReturn(Optional.empty());

        runner.run();

        verify(adminService).create(argThat(a ->
                "Administrador".equals(a.getName())
        ));
    }
}
