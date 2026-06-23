package inventory_server.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String category;

    @Column(name = "color_type")
    private String colorType;

    @Column(name = "box_number")
    private String boxNumber;

    private Integer quantity = 0;

    private String description;

    @Column(name = "supply_date")
    private LocalDate supplyDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}