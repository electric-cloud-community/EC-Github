import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHBranch
@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
import org.kohsuke.github.GitHub

String name = 'imago-storm'
String token = System.getenv('GITHUB_TOKEN')

GitHub github = GitHub.connect(name, token)
github.getOrganization('electric-cloud').getRepositories().each { repoName, repo ->
    if (repoName.startsWith('EC-') || repoName.startsWith('ECSCM-')) {

        GHPullRequest release = repo.getPullRequests(GHIssueState.OPEN).find { it.title == 'Release' }
        if (release) {
            if (release.mergeable) {
                try {
                    release.merge("Merged automatically after release")
                    println "Merged ${release.htmlUrl}"
                } catch (Throwable e) {
                    println "Failed to merge ${release.htmlUrl}: ${e.message}"
                }
            }
        }
    }
}
