package com.example.edgesecai.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesecai.R;

import java.util.ArrayList;
import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.ViewHolder> {

    public static class ChatMessage {
        public final String sender;
        public final String content;
        public final boolean isUser;

        public ChatMessage(String sender, String content, boolean isUser) {
            this.sender = sender;
            this.content = content;
            this.isUser = isUser;
        }
    }

    private final List<ChatMessage> messages = new ArrayList<>();

    public void addMessage(ChatMessage message) {
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessage msg = messages.get(position);
        holder.textChatSender.setText(msg.sender);
        holder.textChatContent.setText(msg.content);

        if (msg.isUser) {
            holder.textChatSender.setTextColor(holder.itemView.getResources().getColor(R.color.text_secondary, null));
        } else {
            holder.textChatSender.setTextColor(holder.itemView.getResources().getColor(R.color.cyber_accent, null));
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textChatSender, textChatContent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textChatSender = itemView.findViewById(R.id.textChatSender);
            textChatContent = itemView.findViewById(R.id.textChatContent);
        }
    }
}
