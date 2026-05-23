package com.ipscentir.appointments.domain.service;

import com.ipscentir.appointments.application.exception.FacilityOperatingHoursViolationException;
import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;
import com.ipscentir.appointments.domain.model.sede.Sede;
import com.ipscentir.appointments.domain.model.facility.FacilityOperatingHour;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlan;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanBlock;
import com.ipscentir.appointments.domain.model.schedule.SchedulePlanSlot;
import com.ipscentir.appointments.domain.repository.FacilityOperatingHoursRepository;
import com.ipscentir.appointments.domain.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;
import com.ipscentir.appointments.domain.service.ColombiaPublicHoliday;

@Service
@RequiredArgsConstructor
public class FacilityOperatingHoursService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final String INSTITUTIONAL_LABEL = "horario institucional de sede";

    private final FacilityOperatingHoursRepository facilityOperatingHoursRepository;
    private final SedeRepository sedeRepository;

    public void assertSlotWithinSedeHours(
            Integer sedeId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada: " + sedeId));
        FacilityOperatingWindow window = resolveWindow(sedeId, dayOfWeek);
        assertWithinWindow(window, dayOfWeek, startTime, endTime, sede.getId(), sede.getNombre());
    }

    /**
     * Validación en creación de plan (antes de conocer la sede de publicación).
     * Usa el horario institucional acordado (mismo para todas las sedes en Fase A).
     */
    public void assertSlotWithinInstitutionalHours(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        FacilityOperatingWindow window = masterDataWindow(dayOfWeek);
        assertWithinWindow(window, dayOfWeek, startTime, endTime, null, INSTITUTIONAL_LABEL);
    }

    public void assertBlockTimeWithinInstitutionalEnvelope(LocalTime startTime, LocalTime endTime) {
        LocalTime earliestOpen = LocalTime.of(7, 0);
        LocalTime latestClose = LocalTime.of(18, 0);
        if (startTime.isBefore(earliestOpen) || endTime.isAfter(latestClose)) {
            throw new FacilityOperatingHoursViolationException(
                    "El bloque " + formatRange(startTime, endTime)
                            + " debe estar entre " + earliestOpen.format(TIME_FORMAT)
                            + " y " + latestClose.format(TIME_FORMAT)
                            + " (" + INSTITUTIONAL_LABEL + ")",
                    null,
                    INSTITUTIONAL_LABEL,
                    new FacilityOperatingWindow(0, earliestOpen, latestClose, false)
            );
        }
    }

    public void assertBlockWithinFacilityHours(
            Integer sedeId,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime
    ) {
        Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new IllegalArgumentException("Sede no encontrada: " + sedeId));

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            // Prohibir reservas/materialización en festivos nacionales
            if (ColombiaPublicHoliday.isHoliday(date)) {
            String name = ColombiaPublicHoliday.getHolidayName(date);
            throw new FacilityOperatingHoursViolationException(
                "El bloque solicitado incluye el festivo: " + name + " (" + date + ")",
                sede.getId(),
                sede.getNombre(),
                null
            );
            }
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            FacilityOperatingWindow window = resolveWindow(sedeId, dayOfWeek);
            try {
                assertWithinWindow(window, dayOfWeek, startTime, endTime, sede.getId(), sede.getNombre());
            } catch (FacilityOperatingHoursViolationException ex) {
                throw new FacilityOperatingHoursViolationException(
                        "El bloque " + formatRange(startTime, endTime)
                                + " del " + spanishDayName(dayOfWeek)
                                + " (fecha " + date + ") excede el horario de la sede "
                                + sede.getNombre() + describeWindow(window),
                        sede.getId(),
                        sede.getNombre(),
                        window
                );
            }
        }
    }

    public boolean isHoliday(LocalDate date) {
        return ColombiaPublicHoliday.isHoliday(date);
    }

    public void assertDateNotHoliday(LocalDate date) {
        if (ColombiaPublicHoliday.isHoliday(date)) {
            String name = ColombiaPublicHoliday.getHolidayName(date);
            throw new FacilityOperatingHoursViolationException(
                    "No se pueden agendar citas en festivo: " + name + " (" + date + ")",
                    null,
                    "festivo",
                    null
            );
        }
    }

    public void assertPlanSlotsWithinSedeHours(Integer sedeId, SchedulePlan plan) {
        for (SchedulePlanSlot slot : plan.getSlots()) {
            if (slot.isActive()) {
                assertSlotWithinSedeHours(
                        sedeId,
                        slot.getDayOfWeek(),
                        slot.getStartTime(),
                        slot.getEndTime()
                );
            }
        }
    }

    public void assertPlanBlocksWithinSedeHours(Integer sedeId, SchedulePlan plan) {
        for (SchedulePlanBlock block : plan.getBlocks()) {
            assertBlockWithinFacilityHours(
                    sedeId,
                    block.getStartDate(),
                    block.getEndDate(),
                    block.getStartTime(),
                    block.getEndTime()
            );
        }
    }

    private void assertWithinWindow(
            FacilityOperatingWindow window,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Integer sedeId,
            String sedeLabel
    ) {
        if (window.closed()) {
            throw new FacilityOperatingHoursViolationException(
                    "La sede " + sedeLabel + " no atiende los " + spanishDayName(dayOfWeek),
                    sedeId,
                    sedeLabel,
                    window
            );
        }

        if (startTime.isBefore(window.openTime()) || endTime.isAfter(window.closeTime())) {
            throw new FacilityOperatingHoursViolationException(
                    "El bloque " + formatRange(startTime, endTime)
                            + " del " + spanishDayName(dayOfWeek)
                            + " excede el horario de la sede " + sedeLabel
                            + describeWindow(window),
                    sedeId,
                    sedeLabel,
                    window
            );
        }
    }

    private FacilityOperatingWindow resolveWindow(Integer sedeId, DayOfWeek dayOfWeek) {
        int isoDay = dayOfWeek.getValue();
        Map<Integer, FacilityOperatingHour> byDay = facilityOperatingHoursRepository
                .findBySedeIdOrderByDayOfWeek(sedeId).stream()
                .collect(Collectors.toMap(FacilityOperatingHour::getDayOfWeek, Function.identity()));

        FacilityOperatingHour hour = byDay.get(isoDay);
        if (hour != null) {
            return new FacilityOperatingWindow(
                    hour.getDayOfWeek(),
                    hour.getOpenTime(),
                    hour.getCloseTime(),
                    hour.isClosed()
            );
        }
        return masterDataWindow(dayOfWeek);
    }

    private FacilityOperatingWindow masterDataWindow(DayOfWeek dayOfWeek) {
        int isoDay = dayOfWeek.getValue();
        List<FacilityMasterData.OperatingHourSeed> seeds = FacilityMasterData.defaultOperatingHours();
        return seeds.stream()
                .filter(seed -> seed.dayOfWeek() == isoDay)
                .findFirst()
                .map(seed -> new FacilityOperatingWindow(
                        seed.dayOfWeek(),
                        seed.openTime(),
                        seed.closeTime(),
                        seed.closed()
                ))
                .orElseThrow(() -> new IllegalStateException("Horario maestro no definido para día " + isoDay));
    }

    private String describeWindow(FacilityOperatingWindow window) {
        if (window.closed()) {
            return " (cerrado)";
        }
        return " (abre " + window.openTime().format(TIME_FORMAT)
                + ", cierra " + window.closeTime().format(TIME_FORMAT) + ")";
    }

    private String formatRange(LocalTime start, LocalTime end) {
        return start.format(TIME_FORMAT) + "–" + end.format(TIME_FORMAT);
    }

    private String spanishDayName(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miércoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }
}
