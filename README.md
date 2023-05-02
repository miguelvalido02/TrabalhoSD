Autores:
Artur Horal 59744
Miguel Valido 60477

Escolhemos a alternativa 1, ou seja, na função postMessage, as mensagens são enviadas para o feed dos subscritores 
e a operação getMessages retorna simplesmente as mensagens do feed.

Como ultrapassámos os desafios desta alternativa:
-Um único pedido por domínio na operação postMessage (função postOutside). Assim, 
se um user tiver 300 seguidores de 2 domínios diferentes, o servidor efetua apenas dois pedidos.

-Cada pedido é feito por uma thread diferente. Desta maneira, todos os pedidos são efetuados independentemente da falha de um deles.

-Quando o envio de um pedido falha, tenta-se várias vezes para quando o servidor do outro lado voltar a estar ativo, receber o pedido.

 Para não criar vários clientes (operação custosa), tanto no UsersClientFactory como no FeedsClientFactory foi criado um hashMap que guarda
 as instâncias de todos os clientes já existentes organizados pela string correspondente ao uri.
 private static Map<String, Users> clients = new ConcurrentHashMap<String, Users>();
 private static Map<String, Feeds> clients = new ConcurrentHashMap<String, Feeds>();

 Para um dado user, são guardados os seguidores no respetivo servidor de feeds organizados por domínio. 
 Desta maneira, quando um user faz post, apenas um pedido é efetuado por domínio.
 private Map<String, Map<String, List<String>>> followers; // <username,<domain,<user@domain>>>
 
 Métodos adicionados:
 -userExists(String name)->Retorna o user se existir sem verificar a palavra-passe. Desta maneira,
 é possível verificar se um user existe ou não sem saber a palavra-passe.
 
 -postOutside(String user, Message msg)->Recebe um pedido quando o user de outro domínio fez post e tem seguidores neste. A mensagem é adicionada ao feed dos
 seguidores deste user.
 
 -deleteFeed(String user, String domain, String pwd)->Pedido efetuado do servidor users ao servidor feeds quando um user é removido do sistema. 
 Remove o feed deste user e todas as subscrições nos vários domínios. Para tal, este pedido é propagado do servidor feeds do domínio do user para os servidores feeds
 dos domínios dos seguidores.
