package jp.readscape.api.interfaces.controller.books.reviews;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books/{id}/reviews")
public class ReviewsController {

    @GetMapping
    public String getReviews() {
        return "Hello, Reviews!";
    }
}
