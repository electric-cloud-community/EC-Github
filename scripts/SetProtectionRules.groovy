@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
import org.kohsuke.github.GitHub

String name = 'imago-storm'
String token = System.getenv('GITHUB_TOKEN')


GitHub github = GitHub.connect(name, token)
github.
github.getOrganization('electric-cloud').getRepositories().each { repoName, repo ->
    if (repoName.startsWith('EC-') || repoName.startsWith('ECSCM-')) {
        def master = repo.getBranch('master')
        try {
            master.enableProtection().requiredReviewers(2).enable()
            println "Enabled protection for ${repo.name} branch ${master.name}"
        } catch (Throwable e) {
            println "Failed to enable protection for master: ${e.message}"
        }

        try {
            repo.getBranch('release').enableProtection().requiredReviewers(1).enable()
        } catch (Throwable e) {
            println "Failed to enable protection for release: ${e.message}"
        }
    }
}

