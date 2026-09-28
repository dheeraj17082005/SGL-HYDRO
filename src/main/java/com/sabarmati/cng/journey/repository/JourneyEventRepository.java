package com.sabarmati.cng.journey.repository;

import com.sabarmati.cng.journey.entity.JourneyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JourneyEventRepository extends JpaRepository<JourneyEvent, Long> {
    List<JourneyEvent> findByJourneyIdOrderByTimestampAsc(Long journeyId);
}
