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

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

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