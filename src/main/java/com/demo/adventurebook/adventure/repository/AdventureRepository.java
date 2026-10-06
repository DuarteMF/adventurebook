package com.demo.adventurebook.adventure.repository;

import com.demo.adventurebook.adventure.entity.Adventure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Set;

public interface AdventureRepository extends JpaRepository<Adventure, Long> {

    @Query("""
            SELECT a.id
            FROM Adventure a
            WHERE (a.saved = false AND a.updatedAt < :cutoff)
            OR a.status = AdventureStatus.ABANDONED
            """)
    Set<Long> findPurgeCandidateIds(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("""
            DELETE from Adventure a
            WHERE a.id = :id
            AND ((a.saved = false AND a.updatedAt < :cutoff)
            OR a.status = AdventureStatus.ABANDONED)
            """)
    int deleteIfStale(@Param("id") Long id, @Param("cutoff") Instant cutoff);
}
