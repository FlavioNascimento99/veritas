/**
 * Rest Controller de Professores
 * 
 * Rotas existentes, validadas e testadas:
 *  1. Criação                          OK
 *  2. Listagem Geral                   OK
 *  3. Busca por unicidade              OK
 *  4. Alteração de Propriedades        
 *  5. Mudança de Estado
 *      5.1. "Desativação" de Objeto
 *      5.2. "Reativação" de Objeto
 * 
 *  6. Implementação de Requisito funcional
*      Listagem de Processos anexados a determinado Professor.
 */

package br.edu.ifpb.veritas.controllers.api;

import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.services.ProcessService;
import br.edu.ifpb.veritas.services.ProfessorService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/professors")
public class ProfessorAPIController {
    private final ProfessorService professorService;
    private final ProcessService processService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Professor> create(@Valid @RequestBody Professor professor) {
        professorService.create(professor);
        return ResponseEntity.ok(professor);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Professor>> findAll() {
        return ResponseEntity.ok(professorService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Professor> findById(@PathVariable Long id) {
        return ResponseEntity.ok(professorService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Professor> update(@PathVariable Long id, @RequestBody Professor professor) {
        return ResponseEntity.ok(professorService.update(id, professor));
    }

    @PatchMapping("/{id}/coordinatorState")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Professor> setAsCoordinator(@PathVariable Long id) {
        professorService.coordinatorStateChanger(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activeState")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        professorService.activeStateChanger(id);
        return ResponseEntity.noContent().build();
    }

    // REQFUNC 3: professor consulta todos os processos designados a ele
    @GetMapping("/{id}/processes")
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN', 'COORDINATOR')")
    public ResponseEntity<List<Process>> getAssignedProcesses(@PathVariable("id") Long professorId,
                                                              Authentication authentication) {
        boolean isProfessor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PROFESSOR"));
        if (isProfessor && !professorId.equals(
                professorService.findByLogin(authentication.getName())
                        .orElseThrow(() -> new AccessDeniedException("Acesso negado."))
                        .getId())) {
            throw new AccessDeniedException("Acesso negado.");
        }

        List<Process> processes = processService.listByProfessor(professorId);
        return ResponseEntity.ok(processes);
    }

}
