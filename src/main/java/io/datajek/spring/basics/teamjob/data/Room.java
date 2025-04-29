package io.datajek.spring.basics.teamjob.data;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Type;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "rooms")
public class Room {
    /**
     * Represents the primary key of the 'rooms' table.
     * This field is automatically generated.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The name of the room.
     * This field is unique and cannot be null.
     */
    @Column(nullable = false, unique = true)
    private String name;

    /**
     * Provides an optional description for the room entity.
     * This field can store additional details or notes about the room.
     */
    @Column()
    private String description;

    /**
     * The tags associated with the room.
     * This field is stored as a PostgreSQL array in the database.
     */
    @Type(io.hypersistence.utils.hibernate.type.array.ListArrayType.class)
    @Column(name = "tags", columnDefinition = "text[]")
    private Set<String> tags = new HashSet<>();

}
