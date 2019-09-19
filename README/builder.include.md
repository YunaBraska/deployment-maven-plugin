### Parameters
| Parameter           | Type    | Default            |  Description                                                                    |
|:--------------------|:--------|:-------------------|:--------------------------------------------------------------------------------|
| builder             | Boolean | false              | Will start translating builder files with pattern "fileName.builder.extension"  |

### Builder file content
* Builder files are **simple templates** mainly used for readme files
* The placeholders will be replaced by **maven environment variables** and **git config variables** (git config -l )
* Git config variables starts with 'git.' like **'git.remote.origin.url'**
* 'target' (optional special variable) defines the target directory in the project
* Example
````text
\[var myVariableName]: # (This is my variable value)
\[var project.description]: # (This overwrites the maven environment variable)
\[var varInVar]: # (This contains !{myVariableName} variable)
\[var target]: # (/new/readme/directory/path)
\[var target]: # (subDirOfCurrent)
\[include]: # (/path/include.file)

# My project name: \!{project.name}
## My project git origin url: \!{git.remote.origin.url}
### My display own variable: \!{varInVar}
```` 