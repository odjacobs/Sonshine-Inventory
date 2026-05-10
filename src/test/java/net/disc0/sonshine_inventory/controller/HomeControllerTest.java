package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.CategoryRepository;
import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.dao.UserRepository;
import net.disc0.sonshine_inventory.entities.Category;
import net.disc0.sonshine_inventory.entities.Item;
import net.disc0.sonshine_inventory.entities.Pledge;
import net.disc0.sonshine_inventory.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(HomeController.class)
@Import(SecurityConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private ItemRepository itemRepository;

    @MockitoBean
    private PledgeRepository pledgeRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsManager userDetailsManager;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private DataSource dataSource;

    private static Category cat(Integer id, String name, boolean active, int order) {
        Category c = new Category(name, active, order);
        c.setId(id);
        return c;
    }

    private static Item item(Integer id, Integer categoryId, String name,
                             Integer quantity, Integer quota, Boolean active) {
        Item it = new Item(categoryId, name, "units", quantity, quota, active);
        it.setId(id);
        return it;
    }

    private static Pledge pledge(Integer id, Integer itemId, int qty,
                                 Pledge.PledgeStatus status) {
        Pledge p = new Pledge(itemId, "Donor", "donor@example.com", qty, status,
                LocalDateTime.now().plusDays(7));
        p.setId(id);
        p.setPublicId("pub-" + id);
        return p;
    }

    @Test
    void homeRendersWithExpectedModelAttributes() throws Exception {
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                cat(1, "Canned Goods", true, 1)
        )));
        when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                item(10, 1, "Beans", 5, 20, true),
                item(11, 1, "Misc", 3, null, true)
        )));
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(
                        pledge(100, 10, 4, Pledge.PledgeStatus.OPEN)
                )));

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists(
                        "categories",
                        "itemsByCategory",
                        "pledgedByItem",
                        "remainingByItem",
                        "stockPctByItem",
                        "pledgedPctByItem",
                        "remainingPctByItem"))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<Integer, Integer> pledgedByItem = (Map<Integer, Integer>)
                result.getModelAndView().getModel().get("pledgedByItem");
        assertEquals(4, pledgedByItem.get(10));

        @SuppressWarnings("unchecked")
        Map<Integer, Integer> remainingByItem = (Map<Integer, Integer>)
                result.getModelAndView().getModel().get("remainingByItem");
        // quota 20, inStock 5, pledged 4 -> remaining = max(0, 20-5-4) = 11
        assertEquals(11, remainingByItem.get(10));
        // null quota -> not in remainingByItem
        assertFalse(remainingByItem.containsKey(11));
    }

    @Test
    void homeFiltersInactiveItemsAndCategories() throws Exception {
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                cat(1, "Active", true, 1),
                cat(2, "Inactive", false, 2)
        )));
        when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                item(10, 1, "ActiveItem", 5, 20, true),
                item(11, 1, "InactiveItem", 0, 10, false)
        )));
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(new ArrayList<>());

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Category> categories = (List<Category>)
                result.getModelAndView().getModel().get("categories");
        assertNotNull(categories);
        assertEquals(1, categories.size());
        assertEquals("Active", categories.get(0).getName());

        @SuppressWarnings("unchecked")
        Map<Integer, List<Item>> itemsByCategory = (Map<Integer, List<Item>>)
                result.getModelAndView().getModel().get("itemsByCategory");
        // Inactive item filtered out
        assertEquals(1, itemsByCategory.get(1).size());
        assertEquals("ActiveItem", itemsByCategory.get(1).get(0).getName());
    }

    @Test
    void homePermitsAnonymousAccess() throws Exception {
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());
        when(itemRepository.findAll()).thenReturn(new ArrayList<>());
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}
