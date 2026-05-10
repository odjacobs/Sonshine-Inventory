package net.disc0.sonshine_inventory.scheduler;

import net.disc0.sonshine_inventory.dao.PledgeRepository;
import net.disc0.sonshine_inventory.entities.Pledge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PledgeExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(PledgeExpirationJob.class);

    private final PledgeRepository pledgeRepository;

    public PledgeExpirationJob(PledgeRepository pledgeRepository) {
        this.pledgeRepository = pledgeRepository;
    }

    @Scheduled(cron = "0 0 2 * * *", zone = "America/New_York")
    @Transactional
    public void expireOpenPledges() {
        List<Pledge> expired = pledgeRepository.findByStatusAndExpiresAtBefore(
                Pledge.PledgeStatus.OPEN, LocalDateTime.now());
        if (expired.isEmpty()) {
            log.info("Pledge expiration job: 0 pledges expired");
            return;
        }
        for (Pledge pledge : expired) {
            pledge.setStatus(Pledge.PledgeStatus.EXPIRED);
        }
        pledgeRepository.saveAll(expired);
        log.info("Pledge expiration job: {} pledges expired", expired.size());
    }
}
