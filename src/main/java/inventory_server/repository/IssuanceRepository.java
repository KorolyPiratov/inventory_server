package inventory_server.repository;

import inventory_server.model.Issuance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssuanceRepository extends JpaRepository<Issuance, Long> {
    List<Issuance> findByItemId(Long itemId);
}