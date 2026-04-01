package br.edu.ifpb.veritas.controllers.api;

import br.edu.ifpb.veritas.models.Administrator;
import br.edu.ifpb.veritas.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminAPIController {

    private final AdminService adminService;

    @PostMapping
    public ResponseEntity<Administrator> create(@Valid @RequestBody Administrator admin) {
        adminService.create(admin);
        return ResponseEntity.ok(admin);
    }

    @GetMapping
    public ResponseEntity<List<Administrator>> findAll() {
        return ResponseEntity.ok(adminService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Administrator> findById(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.find(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Administrator> update(@PathVariable Long id, @RequestBody Administrator admin) {
        return ResponseEntity.ok(adminService.update(id, admin));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        adminService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // A partir daqui iremos colocar
    // os requisitos específicos do projeto

}
