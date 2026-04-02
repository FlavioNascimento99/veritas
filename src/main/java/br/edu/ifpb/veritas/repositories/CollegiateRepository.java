package br.edu.ifpb.veritas.repositories;

import br.edu.ifpb.veritas.models.Collegiate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollegiateRepository extends JpaRepository<Collegiate, Long> {
    // Optional<Collegiate> findByRepresentativeStudentId(Long studentId);
    // Optional<Collegiate> findByCollegiateMeetingsListId(Long meetingId);
    
    /**
     * Busca um colegiado onde um professor específico é membro.
     * Usa JPQL para seguir o relacionamento ManyToMany corretamente.
     */
    @Query("SELECT c FROM Collegiate c JOIN c.collegiateMemberList p WHERE p.id = :professorId")
    Optional<Collegiate> findByCollegiateMemberListId(@Param("professorId") Long professorId);
    
    /**
     * DEBUG: Lista todos os colegiados com seus membros para diagnóstico
     */
    @Query("SELECT c FROM Collegiate c LEFT JOIN FETCH c.collegiateMemberList")
    List<Collegiate> findAllWithMembers();
}