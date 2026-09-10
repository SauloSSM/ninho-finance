package br.com.saulossm.ninho.overview;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/overview/monthly")
public class MonthlyOverviewController {

    private final MonthlyOverviewService service;

    public MonthlyOverviewController(MonthlyOverviewService service) {
        this.service = service;
    }

    @GetMapping
    MonthlyOverviewDtos.Response getMonthly(
            @RequestParam @Positive Integer year,
            @RequestParam @Min(1) @Max(12) Integer month
    ) {
        return service.getMonthly(year, month);
    }

    @GetMapping("/bills")
    List<MonthlyOverviewDtos.BillItem> getBills(
            @RequestParam @Positive Integer year,
            @RequestParam @Min(1) @Max(12) Integer month
    ) {
        return service.getBills(year, month);
    }

    @GetMapping("/invoices")
    List<MonthlyOverviewDtos.InvoiceItem> getInvoices(
            @RequestParam @Positive Integer year,
            @RequestParam @Min(1) @Max(12) Integer month
    ) {
        return service.getInvoices(year, month);
    }
}
