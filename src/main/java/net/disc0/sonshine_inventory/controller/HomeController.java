package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.CategoryRepository;
import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.entities.Item;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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
        Map<Integer, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));

        List<Item> items = itemRepository.findAll().stream()
                .filter(Item::getActive)
                .toList();

        model.addAttribute("items", items);
        model.addAttribute("categoryNames", categoryNames);
        return "index";
    }
}
