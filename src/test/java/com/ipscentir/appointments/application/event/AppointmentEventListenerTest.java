package com.ipscentir.appointments.application.event;

import com.ipscentir.appointments.domain.model.facility.FacilityMasterData;

import com.ipscentir.appointments.domain.model.appointment.Appointment;
import com.ipscentir.appointments.domain.model.appointment.AppointmentType;
import com.ipscentir.appointments.domain.model.appointment.AppointmentScheduleData;
import com.ipscentir.appointments.domain.repository.AppointmentRepository;
import com.ipscentir.appointments.application.service.N8nEventJournalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
class AppointmentEventListenerTest {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @MockBean
    private N8nEventJournalService n8nEventJournalService;

    @Test
    void testAppointmentCreationRecordsN8nEvent() {
        UUID patientId = UUID.randomUUID();
        String doctorId = UUID.randomUUID().toString();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        UUID scheduleId = UUID.randomUUID();

        Appointment appointment = Appointment.scheduleNew(
            patientId, doctorId.toString(), null,
            new AppointmentScheduleData(scheduleId, sedeId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, com.ipscentir.appointments.domain.model.appointment.AppointmentStatus.SCHEDULED, "Checkup")
        );

        appointmentRepository.save(appointment);

        verify(n8nEventJournalService, timeout(5000).times(1)).recordAppointmentCreated(any());
    }

    @Test
    void testAppointmentCancellationRecordsN8nEvent() {
        UUID patientId = UUID.randomUUID();
        String doctorId = UUID.randomUUID().toString();
        Integer sedeId = FacilityMasterData.SEDE_ID_BELEN;
        UUID scheduleId = UUID.randomUUID();

        Appointment appointment = Appointment.scheduleNew(
            patientId, doctorId.toString(), null,
            new AppointmentScheduleData(scheduleId, sedeId, LocalDate.now().plusDays(2), LocalTime.of(10, 0), 30, AppointmentType.PRESENCIAL, com.ipscentir.appointments.domain.model.appointment.AppointmentStatus.SCHEDULED, "Checkup")
        );

        appointmentRepository.save(appointment);
        appointment.cancel("No puede asistir");
        appointmentRepository.save(appointment);

        verify(n8nEventJournalService, timeout(5000).times(1)).recordAppointmentCancelled(any());
    }
}
