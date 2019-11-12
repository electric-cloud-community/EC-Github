import com.electriccloud.client.groovy.ElectricFlow
import com.electriccloud.flowpdf.*
import com.electriccloud.flowpdf.exceptions.UnexpectedMissingValue
import org.kohsuke.github.GHCommit
import org.kohsuke.github.GHCommitState
import org.kohsuke.github.GHCommitStatus
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

/**
* Github
*/
class Github extends FlowPlugin {

    @Override
    Map<String, Object> pluginInfo() {
        return [
                pluginName     : '@PLUGIN_KEY@',
                pluginVersion  : '@PLUGIN_VERSION@',
                configFields   : ['config'],
                configLocations: ['ec_plugin_cfgs'],
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

        Context context = getContext()

        // Setting job step summary to the config name
        sr.setJobStepSummary(runtimeParameters.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
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

        Context context = getContext()

        // Setting job step summary to the config name
        sr.setJobStepSummary(runtimeParameters.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
        sr.apply()
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
    def downloadReleaseAsset(StepParameters runtimeParameters, StepResult sr) {

        /* Log is automatically available from the parent class */
        log.info(
          "downloadReleaseAsset was invoked with StepParameters",
          /* runtimeParameters contains both configuration and procedure parameters */
          runtimeParameters.toString()
        )

        Context context = getContext()

        // Setting job step summary to the config name
        sr.setJobStepSummary(runtimeParameters.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
        sr.apply()
        log.info("step Download Release Asset has been finished")
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

        Context context = getContext()

        // Setting job step summary to the config name
        sr.setJobStepSummary(runtimeParameters.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
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

        Context context = getContext()

        // Setting job step summary to the config name
        sr.setJobStepSummary(runtimeParameters.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
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

        GitHub client = createClient(runtimeParameters)
        GHRepository repo = client.getRepository(runtimeParameters.getRequiredParameter('repoName').value)
        String commitSha = runtimeParameters.getRequiredParameter('sha').value
        String state = runtimeParameters.getRequiredParameter('state').value
        String targetUrl = runtimeParameters.getParameter('targetUrl')?.value
        if (!targetUrl) {
            targetUrl = getRuntimeLink()
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

        Context context = getContext()

        // Setting job step summary to the config name
        sr.setJobStepSummary(runtimeParameters.getParameter('config').getValue() ?: 'null')

        sr.setReportUrl("Sample Report", 'https://cloudbees.com')
        sr.apply()
        log.info("step Create Release has been finished")
    }


// === step ends ===

    GitHub createClient(StepParameters runtime) {
        // String endpoint = runtime.getParameter('endpoint').value
        GitHubBuilder ghBuilder = new GitHubBuilder()
        // ghBuilder.withEndpoint(endpoint)
        if (runtime.isParameterExists('credential')) {
            Credential cred = runtime.getCredential('credential')
            ghBuilder.withPassword(cred.userName, cred.secretValue)
            log.info("Using username and password for the GH Client: $cred.userName, *******")
        }
        else if (runtime.isParameterExists('bearer_credential')) {
            Credential cred = runtime.getCredential('bearer_credential')
            ghBuilder.withOAuthToken(cred.secretValue)
            log.info "Using personal access token"
        }
        else {
            throw new UnexpectedMissingValue("No credential found in the plugin configuration")
        }
        return ghBuilder.build()
    }

    String getRuntimeLink() {
        ElectricFlow client = FlowAPI.getEc()
        String link
        try {
            client.getProperty_0(propertyName: '/myPipelineRuntime/id')
        } catch (Throwable e) {
            String jobId = System.getenv('COMMANDER_JOBID')
            link = '/commander/link/jobDetails/jobs/' + jobId
            // https://vivarium2/commander/link/jobDetails/jobs/9f599642-0498-11ea-b9cf-0242e3464664
        }

        return 'http://$[/server/webServerHost]' + link
    }

}