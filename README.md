# maven-deployment

[![License][License-Image]][License-Url]
[![Build][Build-Status-Image]][Build-Status-Url] 
[![Coverage][Coverage-image]][Coverage-Url] 
[![Maintainable][Maintainable-image]][Maintainable-Url] 
[![Gitter][Gitter-image]][Gitter-Url] 

### Description
This is an example/alternative to [maven-oss-parent](https://github.com/YunaBraska/maven-oss-parent) how to separate the deployment from build process in maven which I am using for my deployments to keep also the pom.xml small and not have a parent which is needed to be also in maven central 

### Requirements
* \[JAVA\] for maven 
* \[MAVEN\] to run maven commands 
* \[GIT\] for tagging

### Parameters
| Parameter       | Type    | Default |  Description                                                               |
|:----------------|:--------|:--------|:---------------------------------------------------------------------------|
| PROJECT_VERSION | String  | ''      | Sets project version in pom                                                |
| SEMANTIC_FORMAT | String  | ''      | Updates semantic version from regex pattern (overwrites PROJECT_VERSION)   |
| MVN_TAG         | Boolean | true    | Tags the project if not already done                                       |
| MVN_TAG_BREAK   | Boolean | false   | Fails at "MVN_TAG" if tag already exists                                   |
| MVN_CLEAN       | Boolean | true    | Purges local maven repository cache                                        |
| MVN_SKIP_TEST   | Boolean | false   | skips all tests                                                            |
| MVN_JAVA_DOC    | Boolean | true    | Creates java doc (-javadoc.jar)                                            |
| MVN_SOURCE      | Boolean | true    | Creates java sources (-sources.jar)                                        |
| MVN_PROFILES    | Boolean | true    | Uses all available profiles                                                |
| MVN_UPDATE      | Boolean | true    | Updates parent, props, dependencies                                        |
| MVN_RELEASE     | Boolean | true    | (Nexus) Releases the deployment                                            |
| MVN_DEPLOY_ID   | String  | ''      | (Nexus) Deploys artifacts (id = Settings.xml)                              |
| MVN_OPTIONS     | String  | ''      | Adds additional maven options                                              |
| GPG_PASSPHRASE  | String  | ''      | Signs artifacts (.asc) with GPG 2.1                                        |
| JAVA_VERSION    | String  | 1.8     | Sets compiler java version                                                 |
| ENCODING        | String  | UTF-8   | Sets compiler encoding                                                     |

### SEMANTIC_FORMAT
* Syntax \[1.2.3\]
````"<separator>::<major>::<minor>::<patch>"````
* Example \[1-2.3.4-5\]
````"[.-]::release::feature::bugfix\|hotfix::custom_1.*[0-9]::custom_2.*[A-Z]"````

### Example
````bash
ci.bash --PROJECT_VERSION=3.2.1.2.3 --JAVA_VERSION=1.8 --ENCODING=UTF-8 --MVN_PROFILES=true --MVN_CLEAN=true --MVN_UPDATE=true --MVN_JAVA_DOC=true --MVN_SOURCE=true --GIT_TAG=true
````

### Technical links
* [maven-javadoc-plugin](https://maven.apache.org/plugins/maven-javadoc-plugin/)
* [maven-source-plugin](https://maven.apache.org/plugins/maven-source-plugin/)
* [maven-surefire-plugin](http://maven.apache.org/surefire/maven-surefire-plugin/test-mojo.html)
* [maven-gpg-plugin](http://maven.apache.org/plugins/maven-gpg-plugin/usage.html)
* [upload-an-artifact-into-Nexus](https://support.sonatype.com/hc/en-us/articles/213465818-How-can-I-programmatically-upload-an-artifact-into-Nexus-2-)

### TODO
* [ ] external settings "--settings "
* [ ] release process
* [ ] set always autoReleaseAfterClose=false and add "mvn nexus-staging:release" to release process
* [ ] tag only at release
* [ ] release needs a new version to be set manually
* [ ] option/param remove snapshot
* [ ] set scm url if not exists or changed
* [ ] reset readme urls, description and title
* [ ] painful... write in other language than bash...
* [ ] option/param git commit changes
* [ ] Deploy Dynamic nexus
* [ ] Deploy artifactory

* [ ] find out how to use GPG 2.1 on command line with original apache maven-gpg-plugin
* [ ] org.sonatype.plugins
* [ ] own or buy logo https://www.designevo.com/apps/logo/?name=blue-hexagon-and-3d-container

![maven-deployment](src/main/resources/banner.png "maven-deployment")

[License-Url]: https://www.apache.org/licenses/LICENSE-2.0
[License-Image]: https://img.shields.io/badge/License-Apache2-blue.svg
[github-release]: https://github.com/YunaBraska/maven-deployment
[Build-Status-Url]: https://travis-ci.org/YunaBraska/maven-deployment
[Build-Status-Image]: https://travis-ci.org/YunaBraska/maven-deployment.svg?branch=master
[Coverage-Url]: https://codecov.io/gh/YunaBraska/maven-deployment?branch=master
[Coverage-image]: https://codecov.io/gh/YunaBraska/maven-deployment/branch/master/graphs/badge.svg
[Version-url]: https://github.com/YunaBraska/maven-deployment
[Version-image]: https://badge.fury.io/gh/YunaBraska%2Fmaven-deployment.svg
[Central-url]: https://search.maven.org/#search%7Cga%7C1%7Ca%3A%22maven-deployment%22
[Central-image]: https://maven-badges.herokuapp.com/maven-central/berlin.yuna/maven-deployment/badge.svg
[Maintainable-Url]: https://codeclimate.com/github/YunaBraska/maven-deployment
[Maintainable-image]: https://codeclimate.com/github/YunaBraska/maven-deployment.svg
[Gitter-Url]: https://gitter.im/nats-streaming-server-embedded/Lobby
[Gitter-image]: https://img.shields.io/badge/gitter-join%20chat%20%E2%86%92-brightgreen.svg
[Javadoc-url]: http://javadoc.io/doc/berlin.yuna/maven-deployment
[Javadoc-image]: http://javadoc.io/badge/berlin.yuna/maven-deployment.svg