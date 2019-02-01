import java.io.File

def procName = 'Create Branch'
procedure procName, description: 'Applies protection rules onto a branch', {

    step 'Create Branch',
        command: new File(pluginDir, "dsl/procedures/CreateBranch/steps/createBranch.groovy").text,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        releaseMode: 'none',
        shell: 'ec-groovy',
        timeLimitUnits: 'minutes'

}
