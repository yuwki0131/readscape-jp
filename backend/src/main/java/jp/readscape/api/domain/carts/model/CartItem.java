package jp.readscape.api.domain.carts.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cartitems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookId;

    private String title;

    private Integer price;

    private Integer quantity;

}
