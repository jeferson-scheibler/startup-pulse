package com.example.startuppulse;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.startuppulse.data.Investor;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class InvestorAdapter extends RecyclerView.Adapter<InvestorAdapter.InvestorViewHolder> {

    private final List<Investor> investorList;
    private final OnInvestorClickListener listener;

    public interface OnInvestorClickListener { void onInvestorClick(Investor investor); }

    public InvestorAdapter(List<Investor> investorList, OnInvestorClickListener listener) {
        this.investorList = investorList;
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
        Investor investor = investorList.get(position);

        holder.bind(investor);
        holder.itemView.setOnClickListener(v -> {
            // Cria a ação passando o ID do investidor
            NavDirections action = InvestidoresFragmentDirections.actionInvestidoresFragmentToInvestorDetailFragment(investor.getId());
            // Encontra o NavController a partir da View e navega
            Navigation.findNavController(v).navigate(action);
        });
    }

    @Override
    public int getItemCount() {
        return investorList.size();
    }

    class InvestorViewHolder extends RecyclerView.ViewHolder {
        private final ShapeableImageView photo;
        private final TextView name;
        private final TextView bio;

        public InvestorViewHolder(@NonNull View itemView) {
            super(itemView);
            photo = itemView.findViewById(R.id.image_view_investor_photo);
            name = itemView.findViewById(R.id.text_view_investor_name);
            bio = itemView.findViewById(R.id.text_view_investor_bio);
        }

        public void bind(final Investor investor) {
            name.setText(investor.getNome());
            bio.setText(investor.getBio());

            Glide.with(itemView.getContext())
                    .load(investor.getFotoUrl())
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .into(photo);

            itemView.setOnClickListener(v -> listener.onInvestorClick(investor));
        }
    }
}