package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.models.Student;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final String STUDENT = "student";
    private static final String PROFESSOR = "professor";

    private final ProfessorService professorService;
    private final StudentService studentService;

    public void registerUser(String fullName, String email, String password, String confirmPassword, String userType) {
        if (!password.equals(confirmPassword)) {
            // Em um cenário real, seria melhor lançar uma exceção e tratá-la no controller
            // para exibir uma mensagem de erro ao usuário.
            throw new IllegalArgumentException("As senhas não conferem.");
        }

        switch (userType) {
            case STUDENT:
                Student student = new Student();
                student.setName(fullName);
                student.setLogin(email);
                student.setPassword(password); // O service irá criptografar
                studentService.create(student);
                break;
            case PROFESSOR:
                Professor professor = new Professor();
                professor.setName(fullName);
                professor.setLogin(email);
                professor.setPassword(password); // O service irá criptografar
                professorService.create(professor);
                break;
            default:
                throw new IllegalArgumentException("Tipo de usuário inválido: " + userType);
        }
    }
}