package dev.cotai.eventtype;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventTypeRepository extends JpaRepository<EventType, String> {

    List<EventType> findAllByOrderByNameAsc();
}
