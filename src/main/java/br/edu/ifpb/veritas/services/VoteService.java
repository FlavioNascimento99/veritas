package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.enums.DecisionType;
import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.enums.StatusProcess;
import br.edu.ifpb.veritas.enums.VoteType;
import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.models.Process;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Vote;
import br.edu.ifpb.veritas.repositories.MeetingRepository;
import br.edu.ifpb.veritas.repositories.ProcessRepository;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import br.edu.ifpb.veritas.repositories.VoteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Serviço responsável pelo gerenciamento de votos no sistema de colegiado.
 *
 * Lógica de votação (pelo menos foi o que entendi):
 * 1) Relator vota primeiro: DEFERIMENTO ou INDEFERIMENTO (com justificativa)
 * 2) Membros do colegiado votam: COM_RELATOR ou DIVERGENTE
 * 3) Sistema calcula resultado: maioria define se prevalece voto do relator ou não
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final ProcessRepository processRepository;
    private final ProfessorRepository professorRepository;
    private final MeetingRepository meetingRepository;

//    =============== ENTENDIMENTO INICIAL DA VOTAÇÃO ===============
//    /**
//     * REQFUNC 5: Professor registra seu voto em um processo.
//     *
//     * Validações:
//     * - Processo não pode estar finalizado
//     * - Professor não pode votar duas vezes no mesmo processo
//     * - Reunião contendo o processo não pode estar finalizada
//     */
   @Transactional
   public Vote registerVote(Long processId, Long professorId, VoteType voteType, String justification) {
       Process process = processRepository.findById(processId)
               .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

       Professor professor = professorRepository.findById(professorId)
               .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

       // Impede voto em processo já finalizado
       if (process.getStatus() == StatusProcess.APPROVED || process.getStatus() == StatusProcess.REJECTED) {
           throw new IllegalStateException("Não é possível votar em um processo já finalizado. Status: " + process.getStatus());
       }

       // Impede voto em reunião finalizada
       validateMeetingNotFinalized(processId);

       // Valida se o processo está sob análise
       if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
           throw new IllegalStateException("Processo não está disponível para votação. Status atual: " + process.getStatus());
       }

       // Verifica se o professor já votou
       Optional<Vote> existingVote = voteRepository.findByProcessIdAndProfessorId(processId, professorId);
       if (existingVote.isPresent()) {
           throw new IllegalStateException("Professor já votou neste processo. ID do voto existente: " + existingVote.get().getId());
       }

       // Cria e salva o voto
       Vote vote = new Vote();
       vote.setProcess(process);
       vote.setProfessor(professor);
       vote.setVoteType(voteType);
       vote.setJustification(justification);
       vote.setAway(false);
       vote.setVotedAt(LocalDateTime.now());

       Vote savedVote = voteRepository.save(vote);

       // Se o professor é o relator, atualiza o voto do relator no processo
    //    if (process.getProcessRapporteur() != null &&
    //            process.getProcessRapporteur().getId().equals(professorId)) {

    //        process.setRapporteurVote(vote.getVoteType());
    //        processRepository.save(process);
    //    }
       
       return savedVote;
   }

    /**
     * Achei melhor separar os métodos (um pro voto do relator e outro pro voto dos membros do colegiado)
     *
     * REQFUNC 5: Professor RELATOR registra seu voto (decisão) sobre o processo.
     *
     * O relator vota pelo DEFERIMENTO ou INDEFERIMENTO do processo, podendo incluir
     * uma justificativa escrita.
     *
     * Observações:
     * - APENAS o relator designado pode votar
     * - Processo deve estar UNDER_ANALYSIS
     * - Relator NÃO PODE votar duas vezes
     * - Processo NÃO PODE estar em reunião já finalizada
     */
    @Transactional
    public Process registerRapporteurDecision(Long processId, Long professorId,
                                              DecisionType decision, String justification) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

        // Valida se o professor é o relator do processo
        if (process.getProcessRapporteur() == null ||
                !process.getProcessRapporteur().getId().equals(professorId)) {
            throw new IllegalStateException("Apenas o relator designado pode registrar a decisão sobre o processo.");
        }

        // Valida se processo está em análise
        if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
            throw new IllegalStateException("Processo não está disponível para votação do relator. Status atual: " + process.getStatus().getStatus());
        }

        // Impede voto duplicado do relator
        if (process.getRapporteurVote() != null) {
            throw new IllegalStateException("Relator já registrou sua decisão para este processo.");
        }

        // Valida se reunião não está finalizada (caso processo esteja em pauta)
        validateMeetingNotFinalized(processId);

        // Registra a decisão do relator
        process.setRapporteurVote(decision);
        process.setRapporteurJustification(justification);

        Process savedProcess = processRepository.save(process);
        
        // NOVO: Verifica se todos os membros já votaram (incluindo relator) e finaliza automaticamente
        if (process.getMeeting() != null) {
            log.info("Relator votou no processo {}. Verificando se todos votaram...", processId);
            if (allMembersHaveVoted(processId, process.getMeeting().getId())) {
                log.info("TODOS votaram (incluindo relator)! Finalizando processo {} automaticamente...", processId);
                finalizeProcessAutomatically(processId);
            } else {
                log.info("Ainda faltam membros votarem no processo {}", processId);
            }
        }
        
        return savedProcess;
    }

    /**
     * Registra o voto de um membro do colegiado (NÃO relator) em um processo.
     *
     * O membro vota se concorda (COM_RELATOR) ou discorda (DIVERGENTE) da decisão do relator.
     *
     * Este voto acontece durante a reunião do colegiado.
     *
     * Validações:
     * - Relator já deve ter votado TODOS os processos da reunião
     * - Processo deve estar EM_ANALISE
     * - Professor não pode ser o relator
     * - Professor não pode votar duas vezes
     * - Reunião não pode estar finalizada
     * - Deve existir reunião ativa
     * - Reunião deve estar em estado DISPONÍVEL
     */
    @Transactional
    public Vote registerMemberVote(Long processId, Long professorId,
                                   VoteType voteType, String justification) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

        // Valida se relator já votou
        if (process.getRapporteurVote() == null) {
            throw new IllegalStateException("O relator ainda não registrou sua decisão sobre este processo.");
        }

        // Valida se processo está em análise
        if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
            throw new IllegalStateException("Processo não está disponível para votação. Status atual: " + process.getStatus().getStatus());
        }

        // Valida que quem está votando NÃO é o relator (talvez seja desnecessário, mas né, validar demais nunca é ruim)
        if (process.getProcessRapporteur() != null &&
                process.getProcessRapporteur().getId().equals(professorId)) {
            throw new IllegalStateException("O relator não vota novamente como membro. Sua decisão já foi registrada.");
        }

        // Verifica se reunião está ativa
        Meeting activeMeeting = meetingRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Não há reunião ativa no momento."));

        // VALIDAÇÃO: Reunião deve estar em estado DISPONIVEL
        if (activeMeeting.getStatus() != MeetingStatus.DISPONIVEL) {
            throw new IllegalStateException("A reunião não está disponível para votação. Status atual: " + activeMeeting.getStatus().getStatus());
        }

        // VALIDAÇÃO: Relator deve ter votado TODOS os processos da reunião
        if (!hasRapporteurVotedAllProcesses(activeMeeting.getId())) {
            throw new IllegalStateException("O relator ainda não votou todos os processos desta reunião. Aguarde que o relator finalize suas votações.");
        }

        // Valida se processo está na pauta da reunião ativa
        boolean isInAgenda = activeMeeting.getProcesses().stream()
                .anyMatch(p -> p.getId().equals(processId));

        if (!isInAgenda) {
            throw new IllegalStateException("Processo não está na pauta da reunião ativa (ID: " + activeMeeting.getId() + ").");
        }

        // Valida se professor é participante da reunião
        boolean isParticipant = activeMeeting.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(professorId));

        if (!isParticipant) {
            throw new IllegalStateException("Professor não é participante da reunião ativa.");
        }

        // Verifica se professor já votou
        Optional<Vote> existingVote = voteRepository.findByProcessIdAndProfessorId(processId, professorId);
        if (existingVote.isPresent()) {
            throw new IllegalStateException("Professor já votou neste processo. ID do voto: " + existingVote.get().getId());
        }

        // Valida reunião não finalizada
        validateMeetingNotFinalized(processId);

        // Cria e salva o voto
        Vote vote = new Vote();
        vote.setProcess(process);
        vote.setProfessor(professor);
        vote.setVoteType(voteType);
        vote.setJustification(justification);
        vote.setAway(false);
        vote.setVotedAt(LocalDateTime.now());

        Vote savedVote = voteRepository.save(vote);
        
        // NOVO: Verifica se todos os membros votaram e finaliza automaticamente
        log.info("Voto registrado. Verificando se todos os membros votaram para o processo {}", processId);
        if (allMembersHaveVoted(processId, activeMeeting.getId())) {
            log.info("TODOS os membros votaram! Finalizando processo {} automaticamente...", processId);
            finalizeProcessAutomatically(processId);
        } else {
            log.info("Ainda faltam membros votarem no processo {}", processId);
        }
        
        return savedVote;
    }

    /**
     * Sobrecarregação: Registra voto de membro SEM justificação obrigatória (REQFUNC 11)
     * Usado quando membros votam em uma reunião ativa
     */
    @Transactional
    public Vote registerMemberVote(Long processId, Long professorId, VoteType voteType) {
        return registerMemberVote(processId, professorId, voteType, "");
    }
    
    /**
     * Verifica se todos os membros do colegiado (excluindo o relator) votaram em um processo.
     * O relator já votou via registerRapporteurDecision, então não é contado aqui.
     * 
     * Casos:
     * 1. Se há membros além do relator: todos devem ter votado
     * 2. Se só há o relator: considera completo assim que ele votar
     */
    private boolean allMembersHaveVoted(Long processId, Long meetingId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado."));
        
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Reunião não encontrada."));
        
        // Total de participantes EXCLUINDO o relator
        long totalParticipantsExcludingRapporteur = meeting.getParticipants().stream()
                .filter(p -> !p.getId().equals(process.getProcessRapporteur().getId()))
                .count();
        
        // Se não há outros participantes além do relator, basta o relator ter votado
        if (totalParticipantsExcludingRapporteur == 0) {
            boolean rapporteurVoted = process.getRapporteurVote() != null;
            log.info("Processo {}: Relator é único participante. Voto do relator registrado: {}", processId, rapporteurVoted);
            return rapporteurVoted;
        }
        
        // Total de votos registrados de membros (presente)
        Long totalMemberVotes = voteRepository.countVotesByProcessId(processId);
        
        log.info("Processo {}: {} votos de membros registrados / {} membros (excluindo relator)", 
                processId, totalMemberVotes, totalParticipantsExcludingRapporteur);
        
        // Todos votaram quando votos >= membros (excluindo relator)
        return totalMemberVotes >= totalParticipantsExcludingRapporteur;
    }
    
    /**
     * Finaliza um processo automaticamente quando todos os membros votam.
     * Calcula o resultado e atualiza o status para APPROVED ou REJECTED.
     * 
     * NOTA: Este é um método PRIVATE, então não tem sua própria transação.
     * Executa dentro da transação do método público que o chamou (registerRapporteurDecision, registerMemberVote, etc).
     */
    private void finalizeProcessAutomatically(Long processId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado."));
        
        log.info("=== INICIANDO FINALIZACAO AUTOMATICA DO PROCESSO {} ===", processId);
        log.info("Status atual do processo: {}", process.getStatus());
        
        // Só finaliza se ainda estiver em análise
        if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
            log.warn("Processo {} nao pode ser finalizado. Status ja alterado para: {}", processId, process.getStatus());
            return;
        }
        
        // Calcula o resultado
        DecisionType result = calculateResult(processId);
        log.info("Resultado da votacao calculado: {}", result);
        
        // Atualiza o status
        StatusProcess novoStatus;
        if (result == DecisionType.DEFERIMENTO) {
            novoStatus = StatusProcess.APPROVED;
        } else {
            novoStatus = StatusProcess.REJECTED;
        }
        
        process.setStatus(novoStatus);
        process.setSolvedAt(LocalDateTime.now());
        Process processoAtualizado = processRepository.save(process);
        
        log.info("=== PROCESSO {} FINALIZADO COM SUCESSO ===", processId);
        log.info("Novo status: {}", novoStatus);
        log.info("Resultado: {}", result);
        log.info("Data de solucao: {}", processoAtualizado.getSolvedAt());
    }

    /**
     * Verifica se reunião contendo o processo não está finalizada
     */
    private void validateMeetingNotFinalized(Long processId) {
        List<Meeting> meetings = meetingRepository.findByProcessInAgenda(processId);

        for (Meeting meeting : meetings) {
            if (meeting.getStatus() == MeetingStatus.FINALIZADA) {
                throw new IllegalStateException(
                        "Não é possível votar em um processo cuja reunião (ID: "
                                + meeting.getId() + ") já foi finalizada."
                );
            }
        }
    }

    /**
     * Valida se todos os votos do relator foram registrados em uma reunião.
     * Apenas após isso, os demais membros podem votar.
     */
    public boolean hasRapporteurVotedAllProcesses(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new ResourceNotFoundException("Reunião não encontrada."));

        return meeting.getProcesses().stream()
                .allMatch(p -> p.getRapporteurVote() != null);
    }

    /**
     * REQFUNC 11: Apregoa um processo e calcula seu resultado final automaticamente.
     *
     * Regra de cálculo:
     * - Conta votos COM RELATOR vs DIVERGENTE DO RELATOR
     * - Se maioria votou COM RELATOR → resultado = decisão do relator
     * - Se maioria votou DIVERGENTE → resultado = contrário à decisão do relator
     * - Em caso de empate, prevalece o voto do relator
     *
     * Atualiza o status do processo para APPROVED ou REJECTED.
     * 
     * Validações:
     * - Processo deve estar UNDER_ANALISYS
     * - Relator já deve ter votado
     * - Reunião deve estar EM_ANDAMENTO
     * - Processo deve estar na pauta
     */
    @Transactional
    public Process announceProcess(Long processId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        // Valida se o processo está sob análise
        if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
            throw new IllegalStateException("Processo não pode ser apregoado. Status atual: " + process.getStatus());
        }

        // Valida se o relator já votou
        if (process.getRapporteurVote() == null) {
            throw new IllegalStateException("Relator ainda não votou neste processo. Apregoamento não pode ser realizado.");
        }

        // Valida se há uma reunião ativa
        Meeting activeMeeting = meetingRepository.findByActiveTrue()
                .orElseThrow(() -> new IllegalStateException("Não há reunião ativa no momento."));

        // Valida se reunião está EM_ANDAMENTO
        if (activeMeeting.getStatus() != MeetingStatus.EM_ANDAMENTO) {
            throw new IllegalStateException("Reunião não está em andamento. Status atual: " + activeMeeting.getStatus().getStatus());
        }

        // Valida se o processo está na pauta da reunião ativa
        boolean isInAgenda = activeMeeting.getProcesses().stream()
                .anyMatch(p -> p.getId().equals(processId));

        if (!isInAgenda) {
            throw new IllegalStateException("Processo não está na pauta da reunião ativa (ID: " + activeMeeting.getId() + ").");
        }

        // Calcula o resultado da votação
        DecisionType result = calculateResult(processId);

        // Atualiza o status do processo baseado no resultado
        if (result == DecisionType.DEFERIMENTO) {
            process.setStatus(StatusProcess.APPROVED);
        } else {
            process.setStatus(StatusProcess.REJECTED);
        }

        process.setSolvedAt(LocalDateTime.now());

        return processRepository.save(process);
    }

    /**
     * REQFUNC 11: Calcula o resultado da votação de um processo.
     *
     * Lógica:
     * 1. Conta quantos membros votaram COM_RELATOR
     * 2. Conta quantos membros votaram DIVERGENTE
     * 3. Se maioria votou COM_RELATOR → mantém decisão do relator
     * 4. Se maioria votou DIVERGENTE → inverte decisão do relator
     * 5. Em empate → prevalece decisão do relator
     */
    public DecisionType calculateResult(Long processId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado."));

        DecisionType rapportVote = process.getRapporteurVote();

        if (rapportVote == null) {
            throw new IllegalStateException("Relator ainda não votou neste processo.");
        }

        // Conta votos dos membros
        Long votosComRelator = voteRepository.countByProcessIdAndVoteType(processId, VoteType.DEFERIDO);
        Long votosDivergentes = voteRepository.countByProcessIdAndVoteType(processId, VoteType.INDEFERIDO);

        // Se não houve votação dos membros, prevalece decisão do relator
        if (votosComRelator == 0 && votosDivergentes == 0) {
            return rapportVote;
        }

        // Se maioria votou COM_RELATOR, mantém decisão do relator
        if (votosComRelator >= votosDivergentes) {
            return rapportVote;
        } else {
            // Se maioria votou DIVERGENTE, inverte a decisão
            return rapportVote == DecisionType.DEFERIMENTO
                    ? DecisionType.INDEFERIMENTO
                    : DecisionType.DEFERIMENTO;
        }
    }

    /**
     * Registra ausência de um professor em uma votação
     */
    @Transactional
    public Vote registerAbsence(Long processId, Long professorId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado."));

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado."));

        // Valida se reunião não está finalizada
        validateMeetingNotFinalized(processId);

        // Valida se processo não está finalizado
        if (process.getStatus() == StatusProcess.APPROVED || process.getStatus() == StatusProcess.REJECTED) {
            throw new IllegalStateException("Não é possível registrar ausência em processo já finalizado.");
        }

        Vote vote = new Vote();
        vote.setProcess(process);
        vote.setProfessor(professor);
        vote.setAway(true);
        vote.setVotedAt(LocalDateTime.now());

        return voteRepository.save(vote);
    }

    // Métodos adicionais de consulta
    public List<Vote> findVotesByProcess(Long processId) {
        return voteRepository.findByProcessId(processId);
    }

    public List<Vote> findActiveVotesByProcess(Long processId) {
        return voteRepository.findActiveVotesByProcessId(processId);
    }

    public boolean hasVoted(Long processId, Long professorId) {
        return voteRepository.findByProcessIdAndProfessorId(processId, professorId).isPresent();
    }

    /**
     * MÉTODO SIMPLIFICADO PARA VOTAÇÃO DE PROFESSOR EM REUNIÃO
     * 
     * Registra o voto de um professor (membro do colegiado) em um processo
     * de forma simplificada, validando apenas o essencial.
     * 
     * Validações:
     * - Processo deve estar UNDER_ANALISYS
     * - Professor não pode ser o relator
     * - Professor não pode votar duas vezes
     * - Reunião do processo deve estar ativa
     */
    @Transactional
    public Vote registerProfessorVote(Long processId, Long professorId, VoteType voteType, String justification) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado com ID: " + processId));

        Professor professor = professorRepository.findById(professorId)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + professorId));

        // Valida se processo está em análise
        if (process.getStatus() != StatusProcess.UNDER_ANALISYS) {
            throw new IllegalStateException("Processo não está disponível para votação. Status atual: " + process.getStatus().getStatus());
        }

        // Valida que quem está votando NÃO é o relator
        if (process.getProcessRapporteur() != null &&
                process.getProcessRapporteur().getId().equals(professorId)) {
            throw new IllegalStateException("O relator não pode votar como membro do colegiado.");
        }

        // Verifica se professor já votou
        Optional<Vote> existingVote = voteRepository.findByProcessIdAndProfessorId(processId, professorId);
        if (existingVote.isPresent()) {
            throw new IllegalStateException("Professor já votou neste processo. Você não pode alterar seu voto.");
        }

        // Valida se processo está em reunião ativa
        Meeting meeting = process.getMeeting();
        if (meeting == null || !meeting.isActive()) {
            throw new IllegalStateException("Processo não está em uma reunião ativa.");
        }

        // Valida se professor é participante da reunião
        boolean isParticipant = meeting.getParticipants().stream()
                .anyMatch(p -> p.getId().equals(professorId));
        if (!isParticipant) {
            throw new IllegalStateException("Professor não é participante da reunião.");
        }

        // Cria e salva o voto
        Vote vote = new Vote();
        vote.setProcess(process);
        vote.setProfessor(professor);
        vote.setVoteType(voteType);
        vote.setJustification(justification != null ? justification : "");
        vote.setAway(false);
        vote.setVotedAt(LocalDateTime.now());

        Vote savedVote = voteRepository.save(vote);

        // LÓGICA: Verifica se todos os participantes votaram
        // Se sim, calcula o resultado final automaticamente
        log.info("Voto de professor registrado. Verificando se todos votaram para o processo {}", processId);
        if (allMembersHaveVoted(processId, meeting.getId())) {
            log.info("TODOS os membros votaram! Finalizando processo {} automaticamente...", processId);
            finalizeProcessAutomatically(processId);
        } else {
            log.info("Ainda faltam membros votarem no processo {}", processId);
        }

        return savedVote;
    }

    /**
     * Retorna informações sobre a votação de um processo
     */
    public VotingStats getVotingStats(Long processId) {
        Process process = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Processo não encontrado."));

        Long totalVotes = voteRepository.countVotesByProcessId(processId);
        Long votesForDeferido = voteRepository.countByProcessIdAndVoteType(processId, VoteType.DEFERIDO);
        Long votesForIndeferido = voteRepository.countByProcessIdAndVoteType(processId, VoteType.INDEFERIDO);

        return new VotingStats(
                totalVotes,
                votesForDeferido,
                votesForIndeferido,
                process.getRapporteurVote(),
                process.getStatus()
        );
    }

    /**
     * Estatísticas de votação (precisa ser revisado)
     */
    public static class VotingStats {
        public final Long totalVotes;
        public final Long votesForDeferido;
        public final Long votesForIndeferido;
        public final DecisionType rapporteurVote;
        public final StatusProcess processStatus;

        public VotingStats(Long totalVotes, Long votesForDeferido, Long votesForIndeferido,
                           DecisionType rapporteurVote, StatusProcess processStatus) {
            this.totalVotes = totalVotes;
            this.votesForDeferido = votesForDeferido;
            this.votesForIndeferido = votesForIndeferido;
            this.rapporteurVote = rapporteurVote;
            this.processStatus = processStatus;
        }
    }
}