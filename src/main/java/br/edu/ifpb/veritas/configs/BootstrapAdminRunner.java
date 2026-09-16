package br.edu.ifpb.veritas.configs;

import br.edu.ifpb.veritas.models.Administrator;
import br.edu.ifpb.veritas.services.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class BootstrapAdminRunner implements CommandLineRunner {

    private final AdminService adminService;
    private final Environment env;

    @Override
    public void run(String... args) {
        String email = env.getProperty("ADMIN_BOOTSTRAP_EMAIL");
        String password = env.getProperty("ADMIN_BOOTSTRAP_PASSWORD");
        String name = env.getProperty("ADMIN_BOOTSTRAP_NAME", "Administrador");

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (password.length() < 6) {
            log.error("[ADMIN BOOTSTRAP] Senha fornecida é muito curta (mínimo 6 caracteres). Admin NÃO foi criado.");
            return;
        }
        if (adminService.findByLogin(email).isPresent()) {
            log.warn("[ADMIN BOOTSTRAP] Admin com email '{}' já existe. Ignorando.", email);
            return;
        }

        Administrator admin = new Administrator();
        admin.setName(name);
        admin.setLogin(email);
        admin.setPassword(password);
        adminService.create(admin);

        log.warn("============================================================");
        log.warn("[ADMIN BOOTSTRAP] Admin criado com sucesso.");
        log.warn("  Email: {}", email);
        log.warn("  O ADMIN_BOOTSTRAP_PASSWORD deve ser removido do Cloudflare agora.");
        log.warn("  Execute:  wrangler secret delete ADMIN_BOOTSTRAP_PASSWORD");
        log.warn("  E para segurança:  wrangler secret delete ADMIN_BOOTSTRAP_EMAIL");
        log.warn("============================================================");
    }
}
