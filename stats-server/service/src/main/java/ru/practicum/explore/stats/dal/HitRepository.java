package ru.practicum.explore.stats.dal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.explore.stats.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Repository
public interface HitRepository extends JpaRepository<EndpointHit, Long> {

    @Query(value = "SELECT app, uri, COUNT(*) AS hits " +
            "FROM hits " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR uri IN (:uris)) " +
            "GROUP BY app, uri " +
            "ORDER BY hits DESC", nativeQuery = true)
    List<Map<String, Object>> findStatsByRangeAndUris(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end,
                                                      @Param("uris") List<String> uris);

    @Query(value = "SELECT app, uri, COUNT(DISTINCT ip) AS hits " +
            "FROM hits " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "AND (:uris IS NULL OR uri IN (:uris)) " +
            "GROUP BY app, uri " +
            "ORDER BY hits DESC", nativeQuery = true)
    List<Map<String, Object>> findUniqueStatsByRangeAndUris(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end,
                                                            @Param("uris") List<String> uris);

    @Query(value = "SELECT app, uri, COUNT(*) AS hits " +
            "FROM hits " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY app, uri " +
            "ORDER BY hits DESC", nativeQuery = true)
    List<Map<String, Object>> findStatsByRangeWithoutUris(@Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);


    @Query(value = "SELECT app, uri, COUNT(DISTINCT ip) AS hits " +
            "FROM hits " +
            "WHERE timestamp BETWEEN :start AND :end " +
            "GROUP BY app, uri " +
            "ORDER BY hits DESC", nativeQuery = true)
    List<Map<String, Object>> findUniqueStatsByRangeWithoutUris(@Param("start") LocalDateTime start,
                                                            @Param("end") LocalDateTime end);
}
