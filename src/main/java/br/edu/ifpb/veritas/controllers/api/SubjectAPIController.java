/**
 * Seguindo o padrão REST, este controller gerencia as operações CRUD para a entidade Subject.
 * Inclui endpoints para criação, listagem, atualização e desativação de subjects.
 * 
 * POST, GET, PUT e DELETE são mapeados para os métodos HTTP correspondentes. @PostMapping cria um novo subject,
 * @GetMapping lista todos os subjects, @PutMapping atualiza um subject existente e @DeleteMapping desativa um subject.
 */

package br.edu.ifpb.veritas.controllers.api;

import java.util.List;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import br.edu.ifpb.veritas.models.Subject;
import br.edu.ifpb.veritas.services.SubjectService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subjects")
public class SubjectAPIController {
   private final SubjectService subjectService;


   /**
    * Post feito para criação de novos 'Subject' da aplicação.
    */
   @PostMapping
   @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
   public ResponseEntity<Subject> createSubject(@Valid @RequestBody Subject subject) {
      ResponseEntity.ok("All done folk");
      return ResponseEntity.ok(subjectService.create(subject));
   }

   /**
    * Get feito para buscar todos os objeto/dado em banco.
    */
   @GetMapping
   public ResponseEntity<List<Subject>> getSubjects() {
      return ResponseEntity.ok(subjectService.findAll());
   }

   /**
    * Put feito para alteração(Opicional) total do objeto/dado.
    */
   @PutMapping("/{id}")
   @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
   public ResponseEntity<Subject> updateSubject(@PathVariable Long id, @RequestBody Subject subject) {
      return ResponseEntity.ok(subjectService.update(id, subject));
   }

   /**
    * Patch feito para alteração de valor 'active' de Subject.
    * 1. Ativa
    * 2. Desativa
    */
   @PatchMapping("/{id}/inactivate")
   @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
   public ResponseEntity<Subject> inactivateSubject(@PathVariable Long id) {
      subjectService.deactivate(id);
      return ResponseEntity.noContent().build();
   }

   @PatchMapping("/{id}/reactivate")
   @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
   public ResponseEntity<Subject> reactivateSubject(@PathVariable Long id) {
      subjectService.reactivate(id);
      return ResponseEntity.noContent().build();
   }

   // A partir daqui iremos colocar
   // os requisitos específicos do projeto

}
