import com.electriccloud.client.groovy.ElectricFlow

class EFPlugin {
    ElectricFlow ef = new ElectricFlow()

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
            retval[formalParameterName.replaceAll('github_', '')] = ef.getProperty(propertyName: formalParameterName)?.property?.value
        }
        return retval
    }

    def getParameterValue(String paramName) {
        String retval
        try {
            String jobStepId = System.getenv('COMMANDER_JOBSTEPID')
            assert jobStepId
            def actualParameter = ef.getActualParameter(actualParameterName: paramName,
                                                        jobStepId: jobStepId)
            println actualParameter
            if (actualParameter.actualParameterName) {
                retval = actualParameter.value
            }
        } catch (Throwable e) {
            println e.message
        }
        if (retval != null) {
            return retval
        }

        try {
            String jobId = ef.getProperty(propertyName: '/myJob/id')?.property?.value
            assert jobId
            def actualParameter = ef.getActualParameter(actualParameterName: paramName,
                                                        jobId: jobId)
            println actualParameter
            if (actualParameter.actualParameterName) {
                retval = actualParameter.value
            }
        } catch (Throwable e) {
            println e.message
        }

        if (retval != null) {
            return retval
        }

        try {
            String parentJobStepId = ef.getProperty(propertyName: '/myJobStep/parent/id')?.property?.value
            assert parentJobStepId
            def actualParameter = ef.getActualParameter(actualParameterName: paramName,
                                                        jobStepId: parentJobStepId)
            println actualParameter
            if (actualParameter.actualParameterName) {
                retval = actualParameter.value
            }
        } catch (Throwable e) {
            println e.message
        }
        if (retval != null) {
            return retval
        }

        throw new RuntimeException("Cannot find parameter $paramName")

    }
}
