package berlin.yuna.project.controller;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class WebControllerUnitTest {

    @Test
    public void runUnitTest() throws Exception {
        assertThat(2 * 2, is(4));
    }
}
