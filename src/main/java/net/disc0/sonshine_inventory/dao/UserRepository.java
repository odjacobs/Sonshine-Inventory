package net.disc0.sonshine_inventory.dao;

import net.disc0.sonshine_inventory.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {
}