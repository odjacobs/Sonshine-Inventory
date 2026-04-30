package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.CategoryRepository;
import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.entities.Category;
import net.disc0.sonshine_inventory.entities.Item;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    public AdminController(CategoryRepository categoryRepository, ItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @GetMapping
    public String adminPage(Model model) {
        List<Category> categories = categoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .toList();

        Map<Integer, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<Item> items = itemRepository.findAll();

        model.addAttribute("categories", categories);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("items", items);
        return "admin";
    }

    @PostMapping("/categories")
    public String addCategory(@RequestParam String name) {
        int nextOrder = categoryRepository.findAll().stream()
                .mapToInt(c -> c.getDisplayOrder() == null ? 0 : c.getDisplayOrder())
                .max()
                .orElse(0) + 1;

        categoryRepository.save(new Category(name, true, nextOrder));
        return "redirect:/admin";
    }

    @PostMapping("/items")
    public String addItem(@RequestParam Integer categoryId,
                          @RequestParam String name,
                          @RequestParam(required = false) String unitLabel,
                          @RequestParam Integer quantity,
                          @RequestParam(required = false) Integer quota) {
        itemRepository.save(new Item(categoryId, name, unitLabel, quantity, quota, true));
        return "redirect:/admin";
    }
}
