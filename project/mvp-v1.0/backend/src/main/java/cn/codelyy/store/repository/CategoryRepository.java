package cn.codelyy.store.repository;
import cn.codelyy.store.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CategoryRepository extends JpaRepository<Category,Long> {
 boolean existsByParentId(Long parentId);
 boolean existsByNameAndParentId(String name,Long parentId);
}
