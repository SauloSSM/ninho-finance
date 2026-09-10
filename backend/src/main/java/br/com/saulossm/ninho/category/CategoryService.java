package br.com.saulossm.ninho.category;

import br.com.saulossm.ninho.error.ConflictException;
import br.com.saulossm.ninho.error.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public CategoryDtos.Response create(CategoryDtos.CreateRequest request) {
        if (repository.existsByNameAndType(request.name(), request.type())) {
            throw new ConflictException("Category with the same name and type already exists");
        }
        return CategoryDtos.Response.from(repository.save(new Category(request.name(), request.type())));
    }

    @Transactional(readOnly = true)
    public List<CategoryDtos.Response> findAll(CategoryType type, Boolean active) {
        List<Category> categories;
        if (type != null && active != null) {
            categories = repository.findAllByTypeAndActive(type, active);
        } else if (type != null) {
            categories = repository.findAllByType(type);
        } else if (active != null) {
            categories = repository.findAllByActive(active);
        } else {
            categories = repository.findAll();
        }
        return categories.stream().map(CategoryDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryDtos.Response findById(Long id) {
        return CategoryDtos.Response.from(require(id));
    }

    @Transactional
    public CategoryDtos.Response changeStatus(Long id, CategoryDtos.StatusRequest request) {
        var category = require(id);
        if (request.active()) {
            category.activate();
        } else {
            category.deactivate();
        }
        return CategoryDtos.Response.from(category);
    }

    private Category require(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
