package net.disc0.sonshine_inventory.dao;

import net.disc0.sonshine_inventory.entities.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Integer> {
}
