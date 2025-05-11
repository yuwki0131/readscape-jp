package jp.readscape.api.domain.books.repository;

import jp.readscape.api.domain.books.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface BookRepository extends JpaRepository<Book, Long> {

    // カスタムクエリ例：
    List<Book> findByTitleContainingOrAuthorContaining(String titleKeyword, String authorKeyword);
    List<Book> findByCategory(String category);

}
