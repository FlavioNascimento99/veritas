package br.edu.ifpb.veritas.repositories;

import br.edu.ifpb.veritas.enums.VoteType;
import br.edu.ifpb.veritas.models.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    // REQFUNC 5: Verifica se um professor já votou em um processo específico (é pra evitar que um professor vote duas vezes no mesmo processo)
    Optional<Vote> findByProcessIdAndProfessorId(Long processId, Long professorId);

    // Busca todos os votos de um processo específico
    List<Vote> findByProcessId(Long processId);

    // Busca todos os votos de um professor específico
    List<Vote> findByProfessorId(Long professorId);

    // REQFUNC 11: Conta quantos votos de um determinado tipo um processo recebeu
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.process.id = :processId AND v.voteType = :voteType AND v.away = false")
    Long countByProcessIdAndVoteType(@Param("processId") Long processId, @Param("voteType") VoteType voteType);

    // Conta quantos professores votaram em um processo (excluindo os ausentes)
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.process.id = :processId AND v.away = false")
    Long countVotesByProcessId(@Param("processId") Long processId);

    // Conta quantos professores já se posicionaram em um processo (voto ou ausência)
    @Query("SELECT COUNT(v) FROM Vote v WHERE v.process.id = :processId")
    Long countAllVotesByProcessId(@Param("processId") Long processId);

    // Busca votos de um processo (excluindo ausentes)
    @Query("SELECT v FROM Vote v WHERE v.process.id = :processId AND v.away = false")
    List<Vote> findActiveVotesByProcessId(@Param("processId") Long processId);
}
