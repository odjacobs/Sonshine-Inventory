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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminDashboardTest {

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

    private static Category buildCategory(Integer id, String name, Integer displayOrder) {
        Category c = new Category(name, true, displayOrder);
        c.setId(id);
        return c;
    }

    private static Item buildItem(Integer id, Integer categoryId, String name,
                                  Integer quantity, Integer quota, Boolean active) {
        Item it = new Item(categoryId, name, "units", quantity, quota, active);
        it.setId(id);
        return it;
    }

    private static Pledge buildPledge(Integer id, Integer itemId, Integer qty,
                                      Pledge.PledgeStatus status) {
        Pledge p = new Pledge(itemId, "Donor", "donor@example.com", qty, status,
                LocalDateTime.of(2026, 5, 15, 12, 0));
        p.setId(id);
        p.setCreatedAt(LocalDateTime.of(2026, 5, 8, 12, 0));
        p.setPublicId("pub-" + id);
        return p;
    }

    @Test
    @WithMockUser
    void dashboardRendersWithExpectedModelAttributes() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(itemRepository.findAll()).thenReturn(List.of());
        when(pledgeRepository.countByStatus(Pledge.PledgeStatus.OPEN)).thenReturn(0L);
        when(pledgeRepository.countByStatus(Pledge.PledgeStatus.EXPIRED)).thenReturn(0L);
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN)).thenReturn(List.of());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attributeExists(
                        "totalItems",
                        "itemsBelowQuota",
                        "itemsBelowQuotaPercent",
                        "openPledgesCount",
                        "openPledgesUnits",
                        "expiredAwaitingReview",
                        "categorySummary"))
                .andExpect(model().attribute("totalItems", 0))
                .andExpect(model().attribute("itemsBelowQuota", 0))
                .andExpect(model().attribute("itemsBelowQuotaPercent", 0))
                .andExpect(model().attribute("openPledgesCount", 0L))
                .andExpect(model().attribute("openPledgesUnits", 0))
                .andExpect(model().attribute("expiredAwaitingReview", 0L));
    }

    @Test
    @WithMockUser
    void dashboardComputesMetricsCorrectly() throws Exception {
        Category cannedGoods = buildCategory(1, "Canned Goods", 1);
        Category bread = buildCategory(2, "Bread", 2);
        when(categoryRepository.findAll()).thenReturn(List.of(bread, cannedGoods));

        // Below quota: quantity 5 < quota 20
        Item belowQuota = buildItem(10, 1, "Green Beans", 5, 20, true);
        // At quota: quantity 10 == quota 10 (NOT below)
        Item atQuota = buildItem(11, 1, "Corn", 10, 10, true);
        // Null quota: ignored for below-quota count, contributes stock only
        Item noQuota = buildItem(12, 2, "Sourdough", 3, null, true);
        when(itemRepository.findAll()).thenReturn(List.of(belowQuota, atQuota, noQuota));

        when(pledgeRepository.countByStatus(Pledge.PledgeStatus.OPEN)).thenReturn(2L);
        when(pledgeRepository.countByStatus(Pledge.PledgeStatus.EXPIRED)).thenReturn(1L);

        Pledge open1 = buildPledge(100, 10, 4, Pledge.PledgeStatus.OPEN);
        Pledge open2 = buildPledge(101, 11, 7, Pledge.PledgeStatus.OPEN);
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(List.of(open1, open2));

        MvcResult result = mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-dashboard"))
                .andExpect(model().attribute("totalItems", 3))
                .andExpect(model().attribute("itemsBelowQuota", 1))
                // 1 of 3 = 33.33 -> rounds half-up to 33
                .andExpect(model().attribute("itemsBelowQuotaPercent", 33))
                .andExpect(model().attribute("openPledgesCount", 2L))
                .andExpect(model().attribute("openPledgesUnits", 11))
                .andExpect(model().attribute("expiredAwaitingReview", 1L))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<AdminController.CategorySummary> summary =
                (List<AdminController.CategorySummary>)
                        result.getModelAndView().getModel().get("categorySummary");
        assertNotNull(summary);
        assertEquals(2, summary.size());

        // Sorted by displayOrder ASC: Canned Goods (1) then Bread (2)
        AdminController.CategorySummary canned = summary.get(0);
        assertEquals("Canned Goods", canned.categoryName());
        // 20 + 10
        assertEquals(30, canned.quotaTotal());
        // 5 + 10
        assertEquals(15, canned.stockTotal());
        // only Green Beans is below quota
        assertEquals(1, canned.belowQuotaCount());

        AdminController.CategorySummary breadRow = summary.get(1);
        assertEquals("Bread", breadRow.categoryName());
        // null quota -> 0
        assertEquals(0, breadRow.quotaTotal());
        assertEquals(3, breadRow.stockTotal());
        assertEquals(0, breadRow.belowQuotaCount());
    }

    @Test
    void dashboardRequiresAuth() throws Exception {
        int statusCode = mockMvc.perform(get("/admin/dashboard"))
                .andReturn().getResponse().getStatus();
        assertTrue(statusCode == 401 || statusCode == 302,
                "expected 401 or 302, got: " + statusCode);
    }
}
