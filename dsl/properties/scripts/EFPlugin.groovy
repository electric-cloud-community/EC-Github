import com.electriccloud.client.groovy.ElectricFlow

class EFPlugin {
    ElectricFlow ef = new ElectricFlow()
    ActualParameterHandler actualParameterHandler = new ActualParameterHandler()


    EFPlugin() {
        ef = new ElectricFlow()
    }

    def getConfiguration(configName) {
        Map configuration = ef.getProperties(path: '/projects/@PLUGIN_NAME@/ec_plugin_cfgs/' + configName)?.propertySheet?.property?.collectEntries {
            [it.propertyName, it.value]
        }
        def credential = ef.getFullCredential(credentialName: configName)
        configuration.userName = credential?.credential?.userName
        configuration.password = credential?.credential?.password
        return configuration
    }

    def setProperty_1(name, value) {
        ef.setProperty(propertyName: name, value: value)
    }

    Map<String, String> getParameters() {
        String procedureName = ef.getProperty(propertyName: '/myProcedure/procedureName')?.property?.value
        String projectName = ef.getProperty(propertyName: '/myProject/projectName')?.property?.value
        def parameters = ef.getFormalParameters(procedureName: procedureName,
                                                projectName: projectName)?.formalParameter?.collect {
            it.formalParameterName
        }
        Map retval = [:]
        parameters.each { formalParameterName ->
            retval[formalParameterName] = actualParameterHandler.getParameterValue(formalParameterName)
        }

        println "Actual parameters: ${retval}"
        return retval
    }


    def setPipelineSummaryLink(String summaryName, String name, String link) {
        String html = "<html><a href=\"$link\" target=\"_blank\">$name</a></html>"
        try {
            setProperty_1("/myPipelineStageRuntime/ec_summary/$summaryName", html)
        } catch(Throwable ignore) {
            println ignore.message
        }
    }
}
