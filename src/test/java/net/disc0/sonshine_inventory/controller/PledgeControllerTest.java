package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.ItemRepository;
import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.dao.UserRepository;
import net.disc0.sonshine_inventory.dao.CategoryRepository;
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

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(PledgeController.class)
@Import(SecurityConfig.class)
class PledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemRepository itemRepository;

    @MockitoBean
    private PledgeRepository pledgeRepository;

    @MockitoBean
    private CategoryRepository categoryRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsManager userDetailsManager;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private DataSource dataSource;

    private static Item item(Integer id, String name, Integer quantity, Integer quota) {
        Item it = new Item(1, name, "units", quantity, quota, true);
        it.setId(id);
        return it;
    }

    private static Pledge pledge(Integer id, Integer itemId, int qty,
                                 Pledge.PledgeStatus status, String publicId) {
        Pledge p = new Pledge(itemId, "Donor", "donor@example.com", qty, status,
                LocalDateTime.now().plusDays(7));
        p.setId(id);
        p.setPublicId(publicId);
        return p;
    }

    @Test
    void getPledgeFormReturnsItemAndNeed() throws Exception {
        when(itemRepository.findById(1)).thenReturn(Optional.of(item(1, "Beans", 5, 20)));
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/pledge").param("itemId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pledge"))
                .andExpect(model().attributeExists("item", "need"))
                // need = max(0, 20 - 5 - 0) = 15
                .andExpect(model().attribute("need", 15));
    }

    @Test
    void getPledgeFormNullQuotaProducesNullNeed() throws Exception {
        when(itemRepository.findById(1)).thenReturn(Optional.of(item(1, "Misc", 3, null)));
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(new ArrayList<>());

        mockMvc.perform(get("/pledge").param("itemId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("pledge"))
                .andExpect(model().attributeExists("item"))
                .andExpect(model().attribute("need", (Object) null));
    }

    @Test
    void postPledgeSaveCallsRepositoryAndRedirectsToPublicId() throws Exception {
        when(pledgeRepository.save(any(Pledge.class))).thenAnswer(invocation -> {
            Pledge p = invocation.getArgument(0);
            p.setPublicId("generated-public-id");
            return p;
        });

        mockMvc.perform(post("/pledge/save")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("itemId", "1")
                        .param("donorName", "Jane")
                        .param("donorContact", "jane@example.com")
                        .param("quantity", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/pledge/generated-public-id"));

        verify(pledgeRepository, times(1)).save(any(Pledge.class));
    }

    @Test
    void getPledgeConfirmationReturnsExpectedModel() throws Exception {
        Pledge p = pledge(100, 1, 4, Pledge.PledgeStatus.OPEN, "pub-100");
        when(pledgeRepository.findByPublicId("pub-100")).thenReturn(Optional.of(p));
        when(itemRepository.findById(1)).thenReturn(Optional.of(item(1, "Beans", 5, 20)));
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(new ArrayList<>(List.of(p)));
        when(itemRepository.findAll()).thenReturn(new ArrayList<>(List.of(
                item(1, "Beans", 5, 20),
                item(2, "Corn", 0, 10)
        )));

        mockMvc.perform(get("/pledge/pub-100"))
                .andExpect(status().isOk())
                .andExpect(view().name("pledge-confirmation"))
                .andExpect(model().attributeExists("pledge", "item", "need", "recommendations"));
    }

    @Test
    void getPledgeConfirmationNotFoundThrows() {
        when(pledgeRepository.findByPublicId("does-not-exist"))
                .thenReturn(Optional.empty());

        // The controller uses .orElseThrow(), which surfaces as a
        // ServletException wrapping NoSuchElementException in MockMvc.
        Exception ex = org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> mockMvc.perform(get("/pledge/does-not-exist")));
        Throwable cause = ex.getCause() == null ? ex : ex.getCause();
        org.junit.jupiter.api.Assertions.assertTrue(
                cause instanceof java.util.NoSuchElementException
                        || ex instanceof java.util.NoSuchElementException,
                "expected NoSuchElementException, got: " + ex);
    }
}
