
// DO NOT EDIT THIS BLOCK === promote_autogen starts ===
import groovy.transform.BaseScript
import com.electriccloud.commander.dsl.util.BasePlugin

//noinspection GroovyUnusedAssignment
@BaseScript BasePlugin baseScript

// Variables available for use in DSL code
def pluginName = args.pluginName
def upgradeAction = args.upgradeAction
def otherPluginName = args.otherPluginName

def pluginKey = getProject("/plugins/$pluginName/project").pluginKey
def pluginDir = getProperty("/projects/$pluginName/pluginDir").value

//List of procedure steps to which the plugin configuration credentials need to be attached
def stepsWithAttachedCredentials = [
    [procedureName: "Create Repository", stepName: "Create Repository"],
    [procedureName: "Delete Release", stepName: "Delete Release"],
    [procedureName: "Download Release Asset", stepName: "Download Release Asset"],
    [procedureName: "Upload Files", stepName: "Upload Files"],
    [procedureName: "Get Files", stepName: "Get Files"],
    [procedureName: "Create Release", stepName: "Create Release"],

]

project pluginName, {
    property 'ec_keepFilesExtensions', value: 'true'
    property 'ec_formXmlCompliant', value: 'true'
    loadPluginProperties(pluginDir, pluginName)
    loadProcedures(pluginDir, pluginKey, pluginName, stepsWithAttachedCredentials)

    // Plugin configuration metadata
    property 'ec_config', {
        configLocation = 'ec_plugin_cfgs'
        form = '$[' + "/projects/$pluginName/procedures/CreateConfiguration/ec_parameterForm]"
        property 'fields', {
            property 'desc', {
                property 'label', value: 'Description'
                property 'order', value: '1'
            }
        }
    }

    }

def retainedProperties = []

upgrade(upgradeAction, pluginName, otherPluginName, stepsWithAttachedCredentials, 'ec_plugin_cfgs', retainedProperties)
// DO NOT EDIT THIS BLOCK === promote_autogen ends, checksum: d034622082501adb2cae068305a4487e ===
// Do not edit the code above this line

project pluginName, {
    // You may add your own DSL instructions below this line, like
    // property 'myprop', {
    //     value: 'value'
    // }
}