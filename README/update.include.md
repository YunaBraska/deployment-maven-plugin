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
