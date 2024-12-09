package com.vet.system.controller;

import com.vet.system.model.Appointment;
import com.vet.system.model.AppointmentStatus;
import com.vet.system.service.AppointmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.vet.system.security.services.UserDetailsImpl;
import com.vet.system.model.User;
import com.vet.system.repository.PetRepository;
import com.vet.system.repository.UserRepository;
import com.vet.system.repository.VetServiceRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.vet.system.dto.AppointmentDTO;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final VetServiceRepository serviceRepository;

    public AppointmentController(AppointmentService appointmentService, UserRepository userRepository, PetRepository petRepository, VetServiceRepository serviceRepository) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
        this.petRepository = petRepository;
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    public List<AppointmentDTO> getAllAppointments(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return appointmentService.getAllAppointments(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setDateTime(appointment.getDateTime());
        dto.setStatus(appointment.getStatus().toString());
        
        // Convert Pet
        AppointmentDTO.PetDTO petDTO = new AppointmentDTO.PetDTO();
        petDTO.setId(appointment.getPet().getId());
        petDTO.setName(appointment.getPet().getName());
        petDTO.setSpecies(appointment.getPet().getSpecies());
        petDTO.setBreed(appointment.getPet().getBreed());
        
        // Convert Owner
        AppointmentDTO.OwnerDTO ownerDTO = new AppointmentDTO.OwnerDTO();
        ownerDTO.setId(appointment.getPet().getOwner().getId());
        ownerDTO.setFirstName(appointment.getPet().getOwner().getFirstName());
        ownerDTO.setLastName(appointment.getPet().getOwner().getLastName());
        petDTO.setOwner(ownerDTO);
        
        dto.setPet(petDTO);
        
        // Convert Service
        AppointmentDTO.ServiceDTO serviceDTO = new AppointmentDTO.ServiceDTO();
        serviceDTO.setId(appointment.getService().getId());
        serviceDTO.setName(appointment.getService().getName());
        serviceDTO.setDuration(appointment.getService().getDuration());
        serviceDTO.setPrice(appointment.getService().getPrice());
        dto.setService(serviceDTO);
        
        return dto;
    }

    @GetMapping("/pet/{petId}")
    public List<Appointment> getAppointmentsByPetId(
            @PathVariable Long petId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return appointmentService.getAppointmentsByPetId(petId, user);
    }

    @GetMapping("/between")
    public List<Appointment> getAppointmentsBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return appointmentService.getAppointmentsBetweenDates(start, end, user);
    }

    @PostMapping
    public ResponseEntity<AppointmentDTO> scheduleAppointment(
            @RequestBody AppointmentDTO appointmentDetails,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Convert DTO to entity
            Appointment appointment = new Appointment();
            appointment.setPet(petRepository.getReferenceById(appointmentDetails.getPet().getId()));
            appointment.setService(serviceRepository.getReferenceById(appointmentDetails.getService().getId()));
            appointment.setDateTime(appointmentDetails.getDateTime());
            appointment.setStatus(AppointmentStatus.valueOf(appointmentDetails.getStatus()));
            
            Appointment created = appointmentService.scheduleAppointment(appointment, user);
            return ResponseEntity.ok(convertToDTO(created));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<AppointmentDTO> updateAppointment(
            @PathVariable Long id,
            @RequestBody AppointmentDTO appointmentDetails,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        try {
            User user = userRepository.findById(currentUser.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
                
            // Convert DTO to entity
            Appointment appointment = new Appointment();
            appointment.setId(appointmentDetails.getId());
            appointment.setPet(petRepository.getReferenceById(appointmentDetails.getPet().getId()));
            appointment.setService(serviceRepository.getReferenceById(appointmentDetails.getService().getId()));
            appointment.setDateTime(appointmentDetails.getDateTime());
            appointment.setStatus(AppointmentStatus.valueOf(appointmentDetails.getStatus()));
            
            Appointment updated = appointmentService.updateAppointment(appointment, user);
            return ResponseEntity.ok(convertToDTO(updated));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
} 