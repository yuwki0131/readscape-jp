package jp.readscape.api.domain.orders.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "orderitems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long bookId;

    private String title;

    private Integer price;

    private Integer quantity;

}
