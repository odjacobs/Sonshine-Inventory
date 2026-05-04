package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.entities.Item;
import net.disc0.sonshine_inventory.entities.Pledge;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/pledge")
public class PledgeController {

    private final ItemRepository itemRepository;
    private final PledgeRepository pledgeRepository;

    public PledgeController(ItemRepository itemRepository, PledgeRepository pledgeRepository) {
        this.itemRepository = itemRepository;
        this.pledgeRepository = pledgeRepository;
    }

    @GetMapping
    public String pledge(Model model, @RequestParam Integer itemId) {
        Item item = itemRepository.findById(itemId).orElseThrow();
        Integer need = null;
        if (item.getQuota() != null) {
            need = Math.max(0, item.getQuota() - item.getQuantity());
        }
        model.addAttribute("item", item);
        model.addAttribute("need", need);
        return "pledge";
    }

    @PostMapping("/save")
    public String savePledge(@RequestParam Integer itemId,
                             @RequestParam String donorName,
                             @RequestParam String donorContact,
                             @RequestParam Integer quantity) {
        Pledge pledge = new Pledge(
                itemId,
                donorName,
                donorContact,
                quantity,
                Pledge.PledgeStatus.OPEN,
                LocalDateTime.now().plusDays(7)
        );
        pledgeRepository.save(pledge);
        return "redirect:/";
    }
}