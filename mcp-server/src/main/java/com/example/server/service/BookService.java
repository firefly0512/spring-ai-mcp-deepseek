package com.example.server.service;

import com.example.server.entity.Book;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class BookService {

    @Tool(name = "findBooksByTitle", description = "根据书名模糊查询图书，支持部分标题匹配")
    public List<Book> findBooksByTitle(@ToolParam(description = "书名关键词") String title) {
        return findByTitleContaining(title);
    }

    @Tool(name = "findBooksByAuthor", description = "根据作者精确查询图书")
    public List<Book> findBooksByAuthor(@ToolParam(description = "作者姓名") String author) {
        return findByAuthor(author);
    }

    @Tool(name = "findBooksByCategory", description = "根据图书分类精确查询图书")
    public List<Book> findBooksByCategory(@ToolParam(description = "图书分类") String category) {
        return findByCategory(category);
    }


    /*
     * 模拟数据库查询
     */

    // 模拟数据库查询：根据书名模糊查询
    private List<Book> findByTitleContaining(String title) {
        return sampleBooks.stream()
                .filter(book -> book.getTitle().contains(title))
                .toList();
    }

    // 模拟数据库查询：根据作者查询
    private List<Book> findByAuthor(String author) {
        return sampleBooks.stream()
                .filter(book -> book.getAuthor().equals(author))
                .toList();
    }

    // 模拟数据库查询：根据分类查询
    private List<Book> findByCategory(String category) {
        return sampleBooks.stream()
                .filter(book -> book.getCategory().equals(category))
                .toList();
    }

    // 准备示例数据
    List<Book> sampleBooks = Arrays.asList(
            new Book(null, "Spring实战（第6版）", "编程", "Craig Walls",
                    LocalDate.of(2022, 1, 15), "9787115582247"),
            new Book(null, "深入理解Java虚拟机", "编程", "周志明",
                    LocalDate.of(2019, 12, 1), "9787111641247"),
            new Book(null, "Java编程思想（第4版）", "编程", "Bruce Eckel",
                    LocalDate.of(2007, 6, 1), "9787111213826"),
            new Book(null, "算法（第4版）", "计算机科学", "Robert Sedgewick",
                    LocalDate.of(2012, 10, 1), "9787115293800"),
            new Book(null, "云原生架构", "架构设计", "张三",
                    LocalDate.of(2023, 3, 15), "9781234567890"),
            new Book(null, "微服务设计模式", "架构设计", "张三",
                    LocalDate.of(2021, 8, 20), "9789876543210"),
            new Book(null, "领域驱动设计", "架构设计", "Eric Evans",
                    LocalDate.of(2010, 4, 10), "9787111214748"),
            new Book(null, "高性能MySQL", "数据库", "Baron Schwartz",
                    LocalDate.of(2013, 5, 25), "9787111464747"),
            new Book(null, "Redis实战", "数据库", "Josiah L. Carlson",
                    LocalDate.of(2015, 9, 30), "9787115419378"),
            new Book(null, "深入浅出Docker", "容器技术", "李四",
                    LocalDate.of(2022, 11, 20), "9787123456789"));
}
