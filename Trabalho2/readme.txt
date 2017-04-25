/*****************************************
*   Seguranca e Confiabilidade 2016/17
*
*     /* Como executar o trabalho */
*
*		Grupo 34
*****************************************/

Para executar o Trabalho 2 terá de ser criado o projeto dentro da diretoria: "${user.home}/git/SC/Trabalho2".

Assim feito, terá de ser criado um par de chaves para o servidor e guardar o seu certificado na truststore do cliente:

1. Criar o par de chaves RSA do servidor
> keytool -genkeypair -alias myServer -keyalg RSA -keysize 2048 -keystore myServer.keyStore
IMPORTANTE: a password que deverá definir para a keystore do servidor será "sc1617"!

2. Exportar o certificado auto-assinado do servidor
> keytool -exportcert -alias myServer -file myServer.cer -keystore myServer.keystore

3. Importar certificado da truststore do servidor para a do cliente (o servidor serve de CA, 
   já que seu certificado é auto-assinado)
> keytool -importcert -alias myServer -keystore myClient.keyStore -file myServer.cer

Enquanto isso, terá de ser criado um par de chaves para o ciente e guardar o seu certificado na truststore do servidor:

1. Criar o par de chaves RSA do cliente
> keytool -genkeypair -alias myClient -keyalg RSA -keysize 2048 -keystore myClient.keyStore
IMPORTANTE: a password que deverá definir para a keystore do cliente será "client1617"!

2. Exportar o certificado auto-assinado do client
> keytool -exportcert -alias myClient -file myClient.cer -keystore myClient.keystore

3. Importar certificado da truststore do cliente para a do servidor (o cliente serve de CA, 
   já que seu certificado é auto-assinado)
> keytool -importcert -alias myClient -keystore myServer.keyStore -file myClient.cer

Criados os pares de chaves, é só executar o servidor "myGitServer" e escrever sobre a linha de comandos 
na pasta bin das classes java: java myGit [args]

Podendo ser os seguintes [args]:

<localUser> <serverAddress> [-p <password>] [methods]

[methods]:

-init <rep_name>
-push <file_name>
-push <rep_name>
-pull <file_name>
-pull <rep_name>
-share <rep_name> <userId>
-remove <rep_name> <userId>