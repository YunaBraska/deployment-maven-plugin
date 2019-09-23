package berlin.yuna.mavendeploy.logic;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SemanticServiceTest {

    @Test
    public void getBranchName_WithBranch_ShouldBeSuccessful() {
        final GitService gitService = mock(GitService.class);
        when(gitService.findOriginalBranchName()).thenReturn(Optional.of("currentBranch"));

        final SemanticService service = new SemanticService(gitService, null);
        assertThat(service.getBranchName().isPresent(), is(true));
        assertThat(service.getBranchName().get(), is(equalTo("currentBranch")));
    }

    @Test
    public void getBranchName_WithOutGitService_ShouldReturnNull() {
        final SemanticService service = new SemanticService(null, null);
        assertThat(service.getBranchName().isPresent(), is(false));
    }

    @Test
    public void getNextSemanticVersion_withoutGitService_shouldReturnFallback() {
        final SemanticService service = new SemanticService(null, null);
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("failed"));
    }

    @Test
    public void getNextSemanticVersion_withoutBranch_shouldReturnFallback() {
        final GitService gitService = mock(GitService.class);
        when(gitService.findOriginalBranchName()).thenReturn(Optional.of(""));

        final SemanticService service = new SemanticService(gitService, null);
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("failed"));
    }

    @Test
    public void getNextSemanticVersion_withNotMatchingSemanticVersion_shouldReturnFallback() {
        final GitService gitService = mock(GitService.class);
        when(gitService.findOriginalBranchName()).thenReturn(Optional.of("Major-update"));

        final SemanticService service = new SemanticService(gitService, "[\\.]::none::none::none");
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("failed"));
    }

    @Test
    public void getNextSemanticVersion_withMatchingSemanticVersion_shouldReturnFallback() {
        final GitService gitService = mock(GitService.class);
        when(gitService.findOriginalBranchName()).thenReturn(Optional.of("Major/update"));

        final SemanticService service = new SemanticService(gitService, "[\\.]::Major.*::none::none");
        final String output = service.getNextSemanticVersion("1.2.3", "failed");
        assertThat(output, is("2.0.0"));
    }
}