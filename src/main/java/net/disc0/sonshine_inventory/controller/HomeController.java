package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.CategoryRepository;
import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.entities.Category;
import net.disc0.sonshine_inventory.entities.Item;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public HomeController(ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Category> categories = categoryRepository.findAll().stream()
                .filter(Category::getActive)
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .toList();

        Map<Integer, List<Item>> itemsByCategory = itemRepository.findAll().stream()
                .filter(Item::getActive)
                .collect(Collectors.groupingBy(Item::getCategoryId));

        model.addAttribute("categories", categories);
        model.addAttribute("itemsByCategory", itemsByCategory);
        return "index";
    }
}
