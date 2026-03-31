package br.edu.ifpb.veritas.controllers.api;

import br.edu.ifpb.veritas.dtos.CollegiateDTO;
import br.edu.ifpb.veritas.models.Collegiate;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.services.CollegiateService;
import br.edu.ifpb.veritas.services.ProfessorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/collegiates")
public class CollegiateAPIController {
    private final CollegiateService collegiateService;
    private final ProfessorService professorService;

    @PostMapping
    public ResponseEntity<Void> create(@Valid @RequestBody CollegiateDTO collegiateDTO) {
        Collegiate collegiate = new Collegiate();
        collegiate.setDescription(collegiateDTO.getDescription());
        collegiate.setCreatedAt(LocalDateTime.now());

        Professor rapporteur = professorService.findById(collegiateDTO.getRapporteurId());
        collegiate.setRapporteur(rapporteur);

        List<Professor> members = collegiateDTO.getMemberIds().stream()
                .map(professorService::findById)
                .collect(Collectors.toList());
        collegiate.setCollegiateMemberList(members);

        collegiateService.create(collegiate);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Collegiate>> findAll() {
        return ResponseEntity.ok(collegiateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Collegiate> findById(@PathVariable Long id) {
        return ResponseEntity.ok(collegiateService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Collegiate> update(@PathVariable Long id, @RequestBody Collegiate collegiate) {
        return ResponseEntity.ok(collegiateService.update(id, collegiate));
    }

    // Retorna o colegiado associado a um representante estudantil específico
    // @GetMapping("/representatives/{studentId}")
    // public ResponseEntity<Collegiate> findByRepresentative(@PathVariable Long studentId) {
    //     return ResponseEntity.ok(collegiateService.findByRepresentativeStudent(studentId));
    // }

    // Retorna o colegiado associado a uma reunião específica
    // @GetMapping("/meetings/{meetingId}")
    // public ResponseEntity<Collegiate> findByMeeting(@PathVariable Long meetingId) {
    //     return ResponseEntity.ok(collegiateService.findByMeeting(meetingId));
    // }

    // Retorna a lista de professores membros de um colegiado específico
    @GetMapping("/{id}/professors")
    public ResponseEntity<List<Professor>> findProfessors(@PathVariable Long id) {
        return ResponseEntity.ok(collegiateService.findProfessorsByCollegiate(id));
    }
}
