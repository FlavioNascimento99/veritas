package br.edu.ifpb.veritas.repositories;

import br.edu.ifpb.veritas.models.Collegiate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegiateRepository extends JpaRepository<Collegiate, Long> {
    // Optional<Collegiate> findByRepresentativeStudentId(Long studentId);
    // Optional<Collegiate> findByCollegiateMeetingsListId(Long meetingId);
    
    /**
     * Override do findById com FETCH JOIN para carregar membros eagerly
     * Evita LazyInitializationException ao renderizar a view
     */
    @Query("SELECT DISTINCT c FROM Collegiate c LEFT JOIN FETCH c.collegiateMemberList LEFT JOIN FETCH c.rapporteur WHERE c.id = :id")
    Optional<Collegiate> findById(@Param("id") Long id);
    
    /**
     * Busca um colegiado onde um professor específico é membro.
     * Usa JPQL para seguir o relacionamento ManyToMany corretamente.
     * FETCH JOIN para carregar membros eagerly.
     */
    @Query("SELECT DISTINCT c FROM Collegiate c LEFT JOIN FETCH c.collegiateMemberList WHERE c.id IN (SELECT c2.id FROM Collegiate c2 JOIN c2.collegiateMemberList p WHERE p.id = :professorId)")
    Optional<Collegiate> findByCollegiateMemberListId(@Param("professorId") Long professorId);
    
    /**
     * DEBUG: Lista todos os colegiados com seus membros para diagnóstico
     */
    @Query("SELECT DISTINCT c FROM Collegiate c LEFT JOIN FETCH c.collegiateMemberList LEFT JOIN FETCH c.rapporteur")
    List<Collegiate> findAllWithMembers();
}