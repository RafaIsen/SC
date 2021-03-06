/*****************************************
*   Seguranca e Confiabilidade 2016/17
*
*     /* Como executar o trabalho */
*
*              Grupo 34
*****************************************/

Para executar o Trabalho 2 ter� de ser criado o projeto dentro da diretoria: "${user.home}/git/SC/Trabalho2".

Criar a diretoria "server".

Assim feito, ter� de ser criado um par de chaves para o servidor dentro do diret�rio "server" e guardar o seu certificado na truststore do cliente:

IMPORTANTE: Todos os comandos seguintes devem ser executados no diret�rio "Trabalho2"

PASSO 1 (PAR DE CHAVES DO SERVIDOR):

IMPORTANTE: Para ser criado o par de chaves do cliente e o certificado do servidor ser importado para a sua truststore ser� preciso primeiro 
criar o seu reposit�rio com o comando: -init <rep_name> , escrito no "Program arguments" em "Run Configurations" do Eclipse.

	1. Criar o par de chaves RSA do servidor
		> keytool -genkeypair -alias myServer -keyalg RSA -keysize 2048 -keystore server/myServer.keyStore
	
IMPORTANTE: A password que dever� definir para a keystore do servidor e do alias ser� "sc1617"!

	2. Exportar o certificado auto-assinado do servidor
		> keytool -exportcert -alias myServer -file server/myServer.cer -keystore server/myServer.keyStore

	3. Importar certificado da truststore do servidor para a do cliente (o servidor serve de CA, 
	   j� que seu certificado � auto-assinado)
		> keytool -importcert -alias myServer -keystore <rep_name>/keys/myClient.keyStore -file server/myServer.cer
		
IMPORTANTE: A password que dever� definir para a keystore do cliente e do alias ser� "client1617"!

Assim poder� criar um par de chaves para cada cliente dentro do seu reposit�rio criado em cima e guardar o seu certificado na truststore do servidor:

PASSO 2 (PAR DE CHAVES DO CLIENTE):

IMPORTANTE: Para ser criado o par de chaves do cliente e o certificado do servidor ser importado para a sua truststore ser� preciso primeiro 
criar o seu reposit�rio com o comando: -init <rep_name>

	1. Criar o par de chaves RSA do cliente
		> keytool -genkeypair -alias <alias> -keyalg RSA -keysize 2048 -keystore <rep_name>/keys/myClient.keyStore
	
IMPORTANTE: a password que dever� definir para a keystore do cliente e do alias ser� "client1617"!

	2. Exportar o certificado auto-assinado do client
		> keytool -exportcert -alias <alias> -file <rep_name>/keys/myClient.cer -keystore <rep_name>/keys/myClient.keyStore

	3. Importar certificado da truststore do cliente para a do servidor (o cliente serve de CA, 
	   j� que seu certificado � auto-assinado)
		> keytool -importcert -alias <alias> -keystore server/myServer.keyStore -file <rep_name>/keys/myClient.cer
	
	4. (Caso ainda n�o feito) Importar certificado da truststore do servidor para a do cliente (o servidor serve de CA, 
	   j� que seu certificado � auto-assinado)
		> keytool -importcert -alias myServer -keystore <rep_name>/keys/myClient.keyStore -file server/myServer.cer

(SHARE)
IMPORTANTE: Sempre que algum utilizador der acesso a outro atrav�s de um share ter� de ser inserido este c�digo na consola:
						
		> keytool -importcert -alias <alias> -keystore <rep>/keys/myClient.keyStore -file <rep_name>/keys/myClient.cer
			
		<alias> = alias do keystore do utilizador que quer dar acesso a outro
		<rep_outro> = reposit�rio do utilizador a quem quer dar acesso
		<rep_name> = reposit�rio do utilizador que quer dar acesso

PASSO 4 (MOVER OS REPOSIT�RIOS CRIADOS PARA UMA PASTA EM "Trabalho2" COM O NOME DO UTILIZADOR QUE TER� DE CRIAR):

	Trabalho2/<rep_criado> -> Trabalho2/<nome_utilizador> => Trabalho2/<nome_utilizador>/<rep_criado>

PASSO 5 (MOVER OS REPOSIT�RIOS "keys" CRIADOS PARA A PASTA EM "Trabalho2" COM O NOME DO UTILIZADOR QUE ACABOU DE CRIAR):

	Trabalho2/<nome_utilizador>/<rep_criado>/keys -> Trabalho2/<nome_utilizador> => Trabalho2/<nome_utilizador>/keys

PASSO 6 (EXECUTAR SERVIDOR)

PASSO 7 (EXECUTAR CLIENTE):

Criados os pares de chaves, � s� executar o servidor "myGitServer" com a password "sc1617". 
E executar o cliente escrevendo no "Program arguments" em "Run Configurations" do Eclipse e executando: [args].

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