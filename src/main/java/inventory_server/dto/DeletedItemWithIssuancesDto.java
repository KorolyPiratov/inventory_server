package inventory_server.dto;

import java.util.List;

public class DeletedItemWithIssuancesDto {
    public DeletedItemDto item;
    public List<DeletedIssuanceDto> issuances;
}