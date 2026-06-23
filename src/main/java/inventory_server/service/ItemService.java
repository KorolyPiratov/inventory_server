package inventory_server.service;

import inventory_server.model.Item;
import inventory_server.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public List<Item> getAll() {
        return itemRepository.findAll();
    }

    public Item getById(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Вещь не найдена"));
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public Item update(Long id, Item updated) {
        Item item = getById(id);
        item.setName(updated.getName());
        item.setCategory(updated.getCategory());
        item.setColorType(updated.getColorType());
        item.setBoxNumber(updated.getBoxNumber());
        item.setQuantity(updated.getQuantity());
        item.setDescription(updated.getDescription());
        item.setSupplyDate(updated.getSupplyDate());
        return itemRepository.save(item);
    }

    public void delete(Long id) {
        itemRepository.deleteById(id);
    }

    public List<Item> search(String name) {
        return itemRepository.findByNameContainingIgnoreCase(name);
    }

    public List<Item> filter(String category, String colorType, String boxNumber) {
        if (category != null) return itemRepository.findByCategory(category);
        if (colorType != null) return itemRepository.findByColorType(colorType);
        if (boxNumber != null) return itemRepository.findByBoxNumber(boxNumber);
        return itemRepository.findAll();
    }
    public void deleteAll() {
        itemRepository.deleteAll();
    }
}