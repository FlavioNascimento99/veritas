package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfessorService {

    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Professor create(Professor professor) {
        if (professor.getLogin() != null && professorRepository.findByLogin(professor.getLogin()).isPresent()) {
            throw new ResourceNotFoundException("Login já cadastrado.");
        }
        if (professor.getRegister() == null || professor.getRegister().isBlank()) {
            professor.setRegister(generateRegister());
        } else if (professorRepository.findByRegister(professor.getRegister()).isPresent()) {
            throw new ResourceNotFoundException("Matrícula já cadastrada.");
        }
        professor.setPassword(passwordEncoder.encode(professor.getPassword()));
        return professorRepository.save(professor);
    }

    private String generateRegister() {
        long count = professorRepository.count();
        return "PRO-" + java.time.Year.now() + "-" + String.format("%04d", count + 1);
    }

    public List<Professor> findAll() {
        return professorRepository.findAll();
    }

    public Professor findById(Long id) {
        return professorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado."));
    }

    @Transactional
    public Professor update(Long id, Professor payload) {
        Professor currentProfessor = findById(id);
        currentProfessor.setName(payload.getName());
        currentProfessor.setPhoneNumber(payload.getPhoneNumber());
        currentProfessor.setLogin(payload.getLogin());
        currentProfessor.setCoordinator(payload.getCoordinator());

        if (payload.getPassword() != null && !payload.getPassword().isEmpty()) {
            currentProfessor.setPassword(passwordEncoder.encode(payload.getPassword()));
        }
        return professorRepository.save(currentProfessor);
    }

    public java.util.Optional<Professor> findByLogin(String login) {
        return professorRepository.findByLogin(login);
    }

    @Transactional
    public void activeStateChanger(Long id) {
        Professor currentProfessor = findById(id);
        currentProfessor.setIsActive(!currentProfessor.getIsActive());
        professorRepository.save(currentProfessor);
    }

    @Transactional
    public void coordinatorStateChanger(Long id) {
        Professor currentProfessor = findById(id);
        currentProfessor.setCoordinator(!currentProfessor.getCoordinator());
        professorRepository.save(currentProfessor);
    }
}
