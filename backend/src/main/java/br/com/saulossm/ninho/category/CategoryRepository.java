package br.com.saulossm.ninho.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByActiveTrue();

    List<Category> findAllByTypeAndActiveTrue(CategoryType type);

    List<Category> findAllByType(CategoryType type);

    List<Category> findAllByActive(boolean active);

    List<Category> findAllByTypeAndActive(CategoryType type, boolean active);

    boolean existsByNameAndType(String name, CategoryType type);
}
