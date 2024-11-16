package jp.readscape.backend.controller.books;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BooksController {

    @GetMapping
    public String getBooks() {
        return "Hello, Books!";
    }
}
