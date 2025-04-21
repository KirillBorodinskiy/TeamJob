package io.datajek.spring.basics.teamjob.data.Repositories;

import io.datajek.spring.basics.teamjob.data.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByName(String name);

    @SuppressWarnings("unused")
    List<Room> findAllByName(String name);

    Boolean existsByName(String name);

    @SuppressWarnings("unused")
    @Query(value = "SELECT * FROM rooms WHERE tags && CAST(:tags AS text[])", nativeQuery = true)
    List<Room> findByTagsAnyMatch(@Param("tags") List<String> tags);


}

