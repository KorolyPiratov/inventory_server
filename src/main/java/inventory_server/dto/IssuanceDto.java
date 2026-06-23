package inventory_server.dto;

import java.time.LocalDate;

public class IssuanceDto {
    private Long id;
    private String itemName;
    private String fullName;
    private LocalDate issuedAt;
    private Boolean isIndefinite;
    private LocalDate returnDate;

    public IssuanceDto(Long id, String itemName, String fullName,
                       LocalDate issuedAt, Boolean isIndefinite, LocalDate returnDate) {
        this.id = id;
        this.itemName = itemName;
        this.fullName = fullName;
        this.issuedAt = issuedAt;
        this.isIndefinite = isIndefinite;
        this.returnDate = returnDate;
    }

    public LocalDate getReturnDate() { return returnDate; }

    public IssuanceDto(Long id, String itemName, String fullName,
                       LocalDate issuedAt, Boolean isIndefinite) {
        this.id = id;
        this.itemName = itemName;
        this.fullName = fullName;
        this.issuedAt = issuedAt;
        this.isIndefinite = isIndefinite;
    }


    public Long getId() { return id; }
    public String getItemName() { return itemName; }
    public String getFullName() { return fullName; }
    public LocalDate getIssuedAt() { return issuedAt; }
    public Boolean getIsIndefinite() { return isIndefinite; }

}