import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.UnsupportedCredentialItem
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import org.kohsuke.github.GHBranch
import org.kohsuke.github.GHOrganization
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GHCommit
import org.eclipse.jgit.api.RemoteAddCommand
import org.eclipse.jgit.transport.RefSpec

@Grapes([
    @Grab('org.kohsuke:github-api:1.95'),
    @Grab(group = 'com.squareup.okhttp', module = 'okhttp', version = '2.7.5'),
    @Grab('org.eclipse.jgit:org.eclipse.jgit:5.0.3.201809091024-r'),
    @Grab('org.eclipse.jgit:org.eclipse.jgit.archive:5.0.3.201809091024-r'),
    @Grab(group = 'org.codehaus.groovy.modules.http-builder', module = 'http-builder', version = '0.7.1')
])
import org.kohsuke.github.GitHub
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.Response
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.RequestBody

String name = System.getenv('GITHUB_USER')
String token = System.getenv('GITHUB_TOKEN')
GitHub github = GitHub.connect(name, token)

String sourceOrg = 'electric-cloud'
String destOrg = 'electric-cloud-community'
String repoName = 'EC-Moogsoft'

//String sourceOrg = 'test-org-another'
//String destOrg = 'test-org-whatever'
//String repoName = 'test-repo'


def processedPluginsFile = new File("processedPlugins.json")
def processedPlugins = []
if (processedPluginsFile.exists()) {
    processedPlugins = new JsonSlurper().parse(processedPluginsFile)
}


def rootNode = new XmlSlurper().parse('http://downloads.electric-cloud.com/plugins/catalog.xml')
def exceptions = [
    'EC-Core'
]
def plugins = []
rootNode.plugin.each {
    def supportLevel = it.ecSupportLevel
    if (supportLevel != 10 && it.pluginName.toString().startsWith("EC-") && !(it.pluginName.toString() in exceptions)) {
        println "${it.pluginKey} is a community plugin"
        def pluginRepoName = it.pluginName.toString()
        plugins << pluginRepoName

    }
}


for (String pluginRepoName : plugins) {
    if (pluginRepoName in processedPlugins) {
        continue
    }
    try {
        duplicateRepo(name, token, sourceOrg, destOrg, pluginRepoName)
        moveRepository(github, pluginRepoName, sourceOrg, destOrg)
        archiveRepo(name, token, sourceOrg, pluginRepoName)
        processedPlugins << pluginRepoName
        saveProcessedPlugins(processedPlugins)
    } catch (Throwable e) {
        println "Failed to move repository ${pluginRepoName}: ${e.message}"
    }
}


duplicateRepo(name, token, sourceOrg, destOrg, repoName)
moveRepository(github, repoName, sourceOrg, destOrg)
archiveRepo(name, token, sourceOrg, repoName)
//makePublic(name, token, destOrg, repoName)


def saveProcessedPlugins(List processedPlugins) {
    new File("processedPlugins.json").write(JsonOutput.toJson(processedPlugins))
}

def moveRepository(GitHub github, String repoName, String sourceOrg, String destOrg) {
//    First, fork
    GHOrganization dest = github.getOrganization(destOrg)
    GHRepository oldRepo = github.getOrganization(sourceOrg).getRepository(repoName)
    GHRepository forkedRepo = dest.getRepository(repoName)

    println "Old Repo description: ${oldRepo.description}"

    if (!(oldRepo.description =~ /Moved to/)) {
        def description = "Moved to ${forkedRepo.htmlUrl}".toString()
        if (oldRepo.description) {
            description = oldRepo.description + " " + description
        }
        try {
            oldRepo.setDescription(description)
            println "Updated description for the old repo"
        } catch (IOException e) {
            println "Cannot set description for ${repoName}: ${e.message}"
        }
    }
//    add license

    String license = new File('/Users/imago/Documents/ecloud/plugins/EC-Github/license').text
    try {
        forkedRepo.createContent(license, "Adding license", "LICENSE")
        println "Added LICENSE file"
    } catch (Throwable e) {
        println "Adding license failed to ${forkedRepo.htmlUrl}: ${e.message}"
    }

    try {
        String licenseContent = forkedRepo.getFileContent("LICENSE").content
        if (licenseContent =~ /TODO/) {
            GHCommit commit = forkedRepo.createContent()
                                        .content(license)
                                        .path("LICENSE")
                                        .message("Updated License")
                                        .sha(forkedRepo.getFileContent("LICENSE").sha)
                                        .commit()
                                        .commit
            println "Updated license: ${commit.SHA1}"
        }
    } catch (Throwable e) {
        println "Failed to update license: ${e.message}"
    }

    try {
        GHBranch master = forkedRepo.getBranch("master")
        master.enableProtection()
              .requiredReviewers(2)
              .dismissStaleReviews(true)
              .enable()
        println "Enabled protection rules for master for ${forkedRepo.name}"
    } catch (Throwable e) {
        println "Failed to apply branch protection rules: ${e.message}"
    }

    try {
        dest.getTeamByName("Plugin Admin").add(forkedRepo, GHOrganization.Permission.ADMIN)
        dest.getTeamByName("EC Internal Plugin Developers").add(forkedRepo, GHOrganization.Permission.PUSH)
        println "Added permissions for the repo ${forkedRepo.name}"
    } catch (Throwable e) {
        println "Failed to add team to the repository ${forkedRepo.name}: ${e.message}"
    }
}


def duplicateRepo(String user, String token, String sourceOrg, String destOrg, String repoName) {
    CredentialsProvider provider = new CredentialsProvider() {
        @Override
        boolean isInteractive() {
            return false
        }

        @Override
        boolean supports(CredentialItem... credentialItems) {
            for (CredentialItem item : credentialItems) {
                if (item instanceof CredentialItem.Username || item instanceof CredentialItem.Password) {
                    return true
                }
            }
            return false
        }

        @Override
        boolean get(URIish urIish, CredentialItem... credentialItems) throws UnsupportedCredentialItem {
            for (CredentialItem item : credentialItems) {
                if (item instanceof CredentialItem.Username) {
                    CredentialItem.Username username = (CredentialItem.Username) item
                    username.setValue(user)
                }
                if (item instanceof CredentialItem.Password) {
                    CredentialItem.Password password = (CredentialItem.Password) item
                    password.setValue(token.toCharArray())
                }
            }
            return true
        }
    }

    File localPath = File.createTempFile("TestGitRepository", "");
    if (!localPath.delete()) {
        throw new IOException("Could not delete temporary file " + localPath);
    }
    Repository
    Git git = Git
        .cloneRepository()
        .setURI("https://github.com/${sourceOrg}/${repoName}.git")
        .setDirectory(localPath)
        .setBare(true)
        .setCredentialsProvider(provider)
        .call()

    git
        .remoteAdd()
        .setName("new-origin")
        .setUri(new URIish("https://github.com/${destOrg}/${repoName}.git"))
        .call()

    GitHub github = GitHub.connect(user, token)
    try {
        github.getOrganization(destOrg).createRepository(repoName).create()
    } catch (Throwable e) {
        println "Failed to create remote repository: ${e.message}"
    }

    git
        .push()
        .setCredentialsProvider(provider)
        .setRemote("new-origin")
        .setRefSpecs(new RefSpec("+refs/*:refs/*"))
        .call()

    println "Duplicated repo $repoName"

    localPath.delete()
}


def archiveRepo(user, password, destOrg, repoName) {
    OkHttpClient client = new OkHttpClient();

    MediaType mediaType = MediaType.parse("application/json");
    RequestBody body = RequestBody.create(mediaType, "{\"archived\": true}");
    Request request = new Request.Builder()
        .url("https://api.github.com/repos/${destOrg}/${repoName}")
        .patch(body)
        .addHeader("authorization", "Basic ${(user + ':' + password).bytes.encodeBase64()}")
        .addHeader("content-type", "application/json")
        .build();

    Response response = client.newCall(request).execute();
    if (response.successful) {
        println "Archived repository ${repoName}"
    } else {
        println "Failed to archive repository ${repoName}"
    }
}

