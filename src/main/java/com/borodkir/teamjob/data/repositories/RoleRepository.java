package com.borodkir.teamjob.data.repositories;

import com.borodkir.teamjob.data.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    @SuppressWarnings("unused")
    Set<Role> findAllByName(String name);

    Boolean existsByName(String name);

}