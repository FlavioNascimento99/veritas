package br.edu.ifpb.veritas.controllers.api;
import br.edu.ifpb.veritas.models.Process;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import br.edu.ifpb.veritas.services.ProcessService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;


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

   /**
    * Estrutura de POST para criação de Processo
    *    1. Body de Process
    *    2. ID do Estudante
    *    3. ID de Subject
    *    4. Quanto ao Professor é desnecessário, porque o Processo não vai inicializar com um Relator(Professor), este será implementado a partir de um Coordenador
    */
   @PostMapping
   @PreAuthorize("hasRole('STUDENT')")
   public ResponseEntity<Process> create(
      @Valid @RequestBody Process process,
      @RequestParam("studentId") Long studentId,
      @RequestParam("subjectId") Long subjectId,
      UriComponentsBuilder uriBuilder
   ) {
      Process saved = processService.createProcess(process, studentId, subjectId);
      var uri = uriBuilder.path("/api/processes/{id}").buildAndExpand(saved.getId()).toUri();
      return ResponseEntity.created(uri).body(saved);
   }


   @GetMapping("/my-processes")
   @PreAuthorize("hasRole('STUDENT')")
   public ResponseEntity<List<Process>> listOwnedStudentProcesses(@RequestParam("studentId") Long studentId) {
      List<Process> processes = processService.listByStudent(studentId);
      return ResponseEntity.ok(processes);
   }


   /**
    * Professor Process Controller
    * 1. Owned Professor Processes (REQFUNC 3)
    */
   @GetMapping("/designated-to-me")
   @PreAuthorize("hasRole('PROFESSOR')")
   public ResponseEntity<List<Process>> listOwnedProfessorProcesses(@RequestParam("professorId") Long professorId) {
      List<Process> processes = processService.listByProfessor(professorId);
      return ResponseEntity.ok(processes);
   }


   /**
    * Coordinator Process Controller
    * 1. Distribute Process (REQFUNC 8)
    */
   @PatchMapping("/{processId}/distribute")
   @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
   public ResponseEntity<Process> distributeProcess(@PathVariable Long processId, @RequestBody Map<String, Long> body) {
      Long professorId = body.get("professorId");
      Process updatedProcess = processService.distribute(processId, professorId);
      return ResponseEntity.ok(updatedProcess);
   }


}
