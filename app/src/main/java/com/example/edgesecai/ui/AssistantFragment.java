package com.example.edgesecai.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.edgesecai.R;
import com.example.edgesecai.ai.AIAssistantExplainer;
import com.example.edgesecai.storage.SecurityRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class AssistantFragment extends Fragment {

    private RecyclerView recyclerChat;
    private TextInputEditText editChatInput;
    private ChatMessageAdapter adapter;
    private SecurityRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerChat = view.findViewById(R.id.recyclerChat);
        editChatInput = view.findViewById(R.id.editChatInput);
        MaterialButton btnSend = view.findViewById(R.id.btnSendChat);

        adapter = new ChatMessageAdapter();
        recyclerChat.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerChat.setAdapter(adapter);

        repository = SecurityRepository.getInstance(requireContext());

        // Welcome message
        adapter.addMessage(new ChatMessageAdapter.ChatMessage(
                "EdgeSec AI",
                AIAssistantExplainer.answerUserPrompt(null, repository.getStats(), repository.getAlerts()),
                false
        ));

        btnSend.setOnClickListener(v -> sendMessage());

        editChatInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });
    }

    private void sendMessage() {
        String query = editChatInput.getText() != null ? editChatInput.getText().toString().trim() : "";
        if (query.isEmpty()) return;

        // User message
        adapter.addMessage(new ChatMessageAdapter.ChatMessage("You", query, true));
        editChatInput.setText("");
        recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);

        // AI response
        String response = AIAssistantExplainer.answerUserPrompt(query, repository.getStats(), repository.getAlerts());
        adapter.addMessage(new ChatMessageAdapter.ChatMessage("EdgeSec AI", response, false));
        recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1);
    }
}
