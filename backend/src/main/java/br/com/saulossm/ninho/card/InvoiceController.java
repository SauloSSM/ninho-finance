package br.com.saulossm.ninho.card;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    InvoiceDtos.Response create(@Valid @RequestBody InvoiceDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<InvoiceDtos.Response> findAll(
            @RequestParam(required = false) @Positive Long creditCardId,
            @RequestParam(required = false) @Positive Integer referenceYear,
            @RequestParam(required = false) @Min(1) @Max(12) Integer referenceMonth
    ) {
        return service.findAll(creditCardId, referenceYear, referenceMonth);
    }

    @GetMapping("/{id}")
    InvoiceDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/close")
    InvoiceDtos.Response close(@PathVariable Long id) {
        return service.close(id);
    }
}
