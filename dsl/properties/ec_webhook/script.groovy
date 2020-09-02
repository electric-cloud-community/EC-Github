println args

def trigger = args.trigger
def headers = args.headers
def method = args.method
def body = args.body
def url = args.url
def query = args.query

// do something

def event = ''
def signature = ''

headers.each { k, v ->
    if (k.toLowerCase() == 'X-GitHub-Event') {
        event = v
    }
    if (k.toLowerCase() == 'X-Hub-Signature') {
        signature = v
    }
}

//validate signature

if (event == 'ping') {
    return [
        eventType        : 'ping',
        webhookData      : ['some data': 'some data'],
        commitId         : null,
        commitAuthorName : null,
        commitAuthorEmail: null,
        branch           : null,
        launchWebhook    : false
    ]
} else if (event == 'push') {
    def payload = new JsonSlurper().parseText(body)
    def commits = payload.commits
    def repo = payload.repository
}

return [
    eventType        : 'push',
    webhookData      : ['some data': 'some data'],
    commitId         : null,
    commitAuthorName : null,
    commitAuthorEmail: null,
    branch           : null,
    launchWebhook    : true
]