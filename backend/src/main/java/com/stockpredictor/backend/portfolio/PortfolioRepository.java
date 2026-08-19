package com.stockpredictor.backend.portfolio;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<PortfolioHoldingEntity, Long> {

    List<PortfolioHoldingEntity> findByUserIdOrderByCreatedAtAsc(String userId);

    Optional<PortfolioHoldingEntity> findByIdAndUserId(Long id, String userId);
}
