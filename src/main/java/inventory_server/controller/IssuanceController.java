package inventory_server.controller;

import inventory_server.dto.IssuanceDto;
import inventory_server.dto.IssuanceRequest;
import inventory_server.model.Issuance;
import inventory_server.service.IssuanceService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/issuances")
@RequiredArgsConstructor
public class IssuanceController {

    private final IssuanceService issuanceService;

    @GetMapping("/item/{itemId}")
    public ResponseEntity<List<Issuance>> getByItem(@PathVariable Long itemId) {
        return ResponseEntity.ok(issuanceService.getByItemId(itemId));
    }
    @GetMapping
    public ResponseEntity<List<IssuanceDto>> getAll() {
        return ResponseEntity.ok(issuanceService.getAll());
    }

    @PostMapping("/item/{itemId}")
    public ResponseEntity<Issuance> issue(
            @PathVariable Long itemId,
            @RequestBody IssuanceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Issuance issuance = issuanceService.issue(
                itemId,
                request.getFullName(),
                request.getIsIndefinite(),
                request.getReturnDate(),
                userDetails.getUsername()
        );
        return ResponseEntity.ok(issuance);
    }

    @SneakyThrows
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<Void> deleteByItem(@PathVariable Long itemId) {
        issuanceService.deleteByItemId(itemId);
        return ResponseEntity.noContent().build();
    }

    @SneakyThrows
    @DeleteMapping("/between")
    public ResponseEntity<Void> deleteBetween(
            @RequestParam String from,
            @RequestParam String to) {
        issuanceService.deleteBetween(LocalDate.parse(from), LocalDate.parse(to));
        return ResponseEntity.noContent().build();
    }
    @SneakyThrows
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        issuanceService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}