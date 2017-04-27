/*****************************************
*   Seguranca e Confiabilidade 2016/17
*
*     /* Como executar o trabalho */
*
*				Grupo 34
*****************************************/

Para executar o Trabalho 2 ter� de ser criado o projeto dentro da diretoria: "${user.home}/git/SC/Trabalho2".

Assim feito, ter� de ser criado um par de chaves para o servidor dentro do diret�rio "server" e guardar o seu certificado na truststore do cliente:

IMPORTANTE: Todos estes comandos devem ser executados no diret�rio "Trabalho2"

IMPORTANTE: Para ser criado o par de chaves do cliente e o certeficado do servidor ser importado para a sua truststore ser� preciso primeiro 
criar o seu reposit�rio com o comando: java myGit -init <rep_name>

	1. Criar o par de chaves RSA do servidor
		> keytool -genkeypair -alias myServer -keyalg RSA -keysize 2048 -keystore server/myServer.keyStore
	
IMPORTANTE: a password que dever� definir para a keystore do servidor e do alias ser� "sc1617"!

	2. Exportar o certificado auto-assinado do servidor
		> keytool -exportcert -alias myServer -file server/myServer.cer -keystore server/myServer.keystore

	3. Importar certificado da truststore do servidor para a do cliente (o servidor serve de CA, 
	   j� que seu certificado � auto-assinado)
		> keytool -importcert -alias myServer -keystore <rep_name>/keys/myClient.keyStore -file server/myServer.cer
		
IMPORTANTE: a password que dever� definir para a keystore do cliente e do alias ser� "client1617"!

Assim poder� criar um par de chaves para cada cliente dentro do seu reposit�rio criado em cima e guardar o seu certificado na truststore do servidor:

IMPORTANTE: todos estes comandos devem ser executados no diret�rio "Trabalho2"

IMPORTANTE: Para ser criado o par de chaves do cliente e o certeficado do servidor ser importado para a sua truststore ser� preciso primeiro 
criar o seu reposit�rio com o comando: java myGit -init <rep_name>

	1. Criar o par de chaves RSA do cliente
		> keytool -genkeypair -alias <alias> -keyalg RSA -keysize 2048 -keystore <rep_name>/keys/myClient.keyStore
	
IMPORTANTE: a password que dever� definir para a keystore do cliente e do alias ser� "client1617"!

	2. Exportar o certificado auto-assinado do client
		> keytool -exportcert -alias <alias> -file <rep_name>/keys/myClient.cer -keystore <rep_name>/keys/myClient.keystore

	3. Importar certificado da truststore do cliente para a do servidor (o cliente serve de CA, 
	   j� que seu certificado � auto-assinado)
		> keytool -importcert -alias <alias> -keystore server/myServer.keyStore -file <rep_name>/keys/myClient.cer
		
IMPORTANTE: Sempre que algum utilizador der acesso a outro atrav�s de um share ter� de ser inserido este c�digo na consola:
						
		> keytool -importcert -alias <alias> -keystore <rep>/myServer.keyStore -file <rep_name>/keys/myClient.cer
			
		<alias> = alias do keystore do utilizador que faz share
		<rep> = reposit�rio do utilizador a quem quer dar acesso

Criados os pares de chaves, � s� executar o servidor "myGitServer" com a password "sc1617". 
E executar o cliente escrevendo sobre a linha de comandos na pasta bin das classes java: java myGit [args]

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