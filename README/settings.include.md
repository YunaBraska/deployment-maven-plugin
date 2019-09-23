Adding servers additional to the settings.xml.
Its also possible to set the properties as environment variables (same as with every property)
### Parameters
| Parameter           | Type    | Default            |  Description                                                               |
|:--------------------|:--------|:-------------------|:---------------------------------------------------------------------------|
| Server              | String | ''                  | server id                                                                  |
| Username            | String | ''                  | username                                                                   |
| Password            | String | ''                  | password                                                                   |
| PrivateKey          | String | ''                  | e.g. ${user.home}/.ssh/id_dsa)                                             |
| Passphrase          | String | ''                  | privateKey, passphrase                                                     |
| FilePermissions     | String | ''                  | permissions, e.g. 664, or 775                                              |
| DirectoryPermissions| String | ''                  | permissions, e.g. 664, or 775                                              |
* There are three different ways to configure the maven settings 
* Settings format one
```bash
settings.xml='--ServerId=servername1 --Username=username1 --Password=password --ServerId="servername2" --Username=username2'
```
* Settings format two
```bash
server='serverId1::username1::password1::privateKey1::passphrase1'
server0='serverI2::username2::password2::privateKey2::passphrase2'
server1='serverI3::username3::password3::privateKey3::passphrase3'
server2='serverI4::username4::password4::privateKey4::passphrase4'
[...]
```
* Settings format three
```bash
server1.Id='serverId1'
server1.username='username1'
server1.password='password1'
server1.privateKey='privateKey1'
server1.passphrase='passphrase1'
server2.Id='serverId2'
server2.username='username2'
server2.password='password2'
server2.privateKey='privateKey2'
server2.passphrase='passphrase2'
[...]
```