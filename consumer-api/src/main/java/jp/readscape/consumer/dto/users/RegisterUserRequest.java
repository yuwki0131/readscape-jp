package jp.readscape.consumer.dto.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterUserRequest {

    @NotBlank(message = "ユーザー名は必須です")
    @Size(min = 3, max = 50, message = "ユーザー名は3文字以上50文字以下で入力してください")
    private String username;

    @NotBlank(message = "メールアドレスは必須です")
    @Email(message = "有効なメールアドレスを入力してください")
    private String email;

    @NotBlank(message = "パスワードは必須です")
    @Size(min = 8, message = "パスワードは8文字以上で入力してください")
    private String password;

    @Size(max = 50, message = "姓は50文字以下で入力してください")
    private String firstName;

    @Size(max = 50, message = "名は50文字以下で入力してください")
    private String lastName;

    @Size(max = 20, message = "電話番号は20文字以下で入力してください")
    private String phone;

    @Size(max = 200, message = "住所は200文字以下で入力してください")
    private String address;
}