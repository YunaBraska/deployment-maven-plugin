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