package br.com.saulossm.ninho.person;

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
@RequestMapping("/api/people")
public class PersonController {

    private final PersonService service;

    public PersonController(PersonService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PersonDtos.Response create(@Valid @RequestBody PersonDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<PersonDtos.Response> findAll(@RequestParam(required = false) Boolean active) {
        return service.findAll(active);
    }

    @GetMapping("/{id}")
    PersonDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/status")
    PersonDtos.Response changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody PersonDtos.StatusRequest request
    ) {
        return service.changeStatus(id, request);
    }
}
