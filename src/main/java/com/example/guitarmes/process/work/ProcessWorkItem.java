package com.example.guitarmes.process.work;

import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;

/** Work内の個別作業記録。業務操作・終了済み判定は後続Serviceで扱う。 */
@Entity
@Table(name = "t_process_work_item", uniqueConstraints = {
        @UniqueConstraint(name = "uk_process_work_item_work_key", columnNames = {"process_work_id", "item_key"}),
        @UniqueConstraint(name = "uk_process_work_item_work_order", columnNames = {"process_work_id", "item_order"})
})
public class ProcessWorkItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "process_work_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_process_work_item_process_work"))
    private ProcessWork processWork;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_key", length = 64, nullable = false)
    private ProcessWorkItemKey itemKey;

    @Column(name = "item_order", nullable = false)
    private Integer itemOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private ProcessWorkItemStatus status = ProcessWorkItemStatus.NOT_STARTED;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private LocalDateTime persistedUpdatedAt;

    public ProcessWorkItem() {
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        // 明示された業務イベント時刻は保持。通常編集では現在時刻に更新する。
        if (updatedAt == null || Objects.equals(updatedAt, persistedUpdatedAt)) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PostLoad
    @PostPersist
    @PostUpdate
    public void rememberUpdatedAt() {
        persistedUpdatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProcessWork getProcessWork() {
        return processWork;
    }

    public void setProcessWork(ProcessWork processWork) {
        this.processWork = processWork;
    }

    public ProcessWorkItemKey getItemKey() {
        return itemKey;
    }

    public void setItemKey(ProcessWorkItemKey itemKey) {
        this.itemKey = itemKey;
    }

    public Integer getItemOrder() {
        return itemOrder;
    }

    public void setItemOrder(Integer itemOrder) {
        this.itemOrder = itemOrder;
    }

    public ProcessWorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessWorkItemStatus status) {
        this.status = status;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
