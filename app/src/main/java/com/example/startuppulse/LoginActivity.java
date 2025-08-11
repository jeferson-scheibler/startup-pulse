package com.example.startuppulse;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.startuppulse.common.Result;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView signUpTextView;
    private LinearLayout googleButtonLayout;
    private FirebaseAuth mAuth;
    private FirestoreHelper dbHelper;

    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_StartupPulse);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: Iniciando LoginActivity");
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new FirestoreHelper();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        signUpTextView = findViewById(R.id.signUpTextView);
        googleButtonLayout = findViewById(R.id.googleButton);

        configureGoogleSignIn();

        loginButton.setOnClickListener(v -> {
            Log.d(TAG, "Botão de login (email/senha) clicado");
            loginUser();
        });

        signUpTextView.setOnClickListener(v -> {
            Log.d(TAG, "Clique em Criar Conta");
            startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
        });

        googleButtonLayout.setOnClickListener(v -> {
            Log.d(TAG, "Botão Google clicado → chamando signInWithGoogle()");
            signInWithGoogle();
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        Log.d(TAG, "Tentando login com email=" + email);

        if (email.isEmpty() || password.isEmpty()) {
            Log.w(TAG, "Campos vazios no login");
            Toast.makeText(this, "Por favor, preencha todos os campos.", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    Log.d(TAG, "signInWithEmailAndPassword: sucesso=" + task.isSuccessful());
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Falha na autenticação email/senha", task.getException());
                        Toast.makeText(LoginActivity.this, "Falha na autenticação.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    Log.d(TAG, "Usuário logado: " + (user != null ? user.getUid() : "null"));
                    if (user == null) {
                        Toast.makeText(this, "Erro: usuário nulo após login.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fotoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;
                    dbHelper.salvarUsuario(
                            user.getUid(),
                            user.getDisplayName(),
                            user.getEmail(),
                            fotoUrl,
                            null,
                            r -> {
                                Log.d(TAG, "Resultado salvarUsuario: " + r.isOk());
                                if (!r.isOk()) {
                                    Log.w(TAG, "salvarUsuario (email/senha) falhou: ", r.error);
                                }
                                Toast.makeText(LoginActivity.this, "Login bem-sucedido.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                    );
                });
    }

    private void configureGoogleSignIn() {
        String clientId = getString(R.string.default_web_client_id);
        Log.d(TAG, "Config GoogleSignIn: default_web_client_id=" + clientId);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(clientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d(TAG, "Resultado do Google Sign-In: resultCode=" + result.getResultCode());
                    if (result.getResultCode() != RESULT_OK) {
                        Log.w(TAG, "Google Sign-In cancelado ou falhou");
                        return;
                    }
                    Intent data = result.getData();
                    if (data == null) {
                        Log.w(TAG, "Intent de retorno nula");
                        return;
                    }
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        Log.d(TAG, "Conta Google obtida: " + (account != null ? account.getEmail() : "null"));
                        if (account != null) {
                            Log.d(TAG, "IdToken=" + account.getIdToken());
                            firebaseAuthWithGoogle(account);
                        }
                    } catch (ApiException e) {
                        Log.e(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Falha no login com Google.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void signInWithGoogle() {
        Log.d(TAG, "Iniciando intent do Google Sign-In");
        // força limpar sessão anterior para abrir o seletor de contas limpinho
        mGoogleSignInClient.signOut().addOnCompleteListener(t -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }


    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle: iniciando autenticação com Firebase");
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    Log.d(TAG, "signInWithCredential: sucesso=" + task.isSuccessful());
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Falha na autenticação com Firebase (Google)", task.getException());
                        Toast.makeText(this, "Falha na autenticação com Firebase.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    FirebaseUser user = mAuth.getCurrentUser();
                    Log.d(TAG, "Usuário Google logado: " + (user != null ? user.getUid() : "null"));
                    if (user == null) {
                        Toast.makeText(this, "Erro: usuário nulo após login (Google).", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String fotoUrl = (user.getPhotoUrl() != null) ? user.getPhotoUrl().toString() : null;
                    dbHelper.salvarUsuario(
                            user.getUid(),
                            user.getDisplayName(),
                            user.getEmail(),
                            fotoUrl,
                            null,
                            r -> {
                                Log.d(TAG, "Resultado salvarUsuario (Google): " + r.isOk());
                                if (!r.isOk()) {
                                    Log.w(TAG, "salvarUsuario (google) falhou: ", r.error);
                                }
                                Toast.makeText(this, "Login com Google bem-sucedido.", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                finish();
                            }
                    );
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d(TAG, "onStart: usuário atual=" + (currentUser != null ? currentUser.getUid() : "null"));
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}