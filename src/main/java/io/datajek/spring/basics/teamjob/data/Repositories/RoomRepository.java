package io.datajek.spring.basics.teamjob.data.Repositories;

import io.datajek.spring.basics.teamjob.data.Rooms;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Rooms, Long> {
    Optional<Rooms> findByName(String name);

    List<Rooms> findAllByName(String name);

    Boolean existsByName(String name);

}

