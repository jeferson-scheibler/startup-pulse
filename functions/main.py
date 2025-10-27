# main.py
import os
from firebase_functions import https_fn, options, pubsub_fn
from firebase_admin import initialize_app, firestore
from google.auth import default as get_credentials
from google.auth.transport.requests import Request
from googleapiclient.discovery import build
from google.api_core.exceptions import InvalidArgument
import google.generativeai as genai
from google.generativeai.types import GenerationConfig
from google.generativeai.types import HarmCategory, HarmBlockThreshold

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