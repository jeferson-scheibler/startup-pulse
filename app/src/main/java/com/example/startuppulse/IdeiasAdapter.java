package com.example.startuppulse;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
// import android.widget.TextView; // Não é mais necessário com View Binding
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

// import com.airbnb.lottie.LottieAnimationView; // Não é mais necessário com View Binding
import com.example.startuppulse.data.models.Ideia;
// --- CORREÇÃO: Importar a classe de Binding ---
import com.example.startuppulse.databinding.ItemIdeiaBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class IdeiasAdapter extends ListAdapter<Ideia, IdeiasAdapter.IdeiaViewHolder> {

    public interface OnIdeiaClickListener {
        void onIdeiaClick(Ideia ideia);
    }

    private final OnIdeiaClickListener clickListener;
    public IdeiasAdapter(@NonNull OnIdeiaClickListener clickListener) {
        super(DIFF_CALLBACK);
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public IdeiaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // --- CORREÇÃO: Inflar usando View Binding ---
        ItemIdeiaBinding binding = ItemIdeiaBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new IdeiaViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull IdeiaViewHolder holder, int position) {
        Ideia ideia = getItem(position);
        holder.bind(ideia, clickListener);
    }

    public Ideia getIdeiaAt(int position) {
        return getItem(position);
    }

    // --- CORREÇÃO: ViewHolder agora usa View Binding ---
    static class IdeiaViewHolder extends RecyclerView.ViewHolder {

        // Armazena a instância do binding
        private final ItemIdeiaBinding binding;

        // --- Remove todos os campos de View (titulo, autor, Lotties, etc.) ---

        IdeiaViewHolder(@NonNull ItemIdeiaBinding binding) {
            // Passa a view raiz (binding.getRoot()) para o construtor super
            super(binding.getRoot());
            // Armazena a instância do binding
            this.binding = binding;
        }

        @SuppressLint("SetTextI18n")
        void bind(final Ideia ideia, final OnIdeiaClickListener listener) {
            if (ideia == null) return;

            // --- CORREÇÃO: Acessa as views através do binding ---
            binding.textViewTituloIdeia.setText(ideia.getNome());
            binding.textViewAutorIdeia.setText("Por: " + ideia.getAutorNome());

            itemView.setOnClickListener(v -> listener.onIdeiaClick(ideia));

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String uid = (user != null) ? user.getUid() : null;

            boolean souMentor = uid != null && uid.equals(ideia.getMentorId());
            boolean souDono = uid != null && uid.equals(ideia.getOwnerId());

            // --- CORREÇÃO: Lógica de visibilidade dos Lotties ---

            // 1. Pega o Status como Enum (mais seguro)
            Ideia.Status status = ideia.getStatus();

            // 2. Esconda todos os ícones individuais primeiro
            binding.mentorHighlightView.setVisibility(View.GONE);
            binding.iconStatusAguardando.setVisibility(View.GONE);
            binding.iconStatusAvaliacaoOk.setVisibility(View.GONE);
            binding.iconStatusReprovada.setVisibility(View.GONE);

            // 3. Assuma que o CONTAINER está escondido
            binding.iconContainer.setVisibility(View.GONE);

            // 4. Decida qual ícone mostrar (e torne o CONTAINER visível)

            // Lógica do Mentor: Só mostra o highlight se for o mentor E a ideia estiver aguardando avaliação
            if (souMentor && status == Ideia.Status.EM_AVALIACAO) {
                binding.iconContainer.setVisibility(View.VISIBLE);
                binding.mentorHighlightView.setVisibility(View.VISIBLE);
                binding.mentorHighlightView.setContentDescription("Ideia aguardando sua avaliação.");

                // Lógica do Dono: Mostra o status se for o dono E a ideia NÃO for um rascunho
            } else if (souDono && status != Ideia.Status.RASCUNHO) {
                binding.iconContainer.setVisibility(View.VISIBLE); // Mostra o container

                // Usa um switch no Enum (muito mais limpo e seguro que comparar Strings)
                switch (status) {
                    case EM_AVALIACAO:
                        binding.iconStatusAguardando.setVisibility(View.VISIBLE);
                        binding.iconStatusAguardando.setContentDescription("Ideia em avaliação");
                        break;
                    case AVALIADA_APROVADA:
                        binding.iconStatusAvaliacaoOk.setVisibility(View.VISIBLE);
                        binding.iconStatusAvaliacaoOk.setContentDescription("Ideia avaliada e aprovada");
                        break;
                    case AVALIADA_REPROVADA:
                        binding.iconStatusReprovada.setVisibility(View.VISIBLE);
                        binding.iconStatusReprovada.setContentDescription("Ideia avaliada e reprovada");
                        break;
                    default:
                        // Se for um status público sem ícone, esconde o container
                        binding.iconContainer.setVisibility(View.GONE);
                        break;
                }
            }
            // Se não for nem mentor, nem dono vendo status, todos ícones ficam GONE (já feito acima).
        }
    }

    private static final DiffUtil.ItemCallback<Ideia> DIFF_CALLBACK = new DiffUtil.ItemCallback<Ideia>() {
        @Override
        public boolean areItemsTheSame(@NonNull Ideia oldItem, @NonNull Ideia newItem) {
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Ideia oldItem, @NonNull Ideia newItem) {
            // Compare todos os campos que afetam a UI
            return Objects.equals(oldItem.getNome(), newItem.getNome())
                    && Objects.equals(oldItem.getAutorNome(), newItem.getAutorNome())
                    && Objects.equals(oldItem.getMentorId(), newItem.getMentorId())
                    // --- CORREÇÃO: Comparar o Enum diretamente ---
                    && oldItem.getStatus() == newItem.getStatus()
                    && Objects.equals(oldItem.getOwnerId(), newItem.getOwnerId());
        }
    };
}