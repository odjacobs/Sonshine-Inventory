package net.disc0.sonshine_inventory.controller;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
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

    // --- Endpoints ---

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
}
