package io.rxbridge.enrollment.common.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id")
    private UUID id;

    @Version
    @Column(name = "version")
    private Integer version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    protected BaseEntity() { }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || effectiveClass(this) != effectiveClass(obj)) return false;
        UUID id = this.getId();
        return id != null && Objects.equals(id, ((BaseEntity) obj).getId());
    }

    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    @Override
    public final String toString() {
        String effectiveClassName = effectiveClass(this).getSimpleName();
        return "%s{id: %s}".formatted(effectiveClassName, this.getId());
    }

    private static Class<?> effectiveClass(Object o) {
        return o instanceof HibernateProxy proxy
            ? proxy.getHibernateLazyInitializer().getPersistentClass()
            : o.getClass();
    }

}
