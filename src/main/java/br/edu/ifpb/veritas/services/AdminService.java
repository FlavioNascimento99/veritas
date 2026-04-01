package br.edu.ifpb.veritas.services;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import br.edu.ifpb.veritas.models.*;
import br.edu.ifpb.veritas.repositories.AdminRepository;
import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;

/**
 * Classe de serviço dos Administradores do Sistema.
 * @author Flavio Nascimento
 * @author Felipe Cartaxo
 * 
 * @see "Matricula" não deveria ser gerado manualmente pelo usuário, 
 * mas sim, possuir uma determinada lógica para a geração do mesmo. 
 * 
 * @see "Listagem" de Administradores não faz sentido existir dentro 
 * da ideia geral da aplicação.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final StudentService studentService;
    private final ProfessorService professorService;
    private final CollegiateService collegiateService;
    private final SubjectService subjectService;
    private final PasswordEncoder passwordEncoder;

/**
 * Métodos de gerenciamento de Entidades Administrativas; 
 * 
 * @author Flavio Nascimento
 */
    public Administrator find(Long id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador não encontrado."));
    }

    public List<Administrator> list() {
        return adminRepository.findAll();
    }

    public List<Administrator> findAll() {
        return adminRepository.findAll();
    }

    public Optional<Administrator> findByLogin(String login) {
        return adminRepository.findByLogin(login);
    }

    @Transactional
    public Administrator create(Administrator admin) {
        if (admin.getLogin() != null && adminRepository.findByLogin(admin.getLogin()).isPresent()) {
            throw new ResourceNotFoundException("Nome de usuário não disponível.");
        }
        if (admin.getRegister() != null && adminRepository.findByRegister(admin.getRegister()).isPresent()) {
            throw new ResourceNotFoundException("Esta matrícula já se encontra cadastrada.");
        }
        admin.setPassword(passwordEncoder.encode(admin.getPassword()));
        return adminRepository.save(admin);
    }

    @Transactional
    public Administrator update(Long id, Administrator payload) {
        Administrator current = find(id);
        current.setName(payload.getName());
        current.setPhoneNumber(payload.getPhoneNumber());
        current.setLogin(payload.getLogin());
        current.setRegister(payload.getRegister());

        if (payload.getPassword() != null && !payload.getPassword().isEmpty()) {
            current.setPassword(passwordEncoder.encode(payload.getPassword()));
        }

        return adminRepository.save(current);
    }

    @Transactional
    public void deactivate(Long id) {
        Administrator current = find(id);

        if (!current.getIsActive()) {
            throw new ResourceNotFoundException("Administrador já está desativado.");
        }
        current.setIsActive(false);
        adminRepository.save(current);
    }

    @Transactional
    public void reactivate(Long id) {
        Administrator current = find(id);

        if (current.getIsActive()) {
            throw new ResourceNotFoundException("Administrador já está ativo.");
        }
        current.setIsActive(true);
        adminRepository.save(current);
    }


/**
 * Métodos de gerenciamento de Entidades Estudantis;
 * 
 * @author Flavio Nascimento
 */
    public List<Student> listStudents() {
        return studentService.findAll();
    }

    public Student findStudentById(Long id) {
        return studentService.findById(id);
    }
    
    @Transactional
    public Student createStudent(Student student) {
        return studentService.create(student);
    }

    @Transactional
    public Student updateStudent(Long id, Student payload) {
        return studentService.update(id, payload);
    }

    @Transactional
    public void deactivateStudent(Long id) {
        studentService.desactivate(id);
    }

    @Transactional
    public void reactivateStudent(Long id) {
        studentService.reactivate(id);
    }

/**
 * Métodos de gerenciamento de Entidades Professores;
 * 
 * @author Flavio Nascimento 
 */
    public List<Professor> listProfessors() {
        return professorService.findAll();
    }

    public Professor findProfessorById(Long id) {
        return professorService.findById(id);
    }
    
    @Transactional
    public Professor createProfessor(Professor professor) {
        return professorService.create(professor);
    }

    @Transactional
    public Professor updateProfessor(Long id, Professor payload) {
        return professorService.update(id, payload);
    }

    @Transactional
    public void toggleProfessorActive(Long id) {
        professorService.activeStateChanger(id);
    }

    @Transactional
    public void toggleCoordinator(Long id) {
        professorService.coordinatorStateChanger(id);
    }


/**
 * Métodos de gerenciamento de Entidade Colegiados;
 * 
 * @author Flavio Nascimento
 */
    @Transactional
    public Collegiate createCollegiate(Collegiate collegiate) {
        return collegiateService.create(collegiate);
    }

    public List<Collegiate> listCollegiates() {
        return collegiateService.findAll();
    }

    public Collegiate findCollegiateById(Long id) {
        return collegiateService.findById(id);
    }
    
    @Transactional
    public Collegiate updateCollegiate(Long id, Collegiate payload) {
        return collegiateService.update(id, payload);
    }

    @Transactional
    public void deleteCollegiate(Long id) {
        collegiateService.unactivate(id);
    }

    public List<Professor> listCollegiateMembers(Long collegiateId) {
        return collegiateService.findProfessorsByCollegiate(collegiateId);
    }

/**
 * Métodos de gerenciamento de Entidade Temas;
 * 
 * @author Flavio Nascimento
 */
    @Transactional
    public Subject createSubject(Subject subject) {
        return subjectService.create(subject);
    }

    public List<Subject> listSubjects() {
        return subjectService.findAll();
    }

    public Subject findSubjectById(Long id) {
        return subjectService.findById(id);
    }
    
    @Transactional
    public Subject updateSubject(Long id, Subject payload) {
        return subjectService.update(id, payload);
    }

    @Transactional
    public void deactivateSubject(Long id) {
        subjectService.deactivate(id);
    }

    @Transactional
    public void reactivateSubject(Long id) {
        subjectService.reactivate(id);
    }

}