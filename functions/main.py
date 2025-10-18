# main.py
import os
from firebase_functions import https_fn, options
from firebase_admin import initialize_app, firestore
from google.auth import default as get_credentials
from google.auth.transport.requests import Request
from googleapiclient.discovery import build

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

@https_fn.on_call()
def validate_purchase(req: https_fn.CallableRequest) -> dict:
    """
    Função 'chamável' que valida um token de compra com a API do Google Play.
    Se válido, atualiza o status do usuário no Firestore.
    """
    # 1. Validação de Autenticação e Entradas
    if not req.auth:
        raise https_fn.HttpsError(
            code="unauthenticated",
            message="Você precisa estar autenticado para validar uma compra."
        )

    uid = req.auth.uid
    purchase_token = req.data.get("purchaseToken")
    sku = req.data.get("sku")

    if not purchase_token or not sku:
        raise https_fn.HttpsError(
            code="invalid-argument",
            message="A função foi chamada sem 'purchaseToken' ou 'sku'."
        )

    try:
        # 2. Autenticação com a API do Google Play
        print("Autenticando com a API do Google Play...")
        credentials, _ = get_credentials(scopes=SCOPES)
        credentials.refresh(Request())

        android_publisher = build(
            "androidpublisher", "v3", credentials=credentials
        )

        # 3. Validação do Token com os Servidores do Google
        print(f"Validando token para o SKU: {sku}")
        response = (
            android_publisher.purchases()
            .subscriptions()
            .get(
                packageName=PACKAGE_NAME,
                subscriptionId=sku,
                token=purchase_token,
            )
            .execute()
        )

        # 4. Processamento da Resposta e Escrita no Firestore
        expiry_time_millis = int(response.get("expiryTimeMillis"))
        start_time_millis = int(response.get("startTimeMillis"))

        db = firestore.client()
        user_doc_ref = db.collection("premium").document(uid)

        dados = {
            "ativo": True,
            "data_assinatura": firestore.SERVER_TIMESTAMP,
            "data_fim": firestore.firestore.DatetimeWithNanoseconds.from_timestamp_millis(expiry_time_millis),
            "plano": "PRO",
            "purchaseToken": purchase_token,
        }

        print(f"Compra válida. Atualizando documento para o usuário: {uid}")
        user_doc_ref.set(dados)

        return {"status": "success", "message": "Assinatura PRO validada e ativada!"}

    except Exception as e:
        print(f"Erro ao validar a compra: {e}")
        raise https_fn.HttpsError(
            code="internal",
            message="Ocorreu um erro interno ao processar sua assinatura."
        )

@https_fn.on_message_published(topic="play-store-notifications")
def handle_play_notification(event: https_fn.CloudEvent) -> None:
    """
    Função acionada por mensagens no Pub/Sub para processar notificações da Play Store.
    """
    print(f"Recebida notificação da Play Store: {event.data}")

    try:
        # A mensagem vem codificada em Base64
        message_data_str = base64.b64decode(event.data["message"]["data"]).decode("utf-8")
        message_json = json.loads(message_data_str)

        # O payload real da notificação está dentro de 'subscriptionNotification'
        notification = message_json.get("subscriptionNotification")
        if not notification:
            print("Notificação não é do tipo 'subscriptionNotification'. Ignorando.")
            return

        purchase_token = notification.get("purchaseToken")
        notification_type = notification.get("notificationType")

        # Para encontrar o UID do usuário, precisamos buscar pelo purchaseToken.
        # Esta é uma operação que pode ser lenta, mas é necessária.
        db = firestore.client()
        premium_ref = db.collection("premium")
        query = premium_ref.where("purchaseToken", "==", purchase_token).limit(1)
        docs = list(query.stream())

        if not docs:
            print(f"Nenhum usuário encontrado com o purchaseToken: {purchase_token}. Ignorando.")
            return

        user_doc = docs[0]
        uid = user_doc.id
        user_doc_ref = user_doc.reference

        print(f"Processando notificação tipo {notification_type} para o usuário {uid}.")

        # --- Lógica para tratar os diferentes tipos de notificação ---
        # Documentação dos tipos: https://developer.android.com/google/play/billing/rtdn-reference

        # Assinatura foi revogada (ex: pelo suporte do Google)
        if notification_type == 5: # SUBSCRIPTION_REVOKED
            user_doc_ref.update({"ativo": False, "plano": "revogado"})
            print(f"Assinatura revogada para o usuário {uid}.")

        # Assinatura expirou
        elif notification_type == 12: # SUBSCRIPTION_EXPIRED
            user_doc_ref.update({"ativo": False, "plano": "expirado"})
            print(f"Assinatura expirada para o usuário {uid}.")

        # Assinatura foi cancelada pelo usuário (mas ainda está ativa até o fim do período)
        elif notification_type == 3: # SUBSCRIPTION_CANCELED
            # Aqui você pode apenas registrar que foi cancelada, mas manter 'ativo' como true.
            # A expiração será tratada pelo evento SUBSCRIPTION_EXPIRED.
            user_doc_ref.update({"statusCancelamento": "cancelado_pelo_usuario"})
            print(f"Assinatura marcada como cancelada para o usuário {uid}.")

        else:
            print(f"Tipo de notificação {notification_type} não tratado. Ignorando.")

    except Exception as e:
        print(f"Erro ao processar a notificação da Play Store: {e}")
        # É importante não lançar um erro aqui, para que o Pub/Sub não tente reenviar a mensagem indefinidamente.