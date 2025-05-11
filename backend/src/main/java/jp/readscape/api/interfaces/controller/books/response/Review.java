package jp.readscape.api.interfaces.controller.books.response;

import lombok.Data;

@Data
public class Review {
    private Long id;
    private String user;
    private int rating;
    private String comment;
}
