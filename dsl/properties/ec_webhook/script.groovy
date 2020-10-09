import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Hex

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.regex.Pattern

import static java.nio.charset.StandardCharsets.UTF_8

println args

def trigger = args.trigger
Map<String, String> headers = args.headers
String method = args.method
String body = args.body
String url = args.url
def query = args.query

final ArrayList<String> SUPPORTED_EVENTS = ['push', 'pull_request', 'status', 'ping']

// Parsing headers
String event = headers.get('X-GitHub-Event')
String signature = headers.get('X-Hub-Signature')
if (!signature) {
    throw new RuntimeException("Request does not contain the signature header")
}
if (!event) {
    throw new RuntimeException("Request does not contain the event header")
}

// As we do not use other restrictions, every trigger should have a signature secret
if (!trigger.webhookSecret) {
    throw new RuntimeException("Trigger '${trigger.getName()}' does not have webhookSecret set up")
}
//validating signature
if (!verifySignedPayload(signature, (String) trigger.webhookSecret, body)) {
    // Todo: change to agreed exception
    throw new RuntimeException("Signatures does not match. Please recheck the shared secrets.")
}

// Receiving trigger parameters
Map<String, String> pluginParameters = trigger.getPluginParameters()
throw new RuntimeException("params:" + pluginParameters)

WebhookEvent webhookEvent = WebhookEvent.getForType(event, body, pluginParameters)
if (webhookEvent == null) {
    return [
            launchWebhook  : false,
            responseMessage: "Ignoring unsupported '${event}' event"
    ]
}

// Check repository
String repositoryName = webhookEvent.getRepositoryName()
if (!repositoryName) {
    throw new RuntimeException("Webhook event '${event}' doesn't contain 'repository' object")
}
if (!doCheckRepositoryIncluded(pluginParameters.get('repositories'), repositoryName)) {
    return [
            eventType      : event,
            responseMessage: "Ignoring ${repositoryName} repository event",
            launchWebhook  : false
    ]
}

// We can respond to ping immediately
if (event == 'ping') {
    return [
            eventType      : 'ping',
            responseMessage: 'pong',
            launchWebhook  : false
    ]
}

if (!webhookEvent.isEnabled()) {
    return [
            responseMessage: "Processing for the '${webhookEvent.getName()}' event is disabled",
            launchWebhook  : false
    ]
}

if (!webhookEvent.isActionEnabled()) {
    String action = webhookEvent.getAction()
    return [
            responseMessage: "Processing for the '${action}' of the '${event}' is disabled",
            launchWebhook  : false
    ]
}

String includeBranches = pluginParameters.get('includeBranches')
String excludeBranches = pluginParameters.get('excludeBranches')

if (includeBranches) {
    ArrayList<String> branches = includeBranches.tokenize(/,\s+?/)
    if (!webhookEvent.isCorrespondingToAnyBranchIn(branches)) {
        String branchName = webhookEvent.getBranchNames().join(', ')
        return [
                launchWebhook  : false,
                responseMessage: "Ignoring '${event}' event for branch '${branchName}'"
        ]
    }
}
if (excludeBranches) {
    ArrayList<String> branches = includeBranches.tokenize(/,\s+?/)
    if (webhookEvent.isCorrespondingToAnyBranchIn(branches)) {
        String branchName = webhookEvent.getBranchNames().join(', ')
        return [
                launchWebhook  : false,
                responseMessage: "Ignoring '${event}' event for exluded branch '${branchName}'"
        ]
    }
}

Map<String, String> webhookData = webhookEvent.collectWebhookData()
Map<String, String> recentCommit = webhookEvent.getRecentCommit()

def response = [
        eventType    : 'push',
        launchWebhook: true,
        branch       : webhookEvent.getBranchNames().join(', ')
]

if (webhookData) {
    response['webhookData'] = webhookData
}

if (recentCommit) {
    response['commitId'] = recentCommit['commitId']
    response['commitAuthorName'] = recentCommit['commitAuthorName']
    response['commitAuthorEmail'] = recentCommit['commitAuthorEmail']
}

return response

private boolean verifySignedPayload(String remoteSignature, String secretToken, String payload) {
    def signature = 'sha1=' + hmacSignature(payload, secretToken)
    return signature.equals(remoteSignature)
}

private String hmacSignature(String data, String key) {
    try {
        final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(UTF_8), "HmacSHA1");
        final Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        final byte[] rawHMACBytes = mac.doFinal(data.getBytes(UTF_8) as byte[]);

        return Hex.encodeHexString(rawHMACBytes);
    } catch (Exception e) {
        throw new RuntimeException("Computed invalid signature: " + e.getMessage())
    }
}

private static boolean doCheckRepositoryIncluded(String parameterValue, String checked) {
    ArrayList<String> list = parameterValue.tokenize(/\n/).collect({ it.trim() })
    return listContainsStrictMatch(list, checked)
}

private static boolean doCheckActionIncluded(String parameterValue, String checked) {
    if (!parameterValue) return true
    ArrayList<String> list = parameterValue.tokenize(/,\s+?/).collect({ it.trim() })
    return listContainsStrictMatch(list, checked)
}

private static boolean doCheckBranchIncluded(String parameterValue, String checked) {
    ArrayList<String> list = parameterValue.tokenize(/,\s+?/).collect({ it.trim() })
    return listContainsGlobMatch(list, checked)
}

private static boolean listContainsStrictMatch(ArrayList<String> list, String checked) {
    return list.contains(checked)
}

private static boolean listContainsGlobMatch(ArrayList<String> list, String checked) {
    for (String l : list) {
        def pattern = Pattern.compile(l)
        if (checked ==~ pattern) {
            return true
        }
    }
    return false
}

abstract class WebhookEvent {
    abstract String name

    @Lazy
    String action = {
        payload.get('action')
    }()

    Map<String, Object> payload
    boolean enabled

    abstract static String enabledParameterName
    abstract static String includedActionsParameterName

    WebhookEvent(String payload, Map<String, String> triggerPluginParameters) {
        this.payload = (new JsonSlurper()).parseText(payload) as Map<String, Object>
        this.enabled = isEnabled(triggerPluginParameters)
    }

    static WebhookEvent getForType(String event, String payload, Map<String, String> triggerPluginParameters) {
        if (event == 'pull_request') {
            return new PullRequestEvent(payload, triggerPluginParameters)
        } else if (event == 'push') {
            return new PushEvent(payload, triggerPluginParameters)
        } else if (event == 'status') {
            return new CommitStatusEvent(payload, triggerPluginParameters)
        } else {
            // This should be handled by the SUPPORTED_EVENTS check, but just in case
            throw new RuntimeException("Yep, there is no handling for '${event}' event yet.")
        }
    }

    boolean isEnabled() { enabled }

    private boolean checkEnabled(Map<String, String> triggerPluginParameters) {
        if (triggerPluginParameters[enabledParameterName] == 'false') {
            return false
        }
        if (includedActionsParameterName) {
            String actionsIncluded = triggerPluginParameters[includedActionsParameterName]
            doCheckActionIncluded(actionsIncluded, this.action)
        }
    }

    String getRepositoryName() { return payload?.get('repository')?.get('full_name') }

    abstract ArrayList<String> getBranchNames()

    abstract ArrayList<Map<String, String>> getCommits()

    abstract Map<String, String> getRecentCommit()

    abstract Map<String, String> collectWebhookData()

    boolean isCorrespondingToBranch(String branchName) {
        return getBranchNames().contains(branchName)
    }

    boolean isCorrespondingToAnyBranchIn(ArrayList<String> branchNames) {
        // TODO: Rewrite to map + constant check
        for (String branchName : branchNames) {
            if (this.isCorrespondingToBranch(branchName)) {
                return true
            }
        }
        return false
    }

}

class PullRequestEvent extends WebhookEvent {

    static String name = 'pull_request'
    static String enabledParameterName = 'prEvent'
    static String includedActionsParameterName = 'includePrActions'

    PullRequestEvent(String payload, Map<String, String> triggerPluginParameters) {
        super(payload, triggerPluginParameters)
    }

    @Lazy
    ArrayList<String> branchNames = {
        String refName = payload.get('pull_request')?.get('head')?.get('ref')
        if (!refName) {
            return null
        }
        return [refName.replace('/refs/heads/', '')]
    }()

    @Lazy
    ArrayList<Map<String, String>> commits = {
        Map<String, Object> prHead = payload.get('head')
        return [
                [
                        commitId         : prHead['sha'],
                        branch           : prHead['ref'],
                        commitAuthorName : prHead['user']['login'],
                        //TODO: check if we should request additionally
                        commitAuthorEmail: null,
                ] as Map<String, String>
        ]
    }()

    @Lazy
    Map<String, String> recentCommit = {
        if (!commits || !commits.size()) {
            return null
        }
        return commits.first()
    }()

    @Override
    Map<String, String> collectWebhookData() {
        return null
    }
}

class PushEvent extends WebhookEvent {

    static String name = 'push'
    static String enabledParameterName = 'pushEvent'
    static String includedActionsParameterName = null

    PushEvent(String payload, Map<String, String> triggerPluginParameters) {
        super(payload, triggerPluginParameters)
    }

    @Lazy
    ArrayList<String> branchNames = {
        String refName = payload.get('ref')
        if (!refName) {
            return null
        }
        if (!refName.matches('/refs/heads')) {
            // This is not a branch push
            throw new RuntimeException("Only the branch 'push' events are supported.")
        }
        return [refName.replace('/refs/heads/', '')]
    }()

    @Lazy
    ArrayList<Map<String, String>> commits = {
        ArrayList<Map<String, String>> res = new ArrayList<>()
        payload.get('commits').each { Map<String, Object> commit ->
            return [
                    commitId         : commit['sha'],
                    commitMessage    : commit['message'],
                    commitAuthorName : commit['author']['name'],
                    commitAuthorEmail: commit['author']['email'],
            ]
        }
        return res
    }()

    @Override
    Map<String, String> getRecentCommit() {
        return null
    }

    @Override
    Map<String, String> collectWebhookData() {
        return null
    }
}

class CommitStatusEvent extends WebhookEvent {

    static String enabledParameterName = 'commitStatusEvent'
    static String includedActions = 'includeCommitStatuses'

    CommitStatusEvent(String payload, Map<String, String> triggerPluginParameters) {
        super(payload, triggerPluginParameters)
    }

    @Lazy
    ArrayList<String> branchNames = {
        ArrayList<Map<String, Object>> commitBranches = payload.get('branches') as ArrayList<Map<String, Object>>
        if (!commitBranches || !commitBranches.size()) {
            return null
        }
        return commitBranches.collect({ it.get('name') })
    }()

    @Override
    ArrayList<Map<String, String>> getCommits() {
        return null
    }

    @Override
    Map<String, String> getRecentCommit() {
        return null
    }

    @Override
    Map<String, String> collectWebhookData() {
        return null
    }
}
