package br.edu.ifpb.veritas.configs;

import br.edu.ifpb.veritas.models.*;
import br.edu.ifpb.veritas.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Classe responsável pela população inicial (seed) do banco de dados com dados de teste.
 * Implementa a interface CommandLineRunner para executar ao iniciar a aplicação.
 * 
 * A classe é IDEMPOTENTE - pode ser executada múltiplas vezes sem causar erros,
 * pois verifica se os dados já existem antes de criar novos registros.
 * 
 * @author Flavio Nascimento
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AdminService adminService;

    @Override
    public void run(String... args) throws Exception {

        System.out.println("\n====== INICIANDO POPULAÇÃO DO BANCO DE DADOS ======");
        System.out.println("ℹ️  O DataSeeder é IDEMPOTENTE - pode ser executado múltiplas vezes\n");

        try {
        
            seedAdministrators();
            System.out.println("\n====== BANCO DE DADOS POPULADO COM SUCESSO ======\n");
        
        } 
        catch (Exception e) {
            
            System.err.println("❌ Erro durante execução do DataSeeder: " + e.getMessage());
            e.printStackTrace();
        
        }
    }

    /**
     * SEED 1: Administradores
     * Cria usuários administradores para gerenciar o sistema.
     */
    private void seedAdministrators() {
        System.out.println("📋 Populando administradores...");

        try {
            if (adminService.list().isEmpty()) {
                Administrator admin1 = new Administrator();
                admin1.setName("Admin Master");
                admin1.setLogin("admin");
                admin1.setPassword("admin123");
                admin1.setRegister("ADM001");
                admin1.setPhoneNumber("(83) 99999-0001");
                admin1.setIsActive(true);
                adminService.create(admin1);

                System.out.println("   ✅ 2 administradores criados");
            } else {
                System.out.println("   ⚠️  Administradores já existem no banco. Pulando...");
            }
        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular administradores: " + e.getMessage());
        }
    }
}