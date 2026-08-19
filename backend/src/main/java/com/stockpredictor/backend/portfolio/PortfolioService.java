package com.stockpredictor.backend.portfolio;

import com.stockpredictor.backend.common.ResourceNotFoundException;
import com.stockpredictor.backend.common.dto.PortfolioHoldingDto;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;

    public PortfolioService(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Transactional(readOnly = true)
    public List<PortfolioHoldingDto> getAll(String userId) {
        return portfolioRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(PortfolioService::toDto)
                .toList();
    }

    @Transactional
    public PortfolioHoldingDto add(String userId, String symbol, BigDecimal quantity, BigDecimal avgBuyPrice) {
        Instant now = Instant.now();
        PortfolioHoldingEntity entity = new PortfolioHoldingEntity();
        entity.setUserId(userId);
        entity.setSymbol(symbol);
        entity.setQuantity(quantity);
        entity.setAvgBuyPrice(avgBuyPrice);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toDto(portfolioRepository.save(entity));
    }

    @Transactional
    public PortfolioHoldingDto update(String userId, Long id, BigDecimal quantity, BigDecimal avgBuyPrice) {
        PortfolioHoldingEntity entity = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio holding not found: " + id));
        if (quantity != null) entity.setQuantity(quantity);
        if (avgBuyPrice != null) entity.setAvgBuyPrice(avgBuyPrice);
        entity.setUpdatedAt(Instant.now());
        return toDto(entity);
    }

    @Transactional
    public void delete(String userId, Long id) {
        PortfolioHoldingEntity entity = portfolioRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio holding not found: " + id));
        portfolioRepository.delete(entity);
    }

    private static PortfolioHoldingDto toDto(PortfolioHoldingEntity entity) {
        return new PortfolioHoldingDto(
                entity.getId(), entity.getSymbol(), entity.getQuantity(), entity.getAvgBuyPrice(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
