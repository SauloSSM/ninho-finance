package br.com.saulossm.ninho.income;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final IncomeService service;

    public IncomeController(IncomeService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    IncomeDtos.Response create(@Valid @RequestBody IncomeDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<IncomeDtos.Response> findAll(
            @RequestParam(required = false) @Positive Long personId,
            @RequestParam(required = false) IncomeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.findAll(personId, status, from, to);
    }

    @GetMapping("/{id}")
    IncomeDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/receive")
    IncomeDtos.Response receive(
            @PathVariable Long id,
            @Valid @RequestBody IncomeDtos.ReceiveRequest request
    ) {
        return service.receive(id, request);
    }

    @PatchMapping("/{id}/cancel")
    IncomeDtos.Response cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
