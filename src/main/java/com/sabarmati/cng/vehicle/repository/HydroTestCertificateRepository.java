package com.sabarmati.cng.vehicle.repository;

import com.sabarmati.cng.vehicle.entity.HydroTestCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HydroTestCertificateRepository extends JpaRepository<HydroTestCertificate, Long> {
    Optional<HydroTestCertificate> findByVehicleId(Long vehicleId);
}
