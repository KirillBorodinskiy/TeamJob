package io.datajek.spring.basics.teamjob.data.Repositories;

import io.datajek.spring.basics.teamjob.data.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByName(String name);

    List<Room> findAllByName(String name);

    Boolean existsByName(String name);

}

