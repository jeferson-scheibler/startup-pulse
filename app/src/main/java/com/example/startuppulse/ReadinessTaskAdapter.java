package com.example.startuppulse;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ReadinessTaskAdapter extends RecyclerView.Adapter<ReadinessTaskAdapter.TaskViewHolder> {

    private final List<ReadinessTask> tasks;
    private final Context context;

    public ReadinessTaskAdapter(List<ReadinessTask> tasks, Context context) {
        this.tasks = tasks;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_readiness_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        ReadinessTask task = tasks.get(position);
        holder.bind(task);
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final ImageView statusIcon;
        private final TextView descriptionText;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            statusIcon = itemView.findViewById(R.id.image_view_status);
            descriptionText = itemView.findViewById(R.id.text_view_task_description);
        }

        public void bind(ReadinessTask task) {
            descriptionText.setText(task.getDescription());
            if (task.isCompleted()) {
                statusIcon.setImageResource(R.drawable.ic_check);
                statusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorSuccess)));
            } else {
                statusIcon.setImageResource(R.drawable.ic_hourglass);
                statusIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.colorWarning)));
            }
        }
    }
}