class GithubRepository {
    String owner
    String name
    String description

}

import groovy.transform.builder.Builder
import groovy.transform.builder.ExternalStrategy

@Builder(builderStrategy = ExternalStrategy, forClass = GithubRepository)
class GithubRepositoryBuilder { }

