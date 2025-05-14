package io.datajek.spring.basics.teamjob.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "rooms")
@EqualsAndHashCode(callSuper = true)
public class Room extends Auditable {
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

    @ElementCollection
    @CollectionTable(name = "room_tags", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

}
