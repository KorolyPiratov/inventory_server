package inventory_server.repository;

import inventory_server.model.DeletedBackup;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface DeletedBackupRepository extends JpaRepository<DeletedBackup, Long> {
    void deleteByDeletedAtBefore(LocalDateTime cutoff);
    List<DeletedBackup> findAllByOrderByDeletedAtDesc();
}