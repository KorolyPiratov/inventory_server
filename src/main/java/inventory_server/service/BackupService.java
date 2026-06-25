package inventory_server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import inventory_server.dto.DeletedIssuanceDto;
import inventory_server.dto.DeletedItemDto;
import inventory_server.model.DeletedBackup;
import inventory_server.model.Issuance;
import inventory_server.model.Item;
import inventory_server.repository.DeletedBackupRepository;
import inventory_server.repository.IssuanceRepository;
import inventory_server.repository.ItemRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final DeletedBackupRepository backupRepo;
    private final ItemRepository itemRepository;
    private final IssuanceRepository issuanceRepository;
    private final ObjectMapper objectMapper;

    // ── Сохранение ──────────────────────────────────────────────────

    public void backupItem(Item item) throws Exception {
        DeletedItemDto dto = toItemDto(item);
        save("ITEM", item.getName(), objectMapper.writeValueAsString(dto));
    }

    public void backupItems(List<Item> items) throws Exception {
        List<DeletedItemDto> dtos = items.stream().map(this::toItemDto).toList();
        save("ITEMS_BULK",
                "Очистка склада (" + items.size() + " записей)",
                objectMapper.writeValueAsString(dtos));
    }

    public void backupIssuance(Issuance issuance) throws Exception {
        DeletedIssuanceDto dto = toIssuanceDto(issuance);
        String desc = (issuance.getItem() != null ? issuance.getItem().getName() : issuance.getRestoredItemName())
                + " → " + issuance.getFullName();
        save("ISSUANCE", desc, objectMapper.writeValueAsString(dto));
    }

    public void backupIssuances(List<Issuance> issuances) throws Exception {
        List<DeletedIssuanceDto> dtos = issuances.stream().map(this::toIssuanceDto).toList();
        save("ISSUANCES_BULK",
                "Очистка архива (" + issuances.size() + " записей)",
                objectMapper.writeValueAsString(dtos));
    }

    private void save(String type, String description, String json) {
        DeletedBackup b = new DeletedBackup();
        b.setType(type);
        b.setDescription(description);
        b.setDataJson(json);
        b.setDeletedAt(LocalDateTime.now());
        backupRepo.save(b);
    }

    // ── Восстановление ───────────────────────────────────────────────

    public void restore(Long backupId) throws Exception {
        DeletedBackup b = backupRepo.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Резервная копия не найдена"));

        switch (b.getType()) {
            case "ITEM" -> {
                DeletedItemDto dto = objectMapper.readValue(b.getDataJson(), DeletedItemDto.class);
                itemRepository.save(fromItemDto(dto));
            }
            case "ITEMS_BULK" -> {
                List<DeletedItemDto> dtos = objectMapper.readValue(
                        b.getDataJson(), new TypeReference<>() {});
                itemRepository.saveAll(dtos.stream().map(this::fromItemDto).toList());
            }
            case "ISSUANCE" -> {
                DeletedIssuanceDto dto = objectMapper.readValue(
                        b.getDataJson(), DeletedIssuanceDto.class);
                issuanceRepository.save(fromIssuanceDto(dto));
            }
            case "ISSUANCES_BULK" -> {
                List<DeletedIssuanceDto> dtos = objectMapper.readValue(
                        b.getDataJson(), new TypeReference<>() {});
                issuanceRepository.saveAll(dtos.stream().map(this::fromIssuanceDto).toList());
            }
        }

        backupRepo.delete(b);
    }

    // ── Список и удаление ────────────────────────────────────────────

    public List<DeletedBackup> getAll() {
        return backupRepo.findAllByOrderByDeletedAtDesc();
    }

    public void deleteBackup(Long id) {
        backupRepo.deleteById(id);
    }

    // ── Автоочистка старше 24 часов ──────────────────────────────────

    @Scheduled(fixedDelay = 3_600_000)
    public void cleanOld() {
        backupRepo.deleteByDeletedAtBefore(LocalDateTime.now().minusHours(24));
    }

    // ── Маппинг Item ──────────────────────────────────────────────────

    private DeletedItemDto toItemDto(Item item) {
        DeletedItemDto dto = new DeletedItemDto();
        dto.name        = item.getName();
        dto.category    = item.getCategory();
        dto.colorType   = item.getColorType();   // colorType, не color
        dto.quantity    = item.getQuantity();
        dto.boxNumber   = item.getBoxNumber();   // String, не Integer
        dto.printerName = item.getPrinterName();
        dto.supplyDate  = item.getSupplyDate();
        dto.description = item.getDescription();
        return dto;
    }

    private Item fromItemDto(DeletedItemDto dto) {
        Item item = new Item();
        item.setName(dto.name);
        item.setCategory(dto.category);
        item.setColorType(dto.colorType);
        item.setQuantity(dto.quantity);
        item.setBoxNumber(dto.boxNumber);
        item.setPrinterName(dto.printerName);
        item.setSupplyDate(dto.supplyDate);
        item.setDescription(dto.description);
        return item;
    }

    // ── Маппинг Issuance ──────────────────────────────────────────────

    private DeletedIssuanceDto toIssuanceDto(Issuance issuance) {
        DeletedIssuanceDto dto = new DeletedIssuanceDto();
        dto.itemName     = issuance.getItem() != null
                ? issuance.getItem().getName()
                : issuance.getRestoredItemName();
        dto.fullName     = issuance.getFullName();
        dto.issuedAt     = issuance.getIssuedAt();       // LocalDate
        dto.isIndefinite = issuance.getIsIndefinite();   // Boolean
        dto.returnDate   = issuance.getReturnDate();     // LocalDate
        return dto;
    }

    private Issuance fromIssuanceDto(DeletedIssuanceDto dto) {
        Issuance issuance = new Issuance();
        issuance.setRestoredItemName(dto.itemName);
        issuance.setFullName(dto.fullName);
        issuance.setIssuedAt(dto.issuedAt);
        issuance.setIsIndefinite(dto.isIndefinite);
        issuance.setReturnDate(dto.returnDate);
        // item = null, createdBy = null — историческая запись
        return issuance;
    }
}