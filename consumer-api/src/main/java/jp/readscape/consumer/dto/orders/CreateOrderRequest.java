package jp.readscape.consumer.dto.orders;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "配送先住所は必須です")
    @Size(max = 500, message = "配送先住所は500文字以下で入力してください")
    private String shippingAddress;

    @Size(max = 20, message = "配送先電話番号は20文字以下で入力してください")
    private String shippingPhone;

    @Pattern(regexp = "^(credit_card|debit_card|bank_transfer|cash_on_delivery)$",
             message = "支払い方法は credit_card, debit_card, bank_transfer, cash_on_delivery のいずれかである必要があります")
    @Size(max = 50, message = "支払い方法は50文字以下で入力してください")
    private String paymentMethod;

    @Size(max = 1000, message = "備考は1000文字以下で入力してください")
    private String notes;
}