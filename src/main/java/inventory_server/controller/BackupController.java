package inventory_server.controller;

import inventory_server.model.DeletedBackup;
import inventory_server.service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/backups")
public class BackupController {

    @Autowired private BackupService backupService;

    @GetMapping
    public List<DeletedBackup> getAll() {
        return backupService.getAll();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restore(@PathVariable Long id) {
        try {
            backupService.restore(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        backupService.deleteBackup(id);
        return ResponseEntity.ok().build();
    }
}