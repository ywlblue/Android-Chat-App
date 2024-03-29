package com.example.wlblu.chatchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;
import com.example.wlblu.chatchat.MessageAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity
{

    private String messageReceiverId;
    private String messageReceiverName;

    private Toolbar chatToolbar;

    private TextView userNameTitle;
    private CircleImageView userChatProfileImage;
    private DatabaseReference rootRef;
    private FirebaseAuth mAuth;
    private String messageSenderId;
    private RecyclerView userMessageList;

    private final List<Messages> messagesList = new ArrayList<>();

    private LinearLayoutManager linearLayoutManager;

    private MessageAdapter messageAdapter;

    private ImageButton SendMessageButton;
    private ImageButton SelectImageButton;
    private EditText InputMessageText;
    private ProgressDialog loadingBar;

    private static int Gallery_pick = 1;

    private StorageReference MessageImageStorageRef;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        rootRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();

        messageReceiverId = getIntent().getExtras().get("visit_user_id").toString();
        messageReceiverName = getIntent().getExtras().get("visit_user_name").toString();
        MessageImageStorageRef = FirebaseStorage.getInstance().getReference().child("Messages_Pictures");

        chatToolbar = (Toolbar)findViewById(R.id.chat_bar_layout);
        setSupportActionBar(chatToolbar);

        loadingBar = new ProgressDialog(this);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater)
                this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View action_bar_view = layoutInflater.inflate(R.layout.chat_custom_bar,null);

        actionBar.setCustomView(action_bar_view);

        userNameTitle = (TextView) findViewById(R.id.custom_profile_name);
        userChatProfileImage = (CircleImageView)findViewById(R.id.custom_profile_image);

        SendMessageButton = (ImageButton)findViewById(R.id.send_message);
        SelectImageButton = (ImageButton)findViewById(R.id.select_image);
        InputMessageText = (EditText)findViewById(R.id.input_message);

        messageAdapter = new MessageAdapter(messagesList);
        userMessageList = (RecyclerView)findViewById(R.id.message_list_of_users);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessageList.setHasFixedSize(true);
        userMessageList.setLayoutManager(linearLayoutManager);
        userMessageList.setAdapter(messageAdapter);

        FetchMessage();

        userMessageList = (RecyclerView)findViewById(R.id.message_list_of_users);


        userNameTitle.setText(messageReceiverName);

        rootRef.child("Users").child(messageReceiverId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final String online = dataSnapshot.child("online").getValue().toString();
                final String userThumb = dataSnapshot.child("user_thumb_image").getValue().toString();

                Picasso.get().load(userThumb).networkPolicy(NetworkPolicy.OFFLINE).into(userChatProfileImage, new Callback() {
                    @Override
                    public void onSuccess() {
                        Picasso.get().load(userThumb).into(userChatProfileImage);
                    }

                    @Override
                    public void onError(Exception e) {
                        Picasso.get().load(userThumb).placeholder(R.drawable.default_profile).into(userChatProfileImage);

                    }
                });

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        SendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendMessage();
            }
        });

        SelectImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,Gallery_pick);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Gallery_pick && resultCode == RESULT_OK && data != null) {
            loadingBar.setTitle("Sending Chat Image");
            loadingBar.setMessage("Please wait, while your image is sending");
            loadingBar.show();

            Uri ImageUri = data.getData();

            final String message_sender_ref = "Message/" + messageSenderId +"/" + messageReceiverId;
            final String message_receiver_ref = "Message/" + messageReceiverId +"/" + messageSenderId;

            DatabaseReference user_message_key = rootRef.child("Message").child(messageSenderId).child(messageReceiverId).push();

            final String message_push_id = user_message_key.getKey();

            StorageReference filepath =  MessageImageStorageRef.child(message_push_id + ".jpg");

            filepath.putFile(ImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if(task.isSuccessful())
                    {
                        final String downloadUrl = task.getResult().getDownloadUrl().toString();

                        Map messageTextBody = new HashMap();
                        messageTextBody.put("message",downloadUrl);
                        messageTextBody.put("seen",false);
                        messageTextBody.put("type","image");
                        messageTextBody.put("time",ServerValue.TIMESTAMP);
                        messageTextBody.put("from",messageSenderId);

                        Map messageBodyDetails = new HashMap();

                        messageBodyDetails.put(message_sender_ref + "/" + message_push_id, messageTextBody);

                        messageBodyDetails.put(message_receiver_ref + "/" + message_push_id, messageTextBody);

                        rootRef.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if(databaseError != null){
                                    Log.d("Chat_Log", databaseError.getMessage().toString());
                                }

                                InputMessageText.setText("");

                                loadingBar.dismiss();
                            }
                        });

                        Toast.makeText(ChatActivity.this, "Picture sent successfully",Toast.LENGTH_LONG).show();
                        loadingBar.dismiss();
                    }

                    else {
                        Toast.makeText(ChatActivity.this, "Picture not sent. Try Again.",Toast.LENGTH_LONG).show();
                        loadingBar.dismiss();
                    }
                }
            });
        }
    }

    private void FetchMessage() {
        rootRef.child("Message").child(messageSenderId).child(messageReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Messages messages = dataSnapshot.getValue(Messages.class);
                        messagesList.add(messages);
                        messageAdapter.notifyDataSetChanged();

                        userMessageList.smoothScrollToPosition(userMessageList.getAdapter().getItemCount());

                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
    }

    private void SendMessage() {
        String messageText = InputMessageText.getText().toString();

        if(TextUtils.isEmpty(messageText)){
            Toast.makeText(ChatActivity.this, "Please write your message", Toast.LENGTH_LONG).show();
        }
        else{
            String message_sender_ref = "Message/" + messageSenderId +"/" + messageReceiverId;

            String message_receiver_ref = "Message/" + messageReceiverId +"/" + messageSenderId;

            DatabaseReference user_message_key = rootRef.child("Message").child(messageSenderId).child(messageReceiverId).push();

            String message_push_id = user_message_key.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message",messageText);
            messageTextBody.put("seen",false);
            messageTextBody.put("type","text");
            messageTextBody.put("time",ServerValue.TIMESTAMP);
            messageTextBody.put("from",messageSenderId);

            Map messageBodyDetails = new HashMap();

            messageBodyDetails.put(message_sender_ref + "/" + message_push_id, messageTextBody);

            messageBodyDetails.put(message_receiver_ref + "/" + message_push_id, messageTextBody);

            rootRef.updateChildren(messageBodyDetails, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null){
                        Log.d("Chat_log",databaseError.getMessage().toString());
                    }

                    InputMessageText.setText("");
                }
            });
        }
    }

}

