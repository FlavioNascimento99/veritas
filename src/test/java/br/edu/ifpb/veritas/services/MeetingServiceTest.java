package br.edu.ifpb.veritas.services;

import br.edu.ifpb.veritas.enums.MeetingStatus;
import br.edu.ifpb.veritas.models.Collegiate;
import br.edu.ifpb.veritas.models.Meeting;
import br.edu.ifpb.veritas.models.Professor;
import br.edu.ifpb.veritas.repositories.CollegiateRepository;
import br.edu.ifpb.veritas.repositories.MeetingRepository;
import br.edu.ifpb.veritas.repositories.ProcessRepository;
import br.edu.ifpb.veritas.repositories.ProfessorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MeetingServiceTest {

    @Autowired
    private MeetingService meetingService;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private CollegiateRepository collegiateRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private ProcessRepository processRepository;

    @Test
    void testCreateSimpleMeeting() {
        System.out.println("\n=== INICIANDO TESTE DE CRIAÇÃO DE REUNIÃO SIMPLES ===");
        
        // Cria uma reunião vazia apenas para testar o save
        Meeting meeting = new Meeting();
        meeting.setDescription("Teste de Reunião Simples");
        meeting.setCreatedAt(LocalDateTime.now());
        meeting.setScheduledDate(LocalDateTime.now());
        meeting.setStatus(MeetingStatus.DISPONIVEL);
        meeting.setActive(false);
        meeting.setParticipants(new ArrayList<>());
        meeting.setProcesses(new ArrayList<>());
        
        System.out.println("Reunião criada em memória: " + meeting);
        
        try {
            Meeting savedMeeting = meetingService.create(meeting);
            System.out.println("Reunião salva com sucesso! ID: " + savedMeeting.getId());
            
            // Verifica se a reunião foi realmente salva no banco
            List<Meeting> allMeetings = meetingRepository.findAll();
            System.out.println("Total de reuniões no banco: " + allMeetings.size());
            
            assertTrue(savedMeeting.getId() != null, "ID da reunião não pode ser nulo");
            assertNotEquals(0, allMeetings.size(), "Pelo menos uma reunião deve estar no banco");
            
        } catch (Exception e) {
            System.err.println("ERRO ao salvar reunião: " + e.getMessage());
            e.printStackTrace();
            fail("Erro ao salvar reunião: " + e.getMessage());
        }
    }

    @Test
    void testFindAllMeetings() {
        System.out.println("\n=== INICIANDO TESTE DE BUSCA DE TODAS AS REUNIÕES ===");
        
        try {
            List<Meeting> meetings = meetingRepository.findAll();
            System.out.println("Total de reuniões encontradas: " + meetings.size());
            meetings.forEach(m -> System.out.println("  - ID: " + m.getId() + ", Status: " + m.getStatus()));
        } catch (Exception e) {
            System.err.println("ERRO ao buscar reuniões: " + e.getMessage());
            e.printStackTrace();
            fail("Erro ao buscar reuniões: " + e.getMessage());
        }
    }
}
