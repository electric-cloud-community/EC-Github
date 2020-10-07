import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Hex

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import static java.nio.charset.StandardCharsets.UTF_8

println args

def trigger = args.trigger
def headers = args.headers
String method = args.method
String body = args.body
String url = args.url
def query = args.query

// Parsing headers
def event = headers.get('X-GitHub-Event')
def signature = headers.get('X-Hub-Signature')

//validating signature
if (!verifySignedPayload(signature, trigger.webhookSecret, body)) {
    // Todo: change to agreed exception
    throw new RuntimeException("Signatures does not match. Please recheck the shared secret")
}

// Receiving trigger parameters
//Map<String, Object> pluginParameters = trigger.pluginParameters
//throw new RuntimeException("params:" + pluginParameters)

// Check branches selected

if (event == 'ping') {
    return [
            eventType        : 'ping',
            webhookData      : ['some data': 'some data'],
            commitId         : null,
            commitAuthorName : null,
            commitAuthorEmail: null,
            branch           : null,
            launchWebhook    : false,
            responseMessage  : 'Pong'
    ]
} else if (event == 'push') {
    def payload = new JsonSlurper().parseText(body)
    def commits = payload.commits
    def repo = payload.repository
} else if (event == 'pull_request') {
//    opened
//    edited
//    closed
//    assigned
//    unassigned
//    review_requested
//    review_request_removed
//    ready_for_review
//    labeled
//    unlabeled
//    synchronize
//    locked
//    unlocked
//    reopened

} else if (event == 'check_run') {
//    created
//    completed
//    rerequested
} else if (event == 'status') {
//    pending,
//    success,
//    failure,
//    error
}


return [
        eventType        : 'push',
        webhookData      : ['some data': 'some data'],
        commitId         : null,
        commitAuthorName : null,
        commitAuthorEmail: null,
        branch           : null,
        launchWebhook    : false
]

boolean verifySignedPayload(String remoteSignature, String secretToken, String payload) {
    def signature = 'sha1=' + hmacSignature(payload, secretToken)
    return signature.equals(remoteSignature)
}

String hmacSignature(String data, String key) {
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