package com.app.messenger.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.ZonedDateTime;

@MappedSuperclass
@Setter
@Getter
public class BaseEntity {
    @Column(name = "created_at")
    protected ZonedDateTime createdAt;

    @Column(name = "updated_at")
    protected ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    protected ZonedDateTime deletedAt;
}

