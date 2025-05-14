package com.borodkir.teamjob.data;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Base class for all auditable entities.
 * Provides fields for tracking creation and modification timestamps and users.
 * This class is meant to be extended by entity classes that need auditing.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    /**
     * The timestamp when the entity was created.
     * This field is automatically set by Spring Data JPA.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * The username of the user who created the entity.
     * This field is automatically set by Spring Data JPA.
     */
    @CreatedBy
    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    /**
     * The timestamp when the entity was last modified.
     * This field is automatically set by Spring Data JPA.
     */
    @LastModifiedDate
    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    /**
     * The username of the user who last modified the entity.
     * This field is automatically set by Spring Data JPA.
     */
    @LastModifiedBy
    @Column(name = "last_modified_by")
    private String lastModifiedBy;
}