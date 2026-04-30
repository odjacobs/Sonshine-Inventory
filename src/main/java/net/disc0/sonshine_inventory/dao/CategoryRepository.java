package net.disc0.sonshine_inventory.dao;

import net.disc0.sonshine_inventory.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
