package jp.readscape.api.interfaces.controller.books.reviews.request;

import lombok.Data;

@Data
public class PostReviewRequest {
    private int rating;
    private String comment;
}
