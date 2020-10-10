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

final Map<String, Object> SUPPORTED_EVENTS = [
        'push'        : [
                enabledParamName: 'pushEvent',
                actionsParamName: null
        ],
        'pull_request': [
                enabledParamName: 'prEvent',
                actionsParamName: 'includePrActions'
        ],
        'status'      : [
                enabledParamName: 'commitStatusEvent',
                actionsParamName: 'includeCommitStatuses'
        ],
        'ping'        : [
                enabledParamName: null,
                actionsParamName: null
        ],
]

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
    throw new RuntimeException("Signatures do not match. Please check that the trigger's 'webhookSecret' field value matches one specified in the Github repository webhook settings.")
}

// Receiving trigger parameters
Map<String, String> pluginParameters = trigger.getPluginParameters()
throw new RuntimeException("params:" + pluginParameters)

WebhookEvent webhookEvent = WebhookEvent.getForType(event, body)
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

// Check event enabled
boolean eventEnabled = pluginParameters.get(SUPPORTED_EVENTS[event]['enabledParamName']) != 'false'
if (!eventEnabled) {
    return [
            responseMessage: "Processing for the '${webhookEvent.getName()}' event is disabled",
            launchWebhook  : false
    ]
}

// Check action enabled
String includedActions = pluginParameters.get(SUPPORTED_EVENTS[event]['actionsParamName'])
String action = webhookEvent.getAction()
boolean actionEnabled = doCheckActionIncluded(includedActions, action)
if (!actionEnabled) {
    return [
            responseMessage: "Processing for the '${action}' action of the '${event}' event is disabled",
            launchWebhook  : false
    ]
}

// Check that branch is included and not excluded
String includeBranches = pluginParameters.get('includeBranches')
String excludeBranches = pluginParameters.get('excludeBranches')

ArrayList<String> eventBranches = webhookEvent.getBranchNames()
String branchName = eventBranches.join(', ')

if (includeBranches) {
    if (!doCheckBranchIncluded(includeBranches, eventBranches)) {
        return [
                launchWebhook  : false,
                responseMessage: "Ignoring '${event}' event for branch '${branchName}'"
        ]
    }
}
if (excludeBranches) {
    if (doCheckBranchIncluded(excludeBranches, eventBranches)) {
        return [
                launchWebhook  : false,
                responseMessage: "Ignoring '${event}' event for exluded branch '${branchName}'"
        ]
    }
}

// Collect data for response
Map<String, String> webhookData = webhookEvent.collectWebhookData()
Map<String, String> recentCommit = webhookEvent.getRecentCommit()

Map<String, Object> response = [
        eventType    : event,
        launchWebhook: true,
        branch       : webhookEvent.getBranchNames().join(', ')
] as Map<String, Object>

if (webhookData) {
    response['webhookData'] = webhookData
}

if (recentCommit) {
    response['commitId'] = recentCommit['commitId']
    response['commitAuthorName'] = recentCommit['commitAuthorName']
    response['commitAuthorEmail'] = recentCommit['commitAuthorEmail']
}

return response

/**
 * These methods depend on the form declaration
 */

private static boolean doCheckRepositoryIncluded(String parameterValue, String checked) {
    ArrayList<String> list = parameterValue.tokenize(/\n/).collect({ it.trim() })
    return list.contains(checked)
}

private static boolean doCheckActionIncluded(String parameterValue, String checked) {
    if (!parameterValue) return true
    ArrayList<String> list = parameterValue.tokenize(/,\s+?/).collect({ it.trim() })
    return list.contains(checked)
}

private static boolean doCheckBranchIncluded(String parameterValue, ArrayList<String> checked) {
    ArrayList<String> list = parameterValue.tokenize(/,\s+?/).collect({ it.trim() })
    for (String b : checked) {
        if (listContainsGlobMatch(list, b)) {
            return true
        }
    }
    return false
}

//////////////////////////////////////////////////////////////////////////////////////////////
// End of business logic
//////////////////////////////////////////////////////////////////////////////////////////////

static boolean verifySignedPayload(String remoteSignature, String secretToken, String payload) {
    def signature = 'sha1=' + hmacSignature(payload, secretToken)
    return signature.equals(remoteSignature)
}

static String hmacSignature(String data, String key) {
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

static boolean listContainsGlobMatch(ArrayList<String> list, String checked) {
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

    WebhookEvent(String payload) {
        this.payload = (new JsonSlurper()).parseText(payload) as Map<String, Object>
    }

    static WebhookEvent getForType(String event, String payload) {
        if (event == 'pull_request') {
            return new PullRequestEvent(payload)
        } else if (event == 'push') {
            return new PushEvent(payload)
        } else if (event == 'status') {
            return new CommitStatusEvent(payload)
        } else {
            // This should be handled by the SUPPORTED_EVENTS check, but just in case
            throw new RuntimeException("Yep, there is no handling for '${event}' event yet.")
        }
    }

    String getRepositoryName() { return payload.get('repository')?.get('full_name') }

    abstract ArrayList<String> getBranchNames()

    abstract ArrayList<Map<String, String>> getCommits()

    abstract Map<String, String> getRecentCommit()

    abstract Map<String, String> collectWebhookData()

}

class PullRequestEvent extends WebhookEvent {

    static String name = 'pull_request'

    PullRequestEvent(String payload) {
        super(payload)
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

    PushEvent(String payload) {
        super(payload)
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

    static String name = 'status'

    CommitStatusEvent(String payload) {
        super(payload)
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
