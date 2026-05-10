package net.disc0.sonshine_inventory.dao;

import net.disc0.sonshine_inventory.entities.Pledge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PledgeRepository extends JpaRepository<Pledge, Integer> {
    Optional<Pledge> findByPublicId(String publicId);

    List<Pledge> findByStatus(Pledge.PledgeStatus status);

    List<Pledge> findByStatusAndExpiresAtBefore(Pledge.PledgeStatus status, LocalDateTime cutoff);

    long countByStatus(Pledge.PledgeStatus status);
}
