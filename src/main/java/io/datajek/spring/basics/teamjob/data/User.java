package io.datajek.spring.basics.teamjob.data;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a user entity mapped to the 'users' table in the database.
 * <ul>
 *   <li>id: The primary key of the 'users' table.</li>
 *   <li>username</li>
 *   <li>email</li>
 *   <li>password: Hashed password</li>
 * </ul>
 */
@Setter
@Getter
@Data
@Entity
@Table(name = "users")
public class User {

    /**
     * The primary key of the 'users' table.
     * This value is automatically generated by the database.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The username of the user.
     * This field is unique and cannot be null.
     */
    @Column(unique = true, nullable = false)
    private String username;

    /**
     * The email address of the user.
     * This field is unique and cannot be null.
     */
    @Column(unique = true, nullable = false)
    private String email;

    /**
     * The password of the user.
     * This field can be nullable and does not need to be unique.
     */
    @Column
    private String password;

    //TODO Add authority field
}