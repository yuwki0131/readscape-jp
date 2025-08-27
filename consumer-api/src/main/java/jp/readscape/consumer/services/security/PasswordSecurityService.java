package jp.readscape.consumer.services.security;

import jp.readscape.consumer.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordSecurityService {

    private final PasswordEncoder passwordEncoder;

    // 定数をSecurityConstantsから取得
    private static final int MIN_PASSWORD_LENGTH = SecurityConstants.MIN_PASSWORD_LENGTH;
    private static final int MAX_PASSWORD_LENGTH = SecurityConstants.MAX_PASSWORD_LENGTH;
    private static final int MIN_PASSWORD_CRITERIA = SecurityConstants.MIN_PASSWORD_CRITERIA;

    // パスワード強度チェック用正規表現
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

    /**
     * パスワードの強度をチェックします
     * 
     * @param password チェック対象のパスワード
     * @return パスワードが有効な場合はtrue
     */
    public boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            log.debug("Password is null or empty");
            return false;
        }

        // 長さチェック
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            log.debug("Password length is invalid: {}", password.length());
            return false;
        }

        // 強度チェック
        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).matches();
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).matches();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).matches();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        // 最低MIN_PASSWORD_CRITERIAの条件を満たす必要がある
        int criteriaCount = 0;
        if (hasLowercase) criteriaCount++;
        if (hasUppercase) criteriaCount++;
        if (hasDigit) criteriaCount++;
        if (hasSpecialChar) criteriaCount++;

        boolean isValid = criteriaCount >= MIN_PASSWORD_CRITERIA;
        
        if (!isValid) {
            log.debug("Password does not meet strength requirements. Criteria met: {}/4", criteriaCount);
        }

        return isValid;
    }

    /**
     * パスワード強度チェックの詳細結果を取得します
     */
    public PasswordStrengthResult checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrengthResult(false, "パスワードが入力されていません");
        }

        if (password.length() < MIN_PASSWORD_LENGTH) {
            return new PasswordStrengthResult(false, 
                String.format("パスワードは%d文字以上である必要があります", MIN_PASSWORD_LENGTH));
        }

        if (password.length() > MAX_PASSWORD_LENGTH) {
            return new PasswordStrengthResult(false, 
                String.format("パスワードは%d文字以下である必要があります", MAX_PASSWORD_LENGTH));
        }

        boolean hasLowercase = LOWERCASE_PATTERN.matcher(password).matches();
        boolean hasUppercase = UPPERCASE_PATTERN.matcher(password).matches();
        boolean hasDigit = DIGIT_PATTERN.matcher(password).matches();
        boolean hasSpecialChar = SPECIAL_CHAR_PATTERN.matcher(password).matches();

        int criteriaCount = 0;
        if (hasLowercase) criteriaCount++;
        if (hasUppercase) criteriaCount++;
        if (hasDigit) criteriaCount++;
        if (hasSpecialChar) criteriaCount++;

        if (criteriaCount < MIN_PASSWORD_CRITERIA) {
            StringBuilder message = new StringBuilder("パスワードには以下の条件のうち" + MIN_PASSWORD_CRITERIA + "つ以上を満たす必要があります:\n");
            message.append("・小文字 (a-z)").append(hasLowercase ? " ✓" : "").append("\n");
            message.append("・大文字 (A-Z)").append(hasUppercase ? " ✓" : "").append("\n");
            message.append("・数字 (0-9)").append(hasDigit ? " ✓" : "").append("\n");
            message.append("・特殊文字 (!@#$%^&*等)").append(hasSpecialChar ? " ✓" : "");
            
            return new PasswordStrengthResult(false, message.toString());
        }

        return new PasswordStrengthResult(true, "パスワード強度は適切です");
    }

    /**
     * パスワードを暗号化します
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * パスワードが一致するかチェックします
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * よくあるパスワードかどうかをチェックします
     */
    public boolean isCommonPassword(String password) {
        if (password == null) return false;
        
        // よくあるパスワードのリスト（実際の運用では外部ファイルから読み込むべき）
        String[] commonPasswords = {
            "12345678", "password", "123456789", "12345678", "qwerty",
            "abc123", "password123", "admin", "letmein", "welcome",
            "monkey", "dragon", "password1", "123123", "sunshine"
        };

        String lowerPassword = password.toLowerCase();
        for (String common : commonPasswords) {
            if (lowerPassword.equals(common)) {
                return true;
            }
        }

        return false;
    }

    /**
     * パスワード強度チェック結果を表すクラス
     */
    public static class PasswordStrengthResult {
        private final boolean valid;
        private final String message;

        public PasswordStrengthResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}