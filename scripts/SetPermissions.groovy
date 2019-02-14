import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHUser
@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
import org.kohsuke.github.GitHub

String name = System.getenv('GITHUB_USER')
String token = System.getenv('GITHUB_TOKEN')

GitHub github = GitHub.connect(name, token)
GHUser imagoStorm = github.getUser('imago-storm')
GHUser valodya = github.getUser('justnoxx')
github.getOrganization('electric-cloud').getRepositories().each { repoName, repo ->
    if (repoName.startsWith('EC-') || repoName.startsWith('ECSCM-')) {
        try {
            repo.addCollaborators(imagoStorm, valodya)
            println "Added collaborators to ${repoName}"
        } catch (Throwable e) {
            println "Failed to add collaborators to $repoName: ${e.message}"
        }
    }
}


