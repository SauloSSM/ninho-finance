package br.com.saulossm.ninho.category;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryDtos.Response create(@Valid @RequestBody CategoryDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<CategoryDtos.Response> findAll(
            @RequestParam(required = false) CategoryType type,
            @RequestParam(required = false) Boolean active
    ) {
        return service.findAll(type, active);
    }

    @GetMapping("/{id}")
    CategoryDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/status")
    CategoryDtos.Response changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDtos.StatusRequest request
    ) {
        return service.changeStatus(id, request);
    }
}
