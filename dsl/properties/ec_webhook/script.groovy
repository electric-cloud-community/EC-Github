println args

def trigger = args.trigger
def headers = args.headers
def method = args.method
def body = args.body
def url = args.url
def query = args.query

// do something


return [
    eventType: 'push',
    webhookData: ['some data': 'some data'],
    commitId: null,
    commitAuthorName: null,
    commitAuthorEmail: null,
    branch: null,
    launchWebhook: true
]