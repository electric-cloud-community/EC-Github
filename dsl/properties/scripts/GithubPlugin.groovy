import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHBranchProtection
import org.kohsuke.github.GHBranchProtectionBuilder
import org.kohsuke.github.GHCreateRepositoryBuilder
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPermissionType
import org.kohsuke.github.GHPerson
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHRef
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitHub

@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
@Slf4j
class GithubPlugin {
    GitHub client

    def GithubPlugin(String username, String password) {
        client = GitHub.connect(username, password)
    }

    def comment() {

    }

    GHRepository createRepository(String ownerName, String repoName, Map parameters, String updateAction = 'update') {
        GHRepository repository
        String description = parameters.description
        boolean isPrivate = parameters.private
        try {
            repository = client.getRepository("${ownerName}/${repoName}")
            log.info "Repository ${repository.name} already exists"
        } catch (IOException e) {
            GHCreateRepositoryBuilder repositoryBuilder
            try {
                repositoryBuilder = client.getOrganization(ownerName).createRepository(repoName)
            } catch (IOException e2) {
                repositoryBuilder = client.createRepository(repoName)
            }
            if (description) {
                repositoryBuilder.description(description)
            }
            repositoryBuilder.private_(isPrivate)
            repository = repositoryBuilder.create()
            log.info "Created repository ${repository.name}"
        }

        if (description && repository.description != description) {
            repository.setDescription(description)
        }

        if (repository.private != isPrivate) {
            // TODO update privacy
        }


        if (parameters.teams) {
            GHOrganization org = client.getOrganization(ownerName)
            def teams = [:]
            parameters.teams.each {
                def teamName = it.name
                assert teamName
                String permissionName = it.permission ?: 'PULL'
                GHOrganization.Permission permission = permissionName as GHOrganization.Permission
                getTeamByNameMemoized(org, teamName).add(repository, permission)
                log.info("Added permission ${permission.name()} for the team ${teamName}")
            }
        }


        parameters.collaborators?.each {
            def name = it.name
            def role = it.role

            GHUser user = getUserByNameMemoized(name)
            repository.addCollaborators(user)
            log.info "Added collaborator ${user.name}"
            // TODO handle role
        }

        parameters.branchProtectionRules?.each {
            def branchName = it.branchName
            def reviewers = it.reviewers as int
            // TODO other
            GHBranch branch = ensureBranch(repository, branchName)
            GHBranchProtectionBuilder builder = branch.enableProtection()
            if (reviewers != null) {
                builder.requiredReviewers(reviewers)
            }
            builder.enable()
            log.info "Enabled protection for branch ${branchName}"
        }

        return repository
    }

    @Memoized
    GHBranch ensureBranch(GHRepository repository, String branchName) {
        GHBranch branch
        try {
            branch = repository.getBranch(branchName)
        } catch (IOException e) {
            repository
                .createContent()
                .branch(branchName)
                .message("Created branch ${branchName}")
                .content("")
                .path("README.md")
                .commit()
            branch = repository.getBranch(branchName)
        }
        return branch
    }

    @Memoized
    GHUser getUserByNameMemoized(String userName) {
        return client.getUser(userName)
    }

    @Memoized
    GHTeam getTeamByNameMemoized(GHOrganization org, String teamName) {
        return org.getTeamByName(teamName)
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


    def createPullRequest(String owner, String repoName, String fromBranch, String toBranch, String title, String content = "") {
        GHRepository repo = client.getRepository("${owner}/${repoName}")
        GHPullRequest request = repo.createPullRequest(title, fromBranch, toBranch, content)
        println "Pull Request is created: ${request.url}"
        println "Diff Url: ${request.diffUrl}"
        println "Mergeable: ${request.mergeable}"
    }

}
