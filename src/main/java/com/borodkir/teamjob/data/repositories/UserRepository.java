package com.borodkir.teamjob.data.repositories;

import com.borodkir.teamjob.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Boolean existsByUsername(String username);

    @SuppressWarnings("unused")
    Boolean existsByEmail(String email);

    Optional<Object> findByEmail(String attr0);

    @SuppressWarnings({"SqlResolve", "unused"})
    @Query(value = "SELECT * FROM users WHERE tags && CAST(:tags AS text[])", nativeQuery = true)
    List<User> findByTagsAnyMatch(@Param("tags") List<String> tags);

    @SuppressWarnings({"SqlResolve", "unused"})
    @Query("SELECT DISTINCT e.user FROM Event e WHERE e.user IS NOT NULL AND e.startTime <= :endTime AND e.endTime >= :startTime")
    List<User> findAllOccupiedByStartTimeAndEndTime(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}
