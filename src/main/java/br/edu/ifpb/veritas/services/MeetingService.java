package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Collegiate;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.repositories.CollegiateRepository;
import br.edu.ifpb.veritas.repositories.MeetingRepository;
import br.edu.ifpb.veritas.repositories.ProcessRepository;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final CollegiateRepository collegiateRepository;
    private final ProcessRepository processRepository;
    private final ProfessorRepository professorRepository;

    @Transactional
    public Meeting create(Meeting meeting) {
        if (meeting.getCreatedAt() == null) {
            meeting.setCreatedAt(LocalDateTime.now());
        }
        return meetingRepository.save(meeting);
    }

    public List<Meeting> findAll() {
        return meetingRepository.findAll();
    }

    public Meeting findById(Long id) {
        return meetingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reunião não encontrada."));
    }

    /**
     * REQFUNC 12: Impede alteração de reunião finalizada
     */
    @Transactional
    public Meeting update(Long id, Meeting payload) {
        Meeting currentMeeting = findById(id);

        // Impede alteração de reunião finalizada
        if (currentMeeting.getStatus() == MeetingStatus.FINALIZADA) {
            throw new IllegalStateException("Não é possível alterar uma reunião finalizada.");
        }

        currentMeeting.setCollegiate(payload.getCollegiate());
        currentMeeting.setProcesses(payload.getProcesses());
        currentMeeting.setScheduledDate(payload.getScheduledDate());
        currentMeeting.setStatus(payload.getStatus());

        return meetingRepository.save(currentMeeting);
    }

    @Transactional
    public Meeting updateStatus(Long id, MeetingStatus status) {
        Meeting meeting = findById(id);

        // Impede alteração de status de reunião já finalizada
        if (meeting.getStatus() == MeetingStatus.FINALIZADA) {
            throw new IllegalStateException("Não é possível alterar o status de uma reunião já finalizada.");
        }

        meeting.setStatus(status);
        return meetingRepository.save(meeting);
    }

    public List<Meeting> findByStatus(MeetingStatus status) {
        return meetingRepository.findByStatus(status);
    }

    public List<Meeting> findByCollegiateId(Long collegiateId) {
        return meetingRepository.findByCollegiateId(collegiateId);
    }

    public List<Meeting> findByCollegiateIdAndStatus(Long collegiateId, MeetingStatus status) {
        return meetingRepository.findByCollegiateIdAndStatus(collegiateId, status);
    }

    public List<Meeting> findByParticipantId(Long professorId) {
        return meetingRepository.findByParticipantsId(professorId);
    }

    public List<Meeting> findScheduledMeetingsByParticipant(Long professorId) {
        return meetingRepository.findByParticipantsIdAndStatus(professorId, MeetingStatus.DISPONIVEL);
    }

    public List<Meeting> findByCollegiateAndParticipant(Long collegiateId, Long professorId) {
        return meetingRepository.findByCollegiateIdAndParticipantsId(collegiateId, professorId);
    }

    /**
     * NOVO: Cria reunião usando TODOS os membros do colegiado automaticamente
     * Parâmetros: collegiateId, scheduledDate, processIds, description
     * Os participantes são automaticamente os membros do colegiado
     */
    @Transactional
    public Meeting createMeetingWithCollegiate(Long collegiateId,
                                               LocalDateTime scheduledDate,
                                               List<Long> processIds,
                                               String description) {
        Collegiate collegiate = collegiateRepository.findById(collegiateId)
                .orElseThrow(() -> new ResourceNotFoundException("Colegiado não encontrado com ID: " + collegiateId));

        // VALIDAÇÃO: Colegiado deve ter membros
        if (collegiate.getCollegiateMemberList() == null || collegiate.getCollegiateMemberList().isEmpty()) {
            throw new IllegalStateException("Colegiado não possui membros. Adicione membros antes de criar uma reunião.");
        }

        // VALIDAÇÃO: Número de membros do colegiado deve ser ÍMPAR para evitar empate
        int memberCount = collegiate.getCollegiateMemberList().size();
        if (memberCount % 2 == 0) {
            throw new IllegalArgumentException("O colegiado deve ter um número ÍMPAR de membros para evitar empates. Atualmente tem: " + memberCount + " membros (par). Por favor, adicione ou remova um membro.");
        }

        log.info("Colegiado {} tem {} membros (ímpar - OK para votação)", collegiateId, memberCount);

        // Usa os membros do colegiado como participantes automaticamente
        List<Professor> participants = new ArrayList<>(collegiate.getCollegiateMemberList());

        // Processa os processos
        List<Process> processes = new ArrayList<>();
        if (processIds != null && !processIds.isEmpty()) {
            for (Long processId : processIds) {
                Process process = processRepository.findById(processId)
                        .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

                if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
                    throw new IllegalStateException("Apenas processos em análise podem ser adicionados à pauta. Processo ID " + processId + " está com status: " + process.getStatus().getStatus());
                }

                if (process.getRapporteurVote() == null) {
                    throw new IllegalStateException("Processo ID " + processId + " não pode ser adicionado pois o relator ainda não votou.");
                }

                if (process.getProcessRapporteur() == null) {
                    throw new IllegalStateException("Processo ID " + processId + " não possui Relator designado.");
                }

                if (process.getMeeting() != null) {
                    throw new IllegalStateException("Processo ID " + processId + " já está vinculado a outra reunião (ID: " + process.getMeeting().getId() + ").");
                }

                processes.add(process);
            }
        }

        Meeting meeting = new Meeting();
        meeting.setCollegiate(collegiate);
        meeting.setScheduledDate(scheduledDate);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setOpenedAt(LocalDateTime.now());
        meeting.setStatus(MeetingStatus.DISPONIVEL);
        meeting.setActive(false);
        meeting.setParticipants(new ArrayList<>(participants));
        
        if (description != null && !description.isEmpty()) {
            meeting.setDescription(description);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            meeting.setDescription("Reunião do Colegiado " + collegiate.getDescription() + " - " + LocalDateTime.now().format(formatter));
        }
        
        for (Process process : processes) {
            process.setMeeting(meeting);
        }
        meeting.setProcesses(new ArrayList<>(processes));
        
        log.info("=== CRIANDO REUNIÃO COM COLEGIADO ===");
        log.info("Colegiado: {}, Processos: {}, Participantes: {}", collegiateId, processes.size(), participants.size());
        
        Meeting savedMeeting = meetingRepository.save(meeting);
        
        log.info("Reunião ID {} criada com sucesso", savedMeeting.getId());
        return savedMeeting;
    }

    @Transactional
    public Meeting createMeetingWithAgenda(Long collegiateId,
                                           LocalDateTime scheduledDate,
                                           List<Long> processIds,
                                           List<Long> participantIds) {
        return createMeetingWithAgenda(collegiateId, scheduledDate, processIds, participantIds, null);
    }

    @Transactional
    public Meeting createMeetingWithAgenda(Long collegiateId,
                                           LocalDateTime scheduledDate,
                                           List<Long> processIds,
                                           List<Long> participantIds,
                                           String description) {

        Collegiate collegiate = collegiateRepository.findById(collegiateId)
                .orElseThrow(() -> new ResourceNotFoundException("Colegiado não encontrado com ID: " + collegiateId));

        List<Process> processes = new ArrayList<>();
        if (processIds != null && !processIds.isEmpty()) {
            for (Long processId : processIds) {
                Process process = processRepository.findById(processId)
                        .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

                if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
                    throw new IllegalStateException("Apenas processos em análise podem ser adicionados à pauta. Processo ID " + processId + " está com status: " + process.getStatus().getStatus());
                }

                if (process.getRapporteurVote() == null) {
                    throw new IllegalStateException("Processo ID " + processId + " não pode ser adicionado à pauta pois o relator ainda não registrou sua decisão.");
                }

                if (process.getProcessRapporteur() == null) {
                    throw new IllegalStateException("Processo ID " + processId + " não possui Relator designado.");
                }

                if (process.getMeeting() != null) {
                    throw new IllegalStateException("Processo ID " + processId + " já está vinculado a outra reunião (ID: " + process.getMeeting().getId() + ").");
                }

                processes.add(process);
            }
        }

        List<Professor> participants = new ArrayList<>();
        if (participantIds != null && !participantIds.isEmpty()) {
            if (participantIds.size() % 2 == 0) {
                throw new IllegalArgumentException("O número de participantes deve ser ÍMPAR para evitar empates. Fornecido: " + participantIds.size() + " (par). Por favor, adicione ou remova um participante.");
            }
            
            for (Long professorId : participantIds) {
                Professor professor = professorRepository.findById(professorId)
                        .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

                if (!collegiate.getCollegiateMemberList().contains(professor)) {
                    throw new IllegalArgumentException("Professor ID " + professorId + " não pertence ao colegiado.");
                }

                participants.add(professor);
            }
        }

        Meeting meeting = new Meeting();
        meeting.setCollegiate(collegiate);
        meeting.setScheduledDate(scheduledDate);
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setOpenedAt(LocalDateTime.now());  // Registra o momento de abertura da reunião
        meeting.setStatus(MeetingStatus.DISPONIVEL);
        meeting.setActive(false);
        meeting.setParticipants(new ArrayList<>(participants));  // Copia a lista para evitar problemas
        
        // Define descrição - usa a fornecida ou gera automaticamente
        if (description != null && !description.isEmpty()) {
            meeting.setDescription(description);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            meeting.setDescription("Reunião do Colegiado - " + LocalDateTime.now().format(formatter));
        }
        
        log.info("=== INICIANDO CRIAÇÃO DE REUNIÃO ===");
        log.info("Processando {} processos e {} participantes", processes.size(), participants.size());

        // Vincula os processos à reunião ANTES de salvar
        for (Process process : processes) {
            process.setMeeting(meeting);
        }
        meeting.setProcesses(new ArrayList<>(processes));  // Copia a lista para evitar problemas
        
        log.info("=== INICIANDO CRIAÇÃO DE REUNIÃO COM {} PROCESSOS ===", processes.size());
        
        // Salva a reunião com todos os relacionamentos definidos em uma única operação
        // CascadeType.ALL garante que processos também serão persistidos
        Meeting savedMeeting = meetingRepository.save(meeting);
        
        log.info("Reunião ID {} criada com sucesso com {} processos e {} participantes", 
                 savedMeeting.getId(), 
                 savedMeeting.getProcesses().size(),
                 savedMeeting.getParticipants().size());
        
        return savedMeeting;
    }

    @Transactional
    public Meeting addProcessesToAgenda(Long meetingId, List<Long> processIds) {
        Meeting meeting = findById(meetingId);

        if (meeting.getStatus() == MeetingStatus.FINALIZADA) {
            throw new IllegalStateException("Não é possível alterar a pauta de uma reunião finalizada.");
        }

        for (Long processId : processIds) {
            Process process = processRepository.findById(processId)
                    .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

            // VALIDAÇÃO: Processo deve estar EM_ANALISE
            if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
                throw new IllegalStateException("Apenas processos em análise podem ser adicionados à pauta. Status atual: " + process.getStatus().getStatus());
            }

            // VALIDAÇÃO: Relator deve ter votado
            if (process.getRapporteurVote() == null) {
                throw new IllegalStateException("O relator do processo (ID: " + processId + ") ainda não registrou sua decisão.");
            }

            // VALIDAÇÃO: Relator deve ser diferente de null
            if (process.getProcessRapporteur() == null) {
                throw new IllegalStateException("Processo ID " + processId + " não possui Relator designado.");
            }

            // VALIDAÇÃO: Processo não deve estar em nenhuma outra reunião
            if (process.getMeeting() != null && !process.getMeeting().getId().equals(meetingId)) {
                throw new IllegalStateException("Processo ID " + processId + " já está vinculado a outra reunião (ID: " + process.getMeeting().getId() + ").");
            }

            if (!meeting.getProcesses().contains(process)) {
                process.setMeeting(meeting);
                meeting.getProcesses().add(process);
            }
        }
        return meetingRepository.save(meeting);
    }

    @Transactional
    public Meeting addParticipants(Long meetingId, List<Long> participantIds) {
        Meeting meeting = findById(meetingId);

        if (meeting.getStatus() == MeetingStatus.FINALIZADA) {
            throw new IllegalStateException("Não é possível alterar participantes de uma reunião finalizada.");
        }

        for (Long professorId : participantIds) {
            Professor professor = professorRepository.findById(professorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

            if (!meeting.getParticipants().contains(professor)) {
                meeting.getParticipants().add(professor);
            }
        }

        return meetingRepository.save(meeting);
    }

    /**
     * REQFUNC 10: Inicia uma reunião (muda status para EM_ANDAMENTO)
     * Validação: Apenas uma reunião pode estar ativa por vez
     */
    @Transactional
    public Meeting startMeeting(Long meetingId) {
        meetingRepository.findByActiveTrue().ifPresent(activeMeeting -> {
            throw new IllegalStateException("Já existe uma reunião ativa (ID: " + activeMeeting.getId() + "). Finalize-a antes de iniciar outra.");
        });

        Meeting meeting = findById(meetingId);

        if (meeting.getStatus() == MeetingStatus.FINALIZADA) {
            throw new IllegalStateException("Não é possível iniciar uma reunião já finalizada.");
        }

        if (meeting.getStatus() == MeetingStatus.EM_ANDAMENTO) {
            throw new IllegalStateException("Esta reunião já está em andamento.");
        }

        meeting.setActive(true);
        meeting.setStatus(MeetingStatus.EM_ANDAMENTO);

        return meetingRepository.save(meeting);
    }

    /**
     * REQFUNC 12: Finaliza uma reunião (muda status para FINALIZADA)
     * 
     * Validações:
     * 1. Reunião deve estar EM_ANDAMENTO
     * 2. Todos os processos da pauta devem estar apregoados (APPROVED ou REJECTED)
     */
    @Transactional
    public Meeting finalizeMeeting(Long meetingId) {
        Meeting meeting = findById(meetingId);

        if (meeting.getStatus() != MeetingStatus.EM_ANDAMENTO) {
            throw new IllegalStateException("Apenas reuniões em andamento podem ser finalizadas. Status atual: " + meeting.getStatus().getStatus());
        }

        // VALIDAÇÃO: Todos os processos devem estar apregoados
        boolean allProcessesAnnounced = meeting.getProcesses().stream()
                .allMatch(p -> p.getStatus() == StatusProcess.APPROVED || p.getStatus() == StatusProcess.REJECTED);

        if (!allProcessesAnnounced) {
            throw new IllegalStateException("Não é possível finalizar a reunião. Existem processos ainda não apregoados. Todos os processos devem estar APROVADOS ou REJEITADOS.");
        }

        meeting.setActive(false);
        meeting.setStatus(MeetingStatus.FINALIZADA);

        return meetingRepository.save(meeting);
    }

    public Meeting findActiveMeeting() {
        return meetingRepository.findByActiveTrue()
                .orElseThrow(() -> new ResourceNotFoundException("Nenhuma reunião está ativa no momento."));
    }
}