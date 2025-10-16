package com.example.startuppulse.ui.investor;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.startuppulse.R;
import com.example.startuppulse.data.Investor;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.Objects;

/**
 * Adapter moderno para a lista de investidores, usando ListAdapter para atualizações eficientes da UI.
 * O adapter não lida mais com a navegação, apenas reporta os eventos de clique para o Fragment.
 */
public class InvestorAdapter extends ListAdapter<Investor, InvestorAdapter.InvestorViewHolder> {

    private final OnInvestorClickListener listener;

    public interface OnInvestorClickListener {
        void onInvestorClick(Investor investor);
    }

    public InvestorAdapter(@NonNull OnInvestorClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    @NonNull
    @Override
    public InvestorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_investor, parent, false);
        return new InvestorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull InvestorViewHolder holder, int position) {
        Investor investor = getItem(position);
        if (investor != null) {
            holder.bind(investor, listener);
        }
    }

    // --- ViewHolder ---
    static class InvestorViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView photo;
        private final TextView name;
        private final TextView bio;

        public InvestorViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.image_view_investor_photo);
            name = itemView.findViewById(R.id.text_view_investor_name);
            bio = itemView.findViewById(R.id.text_view_investor_bio);
        }

        public void bind(final Investor investor, final OnInvestorClickListener listener) {
            name.setText(investor.getNome());
            bio.setText(investor.getBio());

            Glide.with(itemView.getContext())
                    .load(investor.getFotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(photo);

            // A única responsabilidade do clique é chamar o listener
            itemView.setOnClickListener(v -> listener.onInvestorClick(investor));
        }
    }

    // --- DiffUtil Callback ---
    private static final DiffUtil.ItemCallback<Investor> DIFF_CALLBACK = new DiffUtil.ItemCallback<Investor>() {
        @Override
        public boolean areItemsTheSame(@NonNull Investor oldItem, @NonNull Investor newItem) {
            // Itens são os mesmos se seus IDs forem iguais
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Investor oldItem, @NonNull Investor newItem) {
            // O conteúdo é o mesmo se o objeto for igual.
            // Para isso funcionar bem, implemente os métodos .equals() e .hashCode() na sua classe Investor.
            return oldItem.equals(newItem);
        }
    };
}