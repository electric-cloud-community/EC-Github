import java.io.File

def procName = 'Create Repository'
procedure procName, description: 'Creates a GitHub Repository', {

    step 'Create Repository',
        command: new File(pluginDir, "dsl/procedures/CreateRepository/steps/createRepository.groovy").text,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        releaseMode: 'none',
        shell: 'ec-groovy',
        timeLimitUnits: 'minutes'

}
