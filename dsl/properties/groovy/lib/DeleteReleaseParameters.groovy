
// DO NOT EDIT THIS BLOCK BELOW=== Parameters starts ===
// PLEASE DO NOT EDIT THIS FILE

import com.cloudbees.flowpdf.StepParameters

class DeleteReleaseParameters {
    /**
    * Label: Repository Name, type: entry
    */
    String repoName
    /**
    * Label: Tag Name, type: entry
    */
    String tagName

    static DeleteReleaseParameters initParameters(StepParameters sp) {
        DeleteReleaseParameters parameters = new DeleteReleaseParameters()

        def repoName = sp.getRequiredParameter('repoName').value
        parameters.repoName = repoName
        def tagName = sp.getRequiredParameter('tagName').value
        parameters.tagName = tagName

        return parameters
    }
}
// DO NOT EDIT THIS BLOCK ABOVE ^^^=== Parameters ends, checksum: 00affdd028e175b6c9202c690d6985bc ===
