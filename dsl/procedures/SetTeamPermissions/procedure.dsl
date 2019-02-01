import java.io.File

def procName = 'Set Team Permissions'
procedure procName, description: 'Apply team permissions for the repository', {

    step 'Set Team Permissions',
        command: new File(pluginDir, "dsl/procedures/SetTeamPermissions/steps/setTeamPermissions.groovy").text,
        shell: 'ec-groovy'

}
