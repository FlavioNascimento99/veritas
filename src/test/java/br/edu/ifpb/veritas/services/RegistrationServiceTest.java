package br.edu.ifpb.veritas.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    private ProfessorService professorService;
    @Mock
    private StudentService studentService;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registrarEstudanteCriaEstudante() {
        registrationService.registerUser("Maria", "maria@test.com", "123456", "123456", "student");

        ArgumentCaptor<br.edu.ifpb.veritas.models.Student> captor =
                ArgumentCaptor.forClass(br.edu.ifpb.veritas.models.Student.class);
        verify(studentService).create(captor.capture());
        verifyNoInteractions(professorService);

        br.edu.ifpb.veritas.models.Student saved = captor.getValue();
        assertEquals("Maria", saved.getName());
        assertEquals("maria@test.com", saved.getLogin());
        assertEquals("123456", saved.getPassword());
    }

    @Test
    void registrarProfessorCriaProfessor() {
        registrationService.registerUser("João", "joao@test.com", "123456", "123456", "professor");

        ArgumentCaptor<br.edu.ifpb.veritas.models.Professor> captor =
                ArgumentCaptor.forClass(br.edu.ifpb.veritas.models.Professor.class);
        verify(professorService).create(captor.capture());
        verifyNoInteractions(studentService);

        br.edu.ifpb.veritas.models.Professor saved = captor.getValue();
        assertEquals("João", saved.getName());
        assertEquals("joao@test.com", saved.getLogin());
    }

    @Test
    void registrarAdminLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () ->
                registrationService.registerUser("Admin", "a@b.com", "123456", "123456", "admin"));

        verifyNoInteractions(studentService);
        verifyNoInteractions(professorService);
    }

    @Test
    void registrarTipoInvalidoLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () ->
                registrationService.registerUser("X", "x@b.com", "123456", "123456", "INVALID"));

        verifyNoInteractions(studentService);
        verifyNoInteractions(professorService);
    }

    @Test
    void senhasNaoConferemLancaExcecao() {
        assertThrows(IllegalArgumentException.class, () ->
                registrationService.registerUser("Maria", "maria@test.com", "123456", "654321", "student"));

        verifyNoInteractions(studentService);
        verifyNoInteractions(professorService);
    }
}
