
// DO NOT EDIT THIS BLOCK BELOW=== Parameters starts ===
// PLEASE DO NOT EDIT THIS FILE

import com.cloudbees.flowpdf.StepParameters

class GetFilesParameters {
    /**
    * Label: Repository Owner, type: entry
    */
    String ownerName
    /**
    * Label: Repository Name, type: entry
    */
    String repoName
    /**
    * Label: Files, type: textarea
    */
    String files
    /**
    * Label: Folder to Save Files, type: entry
    */
    String destinationFolder
    /**
    * Label: Git Reference, type: entry
    */
    String ref

    static GetFilesParameters initParameters(StepParameters sp) {
        GetFilesParameters parameters = new GetFilesParameters()

        def ownerName = sp.getRequiredParameter('ownerName').value
        parameters.ownerName = ownerName
        def repoName = sp.getRequiredParameter('repoName').value
        parameters.repoName = repoName
        def files = sp.getRequiredParameter('files').value
        parameters.files = files
        def destinationFolder = sp.getParameter('destinationFolder').value
        parameters.destinationFolder = destinationFolder
        def ref = sp.getParameter('ref').value
        parameters.ref = ref

        return parameters
    }
}
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== Parameters ends, checksum: 8c3cce1eca11ae57c56c085399971a7a ===
