package com.example.startuppulse;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.startuppulse.common.Result;
import com.example.startuppulse.data.Ideia;
import com.example.startuppulse.data.Investor;
import com.example.startuppulse.data.PostIt;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Camada de acesso ao Cloud Firestore
 * Baseada em Callback<Result<T>> — sem UI aqui.
 *
 * Convenções:
 * - Sucesso: callback.onComplete(Result.ok(valor))
 * - Erro:    callback.onComplete(Result.err(exception))
 * - "Não encontrado": Result.ok(null) (UI decide empty-state)
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static final String USUARIOS_COLLECTION = "usuarios";
    private static final String IDEIAS_COLLECTION   = "ideias";
    private static final String MENTORES_COLLECTION = "mentores";

    public interface Callback<T> {
        void onComplete(Result<T> r);
    }

    private final FirebaseFirestore db;

    public FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // --- MÉTODOS DE BUSCA DE MENTORES (CORRIGIDOS) ---

    /**
     * Encontra TODOS os mentores disponíveis numa cidade (exclui o autor).
     */

    public void realizarMatchmakingParaIdeia(Context context, Ideia ideia, Callback<Mentor> callback) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onComplete(Result.err(new SecurityException("Permissão de localização negada.")));
            return;
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    String cidade = null;
                    String estado = null;
                    if (location != null) {
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                cidade = addresses.get(0).getLocality();
                                estado = addresses.get(0).getAdminArea();
                            }
                        } catch (IOException e) { /* Ignora, continua com localização nula */ }
                    }

                    // Inicia a busca com a localização que tivermos
                    final String finalCidade = cidade;
                    final String finalEstado = estado;
                    final List<String> areasDaIdeia = ideia.getAreasNecessarias() != null ? ideia.getAreasNecessarias() : new ArrayList<>();
                    final String ownerId = ideia.getOwnerId();

                    findMentoresByAreas(areasDaIdeia, ownerId, rAreas -> {
                        if (rAreas.isOk() && rAreas.data != null && !rAreas.data.isEmpty()) {
                            List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(rAreas.data, areasDaIdeia, location);
                            Mentor mentorEscolhido = ordenados.get(0);
                            vincularMentorAideia(ideia.getId(), mentorEscolhido.getId(), "Mentor encontrado por área.", callback, mentorEscolhido);
                        } else {
                            findMentoresByCity(finalCidade, ownerId, rCity -> {
                                if (rCity.isOk() && rCity.data != null && !rCity.data.isEmpty()) {
                                    List<Mentor> ordenados = MentorMatchService.ordenarPorAfinidadeEProximidade(rCity.data, areasDaIdeia, location);
                                    Mentor mentorEscolhido = ordenados.get(0);
                                    vincularMentorAideia(ideia.getId(), mentorEscolhido.getId(), "Mentor encontrado por proximidade na cidade.", callback, mentorEscolhido);
                                } else {
                                    // Continue com a lógica de fallback para o estado, se desejar...
                                    callback.onComplete(Result.ok(null)); // Nenhum mentor encontrado
                                }
                            });
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    private void vincularMentorAideia(String ideiaId, String mentorId, String log, Callback<Mentor> callback, Mentor mentor) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("mentorId", mentorId);
        updates.put("matchmakingLog", log);
        updates.put("ultimaBuscaMentorTimestamp", FieldValue.serverTimestamp());

        db.collection("ideias").document(ideiaId).update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(mentor)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Busca todos os mentores disponíveis por cidade (exclui autor). */
    public void findMentoresByCity(
                                    @NonNull String cidade,
                                    @Nullable String ownerId,
                                    @NonNull Callback<List<Mentor>> callback
    ) {
        Query query = db.collection(MENTORES_COLLECTION)
                .whereEqualTo("cidade", cidade);

        if (ownerId != null && !ownerId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), ownerId);
        }

        query.get().addOnSuccessListener(q -> {
                    List<Mentor> mentores = new ArrayList<>();
                    for (DocumentSnapshot doc : q) {
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(doc.getId());
                            mentores.add(mentor);
                        }
                    }
                    callback.onComplete(Result.ok(mentores));
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Busca todos os mentores disponíveis por estado (exclui autor). */
    public void findMentoresByState( // Renomeado para 'Mentores' (plural)
                                     @NonNull String estado,
                                     @Nullable String ownerId,
                                     @NonNull Callback<List<Mentor>> callback
    ) {
        Query query = db.collection(MENTORES_COLLECTION)
                .whereEqualTo("estado", estado);

        if (ownerId != null && !ownerId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), ownerId);
        }

        query.get().addOnSuccessListener(q -> {
                    List<Mentor> mentores = new ArrayList<>();
                    for (DocumentSnapshot doc : q) {
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(doc.getId());
                            mentores.add(mentor);
                        }
                    }
                    callback.onComplete(Result.ok(mentores));
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /**
     * Busca todos os mentores que possuem pelo menos uma das áreas de especialidade.
     * Exclui o dono da ideia da lista de resultados.
     */
    public void findMentoresByAreas(
            @NonNull List<String> areas,
            @Nullable String ownerId, // ID do dono da ideia para ser excluído da busca
            @NonNull final Callback<List<Mentor>> callback
    ) {
        if (areas.isEmpty()) {
            callback.onComplete(Result.ok(new ArrayList<>()));
            return;
        }

        // A query base busca mentores que tenham qualquer uma das áreas na sua lista
        Query query = db.collection(MENTORES_COLLECTION)
                .whereArrayContainsAny("areas", areas);

        // [CORREÇÃO CRÍTICA 1] - Exclui o dono da ideia dos resultados
        if (ownerId != null && !ownerId.isEmpty()) {
            query = query.whereNotEqualTo(FieldPath.documentId(), ownerId);
        }

        query.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Mentor> mentores = new ArrayList<>();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Mentor mentor = document.toObject(Mentor.class);
                    if (mentor != null) {
                        // [CORREÇÃO CRÍTICA 2] - Atribui o ID do documento ao objeto
                        mentor.setId(document.getId());
                        mentores.add(mentor);
                    }
                }
                callback.onComplete(Result.ok(mentores));
            } else {
                Log.e(TAG, "Erro ao buscar mentores por área", task.getException());
                callback.onComplete(Result.err(task.getException()));
            }
        });
    }

    public void findAllMentores(@Nullable String excludeUserId, @NonNull Callback<List<Mentor>> callback) {
        Query q = db.collection("mentores");
        if (excludeUserId != null && !excludeUserId.isEmpty()) {
            q = q.whereNotEqualTo(com.google.firebase.firestore.FieldPath.documentId(), excludeUserId);
        }
        q.get().addOnSuccessListener(querySnapshot -> {
            List<Mentor> mentores = new ArrayList<>();
            if (querySnapshot != null) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Mentor mentor = doc.toObject(Mentor.class);
                    if (mentor != null) {
                        mentor.setId(doc.getId());
                        mentores.add(mentor);
                    }
                }
            }
            callback.onComplete(Result.ok(mentores));
        }).addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    // =========================================================
    // ======================= USUÁRIOS ========================
    // =========================================================

    public void salvarUsuario(
            @NonNull String userId,
            @Nullable String nome,
            @Nullable String email,
            @Nullable String fotoUrl,
            @Nullable String bio,
            @NonNull Callback<String> callback
    ) {
        try {
            if (userId.isEmpty()) {
                callback.onComplete(Result.err(new IllegalArgumentException("ID do usuário não pode ser vazio.")));
                return;
            }

            Map<String, Object> usuario = new HashMap<>();
            if (nome != null)    usuario.put("nome", nome);
            if (email != null)   usuario.put("email", email);
            if (fotoUrl != null) usuario.put("foto_perfil", fotoUrl);
            if (bio != null)     usuario.put("bio", bio);
            // Se não vier, mantém/define como false
            if (!usuario.containsKey("isPremium")) usuario.put("isPremium", false);

            db.collection(USUARIOS_COLLECTION).document(userId)
                    .set(usuario, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(userId)))
                    .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
        } catch (Exception ex) {
            callback.onComplete(Result.err(ex));
        }
    }

    public void buscarUsuario(
            @NonNull String usuarioId,
            @NonNull Callback<DocumentSnapshot> callback
    ) {
        if (usuarioId.isEmpty()) {
            callback.onComplete(Result.err(new IllegalArgumentException("ID do usuário não pode ser vazio.")));
            return;
        }
        db.collection(USUARIOS_COLLECTION).document(usuarioId).get()
                .addOnSuccessListener(snap -> callback.onComplete(Result.ok(snap.exists() ? snap : null)))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Erro ao buscar usuário", e);
                    callback.onComplete(Result.err(e));
                });
    }

    public void updatePremiumStatus(
            @NonNull String userId,
            boolean isPremium,
            @NonNull Callback<Void> callback
    ) {
        if (userId.isEmpty()) {
            callback.onComplete(Result.err(new IllegalArgumentException("ID do usuário não pode ser vazio.")));
            return;
        }
        db.collection(USUARIOS_COLLECTION).document(userId)
                .update("isPremium", isPremium)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    // =========================================================
    // ======================== IDEIAS =========================
    // =========================================================

    public void getIdeiasForOwner(String ownerId, com.example.startuppulse.common.Callback<Result<List<Ideia>>> callback) {
        if (ownerId == null) {
            callback.onComplete(Result.err(new IllegalArgumentException("Owner ID não pode ser nulo.")));
            return;
        }

        db.collection("ideias")
                .whereEqualTo("ownerId", ownerId)
                // .orderBy("dataCriacao", Query.Direction.DESCENDING) // Opcional: para pegar a mais recente
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ideia> ideias = queryDocumentSnapshots.toObjects(Ideia.class);
                    callback.onComplete(Result.ok(ideias));
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }
    public void getInvestidores(com.example.startuppulse.common.Callback<Result<List<Investor>>> callback) {
        db.collection("investidores")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Investor> investidores = queryDocumentSnapshots.toObjects(Investor.class);
                    callback.onComplete(Result.ok(investidores));
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }
    public String getNewIdeiaId() {
        return db.collection(IDEIAS_COLLECTION).document().getId();
    }

    /**
     * Gera um ID único para um novo Post-it.
     * Como os Post-its são parte de um mapa dentro de uma Ideia,
     * não precisamos de um caminho real, apenas de um ID aleatório.
     */
    public String getNewPostItId() {
        // Esta é a forma padrão do Firestore para gerar um ID único sem criar um documento.
        return db.collection("dummy_path").document().getId();
    }

    /** Adiciona uma ideia usando o ID já presente em ideia.getId(). */
    public void addIdeia(
            @NonNull Ideia ideia,
            @NonNull Callback<String> callback
    ) {
        try {
            String id = ideia.getId();
            if (id == null || id.isEmpty()) {
                callback.onComplete(Result.err(new IllegalArgumentException("Ideia sem ID. Use getNewIdeiaId().")));
                return;
            }
            db.collection(IDEIAS_COLLECTION).document(id)
                    .set(ideia)
                    .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(id)))
                    .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
        } catch (Exception ex) {
            callback.onComplete(Result.err(ex));
        }
    }

    /**
     * Escuta alterações em uma ideia em tempo real.
     * Result.ok(ideia) a cada update; Result.ok(null) se documento não existir/for removido.
     */
    public ListenerRegistration listenToIdeia(
            @NonNull String ideiaId,
            @NonNull Callback<Ideia> callback
    ) {
        final DocumentReference docRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
        return docRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) { callback.onComplete(Result.err(e)); return; }
            if (snapshot != null && snapshot.exists()) {
                Ideia ideia = snapshot.toObject(Ideia.class);
                if (ideia != null) {
                    ideia.setId(snapshot.getId());
                    callback.onComplete(Result.ok(ideia));
                } else {
                    callback.onComplete(Result.err(new IllegalStateException("Falha ao converter Ideia.")));
                }
            } else {
                callback.onComplete(Result.ok(null));
            }
        });
    }

    /** Adiciona um PostIt numa etapa da ideia. */
    public void addPostitToIdeia(
            @NonNull String ideiaId,
            @NonNull String etapaChave,
            @NonNull String texto,
            @NonNull String cor,
            @NonNull Callback<Void> callback
    ) {
        try {
            DocumentReference ideiaRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
            String key = "postIts." + etapaChave;
            PostIt novoPostIt = new PostIt(texto, cor);
            novoPostIt.setTimestamp(new Date());
            ideiaRef.update(key, FieldValue.arrayUnion(novoPostIt))
                    .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                    .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
        } catch (Exception ex) {
            callback.onComplete(Result.err(ex));
        }
    }

    /** Atualiza um PostIt (remove o antigo, insere um novo com lastModified). */
    public void updatePostitInIdeia(
            @NonNull String ideiaId,
            @NonNull String etapaChave,
            @NonNull PostIt postitAntigo,
            @NonNull String novoTexto,
            @NonNull String novaCor,
            @NonNull Callback<Void> callback
    ) {
        try {
            DocumentReference ideiaRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
            String key = "postIts." + etapaChave;
            db.runTransaction(transaction -> {
                        transaction.update(ideiaRef, key, FieldValue.arrayRemove(postitAntigo));
                        PostIt postitNovo = new PostIt(novoTexto, novaCor);
                        postitNovo.setTimestamp(postitAntigo.getTimestamp());
                        postitNovo.setLastModified(new Date());
                        transaction.update(ideiaRef, key, FieldValue.arrayUnion(postitNovo));
                        return null;
                    })
                    .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                    .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
        } catch (Exception ex) {
            callback.onComplete(Result.err(ex));
        }
    }

    /** Remove um PostIt de uma etapa. */
    public void deletePostitFromIdeia(
            @NonNull String ideiaId,
            @NonNull String etapaChave,
            @NonNull PostIt postitParaApagar,
            @NonNull Callback<Void> callback
    ) {
        try {
            DocumentReference ideiaRef = db.collection(IDEIAS_COLLECTION).document(ideiaId);
            String key = "postIts." + etapaChave;
            ideiaRef.update(key, FieldValue.arrayRemove(postitParaApagar))
                    .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                    .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
        } catch (Exception ex) {
            callback.onComplete(Result.err(ex));
        }
    }

    /** Atualiza o documento completo da ideia (ex.: salvar rascunho). */
    public void updateIdeia(
            @NonNull Ideia ideia,
            @NonNull Callback<Result<Void>> callback
    ) {
        if (ideia.getId() == null || ideia.getId().isEmpty()) {
            callback.onComplete(Result.err(new IllegalArgumentException("ID da ideia não pode ser nulo.")));
            return;
        }

        db.collection(IDEIAS_COLLECTION).document(ideia.getId())
                .set(ideia)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    public void findIdeiaById(
            @NonNull String ideiaId,
            @NonNull Callback<Ideia> callback
    ) {
        if (ideiaId.isEmpty()) {
            callback.onComplete(Result.err(new IllegalArgumentException("ID da ideia vazio.")));
            return;
        }

        db.collection(IDEIAS_COLLECTION).document(ideiaId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Ideia ideia = snap.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(snap.getId());
                        }
                        callback.onComplete(Result.ok(ideia)); // Passa a ideia ou null se a conversão falhar
                    } else {
                        callback.onComplete(Result.ok(null)); // Documento não existe
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Rascunhos do usuário (status=RASCUNHO, ownerId=...). */
    public void getMeusRascunhos(
            @NonNull String ownerId,
            @NonNull Callback<List<Ideia>> callback
    ) {
        if (ownerId.isEmpty()) {
            callback.onComplete(Result.ok(new ArrayList<>()));
            return;
        }

        db.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("status", "RASCUNHO")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    List<Ideia> rascunhos = new ArrayList<>();
                    for (DocumentSnapshot doc : q) {
                        Ideia ideia = doc.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(doc.getId());
                            rascunhos.add(ideia);
                        }
                    }
                    callback.onComplete(Result.ok(rascunhos));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar rascunhos", e);
                    callback.onComplete(Result.err(e));
                });
    }

    /** Remove uma ideia. */
    public void excluirIdeia(
            @NonNull String ideiaId,
            @NonNull Callback<Void> callback
    ) {
        if (ideiaId.isEmpty()) {
            callback.onComplete(Result.err(new IllegalArgumentException("ID da ideia nulo/vazio.")));
            return;
        }
        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Ideias publicadas (status=PUBLICADA). */
    public void getIdeiasPublicadas(
            @NonNull Callback<List<Ideia>> callback
    ) {
        db.collection(IDEIAS_COLLECTION)
                .whereEqualTo("status", "PUBLICADA")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(q -> {
                    List<Ideia> ideias = new ArrayList<>();
                    for (DocumentSnapshot doc : q) {
                        Ideia ideia = doc.toObject(Ideia.class);
                        if (ideia != null) {
                            ideia.setId(doc.getId());
                            ideias.add(ideia);
                        }
                    }
                    callback.onComplete(Result.ok(ideias));
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao buscar ideias publicadas", e);
                    callback.onComplete(Result.err(e));
                });
    }

    /**
     * Escuta a lista de ideias públicas (qualquer status que não seja RASCUNHO) em tempo real.
     */
    public ListenerRegistration listenToIdeiasPublicadas(
            @NonNull Callback<List<Ideia>> callback
    ) {
        List<String> statusPublicos = new ArrayList<>();
        statusPublicos.add(Ideia.Status.EM_AVALIACAO.name());
        statusPublicos.add(Ideia.Status.AVALIADA_APROVADA.name());
        statusPublicos.add(Ideia.Status.AVALIADA_REPROVADA.name());

        return db.collection(IDEIAS_COLLECTION)
                .whereIn("status", statusPublicos)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        callback.onComplete(Result.err(e));
                        return;
                    }
                    List<Ideia> ideias = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Ideia ideia = doc.toObject(Ideia.class);
                            if (ideia != null) {
                                ideia.setId(doc.getId());
                                ideias.add(ideia);
                            }
                        }
                    }
                    callback.onComplete(Result.ok(ideias));
                });
    }

    /** Escuta rascunhos do usuário em tempo real. */
    public ListenerRegistration listenToMeusRascunhos(
            @NonNull String ownerId,
            @NonNull Callback<List<Ideia>> callback
    ) {
        if (ownerId.isEmpty()) {
            callback.onComplete(Result.ok(new ArrayList<>()));
            return null;
        }
        return db.collection(IDEIAS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .whereEqualTo("status", "RASCUNHO")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { callback.onComplete(Result.err(e)); return; }
                    List<Ideia> out = new ArrayList<>();
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Ideia ideia = doc.toObject(Ideia.class);
                            if (ideia != null) { ideia.setId(doc.getId()); out.add(ideia); }
                        }
                    }
                    callback.onComplete(Result.ok(out));
                });
    }

    /** Rebaixa ideia para RASCUNHO e remove o mentor. */
    public void unpublishIdeia(
            @NonNull String ideiaId,
            @NonNull Callback<Void> callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "RASCUNHO");
        updates.put("mentorId", FieldValue.delete());

        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Salva avaliação e marca status. */
    public void salvarAvaliacao(
            @NonNull String ideiaId,
            @NonNull List<Map<String, Object>> avaliacoes,
            @NonNull Callback<Void> callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("avaliacoes", avaliacoes);
        updates.put("avaliacaoStatus", "Avaliada");

        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Publica ideia e (opcional) associa mentor. */
    public void publicarIdeia(
            @NonNull String ideiaId,
            @Nullable String mentorId,
            @NonNull Callback<Void> callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", Ideia.Status.EM_AVALIACAO.name());
        updates.put("timestamp", FieldValue.serverTimestamp());
        if (mentorId != null) {
            updates.put("mentorId", mentorId);
        } else {
            updates.put("ultimaBuscaMentorTimestamp", FieldValue.serverTimestamp());
        }

        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    // =========================================================
    // ======================= MENTORES ========================
    // =========================================================

    /** Cria/atualiza mentor com ID definido. */
    public void addMentorWithId(
            @NonNull String mentorId,
            @NonNull Mentor mentor,
            @NonNull Callback<String> callback
    ) {
        try {
            String uid = FirebaseAuth.getInstance().getUid();
            if (uid == null || uid.isEmpty()) {
                callback.onComplete(Result.err(new IllegalStateException("Usuário não autenticado.")));
                return;
            }

            // MONTA MAP EXPLÍCITO (não confia no POJO para ownerId)
            Map<String, Object> data = new HashMap<>();
            data.put("ownerId", uid);                          // <- ESSENCIAL p/ create
            if (mentor.getNome() != null)   data.put("nome", mentor.getNome());
            if (mentor.getImagem() != null) data.put("imagem", mentor.getImagem());
            if (mentor.getCidade() != null) data.put("cidade", mentor.getCidade());
            if (mentor.getEstado() != null) data.put("estado", mentor.getEstado());
            if (mentor.getLongitude() != 0.0) data.put("longitude", mentor.getLongitude());
            if (mentor.getLatitude() != 0.0) data.put("latitude", mentor.getLatitude());
            if (mentor.getAreas() != null)  data.put("areas", mentor.getAreas());
            if (mentor.getProfissao() != null) data.put("profissao", mentor.getProfissao());
            data.put("verificado", mentor.isVerificado());     // se nulo no POJO, ajuste

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("mentores").document(mentorId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(mentorId)))
                    .addOnFailureListener(e -> callback.onComplete(Result.err(e)));

        } catch (Exception ex) {
            callback.onComplete(Result.err(ex));
        }
    }

    /** Atualiza campos de verificação/áreas do mentor. */
    public void atualizarCamposMentor(
            @NonNull String mentorId,
            @Nullable Boolean verificado,
            @Nullable List<String> areas,
            @NonNull Callback<Void> callback
    ) {
        Map<String, Object> updates = new HashMap<>();
        if (verificado != null) updates.put("verificado", verificado);
        if (areas != null)      updates.put("areas", areas);

        db.collection(MENTORES_COLLECTION)
                .document(mentorId)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onComplete(Result.ok(null)))
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Lista mentores (one-shot, sem filtro). */
    public void getMentoresPublicados(
            @NonNull Callback<List<Mentor>> callback
    ) {
        db.collection(MENTORES_COLLECTION)
                .get()
                .addOnSuccessListener(q -> {
                    List<Mentor> mentores = new ArrayList<>();
                    for (DocumentSnapshot doc : q) {
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) {
                            mentor.setId(doc.getId());
                            mentores.add(mentor);
                        }
                    }
                    callback.onComplete(Result.ok(mentores));
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Primeiro mentor disponível por cidade (exclui autor); ok(null) se vazio. */
    public void findMentorByCity(
            @NonNull String cidade,
            @NonNull String authorId,
            @NonNull Callback<Mentor> callback
    ) {
        db.collection(MENTORES_COLLECTION)
                .whereEqualTo("cidade", cidade)
                .whereNotEqualTo(com.google.firebase.firestore.FieldPath.documentId(), authorId)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        DocumentSnapshot doc = q.getDocuments().get(0);
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) mentor.setId(doc.getId());
                        callback.onComplete(Result.ok(mentor));
                    } else {
                        callback.onComplete(Result.ok(null));
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Primeiro mentor disponível por estado (exclui autor); ok(null) se vazio. */
    public void findMentorByState(
            @NonNull String estado,
            @NonNull String authorId,
            @NonNull Callback<Mentor> callback
    ) {
        db.collection(MENTORES_COLLECTION)
                .whereEqualTo("estado", estado)
                .whereNotEqualTo(com.google.firebase.firestore.FieldPath.documentId(), authorId)
                .limit(1)
                .get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        DocumentSnapshot doc = q.getDocuments().get(0);
                        Mentor mentor = doc.toObject(Mentor.class);
                        if (mentor != null) mentor.setId(doc.getId());
                        callback.onComplete(Result.ok(mentor));
                    } else {
                        callback.onComplete(Result.ok(null));
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    /** Busca mentor por ID; ok(null) se não existir. */
    public void findMentorById(
            @NonNull String mentorId,
            @NonNull Callback<Mentor> callback
    ) {
        if (mentorId.isEmpty()) {
            callback.onComplete(Result.err(new IllegalArgumentException("ID do mentor vazio.")));
            return;
        }

        db.collection(MENTORES_COLLECTION).document(mentorId).get()
                .addOnSuccessListener(snap -> {
                    if (snap.exists()) {
                        Mentor mentor = snap.toObject(Mentor.class);
                        if (mentor != null) mentor.setId(snap.getId());
                        callback.onComplete(Result.ok(mentor));
                    } else {
                        callback.onComplete(Result.ok(null));
                    }
                })
                .addOnFailureListener(e -> callback.onComplete(Result.err(e)));
    }

    public void atualizarTimestampBuscaMentor(String ideiaId) {
        db.collection(IDEIAS_COLLECTION).document(ideiaId)
                .update("ultimaBuscaMentorTimestamp", FieldValue.serverTimestamp());
    }
}

