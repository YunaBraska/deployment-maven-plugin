| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| deploy              | Boolean | ''                 | Start deployment                                                           |
| deploy.snapshot     | Boolean | ''                 | Start snapshot deployment && adds temporary "-SNAPSHOT" to the project version |
| deploy.nexus        | Boolean | '${auto}'          | auto = triggers nexus deploy plugin on url keywords "nexus", "oss", or "sonatype" |
| deploy.id           | String  | ${settings.get(0)} | Id from server settings or settings.xml - default first setting server id containing ids like 'nexus', 'artifact', 'archiva', 'repository', 'snapshot'|
| deploy.url          | String  | ''                 | url to artifact repository - re-prioritize default setting server id if contains keywords from 'deploy.id' |
##### Examples:
```shell script
mvn deployment-maven-plugin:run -Dclean -Djava.doc -Djava.source -Ddeploy.url='https://user:pw@url:port/libs-release' -Ddeploy=true
mvn deployment-maven-plugin:run -Dclean -Djava.doc -Djava.source -Ddeploy.url='http://user:pw@url:port/nexus/content/repositories/snapshots' -Ddeploy=true
mvn deployment-maven-plugin:run -Dclean -Djava.doc -Djava.source -Ddeploy.url='http://user:pw@url:port/nexus/content/repositories/releases' -Ddeploy=true
```
