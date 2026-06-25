package inventory_server.repository;

import inventory_server.model.Issuance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface IssuanceRepository extends JpaRepository<Issuance, Long> {
    List<Issuance> findByItemId(Long itemId);
    List<Issuance> findByIssuedAtBetween(LocalDate from, LocalDate to);
    @Transactional
    @Modifying
    @Query("DELETE FROM Issuance i WHERE i.issuedAt >= :from AND i.issuedAt <= :to")
    void deleteByIssuedAtBetween(LocalDate from, LocalDate to);
}