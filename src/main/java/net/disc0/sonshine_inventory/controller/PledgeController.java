package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.entities.Item;
import net.disc0.sonshine_inventory.entities.Pledge;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        int remaining = calculateRemainingNeed(item, buildOpenPledgeTotals());
        Integer remainingOrNull = item.getQuota() == null ? null : remaining;
        model.addAttribute("item", item);
        model.addAttribute("need", remainingOrNull);
        return "pledge";
    }

    @GetMapping("/{publicId}")
    public String confirmation(@PathVariable String publicId, Model model) {
        Pledge pledge = pledgeRepository.findByPublicId(publicId).orElseThrow();
        Item item = itemRepository.findById(pledge.getItemId()).orElseThrow();
        Map<Integer, Integer> pledgedByItem = buildOpenPledgeTotals();
        int remaining = calculateRemainingNeed(item, pledgedByItem);

        model.addAttribute("pledge", pledge);
        model.addAttribute("item", item);
        model.addAttribute("need", item.getQuota() == null ? null : remaining);
        model.addAttribute("recommendations", buildRecommendations(pledgedByItem, item.getId()));
        return "pledge-confirmation";
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
        return "redirect:/pledge/" + pledge.getPublicId();
    }

    private Map<Integer, Integer> buildOpenPledgeTotals() {
        return pledgeRepository.findAll().stream()
                .filter(pledge -> pledge.getStatus() == Pledge.PledgeStatus.OPEN)
                .collect(Collectors.groupingBy(Pledge::getItemId, Collectors.summingInt(Pledge::getQuantity)));
    }

    private int calculateRemainingNeed(Item item, Map<Integer, Integer> pledgedByItem) {
        Integer quota = item.getQuota();
        if (quota == null || quota <= 0) {
            return 0;
        }
        int inStock = Math.max(0, item.getQuantity() == null ? 0 : item.getQuantity());
        int pledged = Math.max(0, pledgedByItem.getOrDefault(item.getId(), 0));
        return Math.max(0, quota - inStock - pledged);
    }

    private List<ItemRecommendation> buildRecommendations(Map<Integer, Integer> pledgedByItem, Integer excludeItemId) {
        return itemRepository.findAll().stream()
                .filter(Item::getActive)
                .filter(item -> !item.getId().equals(excludeItemId))
                .map(item -> new ItemRecommendation(item, calculateRemainingNeed(item, pledgedByItem)))
                .filter(recommendation -> recommendation.remainingNeed() > 0)
                .sorted(Comparator.comparingInt(ItemRecommendation::remainingNeed).reversed()
                        .thenComparing(recommendation -> recommendation.item().getName()))
                .limit(3)
                .toList();
    }

    private record ItemRecommendation(Item item, int remainingNeed) {}
}