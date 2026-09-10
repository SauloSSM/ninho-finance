package br.com.saulossm.ninho.payment;

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
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService service;

    public SettlementController(SettlementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SettlementDtos.Response create(@Valid @RequestBody SettlementDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<SettlementDtos.Response> findAll(
            @RequestParam(required = false) @Positive Long personId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return service.findAll(personId, from, to);
    }

    @GetMapping("/{id}")
    SettlementDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }
}
