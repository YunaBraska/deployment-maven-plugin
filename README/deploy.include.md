| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| deploy              | Boolean | ''                 | Start deployment                                                           |
| deploy.snapshot     | Boolean | ''                 | Start snapshot deployment && adds temporary "-SNAPSHOT" to the project version |
| deploy.id           | String  | ${settings.get(0)} | Id from server settings or settings.xml - default first setting server id containing ids like 'nexus', 'artifact', 'archiva', 'repository', 'snapshot'|
| deploy.url          | String  | ''                 | url to artifact repository - re-prioritize default setting server id if contains keywords from 'deploy.id' |