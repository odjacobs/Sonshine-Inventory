package net.disc0.sonshine_inventory.scheduler;

import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.entities.Pledge;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PledgeExpirationJobTest {

    @Test
    void expiresOpenPledgesPastTheirExpiration() {
        PledgeRepository repo = mock(PledgeRepository.class);

        Pledge expired1 = new Pledge(1, "Alice", "alice@example.com", 2,
                Pledge.PledgeStatus.OPEN, LocalDateTime.now().minusDays(1));
        Pledge expired2 = new Pledge(2, "Bob", "bob@example.com", 5,
                Pledge.PledgeStatus.OPEN, LocalDateTime.now().minusHours(2));

        when(repo.findByStatusAndExpiresAtBefore(eq(Pledge.PledgeStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of(expired1, expired2));

        PledgeExpirationJob job = new PledgeExpirationJob(repo);
        job.expireOpenPledges();

        assertThat(expired1.getStatus()).isEqualTo(Pledge.PledgeStatus.EXPIRED);
        assertThat(expired2.getStatus()).isEqualTo(Pledge.PledgeStatus.EXPIRED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Pledge>> captor = ArgumentCaptor.forClass(List.class);
        verify(repo, times(1)).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactly(expired1, expired2);
    }

    @Test
    void onlyModifiesPledgesReturnedByQuery() {
        PledgeRepository repo = mock(PledgeRepository.class);

        Pledge stillOpen = new Pledge(3, "Carol", "carol@example.com", 1,
                Pledge.PledgeStatus.OPEN, LocalDateTime.now().plusDays(3));
        Pledge expired = new Pledge(4, "Dave", "dave@example.com", 4,
                Pledge.PledgeStatus.OPEN, LocalDateTime.now().minusDays(2));

        // Repository simulates "before cutoff" filter — only returns expired one.
        when(repo.findByStatusAndExpiresAtBefore(eq(Pledge.PledgeStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(List.of(expired));

        PledgeExpirationJob job = new PledgeExpirationJob(repo);
        job.expireOpenPledges();

        assertThat(expired.getStatus()).isEqualTo(Pledge.PledgeStatus.EXPIRED);
        assertThat(stillOpen.getStatus()).isEqualTo(Pledge.PledgeStatus.OPEN);
    }

    @Test
    void emptyResultDoesNotCallSaveAll() {
        PledgeRepository repo = mock(PledgeRepository.class);
        when(repo.findByStatusAndExpiresAtBefore(eq(Pledge.PledgeStatus.OPEN), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        PledgeExpirationJob job = new PledgeExpirationJob(repo);
        job.expireOpenPledges();

        verify(repo, never()).saveAll(any());
    }
}
