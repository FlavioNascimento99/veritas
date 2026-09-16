package br.edu.ifpb.veritas.controllers.web;

import br.edu.ifpb.veritas.enums.DecisionType;
import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.enums.VoteType;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.services.ProcessService;
import br.edu.ifpb.veritas.services.ProfessorService;
import br.edu.ifpb.veritas.services.StudentService;
import br.edu.ifpb.veritas.services.VoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/processes")
@RequiredArgsConstructor
public class ProcessController {

    private final ProcessService processService;
    private final StudentService studentService;
    private final VoteService voteService;
    private final ProfessorService professorService;

    // REQFUNC 1 + REQFUNC 16: Criação de processo com upload opcional de PDF
    @PostMapping("/create")
    public String createProcess(
            @ModelAttribute Process process,
            @RequestParam Long subjectId,
            @RequestParam(value = "documentFile", required = false) MultipartFile documentFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            String studentLogin = authentication.getName();
            var studentOpt = studentService.findByLogin(studentLogin);
            var student = studentOpt.orElseThrow(() ->
                    new IllegalArgumentException("Estudante não encontrado para o usuário autenticado."));

            // Cria o processo com ou sem documento
            processService.createProcess(process, student.getId(), subjectId, documentFile);

            // Mensagem de sucesso diferenciada
            if (documentFile != null && !documentFile.isEmpty()) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Processo criado com sucesso! Documento anexado: " + documentFile.getOriginalFilename());
            } else {
                redirectAttributes.addFlashAttribute("successMessage", "Processo criado com sucesso!");
            }

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao criar processo: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }

    // REQFUNC 16: Upload de documento para processo existente
    @PostMapping("/{id}/upload")
    public String uploadDocument(
            @PathVariable("id") Long processId,
            @RequestParam("documentFile") MultipartFile documentFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Student student = studentService.findByLogin(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Estudante não encontrado."));

            processService.uploadDocument(processId, student.getId(), documentFile);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Documento anexado com sucesso: " + documentFile.getOriginalFilename());

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao anexar documento: " + e.getMessage());
        }

        return "redirect:/processes/" + processId;
    }

    // REQFUNC 16: Remove documento de um processo
    @PostMapping("/{id}/remove-document")
    public String removeDocument(
            @PathVariable("id") Long processId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Student student = studentService.findByLogin(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Estudante não encontrado."));

            processService.removeDocument(processId, student.getId());

            redirectAttributes.addFlashAttribute("successMessage", "Documento removido com sucesso.");

        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao remover documento: " + e.getMessage());
        }

        return "redirect:/processes/" + processId;
    }

    // REQFUNC 16: Download do documento PDF anexado ao processo
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable("id") Long processId, Authentication authentication) {
        Process process = processService.findById(processId);

        if (process.getDocument() == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canDownloadDocument(process, authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String filename = process.getDocumentFilename();
        if (filename == null || filename.isBlank()) {
            filename = "documento_processo_" + process.getNumber() + ".pdf";
        } else {
            filename = ProcessService.sanitizeFilename(filename);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(process.getDocument());
    }

    private boolean canDownloadDocument(Process process, Authentication authentication) {
        boolean privileged = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_COORDINATOR"));
        if (privileged) {
            return true;
        }

        var studentOpt = studentService.findByLogin(authentication.getName());
        if (studentOpt.isPresent() && process.getProcessCreator() != null) {
            return process.getProcessCreator().getId().equals(studentOpt.get().getId());
        }

        var professorOpt = professorService.findByLogin(authentication.getName());
        if (professorOpt.isPresent()) {
            Long professorId = professorOpt.get().getId();
            boolean isRapporteur = process.getProcessRapporteur() != null
                    && process.getProcessRapporteur().getId().equals(professorId);
            boolean isMeetingParticipant = process.getMeeting() != null
                    && process.getMeeting().getParticipants().stream()
                            .anyMatch(p -> p.getId().equals(professorId));
            return isRapporteur || isMeetingParticipant;
        }

        return false;
    }

    @GetMapping("/{id}")
    public String showProcess(@PathVariable("id") Long id, Model model, Authentication authentication) {
        var process = processService.findById(id);
        model.addAttribute("pageTitle", "Processo - " + (process.getTitle() != null ? process.getTitle() : process.getNumber()));
        model.addAttribute("activePage", "process");
        model.addAttribute("process", process);

        // Verifica se é estudante (para mostrar opções de upload)
        var studentOpt = studentService.findByLogin(authentication.getName());
        if (studentOpt.isPresent()) {
            Student student = studentOpt.get();
            model.addAttribute("loggedStudent", student);

            // Verifica se o estudante é o criador do processo
            boolean isOwner = process.getProcessCreator().getId().equals(student.getId());
            model.addAttribute("isOwner", isOwner);

            // REQFUNC 16: Verifica se pode fazer upload (processo não distribuído)
            boolean canUpload = isOwner && process.getStatus() == StatusProcess.WAITING;
            model.addAttribute("canUpload", canUpload);

            // Verifica se tem documento anexado
            boolean hasDocument = process.getDocument() != null;
            model.addAttribute("hasDocument", hasDocument);
        }

        // ========== REQFUNC 5: Lógica de votação ==========
        // Verifica se o usuário logado é um professor
        var professorOpt = professorService.findByLogin(authentication.getName());
        if (professorOpt.isPresent()) {
            Professor professor = professorOpt.get();
            model.addAttribute("loggedProfessor", professor);

            // Verifica se é o relator do processo
            boolean isRapporteur = process.getProcessRapporteur() != null &&
                    process.getProcessRapporteur().getId().equals(professor.getId());
            model.addAttribute("isRapporteur", isRapporteur);

            // Verifica se o processo está em análise
            boolean canVote = process.getStatus() == StatusProcess.UNDER_ANALISYS;
            model.addAttribute("canVote", canVote);

            // Verifica se o RELATOR já votou (quando é relator)
            if (isRapporteur) {
                boolean hasVoted = process.getRapporteurVote() != null;
                model.addAttribute("hasVoted", hasVoted);
                // Adiciona os tipos de decisão para o formulário do relator
                model.addAttribute("decisionTypes", DecisionType.values());
            } else {
                // Lógica para votação do colegiado (professor membro participante da reunião)
                boolean isProfessorMember = process.getMeeting() != null &&
                        process.getMeeting().getParticipants().stream()
                                .anyMatch(p -> p.getId().equals(professor.getId()));
                model.addAttribute("isProfessorMember", isProfessorMember);
                
                if (isProfessorMember) {
                    boolean hasVotedProcess = processService.hasVotedProcess(process.getId(), professor.getId());
                    model.addAttribute("hasVotedProcess", hasVotedProcess);
                    model.addAttribute("canVoteProcess", canVote && !hasVotedProcess);
                }
            }
        }

        model.addAttribute("mainContent", "pages/process-detail :: content");
        return "home";
    }

    // REQFUNC 5: Endpoint para registrar voto do RELATOR no processo
    @PostMapping("/{id}/vote")
    public String voteOnProcess(
            @PathVariable("id") Long processId,
            @RequestParam("voteType") String voteTypeStr,
            @RequestParam("justification") String justification,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // 1. Busca o professor logado
            Professor professor = professorService.findByLogin(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado."));

            // 2. Busca o processo
            Process process = processService.findById(processId);

            // 3. Verifica se é o relator
            if (process.getProcessRapporteur() == null ||
                    !process.getProcessRapporteur().getId().equals(professor.getId())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Apenas o relator pode votar neste processo.");
                return "redirect:/processes/" + processId;
            }

            // 4. Verifica se o processo está sob análise
            if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
                redirectAttributes.addFlashAttribute("errorMessage", "Este processo não está disponível para votação.");
                return "redirect:/processes/" + processId;
            }

            // 5. Verifica se o relator já votou (usando rapporteurVote do Process, não Vote genérico)
            if (process.getRapporteurVote() != null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Você já registrou sua decisão neste processo.");
                return "redirect:/processes/" + processId;
            }

            // 6. Valida justificativa
            if (justification == null || justification.trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "A justificativa é obrigatória.");
                return "redirect:/processes/" + processId;
            }

            // 7. Converte o valor recebido para DecisionType
            DecisionType decision = DecisionType.fromValue(voteTypeStr);
            if (decision == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tipo de voto inválido: " + voteTypeStr);
                return "redirect:/processes/" + processId;
            }

            // 8. Registra a decisão do relator
            voteService.registerRapporteurDecision(processId, professor.getId(), decision, justification.trim());

            // 9. Mensagem de sucesso e redirect
            String decisionText = decision == DecisionType.DEFERIMENTO ? "Deferimento" : "Indeferimento";
            redirectAttributes.addFlashAttribute("successMessage",
                    "Decisão registrada com sucesso! Tipo: " + decisionText);

        } catch (IllegalStateException e) {
            // Erros de regra de negócio (ex: já votou, reunião finalizada, etc.)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            // Outros erros inesperados
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao registrar decisão: " + e.getMessage());
        }

        return "redirect:/processes/" + processId;
    }

    // MÉTODO PARA VOTAÇÃO DE PROFESSOR (MEMBRO DO COLEGIADO)
    // Registra o voto de um professor em um processo durante a reunião
    @PostMapping("/{id}/vote-professor")
    public String voteProfessor(
            @PathVariable("id") Long processId,
            @RequestParam("voteType") String voteTypeStr,
            @RequestParam(value = "justification", required = false) String justification,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            // 1. Busca o professor logado
            Professor professor = professorService.findByLogin(authentication.getName())
                    .orElseThrow(() -> new IllegalArgumentException("Professor não encontrado."));

            // 2. Converte o tipo de voto
            VoteType voteType;
            try {
                voteType = VoteType.valueOf(voteTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Tipo de voto inválido: " + voteTypeStr);
                return "redirect:/processes/" + processId;
            }

            // 3. Registra o voto
            voteService.registerProfessorVote(processId, professor.getId(), voteType, justification);

            // 4. Mensagem de sucesso
            redirectAttributes.addFlashAttribute("successMessage", "Voto registrado com sucesso!");

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Erro ao registrar voto: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }
}