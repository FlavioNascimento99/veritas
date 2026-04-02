package br.edu.ifpb.veritas.controllers.web;

import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.enums.VoteType;
import br.edu.ifpb.veritas.models.Collegiate;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StudentService studentService;
    private final ProfessorService professorService;
    private final ProcessService processService;
    private final SubjectService subjectService;
    private final MeetingService meetingService;
    private final VoteService voteService;
    private final CollegiateService collegiateService;
    private final AdminService adminService;

    // Número máximo de processos por página (REQNAOFUNC 9)
    private static final int PAGE_SIZE = 5;

    // Dashboard principal com paginação para coordenador
    @GetMapping
    public String dashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long professorId,
            Model model,
            Authentication authentication
    ) {
        String login = authentication.getName();

        // ========== Verificar se é administrador ==========
        var adminOpt = adminService.findByLogin(login);
        if (adminOpt.isPresent()) {
            var admin = adminOpt.get();
            model.addAttribute("admin", admin);
            model.addAttribute("colleges", collegiateService.findAllWithMembers());
            
            // Add counts for dashboard
            List<Professor> professors = professorService.findAll();
            List<Student> students = studentService.findAll();
            List<Process> processes = processService.findAll();
            List<?> subjects = subjectService.findAll();
            
            model.addAttribute("professorsCount", professors.size());
            model.addAttribute("studentsCount", students.size());
            model.addAttribute("processesCount", processes.size());
            model.addAttribute("subjectsCount", subjects.size());
            
            model.addAttribute("pageTitle", "Dashboard - Administrador");
            model.addAttribute("activePage", "dashboard");
            model.addAttribute("mainContent", "pages/dashboard-admin :: content");
            return "home";
        }

        // ========== Verificar tipo de usuário ==========

        // Verifica se é estudante
        var studentOpt = studentService.findByLogin(login);
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            model.addAttribute("student", student);
            model.addAttribute("processes", processService.listByStudent(student.getId()));
            model.addAttribute("subjects", subjectService.findAll());
            model.addAttribute("pageTitle", "Dashboard - Estudante");
            model.addAttribute("activePage", "dashboard");
            model.addAttribute("mainContent", "pages/dashboard-student :: content");
            return "home";
        }

        // Verifica se é professor
        var professorOpt = professorService.findByLogin(login);
        if (professorOpt.isPresent()) {
            Professor professor = professorOpt.get();
            model.addAttribute("professor", professor);

            // Se for coordenador, mostra dashboard do coordenador COM PAGINAÇÃO
            if (Boolean.TRUE.equals(professor.getCoordinator())) {

                // ========== REQNAOFUNC 9: Paginação ==========
                // Cria objeto Pageable com ordenação por data de criação (mais recentes primeiro)
                Pageable pageable = PageRequest.of(page, PAGE_SIZE, Sort.by("createdAt").descending());

                // Busca processos paginados com filtros
                Page<Process> processPage = processService.findAllFilteredPaged(
                        status, studentId, professorId, pageable
                );

                // Adiciona dados de paginação ao modelo
                model.addAttribute("allProcesses", processPage.getContent());
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", processPage.getTotalPages());
                model.addAttribute("totalElements", processPage.getTotalElements());
                model.addAttribute("hasNext", processPage.hasNext());
                model.addAttribute("hasPrevious", processPage.hasPrevious());
                model.addAttribute("pageSize", PAGE_SIZE);

                // Mantém os filtros para a navegação entre páginas
                model.addAttribute("filterStatus", status);
                model.addAttribute("filterStudentId", studentId);
                model.addAttribute("filterProfessorId", professorId);

                // Dados para os dropdowns de filtro
                model.addAttribute("professors", professorService.findAll());
                model.addAttribute("students", studentService.findAll());

                model.addAttribute("pageTitle", "Dashboard - Coordenador");
                model.addAttribute("activePage", "dashboard");
                model.addAttribute("mainContent", "pages/dashboard-coordinator :: content");
                return "home";
            }

            // Professor comum (não coordenador)
            model.addAttribute("processes", processService.listByProfessor(professor.getId()));

            // ========== NOVA FUNCIONALIDADE: Processos pendentes e votados ==========
            // Processos pendentes de voto para este professor
            List<Process> pendingVotes = processService.findPendingVotesByProfessor(professor.getId());
            model.addAttribute("pendingVotes", pendingVotes);

            // Processos já votados (histórico)
            List<Process> votedProcesses = processService.findVotedProcessesByProfessor(professor.getId());
            model.addAttribute("votedProcesses", votedProcesses);

            // Reuniões do professor
            List<Meeting> meetings = findCollegiateMeetingsForProfessor(professor.getId(), null);
            model.addAttribute("meetings", meetings);

            model.addAttribute("pageTitle", "Dashboard - Professor");
            model.addAttribute("activePage", "dashboard");
            model.addAttribute("mainContent", "pages/dashboard-professor :: content");
            return "home";
        }

        // Usuário não encontrado - redireciona para login
        return "redirect:/login";
    }

    /**
     * API: Retorna processos elegíveis para pauta (UNDER_ANALISYS com relator que votou)
     */
    @GetMapping("/api/eligible-processes")
    public ResponseEntity<List<Map<String, Object>>> getEligibleProcesses(Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                return ResponseEntity.status(403).build();
            }

            List<Process> eligible = processService.findAllProcesses().stream()
                    .filter(p -> p.getStatus() == StatusProcess.UNDER_ANALISYS)
                    .filter(p -> p.getRapporteurVote() != null)
                    .collect(Collectors.toList());

            List<Map<String, Object>> result = eligible.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("number", p.getNumber());
                map.put("title", p.getTitle());
                map.put("status", p.getStatus().toString());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * API: Retorna lista de professores
     */
    @GetMapping("/api/professors")
    public ResponseEntity<List<Map<String, Object>>> getProfessors(Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                return ResponseEntity.status(403).build();
            }

            List<Map<String, Object>> result = professorService.findAll().stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("name", p.getName());
                map.put("login", p.getLogin());
                map.put("coordinator", p.getCoordinator());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * POST: Criar nova reunião
     */
    @PostMapping("/create-meeting")
    public String createMeeting(Authentication authentication,
                               @RequestParam(required = false) Long collegiateId,
                               @RequestParam(required = false) List<Long> processIds,
                               @RequestParam(required = false) String description,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("=== INICIANDO CRIAÇÃO DE REUNIÃO ===");
            log.info("Colegiado selecionado: {}", collegiateId);
            log.info("Processos selecionados: {}", processIds);
            log.info("Descrição fornecida: {}", description != null && !description.isEmpty() ? "Sim" : "Não");

            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                log.warn("Acesso negado: usuário não é COORDINATOR");
                redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado.");
                return "redirect:/dashboard";
            }

            // Validação: Colegiado é obrigatório
            if (collegiateId == null) {
                log.warn("Erro: Nenhum colegiado selecionado");
                redirectAttributes.addFlashAttribute("errorMessage", "Selecione um colegiado.");
                return "redirect:/dashboard/meetings/new";
            }

            // Validação: Processos são obrigatórios
            if (processIds == null || processIds.isEmpty()) {
                log.warn("Erro: Nenhum processo selecionado");
                redirectAttributes.addFlashAttribute("errorMessage", "Selecione ao menos um processo.");
                return "redirect:/dashboard/meetings/new";
            }

            LocalDateTime scheduledDateTime = LocalDateTime.now();

            log.info("Validações passadas. Criando reunião com colegiado ID {} e {} processos", 
                    collegiateId, processIds.size());

            // Cria a reunião usando o novo método que automaticamente usa membros do colegiado
            Meeting meeting = meetingService.createMeetingWithCollegiate(
                    collegiateId,
                    scheduledDateTime,
                    processIds,
                    description
            );

            log.info("=== REUNIÃO CRIADA COM SUCESSO ===");
            log.info("Meeting ID: {} | Colegiado: {} | Processos: {} | Participantes: {}", 
                meeting.getId(), 
                collegiateId,
                meeting.getProcesses() != null ? meeting.getProcesses().size() : 0,
                meeting.getParticipants() != null ? meeting.getParticipants().size() : 0);

            redirectAttributes.addFlashAttribute("successMessage", 
                "Reunião criada com sucesso! ID: " + meeting.getId());
        } catch (IllegalStateException e) {
            log.error("Erro de validação ao criar reunião: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard/meetings/new";
        } catch (IllegalArgumentException e) {
            log.error("Argumento inválido ao criar reunião: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/dashboard/meetings/new";
        } catch (Exception e) {
            log.error("=== ERRO INESPERADO AO CRIAR REUNIÃO ===", e);
            log.error("Tipo de erro: {}", e.getClass().getSimpleName());
            log.error("Mensagem: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Erro inesperado ao criar reunião: " + e.getMessage());
            return "redirect:/dashboard/meetings/new";
        }

        return "redirect:/dashboard/meetings";
    }

    /**
     * Método auxiliar para buscar reuniões do colegiado do professor (REQFUNC 4).
     */
    private List<Meeting> findCollegiateMeetingsForProfessor(Long professorId, String meetingStatus) {
        try {
            Collegiate collegiate = collegiateService.findByProfessorId(professorId);

            if (meetingStatus != null && !meetingStatus.isBlank()) {
                MeetingStatus statusEnum = MeetingStatus.valueOf(meetingStatus);
                return meetingService.findByCollegiateIdAndStatus(collegiate.getId(), statusEnum);
            } else {
                return meetingService.findByCollegiateId(collegiate.getId());
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * GET: Página de Professores do Colegiado
     */
    @GetMapping("/professors")
    public String viewProfessors(Model model, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (!roles.contains("ROLE_COORDINATOR")) {
            return "redirect:/dashboard";
        }

        List<Professor> professors = professorService.findAll();
        model.addAttribute("professors", professors);
        model.addAttribute("mainContent", "pages/dashboard-coordinator-professors :: content");
        return "home";
    }

    /**
     * GET: Página de Reuniões do Colegiado
     */
    @GetMapping("/meetings")
    public String viewMeetings(Model model, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (!roles.contains("ROLE_COORDINATOR")) {
            return "redirect:/dashboard";
        }

        Professor professor = professorService.findByLogin(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Coordenador não encontrado."));

        try {
            Collegiate collegiate = collegiateService.findByProfessorId(professor.getId());
            List<Meeting> collegiateMeetings = meetingService.findByCollegiateId(collegiate.getId());
            model.addAttribute("collegiateMeetings", collegiateMeetings);
        } catch (Exception e) {
            model.addAttribute("collegiateMeetings", Collections.emptyList());
        }

        model.addAttribute("mainContent", "pages/dashboard-coordinator-meetings :: content");
        return "home";
    }

    /**
     * GET: Página para criar nova reunião
     */
    @GetMapping("/meetings/new")
    public String newMeeting(Model model, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (!roles.contains("ROLE_COORDINATOR")) {
            return "redirect:/dashboard";
        }

        professorService.findByLogin(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Coordenador não encontrado."));

        // Carregar processos elegíveis:
        // - UNDER_ANALISYS (em análise)
        // - rapporteurVote != null (relator votou)
        // - processRapporteur != null (tem relator designado)
        // - meeting == null (não estão em outras reuniões)
        List<Process> eligibleProcesses = processService.findAllProcesses().stream()
                .filter(p -> p.getStatus() == StatusProcess.UNDER_ANALISYS)
                .filter(p -> p.getRapporteurVote() != null)
                .filter(p -> p.getProcessRapporteur() != null)
                .filter(p -> p.getMeeting() == null)  // NÃO estar em outra reunião
                .toList();

        // Carregar todos os colegiados disponíveis com seus membros
        List<Collegiate> collegiates = collegiateService.findAllWithMembers();

        model.addAttribute("allProcesses", eligibleProcesses);
        model.addAttribute("collegiates", collegiates);
        model.addAttribute("mainContent", "pages/dashboard-coordinator-new-meeting :: content");
        return "home";
    }

    /**
     * GET: Editar reunião existente (REQFUNC 9 - edit)
     * Apenas para reuniões em status DISPONÍVEL
     */
    @GetMapping("/meetings/{id}/edit")
    public String editMeeting(@PathVariable("id") Long meetingId,
                             Model model,
                             Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                return "redirect:/dashboard";
            }

            Meeting meeting = meetingService.findById(meetingId);

            // Validar que a reunião está em DISPONÍVEL
            if (meeting.getStatus() != MeetingStatus.DISPONIVEL) {
                model.addAttribute("errorMessage", "Apenas reuniões em status DISPONÍVEL podem ser editadas.");
                model.addAttribute("mainContent", "pages/error :: content");
                return "home";
            }

            // Carregar processos elegíveis (os não vinculados a outras reuniões + os já na pauta)
            List<Process> allEligibleProcesses = processService.findAllProcesses().stream()
                    .filter(p -> p.getStatus() == StatusProcess.UNDER_ANALISYS)
                    .filter(p -> p.getRapporteurVote() != null)
                    .filter(p -> p.getProcessRapporteur() != null)
                    .filter(p -> p.getMeeting() == null || p.getMeeting().getId().equals(meetingId))
                    .toList();

            model.addAttribute("meeting", meeting);
            model.addAttribute("allProcesses", allEligibleProcesses);
            model.addAttribute("professors", professorService.findAll());
            model.addAttribute("mainContent", "pages/dashboard-coordinator-new-meeting :: content");
            return "home";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao editar reunião: " + e.getMessage());
            model.addAttribute("mainContent", "pages/error :: content");
            return "home";
        }
    }

    /**
     * POST: Atualizar reunião existente (REQFUNC 9 - update)
     */
    @PostMapping("/meetings/{id}")
    public String updateMeeting(@PathVariable("id") Long meetingId,
                               @RequestParam(value = "processIds", required = false) List<Long> processIds,
                               @RequestParam(value = "professorIds", required = false) List<Long> professorIds,
                               @RequestParam(value = "description", required = false) String description,
                               RedirectAttributes redirectAttributes,
                               Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                return "redirect:/dashboard";
            }

            Meeting meeting = meetingService.findById(meetingId);

            // Validar que a reunião está em DISPONÍVEL
            if (meeting.getStatus() != MeetingStatus.DISPONIVEL) {
                redirectAttributes.addFlashAttribute("errorMessage", "Apenas reuniões em status DISPONÍVEL podem ser editadas.");
                return "redirect:/dashboard/meetings";
            }

            // Atualizar descrição se fornecida
            if (description != null && !description.isEmpty()) {
                meeting.setDescription(description);
            }

            // Atualizar processos se fornecidos
            if (processIds != null && !processIds.isEmpty()) {
                List<Process> updatedProcesses = new ArrayList<>();
                for (Long processId : processIds) {
                    Process process = processService.findById(processId);
                    

                    // Validar processo
                    if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
                        throw new IllegalStateException("Processo ID " + processId + " não está em análise.");
                    }
                    if (process.getRapporteurVote() == null) {
                        throw new IllegalStateException("Relator do processo ID " + processId + " ainda não votou.");
                    }

                    updatedProcesses.add(process);
                }
                meeting.setProcesses(updatedProcesses);
            }

            // Atualizar participantes se fornecidos
            if (professorIds != null && !professorIds.isEmpty()) {
                Collegiate collegiate = meeting.getCollegiate();
                List<Professor> updatedParticipants = new ArrayList<>();
                
                for (Long professorId : professorIds) {
                    Professor professor = professorService.findById(professorId);
                    
   

                    if (!collegiate.getCollegiateMemberList().contains(professor)) {
                        throw new IllegalArgumentException("Professor ID " + professorId + " não pertence ao colegiado.");
                    }

                    updatedParticipants.add(professor);
                }
                meeting.setParticipants(updatedParticipants);
            }

            // Salvar alterações
            meetingService.update(meetingId, meeting);

            redirectAttributes.addFlashAttribute("successMessage", "Reunião atualizada com sucesso!");
            return "redirect:/dashboard/meetings";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar reunião: " + e.getMessage());
            return "redirect:/dashboard/meetings";
        }
    }

    /**
     * POST: Atribuir processo a professor
     */
    @PostMapping("/assign")
    public String assignProfessorToProcess(@RequestParam("processId") Long processId,
                                           @RequestParam("professorId") Long professorId,
                                           RedirectAttributes redirectAttributes) {
        processService.distribute(processId, professorId);
        redirectAttributes.addFlashAttribute("successMessage", "Processo distribuído com sucesso.");
        return "redirect:/dashboard";
    }

    /**
     * POST: Iniciar reunião (REQFUNC 10)
     * Muda status para EM_ANDAMENTO e desabilita edição
     */
    @PostMapping("/meetings/{id}/start")
    public String startMeeting(@PathVariable("id") Long meetingId,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Apenas coordenadores podem iniciar reuniões.");
                return "redirect:/dashboard/meetings";
            }

            meetingService.startMeeting(meetingId);
            redirectAttributes.addFlashAttribute("successMessage", "Reunião iniciada com sucesso! Status: EM ANDAMENTO");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao iniciar reunião: " + e.getMessage());
        }

        return "redirect:/dashboard/meetings";
    }

    /**
     * POST: Finalizar reunião (REQFUNC 12)
     * Muda status para FINALIZADA (apenas se todos processos apregoados)
     */
    @PostMapping("/meetings/{id}/finalize")
    public String finalizeMeeting(@PathVariable("id") Long meetingId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Apenas coordenadores podem finalizar reuniões.");
                return "redirect:/dashboard/meetings";
            }

            meetingService.finalizeMeeting(meetingId);
            redirectAttributes.addFlashAttribute("successMessage", "Reunião finalizada com sucesso! Todas as alterações foram travadas.");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao finalizar reunião: " + e.getMessage());
        }

        return "redirect:/dashboard/meetings";
    }

    /**
     * GET: Página de votação de membros para uma reunião ativa (REQFUNC 11)
     */
    @GetMapping("/meetings/{id}/vote")
    public String voteMeetingProcesses(@PathVariable("id") Long meetingId,
                                      Model model,
                                      Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_PROFESSOR")) {
                return "redirect:/dashboard";
            }

            Professor professor = professorService.findByLogin(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado."));

            Meeting meeting = meetingService.findById(meetingId);

            // Valida se reunião está EM_ANDAMENTO
            if (meeting.getStatus() != MeetingStatus.EM_ANDAMENTO) {
                model.addAttribute("errorMessage", "Esta reunião não está em andamento.");
                model.addAttribute("mainContent", "pages/error :: content");
                return "home";
            }

            // Valida se professor é participante
            boolean isParticipant = meeting.getParticipants().stream()
                    .anyMatch(p -> p.getId().equals(professor.getId()));

            if (!isParticipant) {
                model.addAttribute("errorMessage", "Você não é participante desta reunião.");
                model.addAttribute("mainContent", "pages/error :: content");
                return "home";
            }

            model.addAttribute("meeting", meeting);
            model.addAttribute("professor", professor);
            model.addAttribute("mainContent", "pages/meeting-vote :: content");
            return "home";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao carregar página de votação: " + e.getMessage());
            model.addAttribute("mainContent", "pages/error :: content");
            return "home";
        }
    }

    /**
     * POST: Registrar voto de membro em um processo da reunião (REQFUNC 11)
     */
    @PostMapping("/meetings/{meetingId}/processes/{processId}/vote")
    public String voteMeetingProcess(@PathVariable("meetingId") Long meetingId,
                                    @PathVariable("processId") Long processId,
                                    @RequestParam("voteType") VoteType voteType,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            Professor professor = professorService.findByLogin(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado."));

            Meeting meeting = meetingService.findById(meetingId);

            // Valida se reunião está EM_ANDAMENTO
            if (meeting.getStatus() != MeetingStatus.EM_ANDAMENTO) {
                redirectAttributes.addFlashAttribute("errorMessage", "Esta reunião não está em andamento.");
                return "redirect:/dashboard/meetings/" + meetingId + "/vote";
            }

            // Valida se professor é participante
            boolean isParticipant = meeting.getParticipants().stream()
                    .anyMatch(p -> p.getId().equals(professor.getId()));

            if (!isParticipant) {
                redirectAttributes.addFlashAttribute("errorMessage", "Você não é participante desta reunião.");
                return "redirect:/dashboard/meetings/" + meetingId + "/vote";
            }

            // Registra o voto de membro
            voteService.registerMemberVote(processId, professor.getId(), voteType);
            redirectAttributes.addFlashAttribute("successMessage", "Voto registrado com sucesso!");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao registrar voto: " + e.getMessage());
        }

        return "redirect:/dashboard/meetings/" + meetingId + "/vote";
    }

    /**
     * GET: Página para apregoar processos (REQFUNC 11)
     * Exibe os processos da reunião ativa e permite apregoá-los
     */
    @GetMapping("/meetings/{id}/apregoar")
    public String announceProcesses(@PathVariable("id") Long meetingId,
                                   Model model,
                                   Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                model.addAttribute("errorMessage", "Apenas coordenadores podem apregoar processos.");
                model.addAttribute("mainContent", "pages/error :: content");
                return "home";
            }

            Meeting meeting = meetingService.findById(meetingId);

            // Valida se reunião está EM_ANDAMENTO
            if (meeting.getStatus() != MeetingStatus.EM_ANDAMENTO) {
                model.addAttribute("errorMessage", "Esta reunião não está em andamento. Status: " + meeting.getStatus().getStatus());
                model.addAttribute("mainContent", "pages/error :: content");
                return "home";
            }

            model.addAttribute("meeting", meeting);
            model.addAttribute("mainContent", "pages/meeting-announce :: content");
            return "home";

        } catch (Exception e) {
            model.addAttribute("errorMessage", "Erro ao carregar página de apregoação: " + e.getMessage());
            model.addAttribute("mainContent", "pages/error :: content");
            return "home";
        }
    }

    /**
     * POST: Apregoar um processo específico (REQFUNC 11)
     */
    @PostMapping("/meetings/{meetingId}/processes/{processId}/announce")
    public String announceProcess(@PathVariable("meetingId") Long meetingId,
                                 @PathVariable("processId") Long processId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Apenas coordenadores podem apregoar processos.");
                return "redirect:/dashboard/meetings";
            }

            voteService.announceProcess(processId);
            redirectAttributes.addFlashAttribute("successMessage", "Processo apregoado com sucesso!");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao apregoar processo: " + e.getMessage());
        }

        return "redirect:/dashboard/meetings/" + meetingId + "/apregoar";
    }

    // ========================================
    // GERENCIAMENTO DE COLEGIADOS - COORDENADOR
    // ========================================

    /**
     * GET: Listar colegiados (para coordenadores)
     */
    @GetMapping("/collegiates")
    public String listCollegiates(Model model, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (!roles.contains("ROLE_COORDINATOR")) {
            return "redirect:/dashboard";
        }

        List<Collegiate> collegiates = collegiateService.findAllWithMembers();
        
        model.addAttribute("collegiates", collegiates);
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("pageTitle", "Gerenciar Colegiados");
        model.addAttribute("mainContent", "pages/dashboard/collegiates :: content");
        return "home";
    }

    /**
     * GET: Formulário para criar novo colegiado
     */
    @GetMapping("/collegiates/new")
    public String newCollegiate(Model model, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (!roles.contains("ROLE_COORDINATOR")) {
            return "redirect:/dashboard";
        }

        model.addAttribute("collegiateDTO", new br.edu.ifpb.veritas.dtos.CollegiateDTO());
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("pageTitle", "Criar Colegiado");
        model.addAttribute("mainContent", "pages/dashboard/new-collegiate :: content");
        return "home";
    }

    /**
     * POST: Criar novo colegiado
     */
    @PostMapping("/collegiates")
    public String createCollegiate(@org.springframework.web.bind.annotation.ModelAttribute br.edu.ifpb.veritas.dtos.CollegiateDTO collegiateDTO,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado.");
                return "redirect:/dashboard";
            }

            collegiateService.create(collegiateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Colegiado criado com sucesso!");
            log.info("Colegiado criado: {}", collegiateDTO.getDescription());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao criar colegiado: " + e.getMessage());
            log.error("Erro ao criar colegiado", e);
            return "redirect:/dashboard/collegiates/new";
        }
        return "redirect:/dashboard/collegiates";
    }

    /**
     * GET: Formulário para editar colegiado
     */
    @GetMapping("/collegiates/{id}/edit")
    public String editCollegiate(@PathVariable Long id,
                                Model model,
                                Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (!roles.contains("ROLE_COORDINATOR")) {
            return "redirect:/dashboard";
        }

        try {
            Collegiate collegiate = collegiateService.findById(id);
            model.addAttribute("collegiate", collegiate);
            model.addAttribute("professors", professorService.findAll());
            model.addAttribute("pageTitle", "Editar Colegiado");
            model.addAttribute("mainContent", "pages/dashboard/edit-collegiate :: content");
            return "home";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Colegiado não encontrado.");
            model.addAttribute("mainContent", "pages/error :: content");
            return "home";
        }
    }

    /**
     * POST: Atualizar colegiado
     */
    @PostMapping("/collegiates/{id}")
    public String updateCollegiate(@PathVariable Long id,
                                   @org.springframework.web.bind.annotation.ModelAttribute br.edu.ifpb.veritas.dtos.CollegiateEditDTO collegiateDTO,
                                   RedirectAttributes redirectAttributes,
                                   Authentication authentication) {
        try {
            List<String> roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .toList();

            if (!roles.contains("ROLE_COORDINATOR")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado.");
                return "redirect:/dashboard";
            }

            collegiateService.updateFromDTO(id, collegiateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Colegiado atualizado com sucesso!");
            log.info("Colegiado {} atualizado", id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar colegiado: " + e.getMessage());
            log.error("Erro ao atualizar colegiado", e);
            return "redirect:/dashboard/collegiates/" + id + "/edit";
        }
        return "redirect:/dashboard/collegiates";
    }

    /**
     * POST: Deletar colegiado
     * 
     * 
     * WE DON'T DELETE NOTHING HERE, JUST DEACTIVATE
     */
    // @PostMapping("/collegiates/{id}/delete")
    // public String deleteCollegiate(@PathVariable Long id,
    //                               RedirectAttributes redirectAttributes,
    //                               Authentication authentication) {
    //     try {
    //         List<String> roles = authentication.getAuthorities().stream()
    //                 .map(GrantedAuthority::getAuthority)
    //                 .toList();

    //         if (!roles.contains("ROLE_COORDINATOR")) {
    //             redirectAttributes.addFlashAttribute("errorMessage", "Acesso negado.");
    //             return "redirect:/dashboard";
    //         }

    //         collegiateService.delete(id);
    //         redirectAttributes.addFlashAttribute("successMessage", "Colegiado deletado com sucesso!");
    //         log.info("Colegiado {} deletado", id);
    //     } catch (Exception e) {
    //         redirectAttributes.addFlashAttribute("errorMessage", "Erro ao deletar colegiado: " + e.getMessage());
    //         log.error("Erro ao deletar colegiado", e);
    //     }
    //     return "redirect:/dashboard/collegiates";
    // }
}


