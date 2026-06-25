package com.ipscentir.appointments.domain.model.schedule;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "schedule_plans", schema = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SchedulePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "specialist_id", nullable = false)
    private String specialistId;

    @Column(name = "sede_id", nullable = false)
    private Integer sedeId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(name = "is_active_version", nullable = false)
    @Builder.Default
    private boolean activeVersion = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", updatable = false, insertable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "schedulePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SchedulePlanSlot> slots = new ArrayList<>();

    @OneToMany(mappedBy = "schedulePlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SchedulePlanBlock> blocks = new ArrayList<>();

    public void addSlot(SchedulePlanSlot slot) {
        slot.assignPlan(this);
        this.slots.add(slot);
    }

    public void addBlock(SchedulePlanBlock block) {
        block.assignPlan(this);
        this.blocks.add(block);
    }

    public void replaceSlots(List<SchedulePlanSlot> newSlots) {
        this.slots.clear();
        for (SchedulePlanSlot slot : newSlots) {
            slot.assignPlan(this);
            this.slots.add(slot);
        }
    }

    public boolean removeSlot(UUID slotId) {
        return this.slots.removeIf(s -> s.getId().equals(slotId));
    }

    public boolean removeBlock(UUID blockId) {
        return this.blocks.removeIf(b -> b.getId().equals(blockId));
    }

    public void updateDates(LocalDate startDate, LocalDate endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void unpublish() {
        this.published = false;
        this.activeVersion = false;
        this.publishedAt = null;
    }

    public void publishAsActive() {
        this.published = true;
        this.activeVersion = true;
        this.publishedAt = LocalDateTime.now();
    }

    public void markAsInactiveVersion() {
        this.activeVersion = false;
    }

    public void setSedeId(Integer sedeId) {
        this.sedeId = sedeId;
    }

    public void setSpecialistId(String specialistId) {
        this.specialistId = specialistId;
    }
}
