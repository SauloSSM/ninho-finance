package br.com.saulossm.ninho.account;

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
@RequestMapping("/api/accounts")
public class BankAccountController {

    private final BankAccountService service;

    public BankAccountController(BankAccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BankAccountDtos.Response create(@Valid @RequestBody BankAccountDtos.CreateRequest request) {
        return service.create(request);
    }

    @GetMapping
    List<BankAccountDtos.Response> findAll(
            @RequestParam(required = false) @Positive Long ownerId,
            @RequestParam(required = false) Boolean active
    ) {
        return service.findAll(ownerId, active);
    }

    @GetMapping("/{id}")
    BankAccountDtos.Response findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}/status")
    BankAccountDtos.Response changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody BankAccountDtos.StatusRequest request
    ) {
        return service.changeStatus(id, request);
    }
}
