package jp.readscape.consumer.controllers.reviews;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.reviews.ReviewSummary;
import jp.readscape.consumer.services.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/books/reviews")
@RequiredArgsConstructor
@Tag(name = "Global Reviews", description = "グローバルレビューAPI")
public class ReviewsGlobalController {

    private final ReviewService reviewService;

    @Operation(
        summary = "ユーザーのレビュー一覧取得",
        description = "認証済みユーザーが投稿したすべてのレビュー一覧を取得します。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "ユーザーレビュー一覧取得成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です")
    })
    @GetMapping("/my-reviews")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<List<ReviewSummary>> getMyReviews(Authentication auth) {
        log.info("GET /api/books/reviews/my-reviews - user: {}", auth.getName());

        User user = (User) auth.getPrincipal();
        List<ReviewSummary> reviews = reviewService.getUserReviews(user.getId());

        return ResponseEntity.ok(reviews);
    }
}