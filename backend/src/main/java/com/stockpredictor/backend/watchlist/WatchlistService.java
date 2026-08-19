package com.stockpredictor.backend.watchlist;

import com.stockpredictor.backend.common.ResourceNotFoundException;
import com.stockpredictor.backend.common.dto.WatchlistItemDto;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;

    public WatchlistService(WatchlistRepository watchlistRepository) {
        this.watchlistRepository = watchlistRepository;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemDto> getAll(String userId) {
        return watchlistRepository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(WatchlistService::toDto)
                .toList();
    }

    @Transactional
    public WatchlistItemDto add(String userId, String symbol) {
        WatchlistEntity entity = new WatchlistEntity();
        entity.setUserId(userId);
        entity.setSymbol(symbol);
        entity.setAddedAt(System.currentTimeMillis());
        entity.setSortOrder(watchlistRepository.countByUserId(userId));
        return toDto(watchlistRepository.save(entity));
    }

    @Transactional
    public void delete(String userId, Long id) {
        WatchlistEntity entity = watchlistRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found: " + id));
        watchlistRepository.delete(entity);
    }

    /**
     * Reorders the whole list atomically — the transaction wraps every row update so a partial
     * reorder can never be observed (mirrors Phase 2's WatchlistDao.updateSortOrders on Android).
     * Any id not belonging to this user fails the entire request with 404, never partially applies.
     */
    @Transactional
    public List<WatchlistItemDto> reorder(String userId, List<Long> orderedIds) {
        for (int index = 0; index < orderedIds.size(); index++) {
            Long watchlistId = orderedIds.get(index);
            WatchlistEntity entity = watchlistRepository.findByIdAndUserId(watchlistId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Watchlist item not found: " + watchlistId));
            entity.setSortOrder(index);
        }
        return getAll(userId);
    }

    private static WatchlistItemDto toDto(WatchlistEntity entity) {
        return new WatchlistItemDto(entity.getId(), entity.getSymbol(), entity.getAddedAt(), entity.getSortOrder());
    }
}
