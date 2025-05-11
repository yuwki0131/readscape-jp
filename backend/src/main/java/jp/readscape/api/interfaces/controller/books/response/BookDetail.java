package jp.readscape.api.interfaces.controller.books.response;

import lombok.Data;

@Data
public class BookDetail {
    private Long id;
    private String title;
    private String author;
    private int price;
    private String category;
    private String description;
    private double rating;
    private java.util.List<Review> reviews;
}
