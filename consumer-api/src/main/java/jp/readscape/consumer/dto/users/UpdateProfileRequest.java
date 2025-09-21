package jp.readscape.consumer.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "ユーザー名は3-50文字で入力してください")
    private String username;

    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    @Size(max = 50, message = "姓は50文字以下で入力してください")
    private String firstName;

    @Size(max = 50, message = "名は50文字以下で入力してください")
    private String lastName;

    @Size(max = 20, message = "電話番号は20文字以下で入力してください")
    private String phone;

    @Size(max = 200, message = "住所は200文字以下で入力してください")
    private String address;
}