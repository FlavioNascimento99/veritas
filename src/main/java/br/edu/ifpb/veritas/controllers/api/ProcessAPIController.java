package br.edu.ifpb.veritas.controllers.api;

import br.edu.ifpb.veritas.dtos.CreateProcessDTO;
import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.services.ProcessService;
import br.edu.ifpb.veritas.services.ProfessorService;
import br.edu.ifpb.veritas.services.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing Processes.
 * Handles creation, listing, and distribution of processes
 * for students, professors, and coordinators.
 */
@RestController
@RequestMapping("/api/processes")
@RequiredArgsConstructor
public class ProcessAPIController {

    private final ProcessService processService;
    private final StudentService studentService;
    private final ProfessorService professorService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Process> create(
            @Valid @RequestBody CreateProcessDTO dto,
            @RequestParam("subjectId") Long subjectId,
            Authentication authentication,
            UriComponentsBuilder uriBuilder
    ) {
        Process process = new Process();
        process.setTitle(dto.getTitle());
        process.setDescription(dto.getDescription());
        Process saved = processService.createProcess(process, currentStudent(authentication).getId(), subjectId);
        var uri = uriBuilder.path("/api/processes/{id}").buildAndExpand(saved.getId()).toUri();
        return ResponseEntity.created(uri).body(saved);
    }

    @GetMapping("/my-processes")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<Process>> listOwnedStudentProcesses(Authentication authentication) {
        List<Process> processes = processService.listByStudent(currentStudent(authentication).getId());
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/designated-to-me")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<List<Process>> listOwnedProfessorProcesses(Authentication authentication) {
        List<Process> processes = processService.listByProfessor(currentProfessor(authentication).getId());
        return ResponseEntity.ok(processes);
    }

    @PatchMapping("/{processId}/distribute")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Process> distributeProcess(@PathVariable Long processId, @RequestBody Map<String, Long> body) {
        Long professorId = body.get("professorId");
        Process updatedProcess = processService.distribute(processId, professorId);
        return ResponseEntity.ok(updatedProcess);
    }

    private Student currentStudent(Authentication authentication) {
        return studentService.findByLogin(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado."));
    }

    private Professor currentProfessor(Authentication authentication) {
        return professorService.findByLogin(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado."));
    }
}