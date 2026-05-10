package net.disc0.sonshine_inventory.controller;

import jakarta.servlet.http.HttpServletResponse;
import net.disc0.sonshine_inventory.dao.CategoryRepository;
import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.dao.UserRepository;
import net.disc0.sonshine_inventory.entities.Category;
import net.disc0.sonshine_inventory.entities.Item;
import net.disc0.sonshine_inventory.entities.Pledge;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CategoryRepository categoryRepository;
    private final ItemRepository itemRepository;
    private final PledgeRepository pledgeRepository;
    private final UserRepository userRepository;
    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;

    public AdminController(CategoryRepository categoryRepository,
                           ItemRepository itemRepository,
                           PledgeRepository pledgeRepository,
                           UserRepository userRepository,
                           UserDetailsManager userDetailsManager,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
        this.pledgeRepository = pledgeRepository;
        this.userRepository = userRepository;
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
    }

    // --- Form-binding classes for bulk save ---

    public static class AdminSaveRequest {
        private List<CategoryForm> categories = new ArrayList<>();
        private List<ItemForm> items = new ArrayList<>();
        private List<UserForm> users = new ArrayList<>();

        public List<CategoryForm> getCategories() { return categories; }
        public void setCategories(List<CategoryForm> categories) { this.categories = categories; }
        public List<ItemForm> getItems() { return items; }
        public void setItems(List<ItemForm> items) { this.items = items; }
        public List<UserForm> getUsers() { return users; }
        public void setUsers(List<UserForm> users) { this.users = users; }
    }

    public static class CategoryForm {
        private Integer id;
        private String name;
        private Integer displayOrder;
        private boolean active;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getDisplayOrder() { return displayOrder; }
        public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class UserForm {
        private String username;
        private String displayName;
        private boolean enabled;
        private String newPassword;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
    }

    public static class ItemForm {
        private Integer id;
        private Integer categoryId;
        private String name;
        private String unitLabel;
        private Integer quantity;
        private Integer quota;
        private boolean active;

        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        public Integer getCategoryId() { return categoryId; }
        public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUnitLabel() { return unitLabel; }
        public void setUnitLabel(String unitLabel) { this.unitLabel = unitLabel; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Integer getQuota() { return quota; }
        public void setQuota(Integer quota) { this.quota = quota; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    // --- Dashboard view-model ---

    public record CategorySummary(String categoryName,
                                  int quotaTotal,
                                  int stockTotal,
                                  int belowQuotaCount) {}

    // --- Endpoints ---

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<Item> activeItems = itemRepository.findAll().stream()
                .filter(it -> Boolean.TRUE.equals(it.getActive()))
                .toList();

        int totalItems = activeItems.size();

        int itemsBelowQuota = (int) activeItems.stream()
                .filter(it -> it.getQuota() != null
                        && (it.getQuantity() == null ? 0 : it.getQuantity()) < it.getQuota())
                .count();

        int itemsBelowQuotaPercent = totalItems == 0
                ? 0
                : (int) Math.round((itemsBelowQuota * 100.0) / totalItems);

        long openPledgesCount = pledgeRepository.countByStatus(Pledge.PledgeStatus.OPEN);

        int openPledgesUnits = pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN).stream()
                .mapToInt(p -> p.getQuantity() == null ? 0 : p.getQuantity())
                .sum();

        long expiredAwaitingReview = pledgeRepository.countByStatus(Pledge.PledgeStatus.EXPIRED);

        List<Category> categories = categoryRepository.findAll().stream()
                .sorted(Comparator.comparing(Category::getDisplayOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        Map<Integer, List<Item>> itemsByCategory = activeItems.stream()
                .filter(it -> it.getCategoryId() != null)
                .collect(Collectors.groupingBy(Item::getCategoryId));

        List<CategorySummary> categorySummary = new ArrayList<>();
        for (Category cat : categories) {
            List<Item> catItems = itemsByCategory.getOrDefault(cat.getId(), List.of());
            int quotaTotal = catItems.stream()
                    .mapToInt(it -> it.getQuota() == null ? 0 : it.getQuota())
                    .sum();
            int stockTotal = catItems.stream()
                    .mapToInt(it -> it.getQuantity() == null ? 0 : it.getQuantity())
                    .sum();
            int belowQuotaCount = (int) catItems.stream()
                    .filter(it -> it.getQuota() != null
                            && (it.getQuantity() == null ? 0 : it.getQuantity()) < it.getQuota())
                    .count();
            categorySummary.add(new CategorySummary(cat.getName(), quotaTotal, stockTotal, belowQuotaCount));
        }

        model.addAttribute("totalItems", totalItems);
        model.addAttribute("itemsBelowQuota", itemsBelowQuota);
        model.addAttribute("itemsBelowQuotaPercent", itemsBelowQuotaPercent);
        model.addAttribute("openPledgesCount", openPledgesCount);
        model.addAttribute("openPledgesUnits", openPledgesUnits);
        model.addAttribute("expiredAwaitingReview", expiredAwaitingReview);
        model.addAttribute("categorySummary", categorySummary);

        return "admin-dashboard";
    }

    @GetMapping
    public String adminPage(Model model,
                            @RequestParam(required = false, defaultValue = "OPEN") String pledgeStatus) {
        List<Category> categories = categoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparingInt(Category::getDisplayOrder))
                .toList();

        Map<Integer, String> categoryNames = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        List<Item> items = itemRepository.findAll();

        Map<Integer, String> itemNames = items.stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));

        List<Pledge> pledges;
        if ("ALL".equals(pledgeStatus)) {
            pledges = pledgeRepository.findAll();
        } else {
            Pledge.PledgeStatus status = Pledge.PledgeStatus.valueOf(pledgeStatus);
            pledges = pledgeRepository.findAll().stream()
                    .filter(p -> p.getStatus() == status)
                    .toList();
        }
        pledges = pledges.stream()
                .sorted(Comparator.comparing(Pledge::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<net.disc0.sonshine_inventory.entities.User> users = userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(net.disc0.sonshine_inventory.entities.User::getUsername))
                .toList();

        model.addAttribute("categories", categories);
        model.addAttribute("categoryNames", categoryNames);
        model.addAttribute("items", items);
        model.addAttribute("itemNames", itemNames);
        model.addAttribute("pledges", pledges);
        model.addAttribute("pledgeStatus", pledgeStatus);
        model.addAttribute("users", users);
        return "admin";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute AdminSaveRequest saveRequest) {
        if (saveRequest.getCategories() != null) {
            for (CategoryForm cf : saveRequest.getCategories()) {
                categoryRepository.findById(cf.getId()).ifPresent(cat -> {
                    cat.setName(cf.getName());
                    cat.setDisplayOrder(cf.getDisplayOrder());
                    cat.setActive(cf.isActive());
                    categoryRepository.save(cat);
                });
            }
        }
        if (saveRequest.getItems() != null) {
            for (ItemForm itemForm : saveRequest.getItems()) {
                itemRepository.findById(itemForm.getId()).ifPresent(existing -> {
                    existing.setCategoryId(itemForm.getCategoryId());
                    existing.setName(itemForm.getName());
                    existing.setUnitLabel(itemForm.getUnitLabel());
                    existing.setQuantity(itemForm.getQuantity());
                    existing.setQuota(itemForm.getQuota());
                    existing.setActive(itemForm.isActive());
                    itemRepository.save(existing);
                });
            }
        }
        if (saveRequest.getUsers() != null) {
            for (UserForm userForm : saveRequest.getUsers()) {
                userRepository.findById(userForm.getUsername()).ifPresent(existing -> {
                    existing.setDisplayName(userForm.getDisplayName());
                    existing.setEnabled(userForm.isEnabled());
                    if (userForm.getNewPassword() != null && !userForm.getNewPassword().isBlank()) {
                        existing.setPassword(passwordEncoder.encode(userForm.getNewPassword()));
                    }
                    userRepository.save(existing);
                });
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/pledges/{id}/fulfill")
    public String fulfillPledge(@PathVariable Integer id,
                                @RequestParam(required = false, defaultValue = "OPEN") String pledgeStatus) {
        pledgeRepository.findById(id).ifPresent(pledge -> {
            pledge.setStatus(Pledge.PledgeStatus.FULFILLED);
            pledge.setFulfilledAt(LocalDateTime.now());
            pledgeRepository.save(pledge);
            itemRepository.findById(pledge.getItemId()).ifPresent(item -> {
                item.setQuantity(item.getQuantity() + pledge.getQuantity());
                itemRepository.save(item);
            });
        });
        return "redirect:/admin?pledgeStatus=" + pledgeStatus;
    }

    @PostMapping("/pledges/{id}/cancel")
    public String cancelPledge(@PathVariable Integer id,
                               @RequestParam(required = false, defaultValue = "OPEN") String pledgeStatus) {
        pledgeRepository.findById(id).ifPresent(pledge -> {
            pledge.setStatus(Pledge.PledgeStatus.CANCELLED);
            pledgeRepository.save(pledge);
        });
        return "redirect:/admin?pledgeStatus=" + pledgeStatus;
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

    @GetMapping("/users/register")
    public String registerPage(@RequestParam(required = false) String success, Model model) {
        if (success != null) {
            model.addAttribute("success", "Admin user created successfully.");
        }
        return "admin-register";
    }

    @PostMapping("/users/register")
    public String registerAdmin(@RequestParam String username,
                                @RequestParam String displayName,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model) {
        if (username.isBlank() || displayName.isBlank() || password.isBlank()) {
            model.addAttribute("error", "All fields are required.");
            model.addAttribute("username", username);
            model.addAttribute("displayName", displayName);
            return "admin-register";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Passwords do not match.");
            model.addAttribute("username", username);
            model.addAttribute("displayName", displayName);
            return "admin-register";
        }

        if (userDetailsManager.userExists(username)) {
            model.addAttribute("error", "Username \"" + username + "\" is already taken.");
            model.addAttribute("displayName", displayName);
            return "admin-register";
        }

        UserDetails newUser = User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles("ADMIN")
                .build();
        userDetailsManager.createUser(newUser);

        userRepository.findById(username).ifPresent(u -> {
            u.setDisplayName(displayName);
            userRepository.save(u);
        });

        return "redirect:/admin/users/register?success";
    }

    // --- CSV exports ---

    @GetMapping("/export/pledges.csv")
    public void exportPledgesCsv(HttpServletResponse response) throws IOException {
        String filename = "pledges-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv; charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        Map<Integer, String> itemNames = itemRepository.findAll().stream()
                .collect(Collectors.toMap(Item::getId, Item::getName));

        List<Pledge> pledges = pledgeRepository.findAll().stream()
                .sorted(Comparator.comparing(Pledge::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        PrintWriter writer = response.getWriter();
        writer.write("id,public_id,item_id,item_name,donor_name,donor_contact,quantity,status,created_at,expires_at,fulfilled_at\r\n");
        for (Pledge p : pledges) {
            String itemName = itemNames.getOrDefault(p.getItemId(), "");
            String row = String.join(",",
                    csvField(p.getId()),
                    csvField(p.getPublicId()),
                    csvField(p.getItemId()),
                    csvField(itemName),
                    csvField(p.getDonorName()),
                    csvField(p.getDonorContact()),
                    csvField(p.getQuantity()),
                    csvField(p.getStatus()),
                    csvField(p.getCreatedAt()),
                    csvField(p.getExpiresAt()),
                    csvField(p.getFulfilledAt())
            );
            writer.write(row);
            writer.write("\r\n");
        }
        writer.flush();
    }

    @GetMapping("/export/inventory.csv")
    public void exportInventoryCsv(HttpServletResponse response) throws IOException {
        String filename = "inventory-" + LocalDate.now() + ".csv";
        response.setContentType("text/csv; charset=utf-8");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        Map<Integer, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        Map<Integer, Integer> openPledgedByItem = new HashMap<>();
        for (Pledge p : pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN)) {
            openPledgedByItem.merge(p.getItemId(),
                    p.getQuantity() == null ? 0 : p.getQuantity(),
                    Integer::sum);
        }

        List<Item> items = itemRepository.findAll();

        PrintWriter writer = response.getWriter();
        writer.write("id,category_id,category_name,name,unit_label,quantity,quota,need,open_pledged,remaining_need,active\r\n");
        for (Item item : items) {
            Integer quota = item.getQuota();
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            int openPledged = openPledgedByItem.getOrDefault(item.getId(), 0);

            String needStr;
            String remainingStr;
            if (quota != null) {
                int need = Math.max(0, quota - quantity);
                int remaining = Math.max(0, need - openPledged);
                needStr = csvField(need);
                remainingStr = csvField(remaining);
            } else {
                needStr = "";
                remainingStr = "";
            }

            String catName = item.getCategoryId() == null
                    ? ""
                    : categoryNames.getOrDefault(item.getCategoryId(), "");

            String row = String.join(",",
                    csvField(item.getId()),
                    csvField(item.getCategoryId()),
                    csvField(catName),
                    csvField(item.getName()),
                    csvField(item.getUnitLabel()),
                    csvField(item.getQuantity()),
                    csvField(item.getQuota()),
                    needStr,
                    csvField(openPledged),
                    remainingStr,
                    csvField(item.getActive())
            );
            writer.write(row);
            writer.write("\r\n");
        }
        writer.flush();
    }

    static String csvField(Object value) {
        if (value == null) return "";
        String s = value.toString();
        boolean needsQuoting = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (needsQuoting) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }
}
