package jp.readscape.api.interfaces.controller.books.response;

import lombok.Data;

@Data
public class BookSummary {
    private Long id;
    private String title;
    private String author;
    private int price;
    private String category;
    private double rating;
}
