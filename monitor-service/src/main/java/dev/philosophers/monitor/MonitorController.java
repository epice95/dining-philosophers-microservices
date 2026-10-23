package dev.philosophers.monitor;

import dev.philosophers.common.event.PhilosopherEvent;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitorController {

    private static final Logger LOG = LoggerFactory.getLogger(MonitorController.class);

    private final MonitorState state;

    public MonitorController(MonitorState state) {
        this.state = state;
    }

    /** 201 : nouvel événement, 200 : rejeu idempotent, 409 : conflit, 400 : événement invalide. */
    @PostMapping("/events")
    public ResponseEntity<Void> receive(@RequestBody PhilosopherEvent event) {
        try {
            RecordResult result = state.record(event);
            if (result == RecordResult.ACCEPTED) {
                return ResponseEntity.status(HttpStatus.CREATED).build();
            }
            return ResponseEntity.ok().build();
        } catch (ConflictingEventException e) {
            LOG.warn("Conflicting event rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/api/state")
    public List<PhilosopherView> state() {
        return state.snapshot();
    }

    @GetMapping("/api/philosophers/{id}/history")
    public ResponseEntity<List<HistoryEntry>> history(@PathVariable("id") String id) {
        return state.history(id)
                .map(events -> events.stream().map(HistoryEntry::from).toList())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
