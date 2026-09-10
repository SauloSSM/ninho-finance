package br.com.saulossm.ninho.card;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/cards")
public class CreditCardController {

    private final CreditCardService service;

    public CreditCardController(CreditCardService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreditCardDtos.Response create(@Valid @RequestBody CreditCardDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<CreditCardDtos.Response> findAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @Positive Long holderId,
            @RequestParam(required = false) @Positive Long invoicePayerId
    ) {
        return service.findAll(active, holderId, invoicePayerId);
    }

    @GetMapping("/{id}")
    CreditCardDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/status")
    CreditCardDtos.Response changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody CreditCardDtos.StatusRequest request
    ) {
        return service.changeStatus(id, request);
    }

    @PatchMapping("/{id}/limit")
    CreditCardDtos.Response changeLimit(
            @PathVariable Long id,
            @Valid @RequestBody CreditCardDtos.LimitRequest request
    ) {
        return service.changeLimit(id, request);
    }
}
