package io.datajek.spring.basics.teamjob.data.Repositories;

import io.datajek.spring.basics.teamjob.data.Event;
import io.datajek.spring.basics.teamjob.data.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByStartTime(LocalDateTime startTime);
    Optional<Event> findByEndTime(LocalDateTime startTime);

    Optional<Event> findByRoom(Room room);

    Optional<Event> findByTitle(String title);
    List<Event> findAllByTitle(String title);



}

