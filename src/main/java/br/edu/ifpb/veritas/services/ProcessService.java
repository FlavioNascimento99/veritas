package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.models.Subject;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Vote;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import br.edu.ifpb.veritas.repositories.ProcessRepository;
import br.edu.ifpb.veritas.repositories.StudentRepository;
import br.edu.ifpb.veritas.repositories.SubjectRepository;
import br.edu.ifpb.veritas.repositories.VoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessService {

    private final ProcessRepository processRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final ProfessorRepository professorRepository;
    private final VoteRepository voteRepository;

    // Tamanho máximo do arquivo: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Tipo MIME permitido
    private static final String ALLOWED_CONTENT_TYPE = "application/pdf";

    // Cria um processo COM upload opcional de documento PDF
    @Transactional
    public Process createProcess(Process process, Long studentId, Long subjectId, MultipartFile document) {
        if (studentId == null) {
            throw new IllegalArgumentException("O ID do estudante não pode ser nulo.");
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado com o ID: " + studentId));

        if (subjectId == null) {
            throw new IllegalArgumentException("O ID do assunto não pode ser nulo.");
        }

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new ResourceNotFoundException("Assunto não encontrado com o ID: " + subjectId));

        process.setProcessCreator(student);
        process.setSubject(subject);
        process.setCreatedAt(LocalDateTime.now());
        process.setStatus(StatusProcess.WAITING);
        process.setNumber(generateProcessNumber());

        // REQFUNC 16: Processa o upload do documento, se fornecido
        if (document != null && !document.isEmpty()) {
            processDocumentUpload(process, document);
        }

        return processRepository.save(process);
    }

    // Mantém compatibilidade com chamadas existentes (sem documento)
    @Transactional
    public Process createProcess(Process process, Long studentId, Long subjectId) {
        return createProcess(process, studentId, subjectId, null);
    }

    // REQFUNC 16: Upload de documento PDF para um processo existente (processo NÃO pode estar distribuído)
    @Transactional
    public Process uploadDocument(Long processId, Long studentId, MultipartFile document) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        // Valida se o estudante é o criador do processo
        if (!process.getProcessCreator().getId().equals(studentId)) {
            throw new IllegalStateException("Você não tem permissão para alterar este processo.");
        }

        // REQFUNC 16: Valida que o processo ainda não foi distribuído
        if (process.getStatus() != StatusProcess.WAITING) {
            throw new IllegalStateException(
                    "Não é possível anexar documento a um processo que já foi distribuído. " +
                            "Status atual: " + process.getStatus().getStatus()
            );
        }

        // Valida e processa o documento
        if (document == null || document.isEmpty()) {
            throw new IllegalArgumentException("Nenhum arquivo foi enviado.");
        }

        processDocumentUpload(process, document);

        return processRepository.save(process);
    }

    // REQFUNC 16: Remove o documento de um processo (processo NÃO pode estar distribuído)
    @Transactional
    public Process removeDocument(Long processId, Long studentId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        // Valida propriedade
        if (!process.getProcessCreator().getId().equals(studentId)) {
            throw new IllegalStateException("Você não tem permissão para alterar este processo.");
        }

        // Valida status
        if (process.getStatus() != StatusProcess.WAITING) {
            throw new IllegalStateException(
                    "Não é possível remover documento de um processo que já foi distribuído."
            );
        }

        // Remove o documento
        process.setDocument(null);
        process.setDocumentFilename(null);
        process.setDocumentUploadDate(null);

        return processRepository.save(process);
    }

    // Retorna o documento PDF de um processo para download
    public byte[] getDocument(Long processId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        return process.getDocument();
    }

    // Processa e valida o upload do documento PDF
    private void processDocumentUpload(Process process, MultipartFile document) {
        // Valida o tipo do arquivo
        String contentType = document.getContentType();
        if (contentType == null || !contentType.equals(ALLOWED_CONTENT_TYPE)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo inválido. Apenas arquivos PDF são permitidos."
            );
        }

        // Valida o tamanho do arquivo
        if (document.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "O arquivo excede o tamanho máximo permitido de 5MB."
            );
        }

        // Valida o nome do arquivo
        String filename = document.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "documento.pdf";
        }

        // Salva os dados do documento
        try {
            process.setDocument(document.getBytes());
            process.setDocumentFilename(filename);
            process.setDocumentUploadDate(LocalDateTime.now());
        } catch (IOException e) {
            throw new IllegalStateException("Erro ao processar o arquivo: " + e.getMessage());
        }
    }

    public List<Process> listByStudent(Long studentId) {
        return processRepository.findByProcessCreator_Id(studentId);
    }

    public List<Process> findAll() {
        return processRepository.findAll();
    }

    public List<Process> findAllProcesses() {
        return processRepository.findAll();
    }

    public Process findById(Long id) {
        return processRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado."));
    }

    public List<Process> findAllFiltered(String status, Long studentId, Long professorId) {
        Specification<Process> spec = Specification.where(null);

        if (status != null && !status.isBlank()) {
            try {
                StatusProcess statusEnum = StatusProcess.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusEnum));
            } catch (IllegalArgumentException e) {
                // ignore invalid status values
            }
        }

        if (studentId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("processCreator").get("id"), studentId));
        }

        if (professorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("processRapporteur").get("id"), professorId));
        }

        if (spec == null) {
            return processRepository.findAll();
        }

        return processRepository.findAll(spec);
    }

    // ========== REQNAOFUNC 9: Métodos com Paginação ==========

    // Retorna todos os processos com paginação.
    public Page<Process> findAllPaged(Pageable pageable) {
        return processRepository.findAll(pageable);
    }

    // Retorna processos filtrados COM paginação
    public Page<Process> findAllFilteredPaged(String status, Long studentId, Long professorId, Pageable pageable) {
        Specification<Process> spec = Specification.where(null);

        // Filtro por status
        if (status != null && !status.isBlank()) {
            try {
                StatusProcess statusEnum = StatusProcess.valueOf(status.toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), statusEnum));
            } catch (IllegalArgumentException e) {
                // Ignora valores de status inválidos
            }
        }

        // Filtro por estudante
        if (studentId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("processCreator").get("id"), studentId));
        }

        // Filtro por professor relator
        if (professorId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("processRapporteur").get("id"), professorId));
        }

        // Executa a consulta paginada com os filtros
        return processRepository.findAll(spec, pageable);
    }

    public List<Process> findWaitingProcesses() {
        return processRepository.findByStatus(StatusProcess.WAITING);
    }

    public List<Process> listByProfessor(Long professorId) {
        return processRepository.findByProcessRapporteur_Id(professorId);
    }

    public List<Process> listByStudentFiltered(Long studentId, String status, Long subjectId) {
        if (studentId == null) {
            throw new IllegalArgumentException("O ID do estudante não pode ser nulo para filtrar os processos.");
        }
        studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado com o ID: " + studentId));

        Specification<Process> spec = Specification.where((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("processCreator").get("id"), studentId));

        if (status != null && !status.isBlank()) {
            try {
                StatusProcess statusEnum = StatusProcess.valueOf(status.toUpperCase());
                spec = spec.and((root, query, criteriaBuilder) ->
                        criteriaBuilder.equal(root.get("status"), statusEnum));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status inválido: " + status);
            }
        }

        if (subjectId != null) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.equal(root.get("subject").get("id"), subjectId));
        }

        return processRepository.findAll(spec);
    }

    @Transactional
    public Process distribute(Long processId, Long professorId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

        if (process.getStatus() == StatusProcess.APPROVED || process.getStatus() == StatusProcess.REJECTED) {
            throw new IllegalStateException("Não é possível redistribuir um processo já finalizado.");
        }

        if (process.getStatus() == StatusProcess.UNDER_ANALISYS) {
            throw new IllegalStateException("Este processo já foi distribuído para um relator e está em análise. Não é possível redistribuí-lo.");
        }

        if (process.getStatus() != StatusProcess.WAITING) {
            throw new IllegalStateException("O processo não pode ser distribuído pois seu status é: " + process.getStatus());
        }

        process.setProcessRapporteur(professor);
        process.setStatus(StatusProcess.UNDER_ANALISYS);
        process.setDistributedAt(LocalDateTime.now());

        return processRepository.save(process);
    }

    private String generateProcessNumber() {
        int seq = (int) (System.currentTimeMillis() % 100000);
        return String.format("%d-%05d", Year.now().getValue(), seq);
    }

    public List<Process> findFinalizedProcesses() {
        return processRepository.findByStatusIn(List.of(StatusProcess.APPROVED, StatusProcess.REJECTED));
    }

    public boolean canBeVoted(Long processId) {
        Process process = findById(processId);
        return process.getStatus() == StatusProcess.UNDER_ANALISYS
                && process.getProcessRapporteur() != null;
    }

    /**
     * Lista todos os processos pendentes de voto para um professor específico
     * Critério: Processo deve estar UNDER_ANALISYS, estar na mesma reunião do professor,
     * estar associado a um colegiado onde o professor é membro, e o professor NÃO deve ter votado ainda
     */
    public List<Process> findPendingVotesByProfessor(Long professorId) {
        professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

        // Busca todos os processos em análise
        List<Process> allUnderAnalysis = processRepository.findByStatus(StatusProcess.UNDER_ANALISYS);

        // Filtra apenas aqueles em reunião ativa onde o professor é participante
        // e que ainda não votou
        return allUnderAnalysis.stream()
                .filter(process -> {
                    Meeting meeting = process.getMeeting();
                    if (meeting == null || !meeting.isActive()) {
                        return false;
                    }
                    // Verifica se professor é participante da reunião
                    boolean isParticipant = meeting.getParticipants().stream()
                            .anyMatch(p -> p.getId().equals(professorId));
                    if (!isParticipant) {
                        return false;
                    }
                    // Verifica se ainda não votou
                    return !hasVotedProcess(process.getId(), professorId);
                })
                .collect(Collectors.toList());
    }

    /**
     * Lista todos os processos já votados por um professor específico
     * (histórico de votações)
     */
    public List<Process> findVotedProcessesByProfessor(Long professorId) {
        professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

        // Busca todos os votos do professor
        List<Vote> professorVotes = voteRepository.findByProfessorId(professorId);

        // Extrai os processos únicos desses votos e os retorna ordenados por data de voto (mais recentes primeiro)
        return professorVotes.stream()
                .map(Vote::getProcess)
                .distinct()
                .sorted((p1, p2) -> {
                    // Ordena pelos votos mais recentes
                    Vote v1 = professorVotes.stream()
                            .filter(v -> v.getProcess().getId().equals(p1.getId()))
                            .findFirst()
                            .orElse(null);
                    Vote v2 = professorVotes.stream()
                            .filter(v -> v.getProcess().getId().equals(p2.getId()))
                            .findFirst()
                            .orElse(null);
                    if (v1 == null || v2 == null) return 0;
                    return v2.getVotedAt().compareTo(v1.getVotedAt());
                })
                .collect(Collectors.toList());
    }

    /**
     * Verifica se um professor já votou em um processo específico
     */
    public boolean hasVotedProcess(Long processId, Long professorId) {
        return voteRepository.findByProcessIdAndProfessorId(processId, professorId).isPresent();
    }
}