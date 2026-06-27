package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.exceptions.ResourceNotFoundException;
import br.edu.ifpb.veritas.models.Student;
import br.edu.ifpb.veritas.repositories.StudentRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class StudentService {

    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Student create(Student student) {
        if (student.getLogin() != null && studentRepository.findByLogin(student.getLogin()).isPresent()) {
            throw new ResourceNotFoundException("Login já cadastrado.");
        }
        if (student.getRegister() == null || student.getRegister().isBlank()) {
            student.setRegister(generateRegister());
        } else if (studentRepository.findByRegister(student.getRegister()).isPresent()) {
            throw new ResourceNotFoundException("Matrícula já cadastrada.");
        }
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        return studentRepository.save(student);
    }

    private String generateRegister() {
        long count = studentRepository.count();
        return "STU-" + java.time.Year.now() + "-" + String.format("%04d", count + 1);
    }

    public List<Student> findAll() {
        return studentRepository.findAll();
    }

    public Student findById(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudante não encontrado."));
    }



    @Transactional
    public Student update(Long id, Student payload) {
        Student currentStudent = findById(id);
        currentStudent.setName(payload.getName());
        currentStudent.setPhoneNumber(payload.getPhoneNumber());
        currentStudent.setLogin(payload.getLogin());
        if (payload.getPassword() != null && !payload.getPassword().isEmpty()) {
            currentStudent.setPassword(passwordEncoder.encode(payload.getPassword()));
        }
        return studentRepository.save(currentStudent);
    }


    /**
     * Não deletaremos nenhum tipo de informação, seja lá qual for.
     * Todos os dados que irão compor o sistema deve ser considerado 
     * 'sensível'.
     * 
     * Métodos de alteração de estado.
     */
    @Transactional
    public void desactivate(Long id) {
        Student currentStudent = findById(id);
        currentStudent.setIsActive(false);
        studentRepository.save(currentStudent);
    }


    @Transactional
    public void reactivate(Long id) {
        Student currentStudent = findById(id);
        if (!currentStudent.getIsActive()) {
            currentStudent.setIsActive(true);
        }
        studentRepository.save(currentStudent);
    }
    // A partir daqui irei colocar
    // os requisitos específicos do projeto


    /**
     * @param id
     * @return Se existente, retorna um dos usuários por meio de seu Login.
     */
    @Transactional
    public Optional<Student> findByLogin(String login) {
        return studentRepository.findByLogin(login);
    }
}
