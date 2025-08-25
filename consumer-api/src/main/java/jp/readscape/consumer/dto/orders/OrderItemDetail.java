package jp.readscape.consumer.dto.orders;

import jp.readscape.consumer.domain.orders.model.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDetail {

    private Long id;
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String bookIsbn;
    private Integer quantity;
    private Integer unitPrice;
    private BigDecimal subtotal;

    public static OrderItemDetail from(OrderItem orderItem) {
        return OrderItemDetail.builder()
                .id(orderItem.getId())
                .bookId(orderItem.getBook() != null ? orderItem.getBook().getId() : null)
                .bookTitle(orderItem.getBookTitle())
                .bookAuthor(orderItem.getBookAuthor())
                .bookIsbn(orderItem.getBookIsbn())
                .quantity(orderItem.getQuantity())
                .unitPrice(orderItem.getUnitPrice())
                .subtotal(orderItem.getSubtotal())
                .build();
    }

    public String getFormattedUnitPrice() {
        return unitPrice != null ? String.format("¥%,d", unitPrice) : "¥0";
    }

    public String getFormattedSubtotal() {
        return subtotal != null ? String.format("¥%,.0f", subtotal) : "¥0";
    }

    public String getBookDisplayName() {
        StringBuilder sb = new StringBuilder();
        if (bookTitle != null) {
            sb.append(bookTitle);
        }
        if (bookAuthor != null) {
            sb.append(" - ").append(bookAuthor);
        }
        return sb.toString();
    }
}