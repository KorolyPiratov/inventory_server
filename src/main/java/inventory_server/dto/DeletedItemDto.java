package inventory_server.dto;

import java.time.LocalDate;

public class DeletedItemDto {
    public String name;
    public String category;
    public String colorType;    // не color
    public Integer quantity;
    public String boxNumber;    // String, не Integer
    public String printerName;
    public LocalDate supplyDate;
    public String description;
}