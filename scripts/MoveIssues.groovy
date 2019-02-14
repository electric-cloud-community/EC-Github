import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHRepository
@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
import org.kohsuke.github.GitHub

String name = 'imago-storm'
String token = System.getenv('GITHUB_TOKEN')

String repoName = 'EC-DSLIDE'
String sourceName = 'electric-cloud/EC-DSLIDE'
String destName = 'electric-cloud-community/EC-DSLIDE'

GitHub github = GitHub.connect(name, token)
GHRepository source = github.getRepository(sourceName)
GHRepository dest = github.getRepository(destName)

source.getIssues(GHIssueState.OPEN).each {
//    println it.htmlUrl
//    println it.title
//    pritnln it.user
//    it.comments
    GHIssue = dest.createIssue(it.title).body(it.body).create()
}


//github.getOrganization('electric-cloud').getRepositories().each { repoName, repo ->
//    if (repoName.startsWith('EC-') || repoName.startsWith('ECSCM-')) {
//        try {
//            GHBranch release = repo.getBranch('release')
//            GHPullRequest request = repo.createPullRequest("Release", release.name, 'master', '')
//            println "Created Pull Request for ${repo.name}"
//            println "Mergeable: ${request.mergeable}"
//            println "Pull Request: ${request.diffUrl}"
//        } catch (Throwable e) {
//            println "Failed to create PR for ${repoName}: ${e.message}"
//        }
//    }
//}
