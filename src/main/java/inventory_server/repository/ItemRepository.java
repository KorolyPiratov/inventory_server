package inventory_server.repository;

import inventory_server.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByNameContainingIgnoreCase(String name);

    List<Item> findByCategory(String category);

    List<Item> findByColorType(String colorType);

    List<Item> findByBoxNumber(String boxNumber);
}