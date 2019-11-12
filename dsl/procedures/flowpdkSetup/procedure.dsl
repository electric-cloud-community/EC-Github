procedure 'flowpdk-setup',
description: 'Retrieves Groovy dependencies. This procedure can be used as a first step of the procedure which requires dependencies.', {

    property 'standardStepPicker', value: false

    step 'Setup',
        command: new File(pluginDir, "dsl/procedures/flowpdkSetup/steps/setup.pl").text,
        shell: 'ec-perl'

    property 'flowpdk-dsl', {
        value = new File(pluginDir, "dsl/procedures/flowpdkSetup/compressAndDeliver.groovy").text
    }

    formalParameter 'generateClasspathFromFolders', {
        required = '0'
        defaultValue = ''
        type = 'entry'
    }
}

