package io.datajek.spring.basics.teamjob.data.repositories;

import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    @SuppressWarnings("unused")
    Optional<Event> findByStartTime(LocalDateTime startTime);

    @SuppressWarnings("unused")
    Optional<Event> findByEndTime(LocalDateTime startTime);

    @SuppressWarnings("unused")
    Optional<Event> findByRoom(Room room);

    @SuppressWarnings("unused")
    Optional<Event> findByTitle(String title);

    @SuppressWarnings("unused")
    List<Event> findAllByTitle(String title);

    @Query("SELECT e FROM Event e WHERE (e.startTime <= :endTime AND e.endTime >= :startTime)")
    List<Event> findOverlappingEvents(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE (e.startTime <= :endTime AND e.endTime >= :startTime) AND e.room = :room")
    Boolean findOverlappingEventsInRoom(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime, @Param("room") Optional<Room> room);

    @SuppressWarnings({"SqlResolve", "unused"})
    @Query("SELECT e FROM Event e WHERE (e.startTime <= :endTime AND e.endTime >= :startTime) AND e.room = :room")
    List<Event> findAllOverlappingEventsInRoom(LocalDateTime startTime, LocalDateTime endTime, Optional<Room> room);

    @SuppressWarnings({"SqlResolve", "unused"})
    @Query("SELECT e FROM Event e JOIN e.tags t WHERE t IN :tags")
    List<Event> findByTagsAnyMatch(@Param("tags") List<String> tags);

}