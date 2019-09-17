# Semantic- && versioning
### Parameters
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| project.version     | String  | ''                 | Sets project version in pom                                                |
| project.snapshot    | Boolean | false              | Adds -SNAPSHOT to project version in pom                                   |
| remove.snapshot     | Boolean | false              | Removes snapshot from version                                              |
| semantic.format     | String  | ''                 | Updates semantic version from regex pattern (overwrites project.version)   |

### Semantic version
* Sets the version coming from branch name (uses git refLog for that)
* The matching branch names has to be defined by using regex
* Syntax:
* ````"<separator>::<major>::<minor>::<patch>"```` 
* Example:
* ````semantic.format="[.-]::release.*::feature.*::bugfix\|hotfix::custom_1.*[A-Z]"````