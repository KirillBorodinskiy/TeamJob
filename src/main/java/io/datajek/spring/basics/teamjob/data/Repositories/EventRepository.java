package io.datajek.spring.basics.teamjob.data.Repositories;

import io.datajek.spring.basics.teamjob.data.Events;
import io.datajek.spring.basics.teamjob.data.Rooms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Events, Long> {
    Optional<Events> findByStartTime(LocalDateTime startTime);
    Optional<Events> findByEndTime(LocalDateTime startTime);

    Optional<Events> findByRoom(Rooms room);

    Optional<Events> findByTitle(String title);
    List<Events> findAllByTitle(String title);



}

