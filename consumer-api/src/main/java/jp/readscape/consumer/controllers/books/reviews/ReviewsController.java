package jp.readscape.consumer.controllers.books.reviews;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jp.readscape.consumer.constants.SortConstants;
import jp.readscape.consumer.domain.users.model.User;
import jp.readscape.consumer.dto.reviews.BookReviewsResponse;
import jp.readscape.consumer.dto.reviews.PostReviewRequest;
import jp.readscape.consumer.dto.reviews.ReviewResponse;
import jp.readscape.consumer.dto.reviews.ReviewSummary;
import jp.readscape.consumer.services.ReviewService;
import jp.readscape.consumer.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/books/{bookId}/reviews")
@RequiredArgsConstructor
@Tag(name = "Book Reviews", description = "書籍レビューAPI")
public class ReviewsController {

    private final ReviewService reviewService;

    @Operation(
        summary = "書籍レビュー一覧取得",
        description = "指定された書籍のレビュー一覧を取得します。ページング、ソート、フィルタリングが可能です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "レビュー一覧取得成功"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません")
    })
    @GetMapping
    public ResponseEntity<BookReviewsResponse> getReviews(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId,
            
            @Parameter(description = "ページ番号（0から開始）", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "ページサイズ", example = "10")
            @RequestParam(defaultValue = "10") Integer size,
            
            @Parameter(description = "ソート条件", example = "newest",
                schema = @Schema(allowableValues = {"newest", "helpful", "positive", "negative", "verified"}))
            @RequestParam(defaultValue = "newest") String sortBy
    ) {
        log.info("GET /api/books/{}/reviews - page: {}, size: {}, sortBy: {}", bookId, page, size, sortBy);

        // パラメータバリデーション
        if (page < 0) {
            throw new IllegalArgumentException("ページ番号は0以上である必要があります");
        }
        if (size <= 0 || size > 50) {
            throw new IllegalArgumentException("ページサイズは1以上50以下である必要があります");
        }

        try {
            BookReviewsResponse response = reviewService.getBookReviews(bookId, page, size, sortBy);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Book not found: {}", bookId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "レビュー投稿",
        description = "指定された書籍にレビューを投稿します。購入履歴のある書籍のみ投稿可能で、1書籍につき1レビューまでです。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "レビュー投稿成功"),
        @ApiResponse(responseCode = "400", description = "バリデーションエラー",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません"),
        @ApiResponse(responseCode = "409", description = "ビジネスルールエラー（重複投稿、購入履歴なし等）",
            content = @Content(schema = @Schema(implementation = jp.readscape.consumer.dto.ApiResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<?> postReview(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId,
            
            @Valid @RequestBody PostReviewRequest request,
            Authentication auth
    ) {
        log.info("POST /api/books/{}/reviews - user: {}, rating: {}", bookId, auth.getName(), request.getRating());

        try {
            User user = (User) auth.getPrincipal();
            ReviewResponse response = reviewService.postReview(bookId, user.getId(), request);
            
            log.info("Review posted successfully: {} for book: {} by user: {}", 
                response.getId(), bookId, user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Book not found for review: {}", bookId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Review posting failed for book {} by user {}: {}", bookId, auth.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "レビュー検索",
        description = "指定された書籍のレビューをキーワード検索します。レビュータイトルとコメントが検索対象です。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "レビュー検索成功"),
        @ApiResponse(responseCode = "404", description = "書籍が見つかりません")
    })
    @GetMapping("/search")
    public ResponseEntity<BookReviewsResponse> searchReviews(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId,
            
            @Parameter(description = "検索キーワード", example = "面白い", required = true)
            @RequestParam String keyword,
            
            @Parameter(description = "ページ番号（0から開始）", example = "0")
            @RequestParam(defaultValue = "0") Integer page,
            
            @Parameter(description = "ページサイズ", example = "10")
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("GET /api/books/{}/reviews/search - keyword: {}", bookId, keyword);

        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("検索キーワードは必須です");
        }

        try {
            BookReviewsResponse response = reviewService.searchReviews(bookId, keyword.trim(), page, size);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Book not found for review search: {}", bookId);
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "レビューに「役立った」を追加",
        description = "指定されたレビューに「役立った」カウントを追加します。"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "「役立った」追加成功"),
        @ApiResponse(responseCode = "404", description = "レビューが見つかりません")
    })
    @PostMapping("/{reviewId}/helpful")
    public ResponseEntity<?> markReviewAsHelpful(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId,
            
            @Parameter(description = "レビューID", example = "1", required = true)
            @PathVariable Long reviewId
    ) {
        log.info("POST /api/books/{}/reviews/{}/helpful", bookId, reviewId);

        try {
            reviewService.markReviewAsHelpful(reviewId);
            return ResponseEntity.ok(jp.readscape.consumer.dto.ApiResponse.success("「役立った」を追加しました"));
        } catch (IllegalArgumentException e) {
            log.warn("Review not found for helpful marking: {}", reviewId);
            return ResponseEntity.notFound().build();
        }
    }

    // レビュー管理用のエンドポイント（認証済みユーザー用）

    @Operation(
        summary = "レビュー更新",
        description = "自分が投稿したレビューを更新します。投稿者本人のみ実行可能です。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "レビュー更新成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "他人のレビューは更新できません"),
        @ApiResponse(responseCode = "404", description = "レビューが見つかりません")
    })
    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<?> updateReview(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId,
            
            @Parameter(description = "レビューID", example = "1", required = true)
            @PathVariable Long reviewId,
            
            @Valid @RequestBody PostReviewRequest request,
            Authentication auth
    ) {
        log.info("PUT /api/books/{}/reviews/{} - user: {}", bookId, reviewId, auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            ReviewResponse response = reviewService.updateReview(reviewId, user.getId(), request);
            
            log.info("Review updated successfully: {} by user: {}", reviewId, user.getUsername());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Review not found for update: {}", reviewId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Review update failed for review {} by user {}: {}", reviewId, auth.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(
        summary = "レビュー削除",
        description = "自分が投稿したレビューを削除します。投稿者本人のみ実行可能です。",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "レビュー削除成功"),
        @ApiResponse(responseCode = "401", description = "認証が必要です"),
        @ApiResponse(responseCode = "403", description = "他人のレビューは削除できません"),
        @ApiResponse(responseCode = "404", description = "レビューが見つかりません")
    })
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CONSUMER') or hasRole('ADMIN')")
    public ResponseEntity<?> deleteReview(
            @Parameter(description = "書籍ID", example = "1", required = true)
            @PathVariable Long bookId,
            
            @Parameter(description = "レビューID", example = "1", required = true)
            @PathVariable Long reviewId,
            
            Authentication auth
    ) {
        log.info("DELETE /api/books/{}/reviews/{} - user: {}", bookId, reviewId, auth.getName());

        try {
            User user = (User) auth.getPrincipal();
            reviewService.deleteReview(reviewId, user.getId());
            
            log.info("Review deleted successfully: {} by user: {}", reviewId, user.getUsername());
            return ResponseEntity.ok(jp.readscape.consumer.dto.ApiResponse.success("レビューを削除しました"));
            
        } catch (IllegalArgumentException e) {
            log.warn("Review not found for deletion: {}", reviewId);
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.warn("Review deletion failed for review {} by user {}: {}", reviewId, auth.getName(), e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(jp.readscape.consumer.dto.ApiResponse.error(e.getMessage()));
        }
    }

    // 独立したユーザーレビュー取得エンドポイント
    @Operation(
        summary = "ユーザーのレビュー一覧取得",
        description = "認証済みユーザーが投稿したレビュー一覧を取得します。",
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