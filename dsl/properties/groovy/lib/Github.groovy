import com.cloudbees.flowpdf.*
import com.cloudbees.flowpdf.exceptions.UnexpectedMissingValue
import com.electriccloud.client.groovy.ElectricFlow
import groovy.json.JsonBuilder
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.codehaus.groovy.control.CompilerConfiguration
import org.kohsuke.github.*

/**
 * Github
 */
class Github extends FlowPlugin {

    //For downloading assets
    private String password

    @Override
    Map<String, Object> pluginInfo() {
        return [
            pluginName         : '@PLUGIN_KEY@',
            pluginVersion      : '@PLUGIN_VERSION@',
            configFields       : ['config'],
            configLocations    : ['ec_plugin_cfgs'],
            defaultConfigValues: [:]
        ]
    }

    /**
     * createRepository - Create Repository/Create Repository
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param owner (required: true)

     * @param repo (required: true)

     * @param description (required: )

     * @param public (required: )

     * @param teams (required: )

     * @param branchProtectionRules (required: )

     * @param addLicense (required: )

     * @param licenseFile (required: )

     */
    def createRepository(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
            "createRepository was invoked with StepParameters",
            /* runtimeParameters contains both configuration and procedure parameters */
            runtimeParameters.toString()
        )
        Map<String, String> parameters = runtimeParameters.asMap
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

        GHRepository repository = wrapper.createRepository(owner, repo, [
            description          : parameters.description,
            private              : parameters.public == 'false',
            teams                : teams,
            branchProtectionRules: branchProtectionRules,
            addLicense           : parameters.addLicense == "true",
            licenseFile          : parameters.licenseFile,
        ])

        log.info "Repository: ${repository.htmlUrl}"
        sr.setJobStepSummary("Created/Updated repository ${repository.fullName}")
        sr.setReportUrl("Repository", repository.htmlUrl.toString())
        sr.apply()
        log.info("step Create Repository has been finished")
    }

    /**
     * deleteRelease - Delete Release/Delete Release
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param repoName (required: true)

     * @param tagName (required: true)

     */
    def deleteRelease(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
            "deleteRelease was invoked with StepParameters",
            /* runtimeParameters contains both configuration and procedure parameters */
            runtimeParameters.toString()
        )
        Map<String, String> parameters = runtimeParameters.asMap
        wrapper.deleteTag(parameters.repoName, parameters.tagName)

        log.info("step Delete Release has been finished")
    }

    /**
     * downloadReleaseAsset - Download Release Asset/Download Release Asset
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param repoName (required: true)

     * @param tagName (required: true)

     * @param assetName (required: true)

     * @param assetPath (required: false)

     */
    def downloadReleaseAsset(StepParameters p, StepResult sr) {
        String repoName = p.getRequiredParameter('repoName').value
        Map<String, String> parameters = p.asMap as Map<String, String>
        wrapper.downloadReleaseAsset(repoName,
            parameters.tagName,
            parameters.assetName,
            parameters.assetPath,
            password
        )
    }

    /**
     * uploadFiles - Upload Files/Upload Files
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param ownerName (required: true)

     * @param repoName (required: true)

     * @param sourceDirectory (required: false)

     * @param mapping (required: )

     * @param files (required: )

     * @param branch (required: )

     * @param createPr (required: )

     */
    def uploadFiles(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
            "uploadFiles was invoked with StepParameters",
            /* runtimeParameters contains both configuration and procedure parameters */
            runtimeParameters.toString()
        )

        Map<String, String> p = runtimeParameters.asMap
        String repoName = runtimeParameters.getRequiredParameter('ownerName').value + '/' + runtimeParameters.getRequiredParameter('repoName').value
        String mappingRaw = p.mapping
        Map mapping = [:]
        if (mappingRaw) {
            mapping = new JsonSlurper().parseText(mappingRaw)
        }
        String branch = p.branch ?: 'master'
        List<String> files = []
        if (p.files) {
            files = p.files.split(/\n+/)
        }
        String sourceDirectory = p.sourceDirectory ?: ''
        List<GHCommit> commits = wrapper.uploadFiles(repoName, sourceDirectory, files, mapping, branch)
        if (p.createPr && p.branch != 'master') {
            GHPullRequest pr = wrapper.createPullRequest(
                repoName,
                p.branch
            )
            sr.setReportUrl("${pr.repository.name}#${pr.number}", pr.htmlUrl.toString())
            FlowAPI.setFlowProperty("/myJob/githubPr/url", pr.htmlUrl.toString())
        }
        sr.apply()
        log.info("step Upload Files has been finished")
    }

    /**
     * getFiles - Get Files/Get Files
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param ownerName (required: true)

     * @param repoName (required: true)

     * @param files (required: true)

     * @param destinationFolder (required: false)

     * @param ref (required: false)

     */
    def getFiles(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
            "getFiles was invoked with StepParameters",
            /* runtimeParameters contains both configuration and procedure parameters */
            runtimeParameters.toString()
        )
        Map<String, String> parameters = runtimeParameters.asMap
        List<String> files = parameters.files.split(/\n+/)

        if (!parameters.destinationFolder) {
            parameters.destinationFolder = System.getProperty('user.dir')
        }

        List<File> downloadedFiles = wrapper.downloadFiles(
            parameters.ownerName,
            parameters.repoName,
            files,
            parameters.ref,
            parameters.destinationFolder
        )

        def summary = downloadedFiles.collect {
            "Downloaded file ${it.absolutePath}"
        }
        sr.setJobStepSummary(summary.join("\n"))
        sr.apply()

        log.info("step Get Files has been finished")
    }

    /**
     * setCommitStatus - Set Commit Status/Set Commit Status
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param repoName (required: true)

     * @param sha (required: true)

     * @param state (required: true)

     * @param targetUrl (required: false)

     * @param description (required: false)

     */
    def setCommitStatus(StepParameters runtimeParameters, StepResult sr) {

        log.info(
            "setCommitStatus was invoked with StepParameters",
            /* runtimeParameters contains both configuration and procedure parameters */
            runtimeParameters.toString()
        )

        GitHub client = gh
        GHRepository repo = client.getRepository(runtimeParameters.getRequiredParameter('repoName').value)
        String commitSha = runtimeParameters.getRequiredParameter('sha').value
        String state = runtimeParameters.getParameter('state')?.value
        String mimic = runtimeParameters.getParameter('mimic')?.value
        if (!mimic && !state) {
            context.bailOut("Either state or mimic parameter should be provided")
        }

        if (state && mimic) {
            log.warning("Either state or mimic should be provided, you provided both. state will be used.")
        }
        if (mimic && !state) {
            def outcome
            switch (mimic) {
                case 'job':
                    outcome = FlowAPI.getFlowProperty("/myJob/outcome").value
                    log.info "Using outcome $outcome from the job"
                    break
                case 'pipelineRuntime':
                    outcome = FlowAPI.getFlowProperty("/myPipelineRuntime/outcome").value
                    log.info "Using outcome $outcome from the pipeline runtime"
                    break
                case 'stageRuntime':
                    outcome = FlowAPI.getFlowProperty("/myPipelineStageRuntime/outcome").value
                    log.info "Using outcome $outcome from the stage runtime"
                    break
                default:
                    context.bailOut("Wrong mimic value: $mimic")
            }

            if (outcome != 'error') {
                state = 'success'
            } else {
                state = 'error'
            }
            log.info "Identified state as $state from the outcome $outcome"
        }

        String targetUrl = runtimeParameters.getParameter('targetUrl')?.value
        if (!targetUrl) {
            targetUrl = getRuntimeLink()
            log.info("Calculated target URL: $targetUrl")
        }
        String description = runtimeParameters.getParameter('description')?.value
        GHCommitState ghState = GHCommitState.valueOf(state.toUpperCase())
        String contextString = 'CloudBees Flow Plugin @PLUGIN_KEY@'
        GHCommitStatus status = repo.createCommitStatus(commitSha, ghState, targetUrl, description, contextString)
        log.info "Commit status: ${status.state}"
        GHCommit commit = repo.getCommit(commitSha)
        log.info("Updated commit status for commit ${commit.SHA1}: ${commit.commitShortInfo.message}")

        String commitLink = commit.htmlUrl.toString()
        sr.setReportUrl("Commit Link ${repo.name} ${commit.SHA1}", commitLink, "Commit ${repo.name}: ${commit.SHA1}")
        sr.setJobStepSummary("Posted commit status successfully")
        sr.apply()
    }

    /**
     * createRelease - Create Release/Create Release
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param repoName (required: true)

     * @param updateAction (required: true)

     * @param releaseName (required: )

     * @param tagName (required: true)

     * @param commitish (required: )

     * @param body (required: )

     * @param assets (required: )

     * @param prerelease (required: )

     * @param deleteOldTag (required: )

     */
    def createRelease(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
            "createRelease was invoked with StepParameters",
            /* runtimeParameters contains both configuration and procedure parameters */
            runtimeParameters.toString()
        )


        String updateAction = runtimeParameters.getParameter('updateAction').value
        String assetsStr = runtimeParameters.getParameter('assets')?.value
        Map<String, File> assets = [:]
        if (assetsStr) {
            Map assetsJson = new JsonSlurper().parseText(assetsStr) as Map
            for (String name in assetsJson.keySet()) {
                String fileName = assetsJson.get(name)
                File f = new File(fileName)
                if (!f.isAbsolute()) {
                    f = new File(System.getProperty('user.dir'), fileName)
                }
                if (!f.exists()) {
                    throw new RuntimeException("The asset $f.absolutePath does not exist")
                }
                assets.put(name, f)
            }
        }

        GithubWrapper.UpdateAction upd = GithubWrapper.UpdateAction.valueOf(updateAction.toUpperCase())
        String tagName = runtimeParameters.getParameter('tagName')?.value
        String repoName = runtimeParameters.getParameter('repoName').value
        GHRelease release = wrapper.createRelease(repoName, assets, upd, tagName, runtimeParameters.asMap)

        FlowAPI.setFlowProperty("/myJob/report-urls/Release ${release.tagName}", release.htmlUrl.toString())
        sr.setReportUrl("Release ${release.tagName}", release.htmlUrl.toString(), "Release ${release.tagName}")
        sr.apply()
        log.info("step Create Release has been finished")
    }


    /**
     * getCommit - Get Commit/Get Commit
     * Add your code into this method and it will be called when the step runs

     * @param config (required: true)

     * @param repoName (required: true)

     * @param sha (required: true)

     * @param resultPropertySheet (required: )

     */
    def getCommit(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
            "getCommit was invoked with StepParameters",
            /* runtimeParameters contains both configuration and procedure parameters */
            runtimeParameters.toString()
        )
        GitHub client = gh
        String repoName = runtimeParameters.getRequiredParameter('repoName').value
        if (repoName.indexOf("/") == -1) {
            throw new RuntimeException("Repository should be in form of <owner>/<repo>")
        }
        GHRepository repo = client.getRepository(repoName)
        String commitSha = runtimeParameters.getRequiredParameter('sha').value
        GHCommit commit = repo.getCommit(commitSha)
        String resultProperty = runtimeParameters.getRequiredParameter('resultPropertySheet').value

        FlowAPI.setFlowProperty("${resultProperty}/message", commit.commitShortInfo.message)
        FlowAPI.setFlowProperty("${resultProperty}/commitDate", commit.commitShortInfo.commitDate.toString())
        FlowAPI.setFlowProperty("${resultProperty}/author/name", commit.commitShortInfo.author.name)
        FlowAPI.setFlowProperty("${resultProperty}/author/email", commit.commitShortInfo.author.email)

        Map commitData = [
            message   : commit.commitShortInfo.message,
            commitDate: commit.commitShortInfo.commitDate.toString(),
            author    : [
                name : commit.commitShortInfo.author.name,
                email: commit.commitShortInfo.author.email
            ]
        ]

        FlowAPI.setFlowProperty("${resultProperty}/json", JsonOutput.toJson(commitData))
        log.info "Commit data: ${JsonOutput.toJson(commitData)}"

        sr.setJobStepSummary("${commit.author.name} (${commit.author.email}): ${commit.commitShortInfo.message}")
        sr.apply()
        log.info("step Get Commit has been finished")
    }


    /**
     * addIssueComment - Add Issue Comment/Add Issue Comment
     * Add your code into this method and it will be called when the step runs
     * @param config (required: true)
     * @param repoName (required: true)
     * @param issueNum (required: true)
     * @param commentBody (required: true)

     */
    def addIssueComment(StepParameters p, StepResult sr) {
        String repoName = p.getRequiredParameter('repoName')
        String issueNum = p.getRequiredParameter('issueNum')
        String commentBody = p.getRequiredParameter('commentBody')

        if (repoName.indexOf("/") == -1) {
            throw new RuntimeException("Repository should be in form of <owner>/<repo>")
        }
        if (!(issueNum =~ /^[0-9]+$/)) {
            throw new RuntimeException("Issue/PR Id is not a number")
        }

        def comment = wrapper.createComment(repoName, issueNum as int, commentBody)

        // Setting job step summary to the config name
        sr.setJobStepSummary('Created a comment.')
        sr.setOutcomeProperty('/myCall/commentId', comment.getId() as String)
        sr.apply()
        log.info("step Add Comment has been finished")
    }

    /**
     * findPullRequests - Find Pull Requests/Find Pull Requests
     * Add your code into this method and it will be called when the step runs
     * @param config (required: true)
     * @param repoName (required: true)
     * @param branchName (required: false)

     */
    def findPullRequests(StepParameters p, StepResult sr) {
        String repoName = p.getRequiredParameter('repoName')

        List<GHPullRequest> list = wrapper.getOpenPullRequests(repoName)

        if (p.isParameterHasValue('branchName')) {
            String refName = p.getParameter('branchName').getValue()
            list = list.findAll { it -> it.getHead().getRef() == refName }
        }

        int count = list.size()

        if (!list.size()) {
            sr.setJobStepOutcome('warning')
            sr.setJobStepSummary('No open pull requests were found for ' + repoName)
            return
        }

        for (GHPullRequest pr : list) {
            String num = pr.getNumber()

            sr.setOutcomeProperty("/myCall/${num}/id", pr.getId() as String)
            sr.setOutcomeProperty("/myCall/${num}/title", pr.getTitle())
            sr.setOutcomeProperty("/myCall/${num}/branch", pr.getHead().getRef())
            sr.setOutcomeProperty("/myCall/${num}/base", pr.getBase().getRef())
            sr.setOutcomeProperty("/myCall/${num}/author", pr.getUser().getName())
        }

        String prNums = list.collect({ it -> it.getNumber() }).join(', ')

        sr.setOutcomeProperty("/myCall/PRNums", prNums)
        sr.setOutcomeProperty("/myCall/PRcount", count as String)
        sr.setOutputParameter('prNums', prNums)

        // Setting job step summary to the config name
        sr.setJobStepSummary("Found ${count} pull request(s)");
        sr.apply()
    }

    /**
     * setupWebhook - SetupWebhook/SetupWebhook
     * Add your code into this method and it will be called when the step runs
     * @param repositoryNames (required: )
     * @param ec_trigger (required: )
     * @param config (required: false)
     * @param credential (required: )

     */
    def setupWebhook(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        SetupWebhookParameters sp = SetupWebhookParameters.initParameters(p)
        def trigger = sp.ec_trigger
        println sp
        //todo load trigger from somewhere

        sp.repositoryNames.split(/\n/).each { repoName ->
            if (sp.ec_action == "create") {
                def webhook = gh.getRepository(repoName).createWebHook("trigger url")
                //save id to the trigger object
            } else if (sp.ec_action == 'delete') {
                //read id property from the trigger object
                //def webhookId = trigger.webhook
                //gh.getRepository(repoName)
                //gh.getRepository(repoName).getHook(id).delete()
            }

        }
    }

    /**
     * webhook - webhook/webhook
     * Add your code into this method and it will be called when the step runs
     * @param repositories (required: )
     * @param pushEvent (required: )
     * @param createPrEvent (required: )
     * @param prAction (required: )
     * @param includeBranches (required: )
     * @param excludeBranches (required: )

     */
    def webhook(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        WebhookParameters sp = WebhookParameters.initParameters(p)
    }

    /**
     * createPullRequest - Create Pull Request/Create Pull Request
     * Add your code into this method and it will be called when the step runs
     * @param config (required: true)
     * @param base (required: true)
     * @param head (required: true)
     * @param title (required: false)
     * @param body (required: false)
     * @param resultProperty (required: true)
     */
    def createPullRequest(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        CreatePullRequestParameters sp = CreatePullRequestParameters.initParameters(p)

        GHPullRequest pr = wrapper.createPullRequest(sp.repository, sp.head, sp.base, [
            title: sp.title,
            body : sp.body,
        ])

        def prData = [
            link     : pr.htmlUrl.toString(),
            number   : pr.number,
            mergeable: pr.mergeable
        ]

        sr.setOutputParameter('link', pr.htmlUrl.toString())
        sr.setOutputParameter('number', pr.number as String)
        sr.setOutputParameter('pr', JsonOutput.toJson(prData))
        sr.setReportUrl("${pr.title} # ${pr.number}", pr.htmlUrl.toString())
        sr.setJobStepSummary("PR ${pr.number} ${pr.htmlUrl.toString()}")
        sr.apply()

        String resultPropertySheet = sp.resultProperty
        FlowAPI.setFlowProperty("$resultPropertySheet/pr", JsonOutput.toJson(prData))
        FlowAPI.setFlowProperty("$resultPropertySheet/number", pr.number as String)
        FlowAPI.setFlowProperty("$resultPropertySheet/link", pr.htmlUrl.toString())
    }

    /**
     * runScript - Run Script/Run Script
     * Add your code into this method and it will be called when the step runs
     * @param config (required: true)
     * @param script (required: true)

     */
    def runScript(StepParameters p, StepResult sr) {
        // Use this parameters wrapper for convenient access to your parameters
        RunScriptParameters sp = RunScriptParameters.initParameters(p)
        def compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.scriptBaseClass = DelegatingScript.class.name
        def shell = new GroovyShell(this.class.classLoader, new Binding([gh: gh, ef: FlowAPI.ec]), compilerConfiguration)
        def gcpScript = new GithubScript(gh, FlowAPI.ec)
        Script s = shell.parse(sp.script)
        s.setDelegate(gcpScript)
        def result = s.run()
        log.info "Script evaluation result: $result"
        if (result) {
            sr.setOutputParameter('result', JsonOutput.toJson(result))
            sr.setJobStepSummary(new JsonBuilder(result).toPrettyString())
        }

    }

    // === step ends ===


    @Lazy
    GitHub gh = {
        Context c = getContext()
        StepParameters p = c.getRuntimeParameters()
        GitHubBuilder ghBuilder = new GitHubBuilder()
        String endpoint = p.getParameter('endpoint')?.value ?: "https://api.github.com"
        log.info "Using endpoint $endpoint"
        ghBuilder.withEndpoint(endpoint)

        String authScheme = p.getRequiredParameter('authScheme').getValue()
        if (authScheme == 'basic') {
            String credentialParameterName = null
            if (p.isParameterExists('basic_credential')) {
                credentialParameterName = 'basic_credential'
            } else if (p.isParameterExists('credential')) {
                credentialParameterName = 'credential'
            }

            if (credentialParameterName == null) {
                throw new UnexpectedMissingValue("No basic credential found in the plugin configuration")
            }

            Credential cred = p.getCredential(credentialParameterName)
            password = cred.secretValue
            ghBuilder.withPassword(cred.userName, cred.secretValue)
            log.info("Using username and password for the GH Client: $cred.userName, *******")
        } else if (authScheme == 'bearerToken' && p.isParameterHasValue('bearer_credential')) {
            Credential cred = p.getCredential('bearer_credential')
            ghBuilder.withOAuthToken(cred.secretValue)
            password = cred.secretValue
            log.info "Using personal access token"
        } else {
            throw new UnexpectedMissingValue("No credential found in the plugin configuration")
        }
        return ghBuilder.build()
    }()

    @Lazy
    GithubWrapper wrapper = {
        GithubWrapper wrapper = new GithubWrapper(gh, this.log)
        return wrapper
    }()

    String getRuntimeLink() {
        ElectricFlow client = FlowAPI.getEc()
        String link
        try {
            String pipelineRuntimeId = client.getProperty_0(propertyName: '/myPipelineRuntime/id')?.property?.value
            String pipelineId = client.getProperty_0(propertyName: "/myPipeline/id")?.property?.value
            link = '/flow/#pipeline-run/' + pipelineId + '/' + pipelineRuntimeId
            //https://chronic3.electric-cloud.com/flow/#pipeline-run/e0e16734-d88c-11e9-926b-005056bb04e9/6b032f87-12b6-11ea-af15-005056bb380b
        } catch (Throwable e) {
            String jobId = System.getenv('COMMANDER_JOBID')
            link = '/commander/link/jobDetails/jobs/' + jobId
            // https://vivarium2/commander/link/jobDetails/jobs/9f599642-0498-11ea-b9cf-0242e3464664
        }

        return 'http://$[/server/webServerHost]' + link
    }


    enum UpdateAction {
        RECREATE,
        NOOP,
        UPDATE,
        FAIL
    }


}


class GithubScript {
    def gh
    def ef

    GithubScript(github, ef) {
        this.ef = ef
        this.gh = github
    }

}
