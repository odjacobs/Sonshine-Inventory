package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.CategoryRepository;
import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.entities.Category;
import net.disc0.sonshine_inventory.entities.Item;
import net.disc0.sonshine_inventory.entities.Pledge;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final PledgeRepository pledgeRepository;

    public HomeController(ItemRepository itemRepository, CategoryRepository categoryRepository, PledgeRepository pledgeRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.pledgeRepository = pledgeRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(Category::getActive)
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .toList();

        List<Item> items = itemRepository.findAll().stream()
                .filter(Item::getActive)
                .toList();

        Map<Integer, List<Item>> itemsByCategory = items.stream()
                .collect(Collectors.groupingBy(Item::getCategoryId));

        Map<Integer, Integer> pledgedByItem = pledgeRepository.findAll().stream()
                .filter(pledge -> pledge.getStatus() == Pledge.PledgeStatus.OPEN)
                .collect(Collectors.groupingBy(Pledge::getItemId, Collectors.summingInt(Pledge::getQuantity)));

        Map<Integer, Integer> remainingByItem = new HashMap<>();
        Map<Integer, Integer> stockPctByItem = new HashMap<>();
        Map<Integer, Integer> pledgedPctByItem = new HashMap<>();
        Map<Integer, Integer> remainingPctByItem = new HashMap<>();

        for (Item item : items) {
            Integer quota = item.getQuota();
            int inStock = Math.max(0, item.getQuantity() == null ? 0 : item.getQuantity());
            int pledged = Math.max(0, pledgedByItem.getOrDefault(item.getId(), 0));
            if (quota != null && quota > 0) {
                int remaining = Math.max(0, quota - inStock - pledged);
                int cappedStock = Math.min(inStock, quota);
                int cappedPledged = Math.clamp(quota - cappedStock, 0, pledged);
                int stockPct = (int) Math.round((cappedStock * 100.0) / quota);
                int pledgedPct = (int) Math.round((cappedPledged * 100.0) / quota);
                int remainingPct = Math.max(0, 100 - stockPct - pledgedPct);

                remainingByItem.put(item.getId(), remaining);
                stockPctByItem.put(item.getId(), stockPct);
                pledgedPctByItem.put(item.getId(), pledgedPct);
                remainingPctByItem.put(item.getId(), remainingPct);
            }
        }

        model.addAttribute("categories", categories);
        model.addAttribute("itemsByCategory", itemsByCategory);
        model.addAttribute("pledgedByItem", pledgedByItem);
        model.addAttribute("remainingByItem", remainingByItem);
        model.addAttribute("stockPctByItem", stockPctByItem);
        model.addAttribute("pledgedPctByItem", pledgedPctByItem);
        model.addAttribute("remainingPctByItem", remainingPctByItem);
        return "index";
    }
}
