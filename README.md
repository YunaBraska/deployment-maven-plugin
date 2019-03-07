# maven-deployment

[![License][License-Image]][License-Url]
[![Build][Build-Status-Image]][Build-Status-Url] 
[![Coverage][Coverage-image]][Coverage-Url] 
[![Maintainable][Maintainable-image]][Maintainable-Url] 
[![Gitter][Gitter-image]][Gitter-Url] 

### Description
This is an example how to separate the deployment from build process in maven which I am using for my deployments to keep also the pom.xml small and not have a parent which is needed to be also in maven central 

### Example
````bash
ci.bash --PROJECT_VERSION=3.2.1.2.3 --JAVA_VERSION=1.8 --ENCODING=UTF-8 --MVN_PROFILES=true --MVN_CLEAN=true --MVN_UPDATE=true --MVN_JAVA_DOC=true --MVN_SOURCE=true --GIT_TAG=true
````

### Technical links
* https://maven.apache.org/plugins/maven-javadoc-plugin/
* https://maven.apache.org/plugins/maven-source-plugin/
* http://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html

### TODO
* [ ] Maven custom meta data like UTF-8, Java version, custom param....
* [ ] SCM DEPLOY TAG
* [ ] org.sonatype.plugins
* [ ] RELEASE <autoVersionSubmodules>true</autoVersionSubmodules> <useReleaseProfile>true</useReleaseProfile>
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