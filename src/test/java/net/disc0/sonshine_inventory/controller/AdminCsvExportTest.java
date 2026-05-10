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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminCsvExportTest {

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

    // SecurityConfig declares a UserDetailsManager bean depending on DataSource;
    // we mock it so the @Import works in the slice test.
    @MockitoBean
    private DataSource dataSource;

    private static Pledge buildPledge(Integer id, Integer itemId, String donorName,
                                      String donorContact, Integer qty,
                                      Pledge.PledgeStatus status,
                                      LocalDateTime createdAt,
                                      LocalDateTime expiresAt,
                                      LocalDateTime fulfilledAt,
                                      String publicId) {
        Pledge p = new Pledge(itemId, donorName, donorContact, qty, status, expiresAt);
        p.setId(id);
        p.setPublicId(publicId);
        p.setCreatedAt(createdAt);
        p.setFulfilledAt(fulfilledAt);
        return p;
    }

    private static Item buildItem(Integer id, Integer categoryId, String name,
                                  String unit, Integer quantity, Integer quota,
                                  Boolean active) {
        Item it = new Item(categoryId, name, unit, quantity, quota, active);
        it.setId(id);
        return it;
    }

    private static Category buildCategory(Integer id, String name) {
        Category c = new Category(name, true, 1);
        c.setId(id);
        return c;
    }

    @Test
    @WithMockUser
    void pledgesCsvExport() throws Exception {
        Item item = buildItem(10, 1, "Canned Beans, Refried", "cans", 5, 20, true);
        when(itemRepository.findAll()).thenReturn(List.of(item));

        Pledge open = buildPledge(100, 10, "Jane Doe", "jane@example.com",
                3, Pledge.PledgeStatus.OPEN,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 8, 9, 0),
                null,
                "pub-100");
        Pledge fulfilled = buildPledge(101, 10, "John, Jr.", "555-1234",
                2, Pledge.PledgeStatus.FULFILLED,
                LocalDateTime.of(2026, 5, 2, 10, 0),
                LocalDateTime.of(2026, 5, 9, 10, 0),
                LocalDateTime.of(2026, 5, 3, 11, 0),
                "pub-101");
        when(pledgeRepository.findAll()).thenReturn(List.of(open, fulfilled));

        MvcResult result = mockMvc.perform(get("/admin/export/pledges.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        startsWith("attachment; filename=\"pledges-")))
                .andExpect(header().string("Content-Disposition",
                        containsString(".csv\"")))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String[] lines = body.split("\r\n");

        assertEquals(
                "id,public_id,item_id,item_name,donor_name,donor_contact,quantity,status,created_at,expires_at,fulfilled_at",
                lines[0]);

        // Sorted by createdAt DESC -> fulfilled (2026-05-02) before open (2026-05-01)
        assertTrue(lines[1].startsWith("101,pub-101,10,"),
                "expected fulfilled pledge first, got: " + lines[1]);
        // donor name with comma must be quoted
        assertTrue(lines[1].contains("\"John, Jr.\""),
                "expected quoted donor name with comma, got: " + lines[1]);
        assertTrue(lines[1].contains("FULFILLED"));
        assertTrue(lines[1].contains("2026-05-03T11:00"),
                "expected fulfilled_at populated, got: " + lines[1]);

        assertTrue(lines[2].startsWith("100,pub-100,10,"),
                "expected open pledge second, got: " + lines[2]);
        // null fulfilled_at -> blank trailing field
        assertTrue(lines[2].endsWith(","),
                "expected blank fulfilled_at, got: " + lines[2]);
    }

    @Test
    @WithMockUser
    void inventoryCsvExport() throws Exception {
        Category cat = buildCategory(1, "Canned Goods");
        when(categoryRepository.findAll()).thenReturn(List.of(cat));

        Item withQuota = buildItem(10, 1, "Green Beans", "cans", 5, 20, true);
        Item noQuota = buildItem(11, 1, "Misc", "boxes", 3, null, false);
        when(itemRepository.findAll()).thenReturn(List.of(withQuota, noQuota));

        Pledge openPledge = buildPledge(100, 10, "Jane", "jane@example.com",
                4, Pledge.PledgeStatus.OPEN,
                LocalDateTime.of(2026, 5, 1, 9, 0),
                LocalDateTime.of(2026, 5, 8, 9, 0),
                null, "pub-100");
        when(pledgeRepository.findByStatus(Pledge.PledgeStatus.OPEN))
                .thenReturn(List.of(openPledge));

        MvcResult result = mockMvc.perform(get("/admin/export/inventory.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        startsWith("attachment; filename=\"inventory-")))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        String[] lines = body.split("\r\n");

        assertEquals(
                "id,category_id,category_name,name,unit_label,quantity,quota,need,open_pledged,remaining_need,active",
                lines[0]);

        // need = max(0, 20-5) = 15; open_pledged = 4; remaining_need = 11
        assertEquals("10,1,Canned Goods,Green Beans,cans,5,20,15,4,11,true", lines[1]);

        // null quota -> need and remaining_need blank; open_pledged 0
        assertEquals("11,1,Canned Goods,Misc,boxes,3,,,0,,false", lines[2]);
    }

    @Test
    void exportsRequireAuth() throws Exception {
        int statusCode = mockMvc.perform(get("/admin/export/pledges.csv"))
                .andReturn().getResponse().getStatus();
        assertTrue(statusCode == 401 || statusCode == 302,
                "expected 401 or 302, got: " + statusCode);

        int statusCode2 = mockMvc.perform(get("/admin/export/inventory.csv"))
                .andReturn().getResponse().getStatus();
        assertTrue(statusCode2 == 401 || statusCode2 == 302,
                "expected 401 or 302, got: " + statusCode2);
    }
}
