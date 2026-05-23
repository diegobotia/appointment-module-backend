package com.ipscentir.appointments.domain.model.schedule;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "schedules", schema = "appointments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String doctorId;

    @Column(name = "sede_id", nullable = false)
    private Integer sedeId;

    private String specialty;

    @Column(nullable = false, columnDefinition = "integer")
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer slotDurationMinutes;

    @Column(nullable = false)
    private Integer maxPatientsPerSlot;

    @Column(nullable = false)
    private Boolean isActive;

    @Builder.Default
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ScheduleBlock> blocks = new ArrayList<>();

    // Comportamientos de Dominio
    
    public List<LocalTime> getAvailableSlots(LocalDate date) {
        if (!this.isActive) {
            return Collections.emptyList();
        }

        if (date.getDayOfWeek() != this.dayOfWeek) {
            return Collections.emptyList();
        }

        List<LocalTime> slots = new ArrayList<>();
        LocalTime currentSlot = this.startTime;

        while (currentSlot.isBefore(this.endTime)) {
            if (!isSlotBlocked(date, currentSlot)) {
                slots.add(currentSlot);
            }
            currentSlot = currentSlot.plusMinutes(this.slotDurationMinutes);
        }

        return slots;
    }

    public boolean isAvailable(LocalDate date, LocalTime time) {
        if (!this.isActive) return false;
        if (date.getDayOfWeek() != this.dayOfWeek) return false;
        if (time.isBefore(this.startTime) || time.isAfter(this.endTime) || time.equals(this.endTime)) return false;
        if (isSlotBlocked(date, time)) return false;

        return true;
    }

    private boolean isSlotBlocked(LocalDate date, LocalTime time) {
        if (blocks == null || blocks.isEmpty()) return false;
        return blocks.stream().anyMatch(block -> block.isDateTimeBlocked(date, time));
    }

    public void applyPublishedSlotConfiguration(
            String specialty,
            LocalTime startTime,
            LocalTime endTime,
            int slotDurationMinutes,
            int maxPatientsPerSlot
    ) {
        this.specialty = specialty;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotDurationMinutes = slotDurationMinutes;
        this.maxPatientsPerSlot = maxPatientsPerSlot;
        this.isActive = true;
    }

    public void addBlock(LocalDate date, LocalTime start, LocalTime end, String reason) {
        if (this.blocks == null) {
            this.blocks = new ArrayList<>();
        }
        ScheduleBlock block = ScheduleBlock.builder()
                .schedule(this)
                .blockDate(date)
                .startTime(start)
                .endTime(end)
                .reason(reason)
                .build();
        this.blocks.add(block);
    }
}
