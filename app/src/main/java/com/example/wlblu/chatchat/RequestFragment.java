package com.example.wlblu.chatchat;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.NetworkPolicy;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * A simple {@link Fragment} subclass.
 */
public class RequestFragment extends Fragment {

    private RecyclerView myRequestsList;

    private View myMainView;

    private DatabaseReference FriendsRequestReference;
    private FirebaseAuth mAuth;
    private DatabaseReference UserReference;

    private DatabaseReference FriendsDatabaseRef;
    private DatabaseReference FriendsReqDatabaseRef;

    String online_user_id;

    public RequestFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        myMainView = inflater.inflate(R.layout.fragment_request, container, false);

        myRequestsList = (RecyclerView)myMainView.findViewById(R.id.requests_list);

        mAuth = FirebaseAuth.getInstance();

        online_user_id = mAuth.getCurrentUser().getUid();

        FriendsRequestReference = FirebaseDatabase.getInstance().getReference().child("Friend_Requests").child(online_user_id);

        UserReference = FirebaseDatabase.getInstance().getReference().child("Users");

        FriendsDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Friend");
        FriendsReqDatabaseRef = FirebaseDatabase.getInstance().getReference().child("Friend_Requests");


        myRequestsList.setHasFixedSize(true);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);

        myRequestsList.setLayoutManager(linearLayoutManager);


        // Inflate the layout for this fragment
        return myMainView;
    }


    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerAdapter<Requests,RequestViewHolder> firebaseRecyclerAdapter
                = new FirebaseRecyclerAdapter<Requests, RequestViewHolder>(
                Requests.class,
                R.layout.friend_request_all_users_layout,
                RequestFragment.RequestViewHolder.class,
                FriendsRequestReference


        ) {
            @Override
            protected void populateViewHolder(final RequestViewHolder viewHolder, Requests model, int position) {
                final String list_users_id = getRef(position).getKey();

                DatabaseReference get_type_ref = getRef(position).child("request_type").getRef();

                get_type_ref.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String request_type = dataSnapshot.getValue().toString();

                            if (request_type.equals("received")) {
                                UserReference.child(list_users_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                                        final String thumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                                        final String userStatus = dataSnapshot.child("user_status").getValue().toString();

                                        viewHolder.setUserName(userName);
                                        viewHolder.setThumb_user_image(thumbImage);
                                        viewHolder.setUser_status(userStatus);

                                        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[] = new CharSequence[]
                                                        {
                                                                "Accept Friend Request",
                                                                "Decline Friend Request"
                                                        };

                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle("Friend Req Options");

                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int position) {
                                                        if (position == 0) {
                                                            Calendar calForDate = Calendar.getInstance();
                                                            SimpleDateFormat currentDate = new SimpleDateFormat("dd-MMMM-yyyy");
                                                            final String saveCurrentDate = currentDate.format(calForDate.getTime());

                                                            //record of the date sender and receiver become friends
                                                            FriendsDatabaseRef.child(online_user_id).child(list_users_id).child("date").setValue(saveCurrentDate)
                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                        @Override
                                                                        public void onSuccess(Void aVoid) {
                                                                            //the date receiver and sender become friends
                                                                            FriendsDatabaseRef.child(list_users_id).child(online_user_id).child("date").setValue(saveCurrentDate)
                                                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                        @Override
                                                                                        public void onSuccess(Void aVoid) {
                                                                                            FriendsReqDatabaseRef.child(online_user_id).child(list_users_id).removeValue()
                                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                        @Override
                                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                                            if (task.isSuccessful()) {
                                                                                                                FriendsReqDatabaseRef.child(list_users_id).child(online_user_id).removeValue()
                                                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                            @Override
                                                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                                                if (task.isSuccessful()) {
                                                                                                                                    Toast.makeText(getContext(), "Friend Request Accepted Successfully.", Toast.LENGTH_LONG).show();
                                                                                                                                }
                                                                                                                            }
                                                                                                                        });
                                                                                                            }

                                                                                                        }
                                                                                                    });

                                                                                        }
                                                                                    });
                                                                        }
                                                                    });

                                                        }

                                                        if (position == 1) {
                                                            FriendsReqDatabaseRef.child(online_user_id).child(list_users_id).removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                FriendsReqDatabaseRef.child(list_users_id).child(online_user_id).removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if (task.isSuccessful()) {
                                                                                                    Toast.makeText(getContext(), "Friend Requests Declined Successfully.", Toast.LENGTH_LONG).show();
                                                                                                }
                                                                                            }
                                                                                        });

                                                                            }
                                                                        }
                                                                    });
                                                        }

                                                    }
                                                });

                                                builder.show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {
                                    }
                                });
                            }
                            else if (request_type.equals("sent")) {
                                Button req_sent_btn = viewHolder.mView.findViewById(R.id.request_accept_btn);
                                req_sent_btn.setText("Req Sent");
                                viewHolder.mView.findViewById(R.id.request_decline_btn).setVisibility(View.INVISIBLE);

                                UserReference.child(list_users_id).addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        final String userName = dataSnapshot.child("user_name").getValue().toString();
                                        final String thumbImage = dataSnapshot.child("user_thumb_image").getValue().toString();
                                        final String userStatus = dataSnapshot.child("user_status").getValue().toString();

                                        viewHolder.setUserName(userName);
                                        viewHolder.setThumb_user_image(thumbImage);
                                        viewHolder.setUser_status(userStatus);

                                        viewHolder.mView.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                CharSequence options[] = new CharSequence[]
                                                        {
                                                                "Cancel Friend Request"
                                                        };

                                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                builder.setTitle("Friend Request Sent");

                                                builder.setItems(options, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int position) {

                                                        if (position == 0) {
                                                            FriendsReqDatabaseRef.child(online_user_id).child(list_users_id).removeValue()
                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                            if (task.isSuccessful()) {
                                                                                FriendsReqDatabaseRef.child(list_users_id).child(online_user_id).removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                if (task.isSuccessful()) {
                                                                                                    Toast.makeText(getContext(), "Friend Requests Cancelled Successfully.", Toast.LENGTH_LONG).show();
                                                                                                }
                                                                                            }
                                                                                        });

                                                                            }
                                                                        }
                                                                    });
                                                        }

                                                    }
                                                });

                                                builder.show();

                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }

                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }

                });
                }

                };
                myRequestsList.setAdapter(firebaseRecyclerAdapter);
            }


    public static class RequestViewHolder extends RecyclerView.ViewHolder{
        View mView;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
        }

        public void setUserName(String userName) {
            TextView userNameDisplay = (TextView)mView.findViewById(R.id.request_profile_name);
            userNameDisplay.setText(userName);
        }

        public void setThumb_user_image(final String thumbImage) {
            final CircleImageView thumb_image =(CircleImageView)mView.findViewById(R.id.request_profile_image);
            Picasso.get().load(thumbImage).networkPolicy(NetworkPolicy.OFFLINE).placeholder(R.drawable.default_profile).into(thumb_image, new Callback() {
                @Override
                public void onSuccess() {

                }

                @Override
                public void onError(Exception e) {
                    Picasso.get().load(thumbImage).placeholder(R.drawable.default_profile).into(thumb_image);

                }
            });
        }

        public void setUser_status(String userStatus) {
            TextView status = (TextView)mView.findViewById(R.id.request_profile_status);
            status.setText(userStatus);
        }
    }

}
