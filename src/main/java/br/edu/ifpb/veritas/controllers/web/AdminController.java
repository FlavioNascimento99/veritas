package br.edu.ifpb.veritas.controllers.web;

import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.models.Subject;
import br.edu.ifpb.veritas.models.Collegiate;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.dtos.CollegiateDTO;
import br.edu.ifpb.veritas.dtos.CollegiateEditDTO;
import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.services.ProfessorService;
import br.edu.ifpb.veritas.services.StudentService;
import br.edu.ifpb.veritas.services.SubjectService;
import br.edu.ifpb.veritas.services.ProcessService;
import br.edu.ifpb.veritas.services.CollegiateService;
import br.edu.ifpb.veritas.services.MeetingService;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final StudentService studentService;
    private final ProfessorService professorService;
    private final SubjectService subjectService;
    private final ProcessService processService;
    private final CollegiateService collegiateService;
    private final MeetingService meetingService;

    // --- CADASTRO DE ESTUDANTE ---
    @GetMapping("/students/new")
    public String showStudentForm(Model model) {
        model.addAttribute("student", new Student());
        model.addAttribute("pageTitle", "Cadastrar Estudante");
        model.addAttribute("mainContent", "pages/admin/new-student :: content");
        return "home";
    }

    @PostMapping("/students")
    public String createStudent(@ModelAttribute Student student, RedirectAttributes redirectAttributes) {
        try {
            studentService.create(student);
            redirectAttributes.addFlashAttribute("successMessage", "Estudante cadastrado com sucesso!");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/students/new";
        }
        return "redirect:/dashboard";
    }

    // --- CADASTRO DE PROFESSOR ---
    @GetMapping("/professors/new")
    public String showProfessorForm(Model model) {
        model.addAttribute("professor", new Professor());
        model.addAttribute("pageTitle", "Cadastrar Professor");
        model.addAttribute("mainContent", "pages/admin/new-professor :: content");
        return "home";
    }

    @PostMapping("/professors")
    public String createProfessor(@ModelAttribute Professor professor, RedirectAttributes redirectAttributes) {
        try {
            professorService.create(professor);
            redirectAttributes.addFlashAttribute("successMessage", "Professor cadastrado com sucesso!");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/professors/new";
        }
        return "redirect:/dashboard";
    }

    // --- LISTAGENS DE USUÁRIOS (Professores e Estudantes) ---
    @GetMapping("/students")
    public String listStudents(Model model) {
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("pageTitle", "Gerenciar Usuários");
        model.addAttribute("mainContent", "pages/admin/manage-users :: content");
        return "home";
    }

    @GetMapping("/professors")
    public String listProfessors(Model model) {
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("pageTitle", "Gerenciar Usuários");
        model.addAttribute("mainContent", "pages/admin/manage-users :: content");
        return "home";
    }

    // --- GERENCIAMENTO DE USUÁRIOS ---
    @GetMapping("/users")
    public String showUserManagementPage(Model model) {
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("students", studentService.findAll());
        model.addAttribute("pageTitle", "Gerenciamento de Usuários");
        model.addAttribute("mainContent", "pages/admin/manage-users :: content");
        return "home";
    }

    @PostMapping("/professors/{id}/update")
    public String updateProfessor(@PathVariable Long id, @ModelAttribute Professor professor, RedirectAttributes redirectAttributes) {
        try {
            professorService.update(id, professor);
            redirectAttributes.addFlashAttribute("successMessage", "Professor atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar professor: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/students/{id}/update")
    public String updateStudent(@PathVariable Long id, @ModelAttribute Student student, RedirectAttributes redirectAttributes) {
        studentService.update(id, student);
        redirectAttributes.addFlashAttribute("successMessage", "Estudante atualizado com sucesso!");
        return "redirect:/admin/users";
    }

    // --- GERENCIAMENTO DE ASSUNTOS ---
    @GetMapping("/subjects")
    public String showSubjectList(Model model) {
        model.addAttribute("subjects", subjectService.findAll());
        model.addAttribute("subject", new Subject()); // Para o formulário de criação na mesma página
        model.addAttribute("pageTitle", "Gerenciar Assuntos");
        model.addAttribute("mainContent", "pages/admin/subjects :: content");
        return "home";
    }

    @PostMapping("/subjects")
    public String createSubject(@ModelAttribute Subject subject, RedirectAttributes redirectAttributes) {
        subjectService.create(subject);
        redirectAttributes.addFlashAttribute("successMessage", "Assunto cadastrado com sucesso!");
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/deactivate")
    public String deactivateSubject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        subjectService.deactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Assunto desativado com sucesso!");
        return "redirect:/admin/subjects";
    }

    @PostMapping("/subjects/{id}/reactivate")
    public String reactivateSubject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        subjectService.reactivate(id);
        redirectAttributes.addFlashAttribute("successMessage", "Assunto reativado com sucesso!");
        return "redirect:/admin/subjects";
    }

    // --- GERENCIAMENTO DE PROCESSOS ---
    @GetMapping("/processes")
    public String listProcesses(Model model) {
        List<Process> processes = processService.findAllProcesses();
        model.addAttribute("processes", processes);
        
        // Calculate process counts by status
        long totalProcesses = processes.size();
        long waitingProcesses = processes.stream().filter(p -> p.getStatus() == StatusProcess.WAITING).count();
        long underAnalysisProcesses = processes.stream().filter(p -> p.getStatus() == StatusProcess.UNDER_ANALISYS).count();
        long approvedProcesses = processes.stream().filter(p -> p.getStatus() == StatusProcess.APPROVED).count();
        long rejectedProcesses = processes.stream().filter(p -> p.getStatus() == StatusProcess.REJECTED).count();
        
        model.addAttribute("totalProcesses", totalProcesses);
        model.addAttribute("waitingProcesses", waitingProcesses);
        model.addAttribute("underAnalysisProcesses", underAnalysisProcesses);
        model.addAttribute("approvedProcesses", approvedProcesses);
        model.addAttribute("rejectedProcesses", rejectedProcesses);
        
        model.addAttribute("pageTitle", "Gerenciar Processos");
        model.addAttribute("mainContent", "pages/admin/processes :: content");
        return "home";
    }

    // --- GERENCIAMENTO DE COLEGIADOS ---
    @GetMapping("/collegiates")
    public String listCollegiates(Model model) {
        model.addAttribute("collegiates", collegiateService.findAllWithMembers());
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("meetings", meetingService.findAll());
        model.addAttribute("pageTitle", "Gerenciar Colegiados");
        model.addAttribute("mainContent", "pages/admin/collegiates :: content");
        return "home";
    }

    @GetMapping("/collegiates/new")
    public String showCollegiateForm(Model model) {
        model.addAttribute("collegiate", new CollegiateDTO());
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("pageTitle", "Cadastrar Colegiado");
        model.addAttribute("mainContent", "pages/admin/new-collegiate :: content");
        return "home";
    }

    @GetMapping("/collegiates/{id}")
    public String viewCollegiate(@PathVariable Long id, Model model) {
        Collegiate collegiate = collegiateService.findById(id);
        List<Meeting> meetings = meetingService.findByCollegiateId(id);
        
        model.addAttribute("collegiate", collegiate);
        model.addAttribute("meetings", meetings);
        model.addAttribute("pageTitle", "Visualizar Colegiado");
        model.addAttribute("mainContent", "pages/admin/view-collegiate :: content");
        return "home";
    }

    @GetMapping("/collegiates/{id}/edit")
    public String showEditCollegiateForm(@PathVariable Long id, Model model) {
        Collegiate collegiate = collegiateService.findById(id);
        model.addAttribute("collegiate", collegiate);
        model.addAttribute("professors", professorService.findAll());
        model.addAttribute("processes", processService.findAllProcesses());
        model.addAttribute("pageTitle", "Editar Colegiado");
        model.addAttribute("mainContent", "pages/admin/edit-collegiate :: content");
        return "home";
    }

    @PostMapping("/collegiates")
    public String createCollegiate(@ModelAttribute CollegiateDTO collegiateDTO, RedirectAttributes redirectAttributes) {
        try {
            collegiateService.create(collegiateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Colegiado cadastrado com sucesso!");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/collegiates/new";
        }
        return "redirect:/admin/collegiates";
    }

    @PostMapping("/collegiates/{id}")
    public String updateCollegiate(@PathVariable Long id, @ModelAttribute CollegiateEditDTO collegiateDTO, RedirectAttributes redirectAttributes) {
        try {
            collegiateService.updateFromDTO(id, collegiateDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Colegiado atualizado com sucesso!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erro ao atualizar colegiado: " + e.getMessage());
            return "redirect:/admin/collegiates/" + id + "/edit";
        }
        return "redirect:/admin/collegiates/" + id;
    }
    
}
