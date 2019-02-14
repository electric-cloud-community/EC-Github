import org.kohsuke.github.GHOrganization
@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
import org.kohsuke.github.GitHub

String name = 'imago-storm'
String token = System.getenv('GITHUB_TOKEN')

GitHub github = GitHub.connect(name, token)
github.getOrganization('electric-cloud').getRepositories().each { repoName, repo ->
    if (repoName.startsWith('EC-') || repoName.startsWith('ECSCM-')) {

        GHOrganization.Permission write = GHOrganization.Permission.PUSH
        GHOrganization.Permission admin = GHOrganization.Permission.ADMIN

        try {
            github.getOrganization('electric-cloud').getTeamByName('plugins').add(repo, write)
            github.getOrganization('electric-cloud').getTeamByName('Plugins-Admin').add(repo, admin)
        } catch (Throwable e) {
            println "Failed to set permissions on $repoName: ${e.message}"
        }

    }
}

