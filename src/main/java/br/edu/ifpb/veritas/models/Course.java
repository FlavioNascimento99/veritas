package br.edu.ifpb.veritas.models;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "TB_COURSES")
@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; 

    @NotBlank(message = "Título do curso é obrigatório")
    @Column(name = "TB_COURSES_TITLE")
    private String title;
    
    @Column(name = "TB_COURSES_DESCRIPTION")
    private String description;

    @Column(name = "TB_COURSES_ACTIVE")
    private Boolean active;
    
    @OneToMany(mappedBy = "currentCourse", cascade = CascadeType.ALL)
    private List<Student> students; 
    
    @OneToOne
    @JoinColumn(name = "TB_COURSE_COORDINATOR")
    private Professor courseCoordinator;
}
