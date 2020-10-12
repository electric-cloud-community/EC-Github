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

// This map corresponds to the procedure form
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
def paramsPropertySheet = trigger.pluginParameters
Map<String, String> pluginParameters = [:]
paramsPropertySheet['properties'].each { String k, Map<String, String> v ->
    pluginParameters[k] = v['value']
}


WebhookEvent webhookEvent = WebhookEvent.getForType(event, body)
if (webhookEvent == null) {
    return [
            responseMessage: "Ignoring unsupported '${event}' event",
            launchWebhook  : false
    ]
}

// Check repository
String repositoryName = webhookEvent.getRepositoryName()
if (!repositoryName) {
    throw new RuntimeException("Webhook event '${event}' doesn't contain 'repository' object")
}
if (!doCheckRepositoryIncluded(pluginParameters.get('repositories'), repositoryName)) {
    return [
            responseMessage: "Ignoring ${repositoryName} repository event",
            launchWebhook  : false
    ]
}

// Check event enabled
String eventEnabledParamName = SUPPORTED_EVENTS[event]['enabledParamName']
if (eventEnabledParamName != null) {

    boolean eventEnabled = pluginParameters.get(eventEnabledParamName) != 'false'
    if (!eventEnabled) {
        return [
                responseMessage: "Processing for the '${webhookEvent.getName()}' event is disabled",
                launchWebhook  : false
        ]
    }
}


// Check action enabled
String includedActionParamName = SUPPORTED_EVENTS[event]['actionsParamName']
if (includedActionParamName != null) {

    String includedActions = pluginParameters.get(includedActionParamName)
    String eventAction = webhookEvent.getAction()

    boolean actionEnabled = doCheckActionIncluded(includedActions, eventAction)

    if (!actionEnabled) {
        return [
                responseMessage: "Processing for the '${eventAction}' action of the '${event}' event is disabled",
                launchWebhook  : false
        ]
    }
}

// Check that branch is included and not excluded
String includeBranches = pluginParameters.get('includeBranches')
String excludeBranches = pluginParameters.get('excludeBranches')

ArrayList<String> eventBranches = webhookEvent.getBranchNames()
String branchName = eventBranches.join(', ')

if (includeBranches) {
    if (!doCheckBranchIncluded(includeBranches, eventBranches)) {
        return [
                responseMessage: "Ignoring '${event}' event for branch '${branchName}'",
                launchWebhook  : false
        ]
    }
}
if (excludeBranches) {
    if (doCheckBranchIncluded(excludeBranches, eventBranches)) {
        return [
                responseMessage: "Ignoring '${event}' event for exluded branch '${branchName}'",
                launchWebhook  : false
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
    ArrayList<String> list = parameterValue.tokenize("\n").collect({ it.trim() })
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
        final SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(UTF_8), "HmacSHA1")
        final Mac mac = Mac.getInstance("HmacSHA1")
        mac.init(keySpec)
        final byte[] rawHMACBytes = mac.doFinal(data.getBytes(UTF_8) as byte[])

        return Hex.encodeHexString(rawHMACBytes)
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
    abstract String body

    @Lazy
    String action = {
        payload.get('action')
    }()

    Map<String, Object> payload

    WebhookEvent(String payload) {
        this.body = payload
        this.payload = (new JsonSlurper()).parseText(this.body) as Map<String, Object>
    }

    static WebhookEvent getForType(String event, String payload) {
        if (event == 'ping') {
            return new PingEvent(payload)
        } else if (event == 'pull_request') {
            return new PullRequestEvent(payload)
        } else if (event == 'push') {
            return new PushEvent(payload)
        } else if (event == 'status') {
            return new CommitStatusEvent(payload)
        } else {
            return null
        }
    }

    String getRepositoryName() { return payload.get('repository')?.get('full_name') }

    abstract ArrayList<String> getBranchNames()

    abstract Map<String, String> getRecentCommit()

    abstract Map<String, String> collectWebhookData()

}

class PingEvent extends WebhookEvent {
    static String name = 'ping'

    PingEvent(String payload) {
        super(payload)
    }

    @Override
    ArrayList<String> getBranchNames() {
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

class PullRequestEvent extends WebhookEvent {

    static String name = 'pull_request'

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
        def prHead = payload['head']
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

    @Override
    Map<String, String> getRecentCommit() {
        if (!commits || !commits.size()) return null
        return commits.first()
    }

    PullRequestEvent(String payload) {
        super(payload)
    }

    @Override
    Map<String, String> collectWebhookData() {
        def pullRequest = payload['pull_request']
        return [
                number : pullRequest['number'],
                title  : pullRequest['title'],
                body   : pullRequest['body'],
                state  : pullRequest['state'],
                url    : pullRequest['html_url'],
                payload: this.body
        ] as Map<String, String>
    }

    /**
     * We are adding two additional virtual actions: closed_merged, closed_discarded
     */
    @Override
    String getAction() {
        String action = payload.get('action')
        if (action == 'closed') {
            def pullRequest = payload.get('pull_request')
            if (pullRequest['merged'] == 'true') {
                return 'closed_merged'
            } else {
                return 'closed_discarded'
            }

        }
        return action
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
        if (!refName.contains('refs/heads/')) {
            // This is not a branch push
            throw new RuntimeException("Only the branch 'push' events are supported, got '${refName}'")
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
        if (!commits || !commits.size()) return null
        return commits.first()
    }

    @Override
    Map<String, String> collectWebhookData() {
        return [
                ref    : payload['ref'],
                branch : getBranchNames().join(', '),
                payload: this.body
        ] as Map<String, String>
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

    ArrayList<Map<String, String>> getCommits() {
        // Single commit in an array
        return [getRecentCommit()]
    }

    @Override
    Map<String, String> getRecentCommit() {
        def commit = payload['commit']
        return [
                commitId         : commit['sha'],
                commitMessage    : commit['message'],
                commitAuthorName : commit['commiter']['name'],
                commitAuthorEmail: commit['commiter']['email'],
        ] as Map<String, String>
    }

    @Override
    Map<String, String> collectWebhookData() {
        return [
                sha    : payload['sha'],
                state  : payload['state'],
                payload: this.body
        ] as Map<String, String>
    }
}
