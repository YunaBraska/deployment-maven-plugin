package berlin.yuna.mavendeploy.logic;

import berlin.yuna.mavendeploy.model.Logger;
import berlin.yuna.mavendeploy.plugin.PluginSession;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SemanticServiceTest {

    public PluginSession session = new PluginSession(null, new Logger());

    @Test
    public void changeOnlyNumberInVersion_ShouldBeSuccessful() {
        final String version = "1.2.34-SnapShot-4.rc";
        final SemanticService service = new SemanticService(session, null, null);
        final String result = service.increaseNumbersInString(version, true);
        assertThat(result, is(equalTo("2.3.35-SnapShot-5.rc")));
    }

    @Test
    public void getBranchName_WithBranch_ShouldBeSuccessful() {
        final GitService gitService = mock(GitService.class);
        when(gitService.getBranchNameRefLog()).thenReturn(Optional.of("currentBranch"));

        final SemanticService service = new SemanticService(session, gitService, null);
        assertThat(service.getBranchNameRefLog().isPresent(), is(true));
        assertThat(service.getBranchNameRefLog().get(), is(equalTo("currentBranch")));
    }

    @Test
    public void getBranchName_WithOutGitService_ShouldReturnNull() {
        final SemanticService service = new SemanticService(session, null, null);
        assertThat(service.getBranchNameRefLog().isPresent(), is(false));
    }

    @Test
    public void getNextSemanticVersion_withoutGitService_shouldReturnFallback() {
        final SemanticService service = new SemanticService(session, null, null);
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("failed"));
    }

    @Test
    public void getNextSemanticVersion_withoutBranch_shouldReturnFallback() {
        final GitService gitService = mock(GitService.class);
        when(gitService.getBranchNameRefLog()).thenReturn(Optional.of(""));

        final SemanticService service = new SemanticService(session, gitService, null);
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("failed"));
    }

    @Test
    public void getNextSemanticVersion_withNotMatchingSemanticVersion_shouldReturnFallback() {
        final GitService gitService = mock(GitService.class);
        when(gitService.getBranchNameRefLog()).thenReturn(Optional.of("Major-update"));

        final SemanticService service = new SemanticService(session, gitService, "[\\.]::none::none::none");
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("failed"));
    }

    @Test
    public void getNextSemanticVersion_withMatchingSemanticVersion_shouldReturnFallback() {
        final GitService gitService = mock(GitService.class);
        when(gitService.getBranchNameRefLog()).thenReturn(Optional.of("Major/update"));

        final SemanticService service = new SemanticService(session, gitService, "[\\.]::Major.*::none::none");
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("2.0.0"));
    }
}