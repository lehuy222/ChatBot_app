package com.example.ass8;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private EditText messageEditText;
    private ImageButton sendButton;
    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<Message> messageList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        // Initialize the RecyclerView and its components

        messageEditText = findViewById(R.id.messageEditText);

        sendButton = findViewById(R.id.sendButton);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);

        // Initialize the message list and adapter
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);

        // Setup the RecyclerView
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Set click listener for the send button
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = messageEditText.getText().toString().trim();
                if (!text.isEmpty()) {
                    // Add the message to the list and notify the adapter
                    messageList.add(new Message(text, true)); // true for sender, false for receiver
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    messageEditText.setText("");
                    chatRecyclerView.scrollToPosition(messageList.size() - 1);

                    // Send the message to the server and wait for a response
                    sendChatRequest(text, messageList);
                }
            }
        });
    }

    // Inner class for the RecyclerView Adapter
    private class ChatAdapter extends RecyclerView.Adapter<ChatViewHolder> {
        private List<Message> messages;

        ChatAdapter(List<Message> messages) {
            this.messages = messages;
        }

        @Override
        public ChatViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);
            return new ChatViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ChatViewHolder holder, int position) {
            Message message = messages.get(position);
            holder.messageTextView.setText(message.getText());

            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.messageTextView.getLayoutParams();
            if (message.isSender()) {
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                holder.iconView.setVisibility(View.GONE); // Hide icon for sender
            } else {
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                holder.iconView.setVisibility(View.VISIBLE);
            }
            holder.messageTextView.setLayoutParams(params);
        }

        @Override
        public int getItemCount() {
            return messages.size();
        }
    }

    // Inner class for the ViewHolder
    private class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView iconView;

        ChatViewHolder(View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
            iconView = itemView.findViewById(R.id.iconView);
        }
    }

    private void sendChatRequest(String userMessage, List<Message> messageList) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL("http://10.0.2.2:5000/chat");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setDoOutput(true);

                    JSONObject json = new JSONObject();
                    json.put("userMessage", userMessage);
                    JSONArray historyJson = new JSONArray();

                    for (Message message : messageList) {
                        JSONObject chatJson = new JSONObject();
                        chatJson.put("User", message.isSender() ? "User" : "Llama");
                        chatJson.put("Llama", message.getText());
                        historyJson.put(chatJson);
                    }

                    json.put("chatHistory", historyJson);

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.close();

                    // Read the response
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    br.close();

                    // Parse the JSON response and extract the message
                    String serverMessage = "";
                    try {
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        serverMessage = jsonResponse.getString("message");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        serverMessage = "Error parsing response";
                    }

                    String finalServerMessage = serverMessage;
                    // Process the response on the UI thread
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Update the chat with the response from the server
                            messageList.add(new Message(finalServerMessage, false)); // false for receiver
                            chatAdapter.notifyItemInserted(messageList.size() - 1);
                            chatRecyclerView.scrollToPosition(messageList.size() - 1);
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
