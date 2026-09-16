package br.edu.ifpb.veritas.controllers.api;

import br.edu.ifpb.veritas.dtos.CreateMeetingDTO;
import br.edu.ifpb.veritas.dtos.UpdateMeetingDTO;
import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.services.MeetingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingAPIController {
    private final MeetingService meetingService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Meeting> create(@Valid @RequestBody CreateMeetingDTO dto) {
        Meeting meeting = meetingService.createMeetingWithCollegiate(
                dto.getCollegiateId(),
                dto.getScheduledDate(),
                dto.getProcessIds(),
                dto.getDescription());
        return ResponseEntity.ok(meeting);
    }

    @GetMapping
    public ResponseEntity<List<Meeting>> findAll() {
        return ResponseEntity.ok(meetingService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Meeting> findById(@PathVariable Long id) {
        return ResponseEntity.ok(meetingService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Meeting> update(@PathVariable Long id, @Valid @RequestBody UpdateMeetingDTO dto) {
        return ResponseEntity.ok(meetingService.updateFromDTO(id, dto));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'COORDINATOR')")
    public ResponseEntity<Meeting> updateStatus(@PathVariable Long id, @RequestParam MeetingStatus status) {
        return ResponseEntity.ok(meetingService.updateStatus(id, status));
    }

    // Retorna uma ou mais reuniões com base no status fornecido
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Meeting>> findByStatus(@PathVariable MeetingStatus status) {
        return ResponseEntity.ok(meetingService.findByStatus(status));
    }

    // Retorna todas as reuniões associadas a um colegiado específico
    @GetMapping("/collegiate/{collegiateId}")
    public ResponseEntity<List<Meeting>> findByCollegiateId(@PathVariable Long collegiateId) {
        return ResponseEntity.ok(meetingService.findByCollegiateId(collegiateId));
    }
}
