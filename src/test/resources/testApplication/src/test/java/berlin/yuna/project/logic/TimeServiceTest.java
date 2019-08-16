package berlin.yuna.project.logic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class TimeServiceTest {

    @Test
    public void getNextDayTest() {
        TimeService timeService = new TimeService();
        assertEquals(LocalDateTime.now().plusDays(1).getDayOfYear(), timeService.getNextDay().getDayOfYear());
    }
}

