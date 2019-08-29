# deployment-maven-plugin

![Build][Build-shield] 
[![Maintainable][Maintainable-image]][Maintainable-Url]
![Central][Central-shield] 
![Tag][Tag-shield]
![Issues][Issues-shield] 
![Commit][Commit-shield] 
![Size][Size-shield] 
![Dependency][Dependency-shield]
![License][License-shield]
![Label][Label-shield]

# CURRENTLY REFACTORING FROM BASH TO REAL MAVEN MOJO

### Motivation
Writing a project is really easy until it comes to your first deployment. You would need many plugins and manual tests until you project gets deployed in the default way.
Like: The pom file in each project will raise (duplicated), the versioning, tagging, signing, readme updates, credentials and plugin configuration feels a bit hacky.
Its not even testable.
This plugin will handle "everything" default for you. So that you don't need anything in your pom file.
Auto handling semantic versioning, maven plugins, and much more while you can still use the original maven userProperties to configure the plugins

### Usage as a plugin
````xml
<plugin>
    <groupId>berlin.yuna</groupId>
    <artifactId>deployment-maven-plugin</artifactId>
    <version>0.0.1</version>
</plugin>
````

### Usage on command line
````bash
mvn deployment:run -Djava.doc=true -Djava.source -Dupdate.minor
````
* Will create java doc, java sources, and updates dependencies

# Semantic- && versioning
### Parameters
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| project.version     | String  | ''                 | Sets project version in pom                                                |
| remove.snapshot     | Boolean | false              | Removes snapshot from version                                              |
| semantic.format     | String  | ''                 | Updates semantic version from regex pattern (overwrites project.version)   |

### Semantic version
* Sets the version coming from branch name (uses git refLog for that)
* The matching branch names has to be defined by using regex
* Syntax:
* ````"<separator>::<major>::<minor>::<patch>"```` 
* Example:
* ````semantic.format="[.-]::release.*::feature.*::bugfix\|hotfix::custom_1.*[A-Z]"````

# Builder files (like README.builder.md)
### Parameters
| Parameter           | Type    | Default            |  Description                                                                    |
|:--------------------|:--------|:-------------------|:--------------------------------------------------------------------------------|
| builder             | Boolean | false              | Will start translating builder files with pattern "fileName.builder.extension"  |

### Builder file content
* Builder files are simple templates mainly used for readme files
* The placeholder will be replaced by maven environment and git config variables (git config -l )
* Git config variables starts with 'git.' like 'git.remote.origin.url'
* 'target' (optional special variable) defines the target directory (starting from project.basedir) else target is the same folder
* Example
````text

[var myVariableName]: # (This is my variable value)
[var project.description]: # (This overwrites the maven environment variable)
[var varInVar]: # (This contains !{myVariableName} variable)
[var target]: # (/new/readme/directory/path)

# My project name: !{project.name}
## My project git origin url: !{git.remote.origin.url}
### My display own variable: !{varInVar}

```` 

# Tagging & Committing
### Parameters
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| tag                 | Boolean | false              | Tags the project (with project.version or semantic) if not already exists  |
| tag                 | String  | ${project.version} | Tags the project (with project.version or semantic) if not already exists  |
| tag.break           | Boolean | false              | Tags the project (with project.version or semantic) fails if already exists|
| message             | String  | ${auto}            | Commit msg for tag default = \[project.version] \[branchname], \[tag] ...  |
| scm.provider        | String  | scm:git            | needed for tagging & committing                                            |
| COMMIT              | String  | ''                 | Custom commit message on changes - "false" = deactivate commits            |
* Example tag with project.version (or semantic version if active)
* ````tag```` 
* ````tag=true```` 
* ````tag="my.own.version"```` 
* ````message="new release"```` 
* 'tag.break' parameter will stop tagging if the tag already exists 

# Update dependencies
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| update.minor        | Boolean | false              | Updates parent, properties, dependencies                                   |
| update.major        | Boolean | false              | Updates parent, properties, dependencies                                   |

# Testing
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| test.run            | Boolean | false              | runs test.unit and test.integration                                        |
| test.unit           | Boolean | false              | runs failsafe for unitTest                                                 |
| test.int            | Boolean | false              | alias for test.integration                                                 |
| test.integration    | Boolean | false              | runs surefire integration, component, contract, smoke                      |
| JACOCO              | Boolean | false              | runs failsafe integration test and surefire unitTest                       |

# UNDER CONSTRUCTION

### Building
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| clean               | Boolean | false              | cleans target and resolves dependencies                                    |
| clean.cache         | Boolean | false              | Purges local maven repository cache                                        |
| java.doc            | Boolean | false              | Creates java doc (javadoc.jar) if its not a pom artifact                   |
| java.source         | Boolean | false              | Creates java sources (sources.jar) if its not a pom artifact               |
| gpg.pass            | String  | ''                 | Signs artifacts (.asc) with GPG 2.1                                        |
### Deployment
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| DEPLOY_ID           | String  | ''                 | (Nexus) Deploys artifacts (server id = Settings.xml)                       |
| RELEASE             | Boolean | false              | (Nexus) Releases the deployment                                            |
| NEXUS_BASE_URL      | String  | ''                 | (Nexus) The nexus base url (e.g https://my.nexus.com)                      |
| NEXUS_DEPLOY_URL    | String  | ''                 | (Nexus) Staging url (e.g https://my.nexus.com/service/local/staging/deploy)|
### Add to Settings.xml session
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| Server              | String | ''                  | server id (multiple possible && caseInsensitive)                           |
| Username            | String | ''                  | username (multiple possible && caseInsensitive)                            |
| Password            | String | ''                  | password (multiple possible && caseInsensitive)                            |
| PrivateKey          | String | ''                  | e.g. ${user.home}/.ssh/id_dsa) (multiple possible && caseInsensitive)      |
| Passphrase          | String | ''                  | privateKey, passphrase (multiple possible && caseInsensitive)              |
| FilePermissions     | String | ''                  | permissions, e.g. 664, or 775  (multiple possible && caseInsensitive)      |
| DirectoryPermissions| String | ''                  | permissions, e.g. 664, or 775  (multiple possible && caseInsensitive)      |
### Misc
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| REPORT              | Boolean | false              | Generates report about version updates                                     |                                            |                                                  |
| test.skip           | Boolean | false              | same as "maven.test.skip"                                                  |
| project.encoding    | Boolean | false              | sets default encoding to every encoding parameter definition               |
| java.version        | Boolean | false              | sets default java version to every java version parameter definition       |

### Requirements
* \[JAVA\] for maven 
* \[MAVEN\] to run maven commands
* \[GIT\] for tagging

### Technical links
* [maven-javadoc-plugin](https://maven.apache.org/plugins/maven-javadoc-plugin/)
* [maven-source-plugin](https://maven.apache.org/plugins/maven-source-plugin/)
* [maven-surefire-plugin](http://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html)
* [versions-maven-plugin](https://www.mojohaus.org/versions-maven-plugin/set-mojo.html)
* [maven-gpg-plugin](http://maven.apache.org/plugins/maven-gpg-plugin/usage.html)
* [maven-scm-plugin](http://maven.apache.org/scm/maven-scm-plugin/plugin-info.html)
* [upload-an-artifact-into-Nexus](https://support.sonatype.com/hc/en-us/articles/213465818-How-can-I-programmatically-upload-an-artifact-into-Nexus-2-)

### TODO
* [ ] tag message can contain environment properties
* [ ] not tag when last commit was tag commit
* [ ] set always autoReleaseAfterClose=false and add "mvn nexus-staging:release" to release process
* [ ] Deploy dynamic to nexus
* [ ] Deploy dynamic to artifactory
* [ ] try to use JGit for git service
* [ ] org.sonatype.plugins
* [ ] own or buy logo https://www.designevo.com/apps/logo/?name=blue-hexagon-and-3d-container

![deployment-maven-plugin](src/main/resources/banner.png "deployment-maven-plugin")

[License-Url]: https://www.apache.org/licenses/LICENSE-2.0
[Build-Status-Url]: https://travis-ci.org/YunaBraska/deployment-maven-plugin
[Build-Status-Image]: https://travis-ci.org/YunaBraska/deployment-maven-plugin.svg?branch=master
[Coverage-Url]: https://codecov.io/gh/YunaBraska/deployment-maven-plugin?branch=master
[Coverage-image]: https://codecov.io/gh/YunaBraska/deployment-maven-plugin/branch/master/graphs/badge.svg
[Maintainable-Url]: https://codeclimate.com/github/YunaBraska/deployment-maven-plugin
[Maintainable-image]: https://codeclimate.com/github/YunaBraska/deployment-maven-plugin.svg
[Javadoc-url]: http://javadoc.io/doc/berlin.yuna/deployment-maven-plugin
[Javadoc-image]: http://javadoc.io/badge/berlin.yuna/deployment-maven-plugin.svg
[Gitter-Url]: https://gitter.im/nats-streaming-server-embedded/Lobby
[Gitter-image]: https://img.shields.io/badge/gitter-join%20chat%20%E2%86%92-brightgreen.svg

[Dependency-shield]: https://img.shields.io/librariesio/github/YunaBraska/deployment-maven-plugin?style=flat-square
[Tag-shield]: https://img.shields.io/github/v/tag/YunaBraska/deployment-maven-plugin?style=flat-square
[Central-shield]: https://img.shields.io/maven-central/v/berlin.yuna/deployment-maven-plugin?style=flat-square
[Size-shield]: https://img.shields.io/github/repo-size/YunaBraska/deployment-maven-plugin?style=flat-square
[Issues-shield]: https://img.shields.io/github/issues/YunaBraska/deployment-maven-plugin?style=flat-square
[License-shield]: https://img.shields.io/github/license/YunaBraska/deployment-maven-plugin?style=flat-square
[Commit-shield]: https://img.shields.io/github/last-commit/YunaBraska/deployment-maven-plugin?style=flat-square
[Label-shield]: https://img.shields.io/badge/Yuna-QueenInside-blueviolet?style=flat-square
[Build-shield]: https://img.shields.io/travis/YunaBraska/deployment-maven-plugin/master?style=flat-square