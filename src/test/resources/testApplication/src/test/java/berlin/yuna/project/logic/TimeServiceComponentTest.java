package berlin.yuna.project.logic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TimeServiceComponentTest {

    @Autowired
    private TimeService timeService;

    @Test
    public void getPreviousDayTest() {
        assertEquals(LocalDateTime.now().minusDays(1).getDayOfYear(), timeService.getPreviousDay().getDayOfYear());
    }
}
