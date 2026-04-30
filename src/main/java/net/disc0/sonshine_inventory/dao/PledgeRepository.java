package net.disc0.sonshine_inventory.dao;

import net.disc0.sonshine_inventory.entities.Pledge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PledgeRepository extends JpaRepository<Pledge, Integer> {
}
