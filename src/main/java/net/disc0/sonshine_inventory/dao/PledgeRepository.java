package net.disc0.sonshine_inventory.dao;

import net.disc0.sonshine_inventory.entities.Pledge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PledgeRepository extends JpaRepository<Pledge, Integer> {
    Optional<Pledge> findByPublicId(String publicId);
}
