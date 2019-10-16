import groovy.json.JsonSlurper
import org.kohsuke.github.GHPullRequest

$[/myProject/scripts/preamble]

EFPlugin efPlugin = new EFPlugin()
Map p = efPlugin.getParameters()
EFPlugin ef = new EFPlugin()
def config = ef.getConfiguration(p.config)
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
String repoName = p.ownerName + '/' + p.repoName
String mappingRaw = p.mapping
Map mapping = [:]
if (mappingRaw) {
    mapping = new JsonSlurper().parseText(mappingRaw)
}
String branch = p.branch
List<String> files = []
if (p.files) {
    files = p.files.split(/\n+/)
}
List<GHCommit> commits = plugin.uploadFiles(repoName, p.sourceDirectory, files, mapping, branch)
if (p.createPr && p.branch != 'master') {
    GHPullRequest pr = plugin.createPullRequest(
        repoName,
        p.branch
    )
    ef.setProperty_1("/myJob/report-urls/${pr.repository.name}#${pr.number}", pr.htmlUrl.toString())
    ef.setProperty_1("/myJob/githubPr/url", pr.htmlUrl.toString())
}