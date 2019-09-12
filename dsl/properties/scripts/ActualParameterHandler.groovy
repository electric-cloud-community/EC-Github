import com.electriccloud.client.groovy.ElectricFlow
import groovy.xml.MarkupBuilder
import org.apache.http.entity.ContentType

class ActualParameterHandler {
    ElectricFlow ef = new ElectricFlow()
    int requestCounter = 0
    Closure getActualParameterValueMemoized

    String getParameterValue(String paramName, debug = false) {
        def d = { str ->
            if (debug) {
                println "ActualParameterHandler Debug : " + str
            }
        }
        if (getActualParameterValueMemoized) {
            d "Using memoized version of the getter function"
            return getActualParameterValueMemoized(paramName)
        }
        String jobStepId = System.getenv('COMMANDER_JOBSTEPID')
        assert jobStepId
        String jobId = System.getenv('COMMANDER_JOBID')
        assert jobId

        def serverVersion = ef.getVersions()?.serverVersion?.version
        d "Server version is $serverVersion"

        def getActualParameterValueCompatible = { job, jobStep, param ->
            d "Calling closure for getting actualParameter"
            def args = [jobId: job, jobStepId: jobStep, actualParameterName: param]
            def actualParameter
            if (compareMinor(serverVersion, '9.1') >= 0 && false) {
                actualParameter = ef.getActualParameter(args)?.actualParameter
            } else {
                actualParameter = getActualParameter(args)
            }

            if(!actualParameter.actualParameterName.toString()) {
                throw new RuntimeException("Empty actual parameter $paramName")
            }
            return actualParameter.value.toString()
        }

        def value = ''
        boolean got
        got = false

        try {
            d 'Getting the parameter from the job'
            def getParameterFromJob = getActualParameterValueCompatible.curry(jobId, null)
            value = getParameterFromJob(paramName)
            getActualParameterValueMemoized = getParameterFromJob
            got = true
            d "Success: got a parameter from the job"
        } catch (Throwable e) {
            d "Failed to get a parameter from the job: $e.message"
        }

        if (got) {
            return value
        }

        while (jobStepId && !got) {
            try {
                d "Getting a parameter from the job step $jobStepId"
                def getParameterFromJobStep = getActualParameterValueCompatible.curry(null, jobStepId)
                value = getParameterFromJobStep(paramName)
                got = true
                getActualParameterValueMemoized = getParameterFromJobStep
                d "Success: got a parameter from the job step $jobStepId"
            } catch (Throwable e) {
                got = false
                d "Error: $e.message"
            }
            if (!got) {
                try {
                    jobStepId = ef.getProperty(propertyName: "/myJobStep/parent/id",
                                               jobStepId: jobStepId)?.property?.value
                    d "Getting parent job step id $jobStepId"
                } catch (Throwable ignore) {
                    jobStepId = null
                    d "Out of parent job steps"
                }
            }
        }
        if (got) {
            return value
        }
        throw new RuntimeException("Cannot get actual parameter $paramName")
    }

    // Stolen from ElectricFlow client
    protected static  String constructFullyQualifiedServerURL(String url) {
        //cleanup the input url
        url = url.trim()
        if (url.endsWith('/')) {
            url = url.substring(0, url.lastIndexOf('/'))
        }

        def secure
        if (url.startsWith('http://')) {
            secure = 0
        } else if (url.startsWith('https://')) {
            secure = 1
        } else {
            secure = getSystemProperty("COMMANDER_SECURE", 1)
            def protocol = secure ? "https" : "http"
            url = "${protocol}://${url}"
        }

        if (!url.matches('.+:\\d+')) {
            def port = secure ?
                getSystemProperty("COMMANDER_HTTPS_PORT", 8443) :
                getSystemProperty("COMMANDER_PORT", 8000)
            url = "${url}:${port}"
        }

        url
    }

    static String getSystemProperty(String name, defaultValue = null) {
        return ElectricFlow.getSystemProperty(name, defaultValue)
    }

    private static  def oldApiHttpRequest(String uriPath, Map queryParams, String payload) {
        def http = ElectricFlow.buildHttpClient(constructFullyQualifiedServerURL(getSystemProperty('COMMANDER_SERVER')),
                                                true)
        http.handler.success = { response, data ->
            data
        }
        http.handler.failure = { response, data ->
            def errorMessage = data.error ? "[${data.error.code}]: ${data.error.message}" : "${response.statusLine} [${data}]"
            if (data.error?.where || data.error?.details) {
                errorMessage += " [where: ${data.error.where ?: ''}, details: ${data.error.details ?: ''}]"
            }
            throw new RuntimeException("Error $errorMessage")
        }
        return http.post(path: uriPath, query: queryParams, body: payload, contentType: ContentType.TEXT_XML)
    }


    def getActualParameter(Map params) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        def sessionId = getSystemProperty('COMMANDER_SESSIONID')
        assert sessionId
        xml.requests(version: "2.2", timeout: "180", sessionId: sessionId) {
            request(requestId: "${requestCounter++}") {
                getActualParameter() {
                    if (params.jobId) {
                        jobId(params.jobId)
                    }
                    if (params.jobStepId) {
                        jobStepId(params.jobStepId)
                    }
                    actualParameterName(params.actualParameterName)
                }
            }
        }
        def body = writer.toString()
        def result = oldApiHttpRequest("/commanderRequest", [:], body)
        return result?.response?.actualParameter
    }

    static int compareMinor(String version1, String version2) {
        def splitVersion = { stringVersion ->
            def parts = stringVersion.split(/\./)
            assert parts.size() >= 2
            def major = parts[0]
            def minor = parts[1]
            return [major, minor]
        }
        def currentVersion = splitVersion(version1)
        def desiredVersion = splitVersion(version2)
        return currentVersion[0] <=> desiredVersion[0] ?: currentVersion[1] <=> desiredVersion[1]
    }

}
