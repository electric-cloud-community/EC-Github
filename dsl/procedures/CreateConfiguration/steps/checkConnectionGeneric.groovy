
// === check_connection starts ===
import groovy.json.JsonSlurper
import com.electriccloud.client.groovy.ElectricFlow
import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.Method.GET
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.ContentType.JSON
import org.apache.http.auth.*

def checkConnectionMetaString = '''
{"authSchemes":{"basic":{"checkConnectionUri":null,"credentialName":"basic_credential"},"bearerToken":{"checkConnectionUri":null,"prefix":null,"credentialName":"bearer_credential"}},"checkConnectionUri":"/user","headers":null}
'''

def checkConnectionMeta = new JsonSlurper().parseText(checkConnectionMetaString)
println "Check Connection Metadata: $checkConnectionMeta"

ElectricFlow ef = new ElectricFlow()
def formalParameters = ef.getFormalParameters(
    projectName: '$[/myProject/name]',
    procedureName: '$[/myProcedure/name]'
)?.formalParameter

println "Formal parameters: $formalParameters"

def endpoint = ef.getProperty(propertyName: "endpoint")?.property?.value
println "Endpoint: $endpoint"
if (!endpoint) {
    handleError("Endpoint is not found (endpoint field does not exist?)")
}
def authType
try {
    authType = ef.getProperty(propertyName: "authScheme")?.property?.value
} catch (Throwable e) {
    // Deduce auth type
    // If we don't have a parameter for auth type, then we have only one auth type and it should be declared in meta
    authType = checkConnectionMeta?.authSchemes?.keySet().first()
    if (!authType) {
        handleError("Cannot deduce auth type: unclear metadata $checkConnectionMetaString")
    }
    println "Deduced Auth Scheme: $authType"
}
println "Auth Scheme: $authType"

def http = new HTTPBuilder(endpoint)

def proxyUrlFormalParameter = formalParameters.find { it.formalParameterName == 'httpProxyUrl'}
if (proxyUrlFormalParameter) {
  def proxyUrl
  try {
    proxyUrl = ef.getProperty(propertyName: "/myCall/httpProxyUrl")?.property?.value
  } catch  (Throwable e) {
  }
  // Need to split into scheme, host and port
  if (proxyUrl) {
    URL url = new URL(proxyUrl)
    http.setProxy(url.host, url.port, url.protocol)
    println "Set proxy $proxyUrl"

    def proxyCredential
    try {
      proxyCredential = ef.getFullCredential(credentialName: 'proxy_credential')?.credential
    } catch(Throwable e) {
    }

    if (proxyCredential && proxyCredential.userName) {
      http.setProxy(url.host, url.port, 'http')
      http.client.getCredentialsProvider().setCredentials(
        new AuthScope(url.host, url.port),
        new UsernamePasswordCredentials(proxyCredential.userName, proxyCredential.password)
      )
      println "Set proxy auth"
    }
  }
}

// Should be ignored after the proxy is set
http.ignoreSSLIssues()

http.request(GET, JSON) { req ->
  headers.'User-Agent' = 'FlowPDF Check Connection'

  if (checkConnectionMeta.headers) {
    headers.putAll(checkConnectionMeta.headers)
    println "Added headers: $checkConnectionMeta.headers"
  }

  if (checkConnectionMeta.checkConnectionUri != null) {
    uri.path = augmentUri(uri.path, checkConnectionMeta.checkConnectionUri)
    println "URI: $uri"
  }

  if (authType == "basic") {
    def meta = checkConnectionMeta?.authSchemes?.basic
    def credentialName = meta?.credentialName ?: "basic_credential"
    def basicAuth = ef.getFullCredential(credentialName: credentialName)?.credential
    def username = basicAuth.userName
    def password = basicAuth.password
    if (!username) {
      handleError(ef, "Username is not provided for the Basic Authorization")
    }
    headers.Authorization = "Basic " + (basicAuth.userName + ':' + basicAuth.password).bytes.encodeBase64()
    println "Setting Basic Auth: username $basicAuth.userName"
    if (meta.checkConnectionUri != null) {
        uri.path = augmentUri(uri.path, meta.checkConnectionUri)
        println "Check Connection URI: $uri"
    }
  }

  if (authType == "bearerToken") {
    def meta = checkConnectionMeta?.authSchemes?.bearerToken
    def credentialName = meta?.credentialName ?: 'bearer_credential'
    def bearer = ef.getFullCredential(credentialName: credentialName)?.credential
    def prefix = meta.prefix ?: "Bearer"
    headers.Authorization = prefix + " " + bearer.password
    println "Setting Bearer Auth with prefix $prefix"
    if (meta.checkConnectionUri != null) {
        uri.path = augmentUri(uri.path, meta.checkConnectionUri)
        println "Check Connection URI: $uri"
    }
  }

  if (authType == "anonymous") {
    println "Anonymous access"
    def meta = checkConnectionMeta?.authSchemes?.anonymous
    if (meta.checkConnectionUri != null) {
      uri.path = meta.checkConnectionUri
      println "Check Connection URI: $uri"
    }
  }

  response.success = { resp, reader ->
    assert resp.status == 200
    println "Status Line: ${resp.statusLine}"
    println "Response length: ${resp.headers.'Content-Length'}"
    System.out << reader // print response reader
  }

  response.failure = { resp, reader ->
    println "$resp.statusLine"
    println "$reader"
    String message = "Check Connection Failed: ${resp.statusLine}, $reader"
    handleError(ef, message)
  }
}

def handleError(def ef, def message) {
  ef.setProperty(propertyName: "/myJobStep/summary", value: message)
  ef.setProperty(propertyName: "/myJob/configError", value: message)
  System.exit(-1)
}

def augmentUri(path, uri) {
    if (path.endsWith('/') || uri.startsWith('/')) {
        path = path + uri
    }
    else {
        path = path + '/' + uri
    }
    return path
}
// === check_connection ends, checksum: feac227040b244cc8dc8fa9ef8600104 ===
