import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHBranchProtection
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRef
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub

@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
class GithubPlugin {
    GitHub client

    def GithubPlugin(String username, String password) {
        client = GitHub.connect(username, password)
    }

    def protectBranch(String owner, String repositoryName, String branchName, requiredReviewers = 0) {
        GHRepository repository = client.getRepository("${owner}/${repositoryName}")
        println "Repository URL: ${repository.url}"
        GHBranch branch = repository.getBranch(branchName)
        println "Branch: ${branch.name}"
        if (requiredReviewers) {
            GHBranchProtection protection = branch.enableProtection().requiredReviewers(requiredReviewers).enable()
            println "Protection URL: ${protection.url}"
        }
    }

    def createRepository(String owner,
                         String repoName,
                         String description = "",
                         String homepage = "",
                         boolean isPublic = true) {
        GHRepository repository = client.createRepository("${owner}/${repoName}", description, homepage, isPublic)
        println "Repository URL: ${repository.url}"
    }


    def grantTeamPermission(String org, String repoName, String team, String permissionName) {
        GHRepository repository = client.getRepository("${org}/${repoName}")
        GHOrganization.Permission permission = GHOrganization.Permission.valueOf(permissionName)
        client.getOrganization(org).getTeamByName(team).add(repository, permission)
    }


    def createBranch(String owner, String repoName, String branchName, String from = null) {
        GHRepository repo = client.getRepository("${owner}/${repoName}")
        if (!from) {
            from = repo.getDefaultBranch()
        }
        GHBranch parentBranch = repo.getBranch(from)
        GHRef ref = repo.createRef("refs/head/${branchName}", parentBranch.SHA1)
        println "Created a new branch: $branchName, ${ref.url}"
    }

}
