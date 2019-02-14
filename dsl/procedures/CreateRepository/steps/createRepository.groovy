$[/myProject/scripts/preamble]
import org.kohsuke.github.GHRepository


EFPlugin efPlugin = new EFPlugin()
def parameters = efPlugin.getParameters()
def config = new EFPlugin().getConfiguration(parameters.config)

//
GithubPlugin plugin = new GithubPlugin(config.userName, config.password)
String owner = parameters.owner
String repo = parameters.repo
GHRepository repository = plugin.createRepository(owner, repo, [
    description: parameters.description,
    private: parameters.public == 'false',
    teams: parameters.teams?.split('\n+')?.collect {
        def (name, permission) = it.split(/\s*:\s*/)
        [name: name, permission: permission]
    },
    branchProtectionRules: parameters.branchProtectionRules?.split(/\n+/)?.collect {
        def (branchName, rules) = it.split(/\s*:\s*/)
        def rulesList = rules.split(/\s*,\s*/)
        def retval = [branchName: branchName]
        rulesList.each {
            def (key, value) = it.split(/\s*=\s*/)
            retval[key] = value
        }
        retval
    }
])

println "Repository: ${repository.htmlUrl}"

efPlugin.setProperty_1("/myJobStep/summary", "Created/Updated repository ${repository.fullName}")
efPlugin.setProperty_1("/myJob/report-urls/Repository", repository.htmlUrl as String)