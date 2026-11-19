package com.example.librarymanagement.service;

import com.example.librarymanagement.model.Book;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class BookService {

    private final Map<Integer, Book> bookStore = new LinkedHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public BookService() {
        addBook(new Book(1, "Clean Code", "Robert C. Martin", "Programming", 5));
        addBook(new Book(2, "Database System Concepts", "Abraham Silberschatz", "Database", 3));
        idGenerator.set(3);
    }

    public Book addBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book payload is required.");
        }
        if (book.getTitle() == null || book.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Book title cannot be empty.");
        }
        if (book.getAuthor() == null || book.getAuthor().trim().isEmpty()) {
            throw new IllegalArgumentException("Book author cannot be empty.");
        }
        if (book.getCategory() == null || book.getCategory().trim().isEmpty()) {
            throw new IllegalArgumentException("Book category cannot be empty.");
        }
        if (book.getAvailableCopies() == null || book.getAvailableCopies() < 0) {
            throw new IllegalArgumentException("Available copies must be 0 or greater.");
        }

        Integer assignedId = book.getId();
        if (assignedId == null || assignedId <= 0) {
            assignedId = idGenerator.getAndIncrement();
        } else if (bookStore.containsKey(assignedId)) {
            throw new IllegalArgumentException("Book with ID " + assignedId + " already exists.");
        } else if (assignedId >= idGenerator.get()) {
            idGenerator.set(assignedId + 1);
        }

        Book saved = new Book(
                assignedId,
                book.getTitle().trim(),
                book.getAuthor().trim(),
                book.getCategory().trim(),
                book.getAvailableCopies()
        );
        bookStore.put(assignedId, saved);
        return saved;
    }

    public List<Book> getAllBooks() {
        return new ArrayList<>(bookStore.values());
    }

    public Book getBookById(Integer id) {
        if (id == null) {
            return null;
        }
        return bookStore.get(id);
    }

    public Book updateBook(Integer id, Book updatedBook) {
        Book existing = bookStore.get(id);
        if (existing == null) {
            return null;
        }
        if (updatedBook == null) {
            throw new IllegalArgumentException("Updated book details are required.");
        }
        if (updatedBook.getTitle() != null && !updatedBook.getTitle().trim().isEmpty()) {
            existing.setTitle(updatedBook.getTitle().trim());
        }
        if (updatedBook.getAuthor() != null && !updatedBook.getAuthor().trim().isEmpty()) {
            existing.setAuthor(updatedBook.getAuthor().trim());
        }
        if (updatedBook.getCategory() != null && !updatedBook.getCategory().trim().isEmpty()) {
            existing.setCategory(updatedBook.getCategory().trim());
        }
        if (updatedBook.getAvailableCopies() != null) {
            if (updatedBook.getAvailableCopies() < 0) {
                throw new IllegalArgumentException("Available copies must be 0 or greater.");
            }
            existing.setAvailableCopies(updatedBook.getAvailableCopies());
        }
        return existing;
    }

    public boolean deleteBook(Integer id) {
        if (id == null) {
            return false;
        }
        return bookStore.remove(id) != null;
    }
}