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
        try {
            GHBranch release = repo.getBranch('release')
            GHPullRequest request = repo.createPullRequest("Release", release.name, 'master', '')
            println "Created Pull Request for ${repo.name}"
            println "Mergeable: ${request.mergeable}"
            println "Pull Request: ${request.diffUrl}"
        } catch (Throwable e) {
            println "Failed to create PR for ${repoName}: ${e.message}"
        }
    }
}