# main.py
import os
from firebase_functions import https_fn, options, pubsub_fn, firestore_fn
from firebase_admin import initialize_app, firestore
from google.auth import default as get_credentials
from google.auth.transport.requests import Request
from googleapiclient.discovery import build
from google.api_core.exceptions import InvalidArgument
import google.generativeai as genai
from google.generativeai.types import GenerationConfig, HarmCategory, HarmBlockThreshold
from firebase_admin import messaging
import requests
import logging
import re
import unicodedata

import base64
import json


# Inicializa o Firebase Admin SDK para acesso ao Firestore.
initialize_app()

# Define a região, se necessário (ajuda a evitar avisos no deploy).
# Use a região mais próxima de seus usuários, ex: "us-central1" ou "southamerica-east1".
options.set_global_options(region="southamerica-east1")

# --- Constantes de Configuração ---
PACKAGE_NAME = "com.example.startuppulse"  # CONFIRME SE ESTE É O SEU PACKAGE NAME
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]

# @https_fn.on_call()
# def validate_purchase(req: https_fn.CallableRequest) -> dict:
#     """
#     Função 'chamável' que valida um token de compra com a API do Google Play.
#     Se válido, atualiza o status do usuário no Firestore.
#     """
#     # 1. Validação de Autenticação e Entradas
#     if not req.auth:
#         raise https_fn.HttpsError(
#             code="unauthenticated",
#             message="Você precisa estar autenticado para validar uma compra."
#         )
#
#     uid = req.auth.uid
#     purchase_token = req.data.get("purchaseToken")
#     sku = req.data.get("sku")
#
#     if not purchase_token or not sku:
#         raise https_fn.HttpsError(
#             code="invalid-argument",
#             message="A função foi chamada sem 'purchaseToken' ou 'sku'."
#         )
#
#     try:
#         # 2. Autenticação com a API do Google Play
#         print("Autenticando com a API do Google Play...")
#         credentials, _ = get_credentials(scopes=SCOPES)
#         credentials.refresh(Request())
#
#         android_publisher = build(
#             "androidpublisher", "v3", credentials=credentials
#         )
#
#         # 3. Validação do Token com os Servidores do Google
#         print(f"Validando token para o SKU: {sku}")
#         response = (
#             android_publisher.purchases()
#             .subscriptions()
#             .get(
#                 packageName=PACKAGE_NAME,
#                 subscriptionId=sku,
#                 token=purchase_token,
#             )
#             .execute()
#         )
#
#         # 4. Processamento da Resposta e Escrita no Firestore
#         expiry_time_millis = int(response.get("expiryTimeMillis"))
#         start_time_millis = int(response.get("startTimeMillis"))
#
#         db = firestore.client()
#         user_doc_ref = db.collection("premium").document(uid)
#
#         dados = {
#             "ativo": True,
#             "data_assinatura": firestore.SERVER_TIMESTAMP,
#             "data_fim": firestore.firestore.DatetimeWithNanoseconds.from_timestamp_millis(expiry_time_millis),
#             "plano": "PRO",
#             "purchaseToken": purchase_token,
#         }
#
#         print(f"Compra válida. Atualizando documento para o usuário: {uid}")
#         user_doc_ref.set(dados)
#
#         return {"status": "success", "message": "Assinatura PRO validada e ativada!"}
#
#     except Exception as e:
#         print(f"Erro ao validar a compra: {e}")
#         raise https_fn.HttpsError(
#             code="internal",
#             message="Ocorreu um erro interno ao processar sua assinatura."
#         )
#
# @pubsub_fn.on_message_published(topic="play-store-notifications")
# def handle_play_notification(event: https_fn.CloudEvent) -> None:
#     """
#     Função acionada por mensagens no Pub/Sub para processar notificações da Play Store.
#     """
#     print(f"Recebida notificação da Play Store: {event.data}")
#
#     try:
#         # A mensagem vem codificada em Base64
#         message_data_str = base64.b64decode(event.data["message"]["data"]).decode("utf-8")
#         message_json = json.loads(message_data_str)
#
#         # O payload real da notificação está dentro de 'subscriptionNotification'
#         notification = message_json.get("subscriptionNotification")
#         if not notification:
#             print("Notificação não é do tipo 'subscriptionNotification'. Ignorando.")
#             return
#
#         purchase_token = notification.get("purchaseToken")
#         notification_type = notification.get("notificationType")
#
#         # Para encontrar o UID do usuário, precisamos buscar pelo purchaseToken.
#         # Esta é uma operação que pode ser lenta, mas é necessária.
#         db = firestore.client()
#         premium_ref = db.collection("premium")
#         query = premium_ref.where("purchaseToken", "==", purchase_token).limit(1)
#         docs = list(query.stream())
#
#         if not docs:
#             print(f"Nenhum usuário encontrado com o purchaseToken: {purchase_token}. Ignorando.")
#             return
#
#         user_doc = docs[0]
#         uid = user_doc.id
#         user_doc_ref = user_doc.reference
#
#         print(f"Processando notificação tipo {notification_type} para o usuário {uid}.")
#
#         # --- Lógica para tratar os diferentes tipos de notificação ---
#         # Documentação dos tipos: https://developer.android.com/google/play/billing/rtdn-reference
#
#         # Assinatura foi revogada (ex: pelo suporte do Google)
#         if notification_type == 5: # SUBSCRIPTION_REVOKED
#             user_doc_ref.update({"ativo": False, "plano": "revogado"})
#             print(f"Assinatura revogada para o usuário {uid}.")
#
#         # Assinatura expirou
#         elif notification_type == 12: # SUBSCRIPTION_EXPIRED
#             user_doc_ref.update({"ativo": False, "plano": "expirado"})
#             print(f"Assinatura expirada para o usuário {uid}.")
#
#         # Assinatura foi cancelada pelo usuário (mas ainda está ativa até o fim do período)
#         elif notification_type == 3: # SUBSCRIPTION_CANCELED
#             # Aqui você pode apenas registrar que foi cancelada, mas manter 'ativo' como true.
#             # A expiração será tratada pelo evento SUBSCRIPTION_EXPIRED.
#             user_doc_ref.update({"statusCancelamento": "cancelado_pelo_usuario"})
#             print(f"Assinatura marcada como cancelada para o usuário {uid}.")
#
#         else:
#             print(f"Tipo de notificação {notification_type} não tratado. Ignorando.")
#
#     except Exception as e:
#         print(f"Erro ao processar a notificação da Play Store: {e}")
#         # É importante não lançar um erro aqui, para que o Pub/Sub não tente reenviar a mensagem indefinidamente

@https_fn.on_call(secrets=["GEMINI_API_KEY"])
def gerar_pre_analise_ia(req: https_fn.CallableRequest) -> dict:
    """
    Acionado pelo app para analisar uma ideia usando IA.
    """
    # 1. Validação de Autenticação (sem mudanças)
    if not req.auth:
        raise https_fn.HttpsError(
            code="unauthenticated",
            message="Autenticação necessária para solicitar análise."
        )
    uid = req.auth.uid
    ideia_id = req.data.get("ideiaId")
    if not ideia_id:
        raise https_fn.HttpsError(code="invalid-argument", message="O 'ideiaId' é obrigatório.")
    print(f"Iniciando análise de IA para a ideia: {ideia_id} (Usuário: {uid})")

    # 2. Configurar a API de IA (MODIFICADO)
    try:
        genai.configure(api_key=os.environ.get("GEMINI_API_KEY"))

        # <<< ALTERAÇÃO AQUI: Especifica o modelo e as safety settings >>>
        model = genai.GenerativeModel(
            'gemini-2.5-pro', # Mantendo o modelo versionado
            safety_settings={ # Configuração de segurança - ajuste conforme necessário
                HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
                HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
                HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
                HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
            }
        )
    except Exception as e:
        print(f"Erro ao configurar o modelo Gemini: {e}")
        raise https_fn.HttpsError(code="internal", message="Erro ao carregar o modelo de IA.")

    # 3. Buscar os dados da ideia no Firestore (sem mudanças)
    db = firestore.client()
    ideia_ref = db.collection("ideias").document(ideia_id)

    try:
        ideia_doc = ideia_ref.get()
        if not ideia_doc.exists:
            raise https_fn.HttpsError(code="not-found", message="Ideia não encontrada.")
        ideia_data = ideia_doc.to_dict()
        if ideia_data.get("ownerId") != uid:
            raise https_fn.HttpsError(code="permission-denied", message="Você não é o dono desta ideia.")
        print("Dados da ideia recuperados. Construindo prompt...")

        # 4. Construir o Prompt Especialista (sem mudanças)
        prompt = construir_prompt_especialista(ideia_data)

        # 5. Chamar a API de IA Generativa (MODIFICADO)
        # <<< ALTERAÇÃO AQUI: Passa a config direto no generate_content >>>
        generation_config = GenerationConfig( # Note que agora usamos a classe diretamente
            response_mime_type="application/json"
        )
        # <<< ALTERAÇÃO AQUI: Usa a nova config e não especifica api_version aqui >>>
        response = model.generate_content(prompt, generation_config=generation_config)

        # 6. Processar e Salvar a Resposta (sem mudanças)
        print("Resposta da IA recebida. Processando e salvando...")
        ai_feedback = json.loads(response.text) # A resposta já deve ser JSON por causa da config
        ai_feedback["metadata"] = {
            "analysis_timestamp": firestore.SERVER_TIMESTAMP,
            "model_used": 'gemini-1.0-pro' # Atualizado para o modelo usado
        }

        # 7. Salvar a análise no documento da ideia (sem mudanças)
        ideia_ref.update({"avaliacaoIA": ai_feedback})
        print(f"Sucesso! Análise salva na ideia: {ideia_id}")
        return {"status": "success", "message": "Análise da IA concluída!"}

    except InvalidArgument as e:
        print(f"Erro de Argumento Inválido (provavelmente prompt bloqueado): {e}")
        raise https_fn.HttpsError(code="invalid-argument", message=f"A IA não pôde processar esta ideia. Causa: {e}")
    except Exception as e:
        print(f"Erro inesperado ao gerar análise de IA: {e}")
        raise https_fn.HttpsError(code="internal", message=f"Erro interno: {e}")

def construir_prompt_especialista(ideia_data: dict) -> str:
    """
    Cria o prompt que "ensina" a IA a ser um mentor.
    """
    nome = ideia_data.get("nome", "N/A")
    descricao = ideia_data.get("descricao", "N/A")
    postits_map = ideia_data.get("postIts", {})

    postits_str = ""
    for etapa, lista_postits in postits_map.items():
        # Limpa a chave (ex: 'proposta_valor' -> 'Proposta de Valor')
        etapa_formatada = etapa.replace("_", " ").title()
        postits_str += f"\n### {etapa_formatada}:\n"

        if isinstance(lista_postits, list) and lista_postits:
            for postit in lista_postits:
                # O seu PostIt.java não tem 'texto', mas sim 'descricao'.
                # Vamos assumir que os postits são mapas com 'texto' ou 'descricao'.
                # Baseado no seu `Ideia.java`, `postIts` é Map<String, List<PostIt>>.
                # E `PostIt.java` não está nos arquivos.
                # VOU ASSUMIR que `lista_postits` é uma lista de Mapas
                # e que cada mapa tem uma chave 'texto' ou 'descricao'.
                # Se `PostIt` é um objeto, o Firestore o salva como um Map.
                texto_postit = "Post-it vazio"
                if isinstance(postit, dict):
                    texto_postit = postit.get('texto', postit.get('descricao', 'Post-it vazio'))

                postits_str += f"- {texto_postit}\n"
        else:
            postits_str += "- (Nenhum post-it cadastrado)\n"

    # O PROMPT
    prompt = f"""
    **Persona:** Você é o "Mentor IA" da plataforma Startup Pulse. Sua especialidade é analisar
    ideias de startups em estágio inicial (early-stage) com base no Business Model Canvas.
    Seja construtivo, honesto, direto e forneça insights práticos e acionáveis.
    Sua linguagem deve ser profissional, mas encorajadora.

    **Tarefa:** Analise a ideia de startup a seguir e forneça um feedback estruturado.
    Aponte falhas de lógica, elogie pontos fortes e dê sugestões claras.

    **Dados da Ideia:**
    * **Nome:** {nome}
    * **Descrição:** {descricao}
    
    **Dados do Canvas (Post-its):**
    {postits_str}

    **Análise Requerida:**
    Forneça sua análise EXATAMENTE no formato JSON solicitado. Não inclua "```json" ou qualquer
    outro texto fora do objeto JSON.

    **Formato de Saída (JSON):**
    {{
      "resumo_geral": "(Faça um resumo de 1-2 frases sobre a sua impressão geral da ideia,
                      destacando o ponto mais crítico, seja ele bom ou ruim)",
      "pontos_fortes": [
        "(Principal ponto forte. Ex: 'Proposta de valor clara e focada em uma dor real do cliente.')",
        "(Segundo ponto forte...)"
      ],
      "pontos_fracos_e_riscos": [
        "(Principal risco ou fraqueza. Ex: 'O modelo de receita não está claro e parece
                      insustentável.')",
        "(Segundo risco. Ex: 'Os canais de aquisição e o segmento de clientes
                      não parecem alinhados.')"
      ],
      "avaliacoes_por_criterio": [
        {{"criterio": "Potencial de Mercado", "nota": 0.0, "feedback": "(Seu feedback sobre o
                        tamanho e a clareza do mercado-alvo.)"}},
        {{"criterio": "Clareza da Proposta de Valor", "nota": 0.0, "feedback": "(O quão bem a
                        ideia resolve a dor do cliente? É uma solução clara?)"}},
        {{"criterio": "Coerência do Canvas", "nota": 0.0, "feedback": "(As diferentes partes do
                        canvas (ex: clientes, canais, proposta) fazem sentido juntas?)"}},
        {{"criterio": "Viabilidade (Próximos Passos)", "nota": 0.0, "feedback": "(O quão
                        difícil parece ser para o fundador validar e construir um MVP desta ideia?)"}}
      ],
      "sugestoes_proximos_passos": [
        "(Sugestão nº 1, curta e prática. Ex: 'Validar a dor do 'Segmento de Cliente' com
                        10 entrevistas antes de escrever qualquer código.')",
        "(Sugestão nº 2. Ex: 'Refinar a 'Estrutura de Custos', detalhando custos fixos e
                        variáveis.')"
      ]
    }}

    **Instruções para Notas:**
    - Use uma escala de 0.0 a 10.0 para as notas.
    - Seja rigoroso. Uma ideia média que precisa de muito trabalho deve ter nota 5.0.
      Uma ideia pronta para validação, 7.0. Uma ideia excepcional, 9.0+.
    - Baseie o feedback e a nota estritamente nos dados do canvas.
    """
    return prompt

# Gatilho: Acionado sempre que um documento em 'votosComunidade' for escrito (criado, atualizado, deletado)
@firestore_fn.on_document_written(document="ideias/{ideiaId}/votosComunidade/{userId}")
def calcular_media_votos_comunidade(event: firestore_fn.Event[firestore_fn.Change]) -> None:
    """
    Calcula a média ponderada dos votos da comunidade e atualiza o documento da ideia principal.
    """
    # Obtém o ID da ideia a partir do caminho do documento que disparou o evento
    ideia_id = event.params["ideiaId"]
    print(f"Evento de escrita detectado para votos da ideia: {ideia_id}")

    db = firestore.client()
    # Referência para a subcoleção de votos da ideia específica
    votos_ref = db.collection("ideias").document(ideia_id).collection("votosComunidade")

    try:
        # Lê todos os documentos (votos) da subcoleção
        votos_snapshot = list(votos_ref.stream()) # Converte para lista para poder contar

        soma_votos_ponderados = 0.0
        soma_pesos = 0.0
        total_votos = len(votos_snapshot) # Conta quantos votos existem

        # Itera sobre cada voto para calcular as somas
        for voto_doc in votos_snapshot:
            voto_data = voto_doc.to_dict()
            voto = float(voto_data.get("voto", 0.0)) # Pega o valor do voto (default 0 se não existir)
            peso = float(voto_data.get("peso", 1.0)) # Pega o peso (default 1 se não existir)

            soma_votos_ponderados += voto * peso
            soma_pesos += peso

        # Calcula a média ponderada, tratando divisão por zero
        media_ponderada = 0.0
        if soma_pesos > 0:
            media_ponderada = soma_votos_ponderados / soma_pesos
            # Arredonda para 2 casas decimais (opcional)
            media_ponderada = round(media_ponderada, 2)

        print(f"Cálculo para {ideia_id}: Média={media_ponderada}, Total Votos={total_votos}, Soma Pesos={soma_pesos}")

        # Prepara os dados para atualizar o documento principal da ideia
        dados_atualizacao = {
            "mediaPonderadaVotosComunidade": media_ponderada,
            "totalVotosComunidade": total_votos
        }

        # Referência para o documento principal da ideia
        ideia_ref = db.collection("ideias").document(ideia_id)
        # Atualiza os campos no documento da ideia
        ideia_ref.update(dados_atualizacao)

        print(f"Documento da ideia {ideia_id} atualizado com sucesso.")

    except Exception as e:
        print(f"Erro ao calcular média de votos para a ideia {ideia_id}: {e}")
        # É importante não relançar o erro aqui para evitar retentativas infinitas do gatilho

        # Gatilho: Acionado quando um documento 'ideia' é ATUALIZADO

@firestore_fn.on_document_updated(document="ideias/{ideiaId}")
def notificar_avaliacao_mentor(event: firestore_fn.Event[firestore_fn.Change]) -> None:
    """
    Envia uma notificação ao dono da ideia quando uma avaliação de mentor é adicionada/atualizada.
    """
    # --- MODIFICAÇÃO (Etapa 3) ---
    # Defina os IDs dos canais de notificação aqui para referência
    # (Eles devem ser EXATAMENTE iguais aos definidos no app Android)
    CHANNEL_ID_AVALIACOES = "STARTUP_PULSE_AVALIACOES_CHANNEL"
    CHANNEL_ID_INTERACOES = "STARTUP_PULSE_INTERACOES_CHANNEL"
    CHANNEL_ID_GERAL = "STARTUP_PULSE_GERAL_CHANNEL"
    # --- FIM DA MODIFICAÇÃO ---

    ideia_id = event.params["ideiaId"]

    # Dados antes e depois da atualização
    before_data = event.data.before.to_dict() if event.data.before else {}
    after_data = event.data.after.to_dict() if event.data.after else {}

    # Verifica se 'avaliacoes' mudou e se agora existe
    avaliacoes_before = before_data.get("avaliacoes", [])
    avaliacoes_after = after_data.get("avaliacoes", [])

    if avaliacoes_after and avaliacoes_after != avaliacoes_before:
        print(f"Detetada mudança nas avaliações da ideia: {ideia_id}")

        owner_id = after_data.get("ownerId")
        ideia_nome = after_data.get("nome", "sua ideia")
        mentor_nome = "Seu mentor" # TODO: Buscar nome do mentor

        if not owner_id:
            print(f"Erro: Dono (ownerId) não encontrado na ideia {ideia_id}.")
            return

        db = firestore.client()
        user_ref = db.collection("usuarios").document(owner_id)
        user_doc = user_ref.get()

        if not user_doc.exists:
            print(f"Erro: Perfil do usuário {owner_id} não encontrado.")
            return

        user_data = user_doc.to_dict()
        fcm_token = user_data.get("fcmToken")

        if not fcm_token:
            print(f"Usuário {owner_id} não possui token FCM registrado.")
            return

        # Monta a notificação
        notification_title = "Feedback Recebido! 🚀"
        notification_body = f"{mentor_nome} avaliou a ideia '{ideia_nome}'."

        print(f"Enviando notificação para {owner_id} (token: ...{fcm_token[-6:]})")

        try:
            # --- MODIFICAÇÃO (Etapas 2 e 3) ---
            #
            # Este é o novo payload "data-only"
            #
            message = messaging.Message(
                # REMOVIDA a chave "notification"

                # ADICIONADO "title", "body", e "channelId" dentro de "data"
                data={
                    "title": notification_title,
                    "body": notification_body,
                    "ideiaId": ideia_id,
                    "channelId": CHANNEL_ID_AVALIACOES # <-- Canal de Alta Importância
                },
                token=fcm_token,
            )
            # --- FIM DA MODIFICAÇÃO ---

            # Envia a mensagem
            response = messaging.send(message)
            print(f"Notificação enviada com sucesso para {owner_id}: {response}")

        except Exception as e:
            print(f"Erro ao enviar notificação FCM para {owner_id}: {e}")

# --- Configuração da Verificação (sem alteração) ---
VALID_INVESTOR_CNAES = ["6462-0/00", "6463-8/00"]
RECEITA_API_URL = "https://www.receitaws.com.br/v1"

@firestore_fn.on_document_created(
    document="investors/{investorId}",
    region="southamerica-east1",
    secrets=["RECEITAWS_API_TOKEN"]
)
def verify_investor_data(
        event: firestore_fn.Event[firestore_fn.DocumentSnapshot],
) -> None:
    """
    (Revisada) Verifica os dados de um novo investidor com logs e timeouts.
    """

    # --- ALTERAÇÃO 1: Log de ERRO ---
    # Vamos usar logging.error() para que esta linha apareça em VERMELHO
    # e seja impossível de perder nos logs.
    investor_id = event.params['investorId']
    logging.error(f"--- (TESTE) FUNÇÃO ACIONADA PARA INVESTIDOR: {investor_id} ---")

    investor_ref = event.data.reference
    investor_data = event.data.to_dict()

    if investor_data.get("status") != "PENDING_APPROVAL":
        logging.info(f"Investidor {investor_id} já processado. Ignorando.")
        return

    api_token = os.environ.get("RECEITAWS_API_TOKEN")
    if not api_token:
        logging.error(f"ERRO GRAVE: Secret 'RECEITAWS_API_TOKEN' não encontrado.")
        investor_ref.update({
            "status": "REJECTED",
            "rejectionReason": "Erro interno do servidor (Token API ausente).",
            "verifiedAt": firestore.SERVER_TIMESTAMP
        })
        return

    investor_type = investor_data.get("investorType")
    user_name = investor_data.get("nome")
    update_payload = {}
    db = firestore.client()

    try:
        logging.info(f"Iniciando verificação tipo '{investor_type}' para {investor_id}...")
        if investor_type == "INDIVIDUAL":
            cpf = investor_data.get("cpf")
            update_payload = _verify_cpf(cpf, api_token)

        elif investor_type == "FIRM":
            cnpj = investor_data.get("cnpj")
            update_payload = _verify_cnpj(cnpj, api_token, user_name)

        else:
            logging.error(f"InvestorType desconhecido: {investor_type}")
            update_payload = {
                "status": "REJECTED",
                "rejectionReason": "Tipo de investidor inválido."
            }

    # --- ALTERAÇÃO 2: Logging de Erro Explícito ---
    # Captura erros de timeout ou conexão
    except requests.exceptions.Timeout:
        logging.error(f"API TIMEOUT: A API ({RECEITA_API_URL}) demorou demais para responder.")
        update_payload = {"status": "REJECTED", "rejectionReason": "API de verificação demorou para responder (Timeout)."}
    # Captura erros de API (4xx, 5xx)
    except requests.exceptions.RequestException as e:
        logging.error(f"ERRO DE API: Falha ao chamar API externa: {e}")
        update_payload = {"status": "REJECTED", "rejectionReason": f"Erro de comunicação com a API de verificação: {e}"}
    # Captura todos os outros erros (ex: KeyError, etc.)
    except Exception as e:
        logging.error(f"ERRO INESPERADO: {e}", exc_info=True) # exc_info=True mostra o stack trace
        update_payload = {"status": "REJECTED", "rejectionReason": f"Erro interno no servidor: {e}"}

    # Atualiza o documento no Firestore
    update_payload["verifiedAt"] = firestore.SERVER_TIMESTAMP
    logging.info(f"Atualizando investidor {investor_id} com status: {update_payload.get('status')}")
    investor_ref.update(update_payload)


# --- Funções Auxiliares (COM TIMEOUT) ---

def _verify_cnpj(cnpj: str, token: str, user_name: str) -> dict:
    """Verifica um CNPJ na API ReceitaWS."""
    cnpj_clean = "".join(filter(str.isdigit, cnpj))
    headers = {"Authorization": f"Bearer {token}"}
    url = f"{RECEITA_API_URL}/cnpj/{cnpj_clean}"

    logging.info(f"Chamando API de CNPJ: {url}")
    response = requests.get(url, headers=headers, timeout=10)
    response.raise_for_status() # Lança exceção se for (4xx, 5xx)
    data = response.json()

    if data.get("situacao") != "ATIVA":
        logging.warning(f"CNPJ {cnpj_clean} REJEITADO. Situação: {data.get('situacao')}")
        return {
            "status": "REJECTED",
            "rejectionReason": f"CNPJ não está com situação 'ATIVA'.",
            "apiVerificationData": data
        }

    qsa_list = data.get("qsa", [])
    if not qsa_list:
        logging.warning(f"CNPJ {cnpj_clean} REJEITADO. Não possui QSA (Quadro de Sócios).")
        return {
            "status": "REJECTED",
            "rejectionReason": "Este CNPJ não possui um Quadro de Sócios (QSA) disponível para verificação.",
            "apiVerificationData": data
        }

    cleaned_user_name = _clean_name(user_name)
    if not cleaned_user_name:
        logging.error(f"CNPJ {cnpj_clean} REJEITADO. Nome do usuário no App está em branco.")
        return { "status": "REJECTED", "rejectionReason": "Erro interno: Nome do usuário não encontrado." }


    is_socio_verified = False
    for socio in qsa_list:
        cleaned_socio_name = _clean_name(socio.get("nome"))
        if cleaned_user_name == cleaned_socio_name:
            is_socio_verified = True
            logging.info(f"Sócio verificado: {user_name} == {socio.get('nome')}")
            break # Encontrou o sócio, pode parar de procurar

    if not is_socio_verified:
        logging.warning(f"CNPJ {cnpj_clean} REJEITADO. Nome '{user_name}' não encontrado no QSA.")
        return {
            "status": "REJECTED",
            "rejectionReason": f"O seu nome ({user_name}) não foi encontrado no Quadro de Sócios desta empresa.",
            "apiVerificationData": data
        }

    logging.info(f"CNPJ {cnpj_clean} APROVADO. Sócio: {user_name}.")

    # cnae_principal_code = data.get("atividade_principal", [{}])[0].get("code")
    # if cnae_principal_code not in VALID_INVESTOR_CNAES:
    #     logging.warning(f"CNPJ {cnpj_clean} REJEITADO. CNAE: {cnae_principal_code}")
    #     return {
    #         "status": "REJECTED",
    #         "rejectionReason": f"CNAE principal ({cnae_principal_code}) não é de investimento.",
    #         "apiVerificationData": data
    #     }

    logging.info(f"CNPJ {cnpj_clean} APROVADO.")
    return {
        "status": "ACTIVE",
        "companyName": data.get("nome") or "Empresa sem Nome Fantasia",
        "porte": data.get("porte"),
        "apiVerificationData": data
    }

def _verify_cpf(cpf: str, token: str) -> dict:
    """Verifica um CPF na API (requer plano pago)."""
    cpf_clean = "".join(filter(str.isdigit, cpf))

    # --- ALTERAÇÃO 3 (Exemplo): Adicionar timeout aqui também ---
    # url = f"https://api.receitaws.com.br/v1/cpf/{cpf_clean}"
    # response = requests.get(url, headers={"Authorization": f"Bearer {token}"}, timeout=10)
    # ... (lógica real da API) ...

    # **INÍCIO DA SIMULAÇÃO**
    logging.warning("--- SIMULAÇÃO DE API DE CPF ATIVA ---")
    if cpf_clean == "11111111111":
        return {"status": "REJECTED", "rejectionReason": "CPF irregular (simulado)."}

    simulated_data = {
        "situacao_cadastral": "REGULAR", "nome": "Investidor Anjo Simulado",
        "qsa": [{"cnpj": "12345678000199", "empresa": "Startup Famosa 1"}]
    }
    # **FIM DA SIMULAÇÃO**

    logging.info(f"CPF {cpf_clean} APROVADO (via simulação).")
    return {
        "status": "ACTIVE",
        "apiVerificationData": simulated_data
    }

def _clean_name(name: str) -> str:
    """
    Normaliza um nome para comparação: remove acentos,
    converte para minúsculas e remove pontuação.
    """
    if not name:
        return ""
    try:
        # Normalização NFKD separa acentos das letras
        nfkd_form = unicodedata.normalize('NFKD', name)
        # Mantém apenas caracteres não-combinantes (remove acentos)
        only_ascii = u"".join([c for c in nfkd_form if not unicodedata.combining(c)])
    except Exception:
        # Fallback para nomes que já são ascii
        only_ascii = name

    # Converte para minúsculas
    name = only_ascii.lower()
    # Remove pontuações (mantém letras, números e espaços)
    name = re.sub(r'[^\w\s]', '', name)
    # Remove espaços extras no início/fim
    return name.strip()

@https_fn.on_call(secrets=["GEMINI_API_KEY"])
def chat_propulsor(req: https_fn.CallableRequest) -> dict:
    """
    Acionado pela Aba 3 (Propulsor) para análise de faíscas de ideia.
    Usa o System Prompt de Analista de Mercado.
    """
    # 1. Validação de Autenticação (essencial!)
    if not req.auth:
        logging.error("chat_propulsor: Chamada não autenticada.")
        raise https_fn.HttpsError(
            code="unauthenticated",
            message="Autenticação é necessária para usar o Propulsor."
        )

    # 2. Validação da Entrada
    spark_text = req.data.get("text")
    if not spark_text:
        logging.error(f"chat_propulsor: Chamada inválida sem 'text' por {req.auth.uid}.")
        raise https_fn.HttpsError(code="invalid-argument", message="O 'text' da faísca é obrigatório.")

    logging.info(f"Iniciando análise do Propulsor para usuário: {req.auth.uid}")

    try:
        # 3. Configurar a API de IA (Gemini)
        genai.configure(api_key=os.environ.get("GEMINI_API_KEY"))

        # 4. A Persona do "Propulsor" (System Prompt)
        PROPULSOR_SYSTEM_PROMPT = """
        Você é o 'Propulsor', um analista de mercado de classe mundial especializado em startups early-stage. 
        O usuário fornecerá uma 'faísca de ideia'. Sua tarefa é responder em 3 partes claras:
        1. Concorrentes: Identifique 2-3 concorrentes diretos ou indiretos (atuais ou potenciais).
        2. Diferencial: Aponte o principal diferencial ou 'gancho' único na ideia do usuário.
        3. Oportunidade de Nicho: Sugira um nicho de mercado ou use-case específico que os concorrentes podem estar ignorando.
        Seja direto, encorajador e profissional.
        """

        # 5. Configurar e Chamar o Modelo
        model = genai.GenerativeModel(
            'gemini-1.5-pro', # Usando um modelo Pro para chat de alta qualidade
            safety_settings={
                HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
                HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_MEDIUM_AND_ABOVE,
            },
            system_instruction=PROPULSOR_SYSTEM_PROMPT
        )

        # Para uma única chamada (stateless)
        response = model.generate_content(spark_text)

        # 6. Retornar a resposta para o App
        logging.info(f"chat_propulsor: Análise concluída para {req.auth.uid}.")
        return {"status": "success", "analysis_text": response.text}

    except Exception as e:
        logging.error(f"Erro no chat_propulsor para {req.auth.uid}: {e}")
        raise https_fn.HttpsError(code="internal", message=f"Erro interno ao processar sua análise: {e}")