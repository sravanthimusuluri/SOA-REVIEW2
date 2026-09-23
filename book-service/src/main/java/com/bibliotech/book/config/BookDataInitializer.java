package com.bibliotech.book.config;

import com.bibliotech.book.entity.Book;
import com.bibliotech.book.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(BookDataInitializer.class);

    private final BookRepository bookRepository;

    public BookDataInitializer(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() == 0) {
            logger.info("Seeding academic book catalog into Book database...");

            List<Book> initialBooks = List.of(
                new Book("Building Microservices: Designing Fine-Grained Systems", "Sam Newman", "978-1492034025", "Cloud Architecture", 5, 5, "Main Campus Library"),
                new Book("Designing Data-Intensive Applications", "Martin Kleppmann", "978-1449373320", "Distributed Systems", 4, 4, "Engineering Block Branch"),
                new Book("Clean Architecture: A Craftsman's Guide", "Robert C. Martin", "978-0134494166", "Software Engineering", 6, 6, "Tech Center Library"),
                new Book("Cloud Native Patterns: Designing High-Availability Systems", "Cornelia Davis", "978-1617294297", "Cloud Computing", 3, 3, "Main Campus Library"),
                new Book("Introduction to Algorithms (4th Edition)", "Thomas H. Cormen", "978-0262046305", "Computer Science", 8, 8, "Main Campus Library"),
                new Book("Spring Boot in Action", "Craig Walls", "978-1617292545", "Web Development", 5, 5, "Engineering Block Branch"),
                new Book("Artificial Intelligence: A Modern Approach", "Stuart Russell", "978-0134610993", "Artificial Intelligence", 4, 4, "Tech Center Library"),
                new Book("Kafka: The Definitive Guide", "Gwen Shapira", "978-1492043089", "Distributed Systems", 3, 3, "Engineering Block Branch")
            );

            bookRepository.saveAll(initialBooks);
            logger.info("Successfully seeded {} academic books across campus branches.", initialBooks.size());
        }
    }
}
