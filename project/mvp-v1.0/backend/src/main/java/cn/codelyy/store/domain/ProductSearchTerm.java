package cn.codelyy.store.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_search_terms")
public class ProductSearchTerm {
    @Id
    @Column(length = 300, name = "keyword_key")
    private String keywordKey;

    @Column(length = 100, nullable = false, name = "display_keyword")
    private String displayKeyword;

    @Column(nullable = false, name = "search_count")
    private long searchCount;

    protected ProductSearchTerm() {}
}
