package inventory_server.controller;

import inventory_server.model.Item;
import inventory_server.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAll() {
        return ResponseEntity.ok(itemService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(@PathVariable Long id) {
        return ResponseEntity.ok(itemService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Item> create(@RequestBody Item item) {
        return ResponseEntity.ok(itemService.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> update(@PathVariable Long id, @RequestBody Item item) {
        return ResponseEntity.ok(itemService.update(id, item));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Item>> search(@RequestParam String name) {
        return ResponseEntity.ok(itemService.search(name));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Item>> filter(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String colorType,
            @RequestParam(required = false) String boxNumber) {
        return ResponseEntity.ok(itemService.filter(category, colorType, boxNumber));
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAll() {
        try {
            itemService.deleteAll();
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteOne(@PathVariable Long id) {
        try {
            itemService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}