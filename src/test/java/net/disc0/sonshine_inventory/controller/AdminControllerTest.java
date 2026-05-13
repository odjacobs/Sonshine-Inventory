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
import org.mockito.ArgumentCaptor;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

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

    private static Category cat(Integer id, String name, int order) {
        Category c = new Category(name, true, order);
        c.setId(id);
        return c;
    }

    private static Item item(Integer id, Integer categoryId, String name,
                             Integer quantity, Integer quota) {
        Item it = new Item(categoryId, name, "units", quantity, quota, true);
        it.setId(id);
        return it;
    }

    private static Pledge pledge(Integer id, Integer itemId, int qty,
                                 Pledge.PledgeStatus status) {
        Pledge p = new Pledge(itemId, "Donor", "donor@example.com", qty, status,
                LocalDateTime.now().plusDays(7));
        p.setId(id);
        p.setPublicId("pub-" + id);
        p.setCreatedAt(LocalDateTime.of(2026, 5, 8, 12, 0));
        return p;
    }

    @Test
    @WithMockUser
    void adminPageDefaultStatusIsOpen() throws Exception {
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                cat(1, "Canned Goods", 1)
        )));
        when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                item(10, 1, "Beans", 5, 20)
        )));
        when(pledgeRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                pledge(100, 10, 3, Pledge.PledgeStatus.OPEN),
                pledge(101, 10, 2, Pledge.PledgeStatus.FULFILLED)
        )));
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        MvcResult result = mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("categories", "items", "pledges", "users"))
                .andExpect(model().attribute("pledgeStatus", "OPEN"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Pledge> pledges = (List<Pledge>) result.getModelAndView().getModel().get("pledges");
        // Default OPEN: only the OPEN pledge
        assertEquals(1, pledges.size());
        assertEquals(Pledge.PledgeStatus.OPEN, pledges.get(0).getStatus());
    }

    @Test
    @WithMockUser
    void adminPageAllStatusReturnsEverything() throws Exception {
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());
        when(itemRepository.findAll()).thenReturn(new ArrayList<>());
        when(pledgeRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                pledge(100, 10, 3, Pledge.PledgeStatus.OPEN),
                pledge(101, 10, 2, Pledge.PledgeStatus.FULFILLED),
                pledge(102, 10, 1, Pledge.PledgeStatus.CANCELLED)
        )));
        when(userRepository.findAll()).thenReturn(new ArrayList<>());

        MvcResult result = mockMvc.perform(get("/admin").param("pledgeStatus", "ALL"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attribute("pledgeStatus", "ALL"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Pledge> pledges = (List<Pledge>) result.getModelAndView().getModel().get("pledges");
        assertEquals(3, pledges.size());
    }

    @Test
    @WithMockUser
    void fulfillPledgeUpdatesStatusAndIncrementsItemQuantity() throws Exception {
        Item it = item(10, 1, "Beans", 5, 20);
        Pledge p = pledge(100, 10, 7, Pledge.PledgeStatus.OPEN);

        when(pledgeRepository.findById(100)).thenReturn(Optional.of(p));
        when(itemRepository.findById(10)).thenReturn(Optional.of(it));

        mockMvc.perform(post("/admin/pledges/100/fulfill")
                        .with(csrf())
                        .param("pledgeStatus", "OPEN"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?pledgeStatus=OPEN"));

        assertEquals(Pledge.PledgeStatus.FULFILLED, p.getStatus());
        assertNotNull(p.getFulfilledAt());
        // quantity 5 + pledge 7 = 12
        assertEquals(12, it.getQuantity());

        verify(pledgeRepository, times(1)).save(p);
        verify(itemRepository, times(1)).save(it);
    }

    @Test
    @WithMockUser
    void cancelPledgeUpdatesStatusAndRedirects() throws Exception {
        Pledge p = pledge(100, 10, 7, Pledge.PledgeStatus.OPEN);
        when(pledgeRepository.findById(100)).thenReturn(Optional.of(p));

        mockMvc.perform(post("/admin/pledges/100/cancel")
                        .with(csrf())
                        .param("pledgeStatus", "ALL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin?pledgeStatus=ALL"));

        assertEquals(Pledge.PledgeStatus.CANCELLED, p.getStatus());
        verify(pledgeRepository, times(1)).save(p);
    }

    @Test
    @WithMockUser
    void addCategoryAssignsNextDisplayOrder() throws Exception {
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                cat(1, "First", 1),
                cat(2, "Second", 5)
        )));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", "Bread"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(categoryRepository).save(captor.capture());
        Category saved = captor.getValue();
        assertEquals("Bread", saved.getName());
        assertEquals(6, saved.getDisplayOrder());
        assertTrue(saved.getActive());
    }

    @Test
    @WithMockUser
    void addCategoryWhenEmptyAssignsOrderOne() throws Exception {
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", "Bread"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(categoryRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getDisplayOrder());
    }

    @Test
    @WithMockUser
    void addItemSavesActiveTrueAndRedirects() throws Exception {
        ArgumentCaptor<Item> captor = ArgumentCaptor.forClass(Item.class);

        mockMvc.perform(post("/admin/items")
                        .with(csrf())
                        .param("categoryId", "1")
                        .param("name", "Beans")
                        .param("quantity", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(itemRepository).save(captor.capture());
        Item saved = captor.getValue();
        assertEquals(1, saved.getCategoryId());
        assertEquals("Beans", saved.getName());
        assertEquals(5, saved.getQuantity());
        assertTrue(saved.getActive());
    }

    @Test
    void anonymousAccessToAdminIsBlocked() throws Exception {
        int statusCode = mockMvc.perform(get("/admin"))
                .andReturn().getResponse().getStatus();
        assertTrue(statusCode == 401 || statusCode == 302,
                "expected 401 or 302, got: " + statusCode);
    }

    // --- Fragment endpoint tests ---

    @Test
    @WithMockUser
    void pledgesFragmentDefaultStatusIsOpen() throws Exception {
        when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                item(10, 1, "Beans", 5, 20)
        )));
        when(pledgeRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                pledge(100, 10, 3, Pledge.PledgeStatus.OPEN),
                pledge(101, 10, 2, Pledge.PledgeStatus.FULFILLED)
        )));

        MvcResult result = mockMvc.perform(get("/admin/pledges/fragment"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pledgeStatus", "OPEN"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Pledge> pledges = (List<Pledge>) result.getModelAndView().getModel().get("pledges");
        assertEquals(1, pledges.size());
        assertEquals(Pledge.PledgeStatus.OPEN, pledges.get(0).getStatus());
    }

    @Test
    @WithMockUser
    void pledgesFragmentAllStatusReturnsEverything() throws Exception {
        when(itemRepository.findAll()).thenReturn(new ArrayList<>());
        when(pledgeRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                pledge(100, 10, 3, Pledge.PledgeStatus.OPEN),
                pledge(101, 10, 2, Pledge.PledgeStatus.FULFILLED),
                pledge(102, 10, 1, Pledge.PledgeStatus.CANCELLED)
        )));

        MvcResult result = mockMvc.perform(get("/admin/pledges/fragment").param("pledgeStatus", "ALL"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pledgeStatus", "ALL"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Pledge> pledges = (List<Pledge>) result.getModelAndView().getModel().get("pledges");
        assertEquals(3, pledges.size());
    }

    @Test
    @WithMockUser
    void pledgesFragmentFiltersToRequestedStatus() throws Exception {
        when(itemRepository.findAll()).thenReturn(new ArrayList<>());
        when(pledgeRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                pledge(100, 10, 3, Pledge.PledgeStatus.OPEN),
                pledge(101, 10, 2, Pledge.PledgeStatus.FULFILLED),
                pledge(102, 10, 1, Pledge.PledgeStatus.EXPIRED)
        )));

        MvcResult result = mockMvc.perform(get("/admin/pledges/fragment").param("pledgeStatus", "FULFILLED"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("pledgeStatus", "FULFILLED"))
                .andReturn();

        @SuppressWarnings("unchecked")
        List<Pledge> pledges = (List<Pledge>) result.getModelAndView().getModel().get("pledges");
        assertEquals(1, pledges.size());
        assertEquals(Pledge.PledgeStatus.FULFILLED, pledges.get(0).getStatus());
    }

    @Test
    @WithMockUser
    void pledgesFragmentExposesItemNamesInModel() throws Exception {
        when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                item(10, 1, "Beans", 5, 20),
                item(11, 1, "Rice", 3, 10)
        )));
        when(pledgeRepository.findAll()).thenReturn(new ArrayList<>());

        MvcResult result = mockMvc.perform(get("/admin/pledges/fragment"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        java.util.Map<Integer, String> itemNames =
                (java.util.Map<Integer, String>) result.getModelAndView().getModel().get("itemNames");
        assertNotNull(itemNames);
        assertEquals("Beans", itemNames.get(10));
        assertEquals("Rice", itemNames.get(11));
    }

    @Test
    void anonymousAccessToPledgesFragmentIsBlocked() throws Exception {
        int statusCode = mockMvc.perform(get("/admin/pledges/fragment"))
                .andReturn().getResponse().getStatus();
        assertTrue(statusCode == 401 || statusCode == 302,
                "expected 401 or 302, got: " + statusCode);
    }
}
