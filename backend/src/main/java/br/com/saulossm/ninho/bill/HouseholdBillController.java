package br.com.saulossm.ninho.bill;

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
@RequestMapping("/api/bills")
public class HouseholdBillController {

    private final HouseholdBillService service;

    public HouseholdBillController(HouseholdBillService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    HouseholdBillDtos.Response create(@Valid @RequestBody HouseholdBillDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<HouseholdBillDtos.Response> findAll(
            @RequestParam(required = false) @Positive Integer referenceYear,
            @RequestParam(required = false) @Min(1) @Max(12) Integer referenceMonth,
            @RequestParam(required = false) HouseholdBillStatus status,
            @RequestParam(required = false) @Positive Long responsiblePersonId
    ) {
        return service.findAll(referenceYear, referenceMonth, status, responsiblePersonId);
    }

    @GetMapping("/{id}")
    HouseholdBillDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/amount")
    HouseholdBillDtos.Response updateAmount(
            @PathVariable Long id,
            @Valid @RequestBody HouseholdBillDtos.AmountRequest request
    ) {
        return service.updateAmount(id, request);
    }

    @PatchMapping("/{id}/cancel")
    HouseholdBillDtos.Response cancel(@PathVariable Long id) {
        return service.cancel(id);
    }
}
