package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Collegiate;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.dtos.CollegiateDTO;
import br.edu.ifpb.veritas.dtos.CollegiateEditDTO;
import br.edu.ifpb.veritas.repositories.CollegiateRepository;
import br.edu.ifpb.veritas.repositories.MeetingRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CollegiateService {

    private final CollegiateRepository collegiateRepository;
    private final MeetingRepository meetingRepository;
    private final ProfessorService professorService;

    @Transactional
    public Collegiate create(Collegiate collegiate) {
        // Seta a data de criação se não estiver definida
        if (collegiate.getCreatedAt() == null) {
            collegiate.setCreatedAt(LocalDateTime.now());
        }
        // Colocar validações

        return collegiateRepository.save(collegiate);
    }

    @Transactional
    public Collegiate create(CollegiateDTO dto) {
        Collegiate collegiate = new Collegiate();
        collegiate.setCreatedAt(LocalDateTime.now());
        collegiate.setDescription(dto.getDescription());
        
        // Busca e seta o relator (rapporteur)
        if (dto.getRapporteurId() != null) {
            Professor rapporteur = professorService.findById(dto.getRapporteurId());
            collegiate.setRapporteur(rapporteur);
        }
        
        // Busca e seta os membros
        if (dto.getMemberIds() != null && !dto.getMemberIds().isEmpty()) {
            List<Professor> members = dto.getMemberIds().stream()
                    .map(professorService::findById)
                    .toList();
            collegiate.setCollegiateMemberList(members);
        }
        
        return collegiateRepository.save(collegiate);
    }

    public List<Collegiate> findAll() {
        return collegiateRepository.findAll();
    }

    public List<Collegiate> findAllWithMembers() {
        return collegiateRepository.findAllWithMembers();
    }

    public Collegiate findById(Long id) {
        return collegiateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Colegiado não encontrado."));
    }

    @Transactional
    public Collegiate update(Long id, Collegiate payload) {
        Collegiate current = findById(id);
        current.setDescription(payload.getDescription());
        current.setRapporteur(payload.getRapporteur());
        current.setCollegiateMemberList(payload.getCollegiateMemberList());
        return collegiateRepository.save(current);
    }

    @Transactional
    public Collegiate updateFromDTO(Long id, CollegiateEditDTO dto) {
        Collegiate current = findById(id);
        
        // Atualiza descrição
        current.setDescription(dto.getDescription());
        
        // Atualiza relator
        if (dto.getRapporteurId() != null) {
            Professor rapporteur = professorService.findById(dto.getRapporteurId());
            current.setRapporteur(rapporteur);
        } else {
            current.setRapporteur(null);
        }
        
        // Atualiza membros
        if (dto.getMemberIds() != null && !dto.getMemberIds().isEmpty()) {
            List<Professor> members = dto.getMemberIds().stream()
                    .map(professorService::findById)
                    .toList();
            current.setCollegiateMemberList(new java.util.ArrayList<>(members));
        } else {
            current.setCollegiateMemberList(new java.util.ArrayList<>());
        }
        
        return collegiateRepository.save(current);
    }

    // Retorna o colegiado associado a um estudante representante específico
    // public Collegiate findByRepresentativeStudent(Long studentId) {
    //     return collegiateRepository.findByRepresentativeStudentId(studentId)
    //             .orElseThrow(() -> new ResourceNotFoundException("Colegiado com o representante informado não encontrado."));
    // }

    // Retorna o colegiado associado a uma reunião específica
    // public Collegiate findByMeeting(Long meetingId) {
    //     return collegiateRepository.findByCollegiateMeetingsListId(meetingId)
    //             .orElseThrow(() -> new ResourceNotFoundException("Colegiado associado à reunião informado não encontrado."));
    // }

    // Retorna a lista de professores membros de um determinado colegiado
    public List<Professor> findProfessorsByCollegiate(Long id) {
        Collegiate collegiate = findById(id);
        return List.copyOf(collegiate.getCollegiateMemberList());
    }


    /**
     * Log 1: Não teremos exclusão de dados dentro da aplicação.
     */
    @Transactional
    public void unactivate(Long id) {
        Collegiate current = findById(id);
        current.setClosedAt(LocalDateTime.now());
    }

    // Retorna o colegiado associado a um professor específico
    public Collegiate findByProfessorId(Long professorId) {
        return collegiateRepository.findByCollegiateMemberListId(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Colegiado não encontrado para o professor."));
    }

}
