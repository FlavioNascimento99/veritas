package br.edu.ifpb.veritas.configs;

import br.edu.ifpb.veritas.enums.DecisionType;
import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.models.*;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DataSeeder: Classe responsável por popular o banco de dados com dados iniciais
 * para testes e desenvolvimento.
 *
 * Executa automaticamente ao iniciar a aplicação (implementa CommandLineRunner).
 *
 * IMPORTANTE: É IDEMPOTENTE - Pode ser executado múltiplas vezes sem causar erros.
 * Verifica a existência de dados antes de criar novos registros.
 *
 * FLUXO DE VOTAÇÃO:
 * 1. Professor RELATOR vota: DecisionType (DEFERIMENTO ou INDEFERIMENTO)
 * 2. Membros do COLEGIADO votam: VoteType (COM_RELATOR ou DIVERGENTE)
 * 3. Sistema calcula resultado:
 *    - Maioria COM_RELATOR → resultado = decisão do relator
 *    - Maioria DIVERGENTE → resultado = contrário da decisão do relator
 *
 * ORDEM DE EXECUÇÃO (CRÍTICA):
 * 1. Administradores
 * 2. Assuntos (Subjects)
 * 3. Professores
 * 4. Estudantes
 * 5. Colegiados
 * 6. Processos (criados por alunos - estado WAITING)
 * 7. Distribuição de Processos (coordenador atribui relator - estado UNDER_ANALISYS)
 * 8. Votos dos Relatores (DecisionType - OBRIGATÓRIO antes de criar reuniões)
 * 9. Reuniões (só podem ter processos com relator que já votou)
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AdminService adminService;
    private final StudentService studentService;
    private final ProfessorService professorService;
    private final SubjectService subjectService;
    private final CollegiateService collegiateService;
    private final ProcessService processService;
    private final MeetingService meetingService;
    private final VoteService voteService;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n====== INICIANDO POPULAÇÃO DO BANCO DE DADOS ======");
        System.out.println("ℹ️  O DataSeeder é IDEMPOTENTE - pode ser executado múltiplas vezes\n");

        try {
            // ✅ Apenas administradores ativados para testes
            seedAdministrators();
            
            // ❌ COMENTADO: Todas as outras seeds sendo debugadas
            // seedSubjects();
            // seedProfessors();
            // seedStudents();
            // seedCollegiates();
            // seedProcesses();
            // seedDistributeProcesses();
            // seedRapporteurVotes();
            // seedMeetings();

            System.out.println("\n====== BANCO DE DADOS POPULADO COM SUCESSO ======\n");
        } catch (Exception e) {
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
            if (adminService.listAdmins().isEmpty()) {
                Administrator admin1 = new Administrator();
                admin1.setName("Admin Master");
                admin1.setLogin("admin");
                admin1.setPassword("admin123");
                admin1.setRegister("ADM001");
                admin1.setPhoneNumber("(83) 99999-0001");
                admin1.setIsActive(true);
                adminService.create(admin1);

                Administrator admin2 = new Administrator();
                admin2.setName("Administrador Secundário");
                admin2.setLogin("admin2");
                admin2.setPassword("admin456");
                admin2.setRegister("ADM002");
                admin2.setPhoneNumber("(83) 99999-0002");
                admin2.setIsActive(true);
                adminService.create(admin2);

                System.out.println("   ✅ 2 administradores criados");
            } else {
                System.out.println("   ⚠️  Administradores já existem no banco. Pulando...");
            }
        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular administradores: " + e.getMessage());
        }
    }

    /**
     * SEED 2: Assuntos de Processos
     * Cria os tipos de assuntos que podem ser solicitados pelos alunos.
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedSubjects() {
        System.out.println("\n📋 Populando assuntos de processos...");

        try {
            if (subjectService.findAll().isEmpty()) {
                String[] subjectTitles = {
                        "Reabertura de Matrícula",
                        "Dilatação de Prazo",
                        "Trancamento de Disciplina",
                        "Aproveitamento de Estudos",
                        "Transferência de Curso",
                        "Revisão de Nota",
                        "Quebra de Pré-requisito",
                        "Cancelamento de Disciplina",
                        "Recurso de Avaliação"
                };

                for (String title : subjectTitles) {
                    Subject subject = new Subject();
                    subject.setTitle(title);
                    subject.setDescription("Assunto referente a " + title.toLowerCase());
                    subject.setActive(true);
                    subjectService.create(subject);
                }

                System.out.println("   ✅ " + subjectTitles.length + " assuntos criados");
            } else {
                System.out.println("   ⚠️  Assuntos já existem no banco. Pulando...");
            }
        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular assuntos: " + e.getMessage());
        }
    }
    */

    /**
     * SEED 3: Professores
     * Cria professores (alguns como coordenadores).
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedProfessors() {
        System.out.println("\n📋 Populando professores...");

        try {
            if (professorService.findAll().isEmpty()) {
                // Professor 1: Coordenador
                Professor prof1 = new Professor();
                prof1.setName("Dr. João Silva");
                prof1.setLogin("joao.silva");
                prof1.setPassword("prof123");
                prof1.setRegister("PROF001");
                prof1.setPhoneNumber("(83) 98888-0001");
                prof1.setCoordinator(true);
                prof1.setIsActive(true);
                professorService.create(prof1);

                // Professor 2: Membro do colegiado
                Professor prof2 = new Professor();
                prof2.setName("Dra. Maria Santos");
                prof2.setLogin("maria.santos");
                prof2.setPassword("prof123");
                prof2.setRegister("PROF002");
                prof2.setPhoneNumber("(83) 98888-0002");
                prof2.setCoordinator(false);
                prof2.setIsActive(true);
                professorService.create(prof2);

                // Professor 3: Membro do colegiado
                Professor prof3 = new Professor();
                prof3.setName("Dr. Carlos Oliveira");
                prof3.setLogin("carlos.oliveira");
                prof3.setPassword("prof123");
                prof3.setRegister("PROF003");
                prof3.setPhoneNumber("(83) 98888-0003");
                prof3.setCoordinator(false);
                prof3.setIsActive(true);
                professorService.create(prof3);

                // Professor 4: Membro do colegiado
                Professor prof4 = new Professor();
                prof4.setName("Dra. Ana Costa");
                prof4.setLogin("ana.costa");
                prof4.setPassword("prof123");
                prof4.setRegister("PROF004");
                prof4.setPhoneNumber("(83) 98888-0004");
                prof4.setCoordinator(false);
                prof4.setIsActive(true);
                professorService.create(prof4);

                // Professor 5: Membro do colegiado
                Professor prof5 = new Professor();
                prof5.setName("Dr. Pedro Mendes");
                prof5.setLogin("pedro.mendes");
                prof5.setPassword("prof123");
                prof5.setRegister("PROF005");
                prof5.setPhoneNumber("(83) 98888-0005");
                prof5.setCoordinator(false);
                prof5.setIsActive(true);
                professorService.create(prof5);

                System.out.println("   ✅ 5 professores criados");
            } else {
                System.out.println("   ⚠️  Professores já existem no banco. Pulando...");
            }
        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular professores: " + e.getMessage());
        }
    }
    */

    /**
     * SEED 4: Estudantes
     * Cria alunos que podem criar processos.
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedStudents() {
        System.out.println("\n📋 Populando estudantes...");

        try {
            if (studentService.findAll().isEmpty()) {
                Student student1 = new Student();
                student1.setName("Lucas Ferreira");
                student1.setLogin("lucas.ferreira");
                student1.setPassword("aluno123");
                student1.setRegister("20231001");
                student1.setPhoneNumber("(83) 97777-0001");
                student1.setIsActive(true);
                studentService.create(student1);

                Student student2 = new Student();
                student2.setName("Juliana Souza");
                student2.setLogin("juliana.souza");
                student2.setPassword("aluno123");
                student2.setRegister("20231002");
                student2.setPhoneNumber("(83) 97777-0002");
                student2.setIsActive(true);
                studentService.create(student2);

                Student student3 = new Student();
                student3.setName("Rafael Lima");
                student3.setLogin("rafael.lima");
                student3.setPassword("aluno123");
                student3.setRegister("20231003");
                student3.setPhoneNumber("(83) 97777-0003");
                student3.setIsActive(true);
                studentService.create(student3);

                Student student4 = new Student();
                student4.setName("Mariana Oliveira");
                student4.setLogin("mariana.oliveira");
                student4.setPassword("aluno123");
                student4.setRegister("20231004");
                student4.setPhoneNumber("(83) 97777-0004");
                student4.setIsActive(true);
                studentService.create(student4);

                System.out.println("   ✅ 4 estudantes criados");
            } else {
                System.out.println("   ⚠️  Estudantes já existem no banco. Pulando...");
            }
        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular estudantes: " + e.getMessage());
        }
    }
    */

    /**
     * SEED 5: Colegiados
     * Cria colegiados com membros (professores).
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedCollegiates() {
        System.out.println("\n📋 Populando colegiados...");

        try {
            if (collegiateService.findAll().isEmpty()) {
                List<Professor> allProfessors = professorService.findAll();

                if (allProfessors.size() >= 5) {
                    Collegiate collegiate1 = new Collegiate();
                    collegiate1.setDescription("Colegiado de Ciência da Computação");
                    collegiate1.setCreatedAt(LocalDateTime.now().minusMonths(12));
                    collegiate1.setRapporteur(allProfessors.get(0)); // Dr. João (Coordenador)
                    collegiate1.setCollegiateMemberList(new ArrayList<>(allProfessors));
                    collegiateService.create(collegiate1);

                    System.out.println("   ✅ 1 colegiado criado com " + allProfessors.size() + " membros");
                } else {
                    System.out.println("   ⚠️  Não há professores suficientes para criar colegiado. Mínimo: 5");
                }
            } else {
                System.out.println("   ⚠️  Colegiados já existem no banco. Pulando...");
            }
        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular colegiados: " + e.getMessage());
        }
    }
    */

    /**
     * SEED 6: Processos
     * Cria processos de alunos em estado WAITING (aguardando distribuição).
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedProcesses() {
        System.out.println("\n📋 Populando processos...");

        try {
            List<Process> existingProcesses = processService.findAllProcesses();
            if (!existingProcesses.isEmpty()) {
                System.out.println("   ⚠️  Processos já existem no banco (" + existingProcesses.size() +
                        " encontrados). Pulando criação...");
                return;
            }

            List<Student> students = studentService.findAll();
            List<Subject> subjects = subjectService.findAll();

            if (students.isEmpty() || subjects.isEmpty()) {
                System.out.println("   ⚠️  Não há estudantes ou assuntos para criar processos.");
                return;
            }

            // Processo 1: Lucas - Reabertura de Matrícula
            Process process1 = new Process();
            process1.setTitle("Solicitação de Reabertura de Matrícula");
            process1.setDescription("Solicito a reabertura de matrícula devido a problemas de saúde graves que me impediram de efetuar a matrícula no prazo regulamentar.");
            processService.createProcess(process1, students.get(0).getId(), subjects.get(0).getId());

            // Processo 2: Juliana - Dilatação de Prazo
            Process process2 = new Process();
            process2.setTitle("Solicitação de Dilatação de Prazo para TCC");
            process2.setDescription("Solicito dilatação de prazo de 6 meses para conclusão do Trabalho de Conclusão de Curso devido a complicações no desenvolvimento da pesquisa.");
            processService.createProcess(process2, students.get(1).getId(), subjects.get(1).getId());

            // Processo 3: Rafael - Trancamento de Disciplina
            Process process3 = new Process();
            process3.setTitle("Solicitação de Trancamento de Disciplina");
            process3.setDescription("Solicito trancamento da disciplina de Cálculo II devido a incompatibilidade de horário com meu trabalho.");
            processService.createProcess(process3, students.get(2).getId(), subjects.get(2).getId());

            // Processo 4: Lucas - Aproveitamento de Estudos
            Process process4 = new Process();
            process4.setTitle("Solicitação de Aproveitamento de Estudos");
            process4.setDescription("Solicito aproveitamento da disciplina de Programação I cursada na UFPB.");
            processService.createProcess(process4, students.get(0).getId(), subjects.get(3).getId());

            // Processo 5: Mariana - Revisão de Nota
            Process process5 = new Process();
            process5.setTitle("Solicitação de Revisão de Nota");
            process5.setDescription("Solicito revisão da nota da prova final de Estrutura de Dados, pois acredito que houve erro na correção.");
            processService.createProcess(process5, students.get(3).getId(), subjects.get(5).getId());

            // Processo 6: Juliana - Quebra de Pré-requisito
            Process process6 = new Process();
            process6.setTitle("Solicitação de Quebra de Pré-requisito");
            process6.setDescription("Solicito quebra de pré-requisito para cursar Banco de Dados II antes de Banco de Dados I, pois já possuo conhecimento prévio.");
            processService.createProcess(process6, students.get(1).getId(), subjects.get(6).getId());

            System.out.println("   ✅ 6 processos criados (todos em estado WAITING)");
        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular processos: " + e.getMessage());
        }
    }
    */

    /**
     * SEED 7: Distribuição de Processos
     * Coordenador distribui processos para professores relatores.
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedDistributeProcesses() {
        System.out.println("\n📋 Distribuindo processos para relatores...");

        try {
            List<Process> waitingProcesses = processService.findWaitingProcesses();

            if (waitingProcesses.isEmpty()) {
                System.out.println("   ℹ️  Não há processos em estado WAITING para distribuir.");

                List<Process> underAnalysis = processService.findAllProcesses().stream()
                        .filter(p -> p.getStatus() == StatusProcess.UNDER_ANALISYS)
                        .collect(Collectors.toList());

                if (!underAnalysis.isEmpty()) {
                    System.out.println("   ℹ️  Já existem " + underAnalysis.size() +
                            " processos distribuídos (UNDER_ANALISYS)");
                }
                return;
            }

            List<Professor> professors = professorService.findAll();

            if (professors.size() < 2) {
                System.out.println("   ⚠️  Não há professores suficientes para distribuir processos.");
                return;
            }

            int distributed = 0;

            // Distribui processos (deixa alguns aguardando para teste)
            for (int i = 0; i < Math.min(4, waitingProcesses.size()); i++) {
                Process process = waitingProcesses.get(i);

                if (process.getProcessRapporteur() != null) {
                    System.out.println("   ⚠️  Processo " + process.getNumber() +
                            " já possui relator (" + process.getProcessRapporteur().getName() + "). Pulando...");
                    continue;
                }

                // Alterna entre professores 2, 3, 4 (pula coordenador)
                Professor relator = professors.get((i % 3) + 1);

                try {
                    processService.distribute(process.getId(), relator.getId());
                    System.out.println("   ✅ Processo " + process.getNumber() +
                            " distribuído para " + relator.getName());
                    distributed++;
                } catch (IllegalStateException e) {
                    System.out.println("   ⚠️  Não foi possível distribuir processo " +
                            process.getNumber() + ": " + e.getMessage());
                }
            }

            System.out.println("   ✅ " + distributed + " processos distribuídos nesta execução");

            if (waitingProcesses.size() > distributed) {
                System.out.println("   ℹ️  " + (waitingProcesses.size() - distributed) +
                        " processos permanecem em WAITING para testes");
            }

        } catch (Exception e) {
            System.err.println("   ❌ Erro ao distribuir processos: " + e.getMessage());
        }
    }
    */

    /**
     * SEED 8: Votos dos Relatores
     * Professor relator vota pelo DEFERIMENTO ou INDEFERIMENTO do processo.
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedRapporteurVotes() {
        System.out.println("\n📋 Populando votos dos relatores (DecisionType: DEFERIMENTO/INDEFERIMENTO)...");

        try {
            List<Process> processes = processService.findAllProcesses();

            List<Process> processesUnderAnalysis = processes.stream()
                    .filter(p -> p.getStatus() == StatusProcess.UNDER_ANALISYS)
                    .filter(p -> p.getProcessRapporteur() != null)
                    .collect(Collectors.toList());

            if (processesUnderAnalysis.isEmpty()) {
                System.out.println("   ℹ️  Não há processos UNDER_ANALISYS com relator para votar.");
                return;
            }

            int votosRegistrados = 0;
            int votosJaExistentes = 0;

            // Tenta votar nos processos (deixa alguns sem voto para teste)
            for (int i = 0; i < Math.min(3, processesUnderAnalysis.size()); i++) {
                Process process = processesUnderAnalysis.get(i);
                Professor relator = process.getProcessRapporteur();

                // VALIDAÇÃO: Verifica se relator já votou
                if (process.getRapporteurVote() != null) {
                    System.out.println("   ℹ️  Processo " + process.getNumber() +
                            " - Relator " + relator.getName() +
                            " já votou (" + process.getRapporteurVote() + "). Pulando...");
                    votosJaExistentes++;
                    continue;
                }

                // Alterna entre DEFERIMENTO e INDEFERIMENTO
                DecisionType decision = (i % 2 == 0) ? DecisionType.DEFERIMENTO : DecisionType.INDEFERIMENTO;

                String justification = decision == DecisionType.DEFERIMENTO
                        ? "Após análise criteriosa da documentação apresentada, considero que o pedido do aluno está bem fundamentado e atende aos requisitos regulamentares. Voto pelo DEFERIMENTO."
                        : "Após análise da documentação, verifico que o pedido não atende aos requisitos estabelecidos pela instituição. Faltam documentos comprobatórios essenciais. Voto pelo INDEFERIMENTO.";

                try {
                    voteService.registerRapporteurDecision(
                            process.getId(),
                            relator.getId(),
                            decision,
                            justification
                    );

                    System.out.println("   ✅ Relator " + relator.getName() +
                            " votou " + decision +
                            " no processo " + process.getNumber());
                    votosRegistrados++;

                } catch (IllegalStateException e) {
                    if (e.getMessage().contains("já registrou sua decisão")) {
                        System.out.println("   ℹ️  Processo " + process.getNumber() +
                                " - Relator já votou anteriormente. Pulando...");
                        votosJaExistentes++;
                    } else {
                        System.out.println("   ⚠️  Não foi possível registrar voto do relator no processo " +
                                process.getNumber() + ": " + e.getMessage());
                    }
                }
            }

            System.out.println("   ✅ " + votosRegistrados + " votos de relatores registrados nesta execução");

            if (votosJaExistentes > 0) {
                System.out.println("   ℹ️  " + votosJaExistentes + " processos já possuíam voto do relator");
            }

            int semVoto = processesUnderAnalysis.size() - votosRegistrados - votosJaExistentes;
            if (semVoto > 0) {
                System.out.println("   ℹ️  " + semVoto + " processos permanecem sem voto do relator (para testes)");
            }

        } catch (Exception e) {
            System.err.println("   ❌ Erro ao popular votos dos relatores: " + e.getMessage());
        }
    }
    */

    /**
     * SEED 9: Reuniões
     * Coordenador cria reuniões e define pauta com processos elegíveis.
     * ❌ COMENTADO - Debugando problemas de autenticação
     */
    /*
    private void seedMeetings() {
        System.out.println("\n📋 Populando reuniões...");

        try {
            List<Meeting> existingMeetings = meetingService.findAll();

            if (!existingMeetings.isEmpty()) {
                System.out.println("   ⚠️  Reuniões já existem no banco (" + existingMeetings.size() +
                        " encontradas). Pulando criação...");
                return;
            }

            List<Collegiate> collegiates = collegiateService.findAll();
            List<Process> processes = processService.findAllProcesses();
            List<Professor> professors = professorService.findAll();

            if (collegiates.isEmpty()) {
                System.out.println("   ⚠️  Não há colegiados para criar reuniões.");
                return;
            }

            if (processes.isEmpty() || professors.size() < 4) {
                System.out.println("   ⚠️  Não há processos ou professores suficientes para criar reuniões.");
                return;
            }

            Collegiate collegiate = collegiates.get(0);

            // Filtra processos elegíveis
            List<Process> eligibleProcesses = processes.stream()
                    .filter(p -> p.getStatus() == StatusProcess.UNDER_ANALISYS)
                    .filter(p -> p.getRapporteurVote() != null)
                    .filter(p -> p.getMeeting() == null)
                    .collect(Collectors.toList());

            if (eligibleProcesses.size() < 2) {
                System.out.println("   ⚠️  Não há processos elegíveis suficientes para criar reuniões.");
                System.out.println("      Processos precisam estar UNDER_ANALISYS, com voto do relator e sem reunião.");
                return;
            }

            // REUNIÃO 1: Programada (DISPONÍVEL)
            List<Long> processIds1 = eligibleProcesses.subList(0, Math.min(2, eligibleProcesses.size())).stream()
                    .map(Process::getId)
                    .collect(Collectors.toList());

            List<Long> participantIds1 = professors.stream()
                    .limit(4)
                    .map(Professor::getId)
                    .collect(Collectors.toList());

            Meeting meeting1 = meetingService.createMeetingWithAgenda(
                    collegiate.getId(),
                    LocalDateTime.now().plusDays(7),
                    processIds1,
                    participantIds1,
                    "Reunião Ordinária do Colegiado - Fevereiro/2026"
            );

            System.out.println("   ✅ Reunião 1 criada (DISPONÍVEL) - " + processIds1.size() +
                    " processos, " + participantIds1.size() + " participantes");

            // REUNIÃO 2: Antiga (FINALIZADA)
            if (eligibleProcesses.size() >= 3) {
                List<Long> processIds2 = eligibleProcesses.subList(2, 3).stream()
                        .map(Process::getId)
                        .collect(Collectors.toList());

                List<Long> participantIds2 = professors.stream()
                        .limit(4)
                        .map(Professor::getId)
                        .collect(Collectors.toList());

                Meeting meeting2 = meetingService.createMeetingWithAgenda(
                        collegiate.getId(),
                        LocalDateTime.now().minusMonths(2),
                        processIds2,
                        participantIds2,
                        "Reunião Ordinária do Colegiado - Dezembro/2025"
                );

                meetingService.updateStatus(meeting2.getId(), MeetingStatus.FINALIZADA);

                System.out.println("   ✅ Reunião 2 criada (FINALIZADA) - " + processIds2.size() +
                        " processos, " + participantIds2.size() + " participantes");
            }

            System.out.println("   ✅ Reuniões criadas com sucesso");

        } catch (Exception e) {
            System.err.println("   ❌ Erro ao criar reuniões: " + e.getMessage());
        }
    }
    */
}