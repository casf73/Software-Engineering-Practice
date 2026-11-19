package cn.codelyy.store.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Service
public class ProductSearchService {
    private static final int LIMIT = 5;
    private static final List<String> DEFAULTS = List.of("机械键盘", "键盘");
    private final JdbcTemplate jdbc;

    public ProductSearchService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void record(String keyword) {
        String key = key(keyword);
        if (increment(key) != 0) return;
        try {
            jdbc.update("insert into product_search_terms (keyword_key, display_keyword, search_count) values (?, ?, 1)",
                    key, keyword);
        } catch (DuplicateKeyException e) {
            // 另一个请求已经创建了同一个词，继续对该行加一。
            increment(key);
        }
    }

    private int increment(String key) {
        return jdbc.update("update product_search_terms set search_count = search_count + 1 where keyword_key = ?", key);
    }

    public List<String> topKeywords() {
        List<String> popular = new ArrayList<>(jdbc.queryForList("""
                select display_keyword from product_search_terms
                order by search_count desc, keyword_key asc
                fetch first 5 rows only
                """, String.class));
        var included = new HashSet<String>();
        popular.forEach(keyword -> included.add(key(keyword)));
        for (String keyword : DEFAULTS) {
            if (popular.size() == LIMIT) break;
            if (included.add(key(keyword))) popular.add(keyword);
        }
        return popular;
    }

    private String key(String keyword) {
        return keyword.toLowerCase(Locale.ROOT);
    }
}
