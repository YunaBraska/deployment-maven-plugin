package berlin.yuna.project.logic;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TimeService {

    LocalDateTime getPreviousDay() {
        return LocalDateTime.now().minusDays(1);
    }

    LocalDateTime getNextDay() {
        return LocalDateTime.now().plusDays(1);
    }
}
