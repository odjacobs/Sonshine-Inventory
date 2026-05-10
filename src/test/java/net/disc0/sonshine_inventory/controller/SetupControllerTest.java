package net.disc0.sonshine_inventory.controller;

import net.disc0.sonshine_inventory.dao.UserRepository;
import net.disc0.sonshine_inventory.entities.User;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

@WebMvcTest(SetupController.class)
@Import(SecurityConfig.class)
class SetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserDetailsManager userDetailsManager;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private DataSource dataSource;

    @Test
    void getSetupNoUsersReturnsSetupView() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        mockMvc.perform(get("/setup"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup"));
    }

    @Test
    void getSetupWhenUsersExistRedirectsToAdmin() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        mockMvc.perform(get("/setup"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    void postSetupMismatchedPasswordsReturnsSetupWithError() throws Exception {
        when(userRepository.count()).thenReturn(0L);

        mockMvc.perform(post("/setup")
                        .with(csrf())
                        .param("username", "admin")
                        .param("displayName", "Admin")
                        .param("password", "secret1")
                        .param("confirmPassword", "secret2"))
                .andExpect(status().isOk())
                .andExpect(view().name("setup"))
                .andExpect(model().attributeExists("error"));

        verify(userDetailsManager, never()).createUser(any());
    }

    @Test
    void postSetupValidInputCreatesUserAndRedirects() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");

        User existing = new User("admin", "encoded-secret", true, null);
        when(userRepository.findById("admin")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/setup")
                        .with(csrf())
                        .param("username", "admin")
                        .param("displayName", "Admin User")
                        .param("password", "secret")
                        .param("confirmPassword", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(userDetailsManager, times(1)).createUser(any());
        verify(userRepository, times(1)).save(existing);
        org.junit.jupiter.api.Assertions.assertEquals("Admin User", existing.getDisplayName());
    }

    @Test
    void postSetupWhenUsersExistBypassesCreation() throws Exception {
        when(userRepository.count()).thenReturn(1L);

        mockMvc.perform(post("/setup")
                        .with(csrf())
                        .param("username", "admin")
                        .param("displayName", "Admin")
                        .param("password", "secret")
                        .param("confirmPassword", "secret"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));

        verify(userDetailsManager, never()).createUser(any());
    }
}
