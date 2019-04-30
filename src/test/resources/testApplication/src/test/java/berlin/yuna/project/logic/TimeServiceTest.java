package berlin.yuna.project.logic;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

@SpringBootTest
public class TimeServiceTest {

    @Autowired
    private TimeService timeService;

    @Test
    public void getNextDayTest() {
        assertEquals(LocalDateTime.now().plusDays(1).getDayOfYear(), timeService.getNextDay().getDayOfYear());
    }
}

