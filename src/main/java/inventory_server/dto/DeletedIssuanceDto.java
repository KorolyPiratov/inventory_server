package inventory_server.dto;

import java.time.LocalDate;

public class DeletedIssuanceDto {
    public String itemName;
    public String fullName;
    public LocalDate issuedAt;
    public Boolean isIndefinite;
    public LocalDate returnDate;
}