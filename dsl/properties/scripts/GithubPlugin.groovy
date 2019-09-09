import groovy.transform.Memoized
import groovy.util.logging.Slf4j
import org.kohsuke.github.GHAsset
import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHBranchProtection
import org.kohsuke.github.GHBranchProtectionBuilder
import org.kohsuke.github.GHContent
import org.kohsuke.github.GHContentUpdateResponse
import org.kohsuke.github.GHCreateRepositoryBuilder
import org.kohsuke.github.GHIssueState
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHPermissionType
import org.kohsuke.github.GHPerson
import org.kohsuke.github.GHPullRequest
import org.kohsuke.github.GHPullRequestReviewEvent
import org.kohsuke.github.GHRef
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHTeam
import org.kohsuke.github.GHUser
import org.kohsuke.github.GitHub

import java.nio.file.Files
import java.nio.file.Path

@Grapes([
    @Grab('org.kohsuke:github-api:1.95')
])
@Slf4j
class GithubPlugin {
    GitHub client

    final String LICENSE = 'LICENSE'

    def GithubPlugin(String username, String password) {
        client = GitHub.connect(username, password)
    }

    def comment() {

    }

    GHPullRequest createPullRequest(String ownerName, String repoName, String sourceBranch, String destBranch, Map parameters) {
        GHRepository repository
        try {
            repository = client.getRepository("${ownerName}/${repoName}")
        } catch (IOException e) {
            log.info "Repository ${ownerName}/${repoName} does not exist"
            throw e
        }
        String title = parameters.title
        if (!title) {
            String lastCommitSha = repository.getBranch(sourceBranch).SHA1
            String lastCommitMessage = repository.getCommit(lastCommitSha).commitShortInfo.message
            title = lastCommitMessage
        }
        GHPullRequest request = repository.createPullRequest(title, sourceBranch, destBranch, "")
        log.info "Created Pull Request: ${request.htmlUrl}"
        log.info "Mergeable: ${request.mergeable}"

        if (parameters.approve) {
            request.createReview().event(GHPullRequestReviewEvent.APPROVE).create()
            log.info "Approved pull request #${request.number}"
        }

        if (parameters.merge) {
            if (request.mergeable) {
                log.info "Trying to auto-merge request #${request.number}"
                request.merge()
                log.info "Merged #${request.number} automatically"
            } else {
                log.info "Request #${request.number} cannot be merged automatically"
            }
        }
        return request
    }

    def createRelease(String repoName, Map<String, File> assets, UpdateAction updateAction, String tagName, Map parameters) {
        GHRepository repository = client.getRepository(repoName)

        GHRelease release
        try {
            release = repository.getReleaseByTagName(tagName)
            if (release) {
                log.info "Found release: ${release.tagName}"
            }
        } catch(Throwable e) {
            log.info "Cannot get release $tagName: $e.message"
        }

        if (updateAction == UpdateAction.NOOP && release) {
            log.info "The release already exists, doing nothing"
            return
        }

        if (updateAction == UpdateAction.FAIL && release) {
            throw new RuntimeException("Failed to create release, the release already exists")
        }

        if (updateAction == UpdateAction.RECREATE) {
            try {
                if (release) {
                    release.delete()
                    log.info "Deleted release $release.tagName"
                }
                if (parameters.deleteOldTag == "true") {
                    repository.refs.find { it.ref == "refs/tags/$tagName"}.delete()
                    log.info "Deleted old tag $tagName"
                }
                else {
                    log.warn "The old tag will not be deleted, it may cause inconsistency within the repository releases"
                }
            } catch (IOException e) {
                log.debug "Failed to delete release $e.message"
            }
        }

        GHReleaseBuilder builder = repository.createRelease(tagName)
        if (parameters.commitish) {
            builder.commitish(parameters.commitish as String)
            log.info "Added commitish: ${parameters.commitish}"
        }
        if (parameters.body) {
            builder.body(parameters.body as String)
            log.info "Added body: $parameters.body"
        }

        if (parameters.prerelease) {
            builder.prerelease(true)
            log.info "Release is marked as a prerelease"
        }
        if (parameters.releaseName) {
            builder.name(parameters.releaseName as String)
            log.info "Release name will be $parameters.releaseName"
        }

        release = builder.create()
        try {
            assets.keySet().each { name ->
                File asset = assets.get(name)
                Path path = asset.toPath()
                String mimeType = Files.probeContentType(path)
                log.info "Asset $asset, type: $mimeType"
                asset.withInputStream { stream ->
                    GHAsset gasset = release.uploadAsset(name, stream, mimeType)
                    log.info "Uploaded asset: ${gasset.name}"
                    log.info "Download URL: ${gasset.browserDownloadUrl}"
                }
            }
        } catch (Throwable e) {
            log.info "Failed to upload asset: $e.message"
            if (!parameters.keepPartly) {
                release.delete()
            }
            log.info "Deleted invalid release"
        }

        log.info "Created release $release.htmlUrl"
        return release
    }

    GHRepository createRepository(String ownerName, String repoName, Map parameters, String updateAction = 'update') {
        GHRepository repository
        String description = parameters.description
        boolean isPrivate = parameters.private
        if (!repoName) {
            throw new RuntimeException("Repository name should be provided")
        }
        try {
            repository = client.getRepository("${ownerName}/${repoName}")
            log.info "Repository ${repository.name} already exists"
        } catch (Throwable e) {
            GHCreateRepositoryBuilder repositoryBuilder
            try {
                repositoryBuilder = client.getOrganization(ownerName).createRepository(repoName)
            } catch (Throwable e2) {
                log.info "Owner is a user"
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
            log.info "Setting description to $description"
            repository.setDescription(description)
        }

        if (repository.private != isPrivate) {
            // TODO update privacy
        }


        if (parameters.teams?.size() > 0) {
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

        if (parameters.addLicense) {
            File licenseFile = new File(parameters.licenseFile as String)
            if (!licenseFile.exists()) {
                throw new RuntimeException("License file ${licenseFile.absolutePath}")
            }

            try {
                GHContent content = repository.getFileContent(LICENSE)
                if (content.content == licenseFile.text) {
                    log.info "No update is required"
                } else {
                    String sha = content.sha
                    GHContentUpdateResponse response = repository.createContent()
                                                                 .path(LICENSE)
                                                                 .message("Updated LICENSE")
                                                                 .sha(sha)
                                                                 .content(licenseFile.text)
                                                                 .commit()
                    log.info "Updated license: ${response.commit.SHA1}"
                    log.info response.commit.htmlUrl as String
                }
            } catch (IOException e) {
                log.info "License file does not exist in the repository"
                GHContentUpdateResponse response = repository.createContent()
                                                             .path(LICENSE)
                                                             .content(licenseFile.text)
                                                             .message("Created LICENSE")
                                                             .commit()
                log.info "Created License: ${response.commit.SHA1}"
                log.info response.commit.htmlUrl as String
            }
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


    enum UpdateAction {
        RECREATE,
        NOOP,
        UPDATE,
        FAIL
    }

}
