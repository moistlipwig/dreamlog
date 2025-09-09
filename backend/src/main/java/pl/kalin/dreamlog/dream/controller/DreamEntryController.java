package pl.kalin.dreamlog.dream.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/dreams")
@RequiredArgsConstructor
public class DreamEntryController {

    private final DreamEntryRepository repository;

    @GetMapping
    public List<DreamEntry> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DreamEntry> byId(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public DreamEntry create(@Valid @RequestBody DreamEntry dream) {
        return repository.save(dream);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DreamEntry> update(@PathVariable UUID id, @Valid @RequestBody DreamEntry dream) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        dream.setId(id);
        return ResponseEntity.ok(repository.save(dream));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
