package com.sabarmati.cng.security.config;

import com.sabarmati.cng.fueling.entity.BayStatus;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.security.entity.Role;
import com.sabarmati.cng.security.entity.User;
import com.sabarmati.cng.security.repository.UserRepository;
import com.sabarmati.cng.station.entity.Camera;
import com.sabarmati.cng.station.entity.CameraType;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.entity.StationZone;
import com.sabarmati.cng.station.entity.ZoneType;
import com.sabarmati.cng.station.repository.CameraRepository;
import com.sabarmati.cng.station.repository.StationRepository;
import com.sabarmati.cng.station.repository.StationZoneRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StationRepository stationRepository;
    private final FuelingBayRepository fuelingBayRepository;
    private final StationZoneRepository stationZoneRepository;
    private final CameraRepository cameraRepository;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      StationRepository stationRepository,
                      FuelingBayRepository fuelingBayRepository,
                      StationZoneRepository stationZoneRepository,
                      CameraRepository cameraRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.stationRepository = stationRepository;
        this.fuelingBayRepository = fuelingBayRepository;
        this.stationZoneRepository = stationZoneRepository;
        this.cameraRepository = cameraRepository;
    }

    @Override
    public void run(String... args) {
        seedUserIfAbsent("admin", "admin123", Role.ADMIN);
        seedUserIfAbsent("manager", "manager123", Role.STATION_MANAGER);
        seedUserIfAbsent("operator", "operator123", Role.STATION_OPERATOR);
        seedUserIfAbsent("compliance", "compliance123", Role.COMPLIANCE_OFFICER);
        seedUserIfAbsent("auditor", "auditor123", Role.AUDITOR);
        seedDefaultStationAndCamera();
        seedDefaultStationBays();
    }

    private void seedUserIfAbsent(String username, String password, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(role)
                    .active(true)
                    .build();
            userRepository.save(user);
        }
    }

    private void seedDefaultStationAndCamera() {
        if (stationRepository.count() == 0) {
            Station station = Station.builder()
                    .name("SGL Main CNG Station")
                    .code("SGL-STN-001")
                    .address("SG Highway, Ahmedabad, Gujarat")
                    .latitude(23.0225)
                    .longitude(72.5714)
                    .active(true)
                    .build();
            station = stationRepository.save(station);

            StationZone entryZone = StationZone.builder()
                    .station(station)
                    .zoneType(ZoneType.ENTRY)
                    .name("Entry Gate Zone")
                    .build();
            entryZone = stationZoneRepository.save(entryZone);

            Camera camera = Camera.builder()
                    .station(station)
                    .zone(entryZone)
                    .cameraIdentifier("CAM-ENTRY-01")
                    .cameraType(CameraType.ANPR)
                    .active(true)
                    .build();
            cameraRepository.save(camera);
        }
    }
    private void seedDefaultStationBays() {
        stationRepository.findAll().stream().findFirst().ifPresent(station -> {
            if (fuelingBayRepository.findByStationId(station.getId()).isEmpty()) {
                for (int bayNumber = 1; bayNumber <= 6; bayNumber++) {
                    fuelingBayRepository.save(FuelingBay.builder()
                            .station(station)
                            .bayNumber(bayNumber)
                            .status(BayStatus.AVAILABLE)
                            .build());
                }
            }
        });
    }

}
