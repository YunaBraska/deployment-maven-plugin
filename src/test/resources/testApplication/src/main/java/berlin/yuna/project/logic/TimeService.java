package berlin.yuna.project.logic;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

@Service
public class TimeService {

    /**
     * This prints the timestamp at every second hour
     */
    @Scheduled(cron = "0 0 0/2 * * ?")
    public void printTimestamp() {
        System.out.println(new Date().getTime());
    }

    public LocalDateTime getPreviousDay() {
        return LocalDateTime.now().minusDays(1);
    }

    public LocalDateTime getNextDay() {
        return LocalDateTime.now().plusDays(1);
    }
}
