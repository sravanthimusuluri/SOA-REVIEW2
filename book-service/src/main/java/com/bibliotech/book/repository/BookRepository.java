package com.bibliotech.book.repository;

import com.bibliotech.book.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findByIsbn(String isbn);
    boolean existsByIsbn(String isbn);
    List<Book> findByCategoryIgnoreCase(String category);
    List<Book> findByBranchIgnoreCase(String branch);

    @Query("SELECT b FROM Book b WHERE " +
           "(:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(b.isbn) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:category IS NULL OR LOWER(b.category) = LOWER(:category)) " +
           "AND (:branch IS NULL OR LOWER(b.branch) = LOWER(:branch))")
    List<Book> searchBooks(@Param("keyword") String keyword, 
                           @Param("category") String category, 
                           @Param("branch") String branch);
}
