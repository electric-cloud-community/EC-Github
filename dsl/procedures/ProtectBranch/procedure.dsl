import java.io.File

def procName = 'Protect Branch'
procedure procName, description: 'Applies protection rules onto a branch', {

    step 'Protect Branch',
        command: new File(pluginDir, "dsl/procedures/ProtectBranch/steps/protectBranch.groovy").text,
        errorHandling: 'failProcedure',
        exclusiveMode: 'none',
        releaseMode: 'none',
        shell: 'ec-groovy',
        timeLimitUnits: 'minutes'

}
