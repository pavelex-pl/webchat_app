package com.webchat.message;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReadMarkerRepository extends JpaRepository<ReadMarker, ReadMarkerId> {
}
