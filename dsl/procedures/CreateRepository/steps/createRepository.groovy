$[/myProject/scripts/preamble]
import org.kohsuke.github.GHRepository


EFPlugin efPlugin = new EFPlugin()
def parameters = efPlugin.getParameters()
def config = new EFPlugin().getConfiguration(parameters.config)

//
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
String owner = parameters.owner
String repo = parameters.repo

def branchProtectionRules = parameters.branchProtectionRules?.split(/\n+/)?.collect {
    def branchNameRules = it.split(/\s*:\s*/)
    if (branchNameRules?.size() == 2) {
        def rules = branchNameRules[1]
        def branchName = branchNameRules[0]
        def rulesList = rules.split(/\s*,\s*/)

        def retval = [branchName: branchName]
        rulesList.each {
            def (key, value) = it.split(/\s*=\s*/)
            retval[key] = value
        }
        retval
    }
}?.findAll { it }


def teams = parameters.teams?.split('\n+')?.collect {
    def namePermission = it.split(/\s*:\s*/)
    if (namePermission?.size() == 2) {
        [name: namePermission[0], permission: namePermission[1]]
    }
}?.findAll { it }

GHRepository repository = plugin.createRepository(owner, repo, [
    description          : parameters.description,
    private              : parameters.public == 'false',
    teams                : teams,
    branchProtectionRules: branchProtectionRules,
    addLicense           : parameters.addLicense == "true",
    licenseFile          : parameters.licenseFile,
])

println "Repository: ${repository.htmlUrl}"

efPlugin.setProperty_1("/myJobStep/summary", "Created/Updated repository ${repository.fullName}")
efPlugin.setProperty_1("/myJob/report-urls/Repository", repository.htmlUrl as String)