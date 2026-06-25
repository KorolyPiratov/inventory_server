package inventory_server.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "issuances")
public class Issuance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @ManyToOne(optional = true)
    @JoinColumn(name = "item_id", nullable = true)
    private Item item;

    @Column(name = "restored_item_name")
    private String restoredItemName;
    public String getRestoredItemName() { return restoredItemName; }
    public void setRestoredItemName(String restoredItemName) { this.restoredItemName = restoredItemName; }

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "issued_at")
    private LocalDate issuedAt = LocalDate.now();

    @Column(name = "is_indefinite")
    private Boolean isIndefinite = false;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

}