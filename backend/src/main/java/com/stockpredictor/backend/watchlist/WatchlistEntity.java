package com.stockpredictor.backend.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "watchlist", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WatchlistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String symbol;

    @Column(name = "added_at", nullable = false)
    private Long addedAt;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}
