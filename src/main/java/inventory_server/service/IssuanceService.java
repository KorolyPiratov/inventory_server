package inventory_server.service;

import inventory_server.dto.IssuanceDto;
import inventory_server.model.Issuance;
import inventory_server.model.Item;
import inventory_server.model.User;
import inventory_server.repository.IssuanceRepository;
import inventory_server.repository.ItemRepository;
import inventory_server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuanceService {

    private final IssuanceRepository issuanceRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;

    public List<Issuance> getByItemId(Long itemId) {
        return issuanceRepository.findByItemId(itemId);
    }

    public Issuance issue(Long itemId, String fullName,
                          Boolean isIndefinite, String username) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Вещь не найдена"));

        if (item.getQuantity() <= 0) {
            throw new RuntimeException("Вещь закончилась на складе");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        item.setQuantity(item.getQuantity() - 1);
        itemRepository.save(item);

        Issuance issuance = new Issuance();
        issuance.setItem(item);
        issuance.setFullName(fullName);
        issuance.setIsIndefinite(isIndefinite);
        issuance.setCreatedBy(user);

        return issuanceRepository.save(issuance);
    }

    public List<IssuanceDto> getAll() {
        return issuanceRepository.findAll().stream()
                .map(i -> new IssuanceDto(
                        i.getId(),
                        i.getItem() != null ? i.getItem().getName() : "—",
                        i.getFullName(),
                        i.getIssuedAt(),
                        i.getIsIndefinite(),
                        i.getReturnDate()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    public Issuance issue(Long itemId, String fullName,
                          Boolean isIndefinite, LocalDate returnDate, String username) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Вещь не найдена"));

        if (item.getQuantity() <= 0) {
            throw new RuntimeException("Вещь закончилась на складе");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        item.setQuantity(item.getQuantity() - 1);
        itemRepository.save(item);

        Issuance issuance = new Issuance();
        issuance.setItem(item);
        issuance.setFullName(fullName);
        issuance.setIsIndefinite(isIndefinite);
        issuance.setReturnDate(returnDate);
        issuance.setCreatedBy(user);

        return issuanceRepository.save(issuance);
    }
}