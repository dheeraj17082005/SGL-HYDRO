package com.sabarmati.cng.station.service;

import com.sabarmati.cng.common.exception.DuplicateResourceException;
import com.sabarmati.cng.common.exception.ResourceNotFoundException;
import com.sabarmati.cng.station.dto.CameraDto;
import com.sabarmati.cng.station.dto.StationRequest;
import com.sabarmati.cng.station.dto.StationResponse;
import com.sabarmati.cng.station.dto.StationZoneDto;
import com.sabarmati.cng.fueling.entity.FuelingBay;
import com.sabarmati.cng.fueling.repository.FuelingBayRepository;
import com.sabarmati.cng.station.dto.FuelingBayResponse;
import com.sabarmati.cng.station.entity.Camera;
import com.sabarmati.cng.station.entity.Station;
import com.sabarmati.cng.station.entity.StationZone;
import com.sabarmati.cng.station.repository.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StationService {

    private final StationRepository stationRepository;
    private final FuelingBayRepository fuelingBayRepository;

    public StationService(StationRepository stationRepository, FuelingBayRepository fuelingBayRepository) {
        this.stationRepository = stationRepository;
        this.fuelingBayRepository = fuelingBayRepository;
    }

    @Transactional(readOnly = true)
    public List<StationResponse> getAllStations() {
        return stationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StationResponse getStationById(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station not found with id: " + id));
        return mapToResponse(station);
    }

    @Transactional(readOnly = true)
    public List<FuelingBayResponse> getStationBays(Long stationId) {
        if (!stationRepository.existsById(stationId)) {
            throw new ResourceNotFoundException("Station not found with id: " + stationId);
        }
        return fuelingBayRepository.findByStationId(stationId).stream()
                .sorted(java.util.Comparator.comparing(FuelingBay::getBayNumber))
                .map(bay -> new FuelingBayResponse(bay.getId(), bay.getBayNumber(), bay.getStatus().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public StationResponse createStation(StationRequest request) {
        if (stationRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Station code already exists: " + request.getCode());
        }

        Station station = Station.builder()
                .name(request.getName())
                .code(request.getCode())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        if (request.getZones() != null) {
            List<StationZone> zones = request.getZones().stream()
                    .map(z -> StationZone.builder()
                            .station(station)
                            .zoneType(z.getZoneType())
                            .name(z.getName())
                            .build())
                    .collect(Collectors.toList());
            station.setZones(zones);
        }

        if (request.getCameras() != null) {
            List<Camera> cameras = request.getCameras().stream()
                    .map(c -> Camera.builder()
                            .station(station)
                            .cameraType(c.getCameraType())
                            .cameraIdentifier(c.getCameraIdentifier())
                            .active(c.getActive() != null ? c.getActive() : true)
                            .build())
                    .collect(Collectors.toList());
            station.setCameras(cameras);
        }

        Station savedStation = stationRepository.save(station);
        return mapToResponse(savedStation);
    }

    private StationResponse mapToResponse(Station station) {
        List<StationZoneDto> zoneDtos = station.getZones() != null ?
                station.getZones().stream()
                        .map(z -> StationZoneDto.builder()
                                .id(z.getId())
                                .zoneType(z.getZoneType())
                                .name(z.getName())
                                .build())
                        .collect(Collectors.toList()) : Collections.emptyList();

        List<CameraDto> cameraDtos = station.getCameras() != null ?
                station.getCameras().stream()
                        .map(c -> CameraDto.builder()
                                .id(c.getId())
                                .zoneId(c.getZone() != null ? c.getZone().getId() : null)
                                .cameraType(c.getCameraType())
                                .cameraIdentifier(c.getCameraIdentifier())
                                .active(c.getActive())
                                .build())
                        .collect(Collectors.toList()) : Collections.emptyList();

        return StationResponse.builder()
                .id(station.getId())
                .name(station.getName())
                .code(station.getCode())
                .address(station.getAddress())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .active(station.getActive())
                .zones(zoneDtos)
                .cameras(cameraDtos)
                .build();
    }
}
