package com.stockpredictor.backend.watchlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<WatchlistEntity, Long> {

    List<WatchlistEntity> findByUserIdOrderBySortOrderAsc(String userId);

    Optional<WatchlistEntity> findByIdAndUserId(Long id, String userId);

    int countByUserId(String userId);
}
