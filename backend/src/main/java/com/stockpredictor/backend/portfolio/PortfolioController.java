package com.stockpredictor.backend.portfolio;

import com.stockpredictor.backend.common.dto.CreatePortfolioHoldingRequest;
import com.stockpredictor.backend.common.dto.PortfolioHoldingDto;
import com.stockpredictor.backend.common.dto.UpdatePortfolioHoldingRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping
    public List<PortfolioHoldingDto> getAll(Authentication authentication) {
        return portfolioService.getAll(uid(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PortfolioHoldingDto add(Authentication authentication, @Valid @RequestBody CreatePortfolioHoldingRequest request) {
        return portfolioService.add(uid(authentication), request.symbol(), request.quantity(), request.avgBuyPrice());
    }

    @PutMapping("/{id}")
    public PortfolioHoldingDto update(Authentication authentication, @PathVariable Long id,
                                       @Valid @RequestBody UpdatePortfolioHoldingRequest request) {
        return portfolioService.update(uid(authentication), id, request.quantity(), request.avgBuyPrice());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication authentication, @PathVariable Long id) {
        portfolioService.delete(uid(authentication), id);
    }

    private static String uid(Authentication authentication) {
        return (String) authentication.getPrincipal();
    }
}
