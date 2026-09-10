package br.com.saulossm.ninho.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseService service;

    public ExpenseController(ExpenseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ExpenseDtos.Response create(@Valid @RequestBody ExpenseDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<ExpenseDtos.Response> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) ExpenseScope scope,
            @RequestParam(required = false) @Positive Long categoryId,
            @RequestParam(required = false) @Positive Long creditCardInvoiceId
    ) {
        return service.findAll(from, to, scope, categoryId, creditCardInvoiceId);
    }

    @GetMapping("/{id}")
    ExpenseDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
