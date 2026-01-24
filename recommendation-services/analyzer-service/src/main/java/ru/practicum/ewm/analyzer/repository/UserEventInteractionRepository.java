package ru.practicum.ewm.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.ewm.analyzer.model.UserEventInteraction;
import ru.practicum.ewm.analyzer.model.UserEventInteractionId;

import java.util.List;

@Repository
public interface UserEventInteractionRepository extends JpaRepository<UserEventInteraction, UserEventInteractionId> {

    @Query("SELECT uei FROM UserEventInteraction uei WHERE uei.userId = :userId ORDER BY uei.lastActionTimestamp DESC")
    List<UserEventInteraction> findByUserIdOrderByTimestampDesc(@Param("userId") Long userId);

    @Query("SELECT uei FROM UserEventInteraction uei WHERE uei.userId = :userId AND uei.eventId IN :eventIds")
    List<UserEventInteraction> findByUserIdAndEventIdIn(@Param("userId") Long userId, @Param("eventIds") List<Long> eventIds);

    @Query(value = "SELECT event_id, SUM(max_weight) FROM user_event_interactions WHERE event_id IN :eventIds GROUP BY event_id", nativeQuery = true)
    List<Object[]> sumMaxWeightsByEventId(@Param("eventIds") List<Long> eventIds);

    boolean existsByUserIdAndEventId(Long userId, Long eventId);
}
