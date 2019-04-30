package berlin.yuna.project.logic;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

@SpringBootTest
public class TimeServiceComponentTest {

    @Autowired
    private TimeService timeService;

    @Test
    public void getPreviousDayTest() {
        assertEquals(LocalDateTime.now().minusDays(1).getDayOfYear(), timeService.getPreviousDay().getDayOfYear());
    }
}
