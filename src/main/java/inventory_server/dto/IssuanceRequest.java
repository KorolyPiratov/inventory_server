package inventory_server.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class IssuanceRequest {
    private String fullName;
    private Boolean isIndefinite;
    private LocalDate returnDate;
}