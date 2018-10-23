package com.iremember.iremember;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Iterator;

public class List_screen extends AppCompatActivity {

    private static final String TAG = "List_screen";
    private ListView dataListView;
    private EditText itemText;
    private Button findButton;
    private Button deleteButton;
    private Boolean searchMode = false;
    private Boolean itemSelected = false;

    private int selectedPosition = 0;

    //path dbRef.child("users").child(uid)

    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    String path = "users/" + currentUser.getUid() + "/items";

    private FirebaseDatabase db= FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = db.getReference(path);

    ArrayList<String> listItems = new ArrayList<>();
    ArrayList<String> listKeys = new ArrayList<>();
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_screen);

        Log.i(TAG, path);

        dataListView = findViewById(R.id.dataListView);
        itemText = findViewById(R.id.itemText);
        findButton = findViewById(R.id.findBtn);
        deleteButton = findViewById(R.id.deleteBtn);
        deleteButton.setEnabled(false);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, listItems);
        dataListView.setAdapter(adapter);
        dataListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        dataListView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position; itemSelected = true;
                deleteButton.setEnabled(true);
            }
        });

        addChildEventListener();
    }

    public void findItems(View view) {
        Query query;

        if (!searchMode) {
            findButton.setText("Clear");
            query = dbRef.orderByChild("description"). equalTo(itemText.getText().toString());
            searchMode = true;
        } else {
            searchMode = false;
            findButton.setText("Find");
            query = dbRef.orderByKey();
        }

        if (itemSelected) {
            dataListView.setItemChecked(selectedPosition, false);
            itemSelected = false; deleteButton.setEnabled(false);
        }

        query.addListenerForSingleValueEvent(queryValueListener);
    }

    ValueEventListener queryValueListener = new ValueEventListener() {
        @Override public void onDataChange(DataSnapshot dataSnapshot) {
            Iterable<DataSnapshot> snapshotIterator = dataSnapshot.getChildren();
            Iterator<DataSnapshot> iterator = snapshotIterator.iterator();
            adapter.clear();
            listKeys.clear();
            while (iterator.hasNext()) {
                DataSnapshot next = iterator.next();
                String match = (String) next.child("description").getValue();
                String key = next.getKey();
                listKeys.add(key);
                adapter.add(match);
            }
        }

        @Override public void onCancelled(DatabaseError databaseError) {

        }
    };

    // dataSnapshot.child(currentUser.getUid())

    public void addItem(View view) {
        String item = itemText.getText().toString();
        String key = dbRef.push().getKey();
        itemText.setText("");
        dbRef.child(key).child("description").setValue(item);
        adapter.notifyDataSetChanged();
    }

    public void deleteItem(View view) {
        dataListView.setItemChecked(selectedPosition, false);
        dbRef.child(listKeys.get(selectedPosition)).removeValue();
    }

    private void addChildEventListener() {
        ChildEventListener childListener = new ChildEventListener() {
            @Override public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                adapter.add( (String) dataSnapshot.child("description").getValue());
                listKeys.add(dataSnapshot.getKey());
            }

            @Override public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override public void onChildRemoved(DataSnapshot dataSnapshot) {
                String key = dataSnapshot.getKey();
                int index = listKeys.indexOf(key);

                if (index != -1) {
                    listItems.remove(index);
                    listKeys.remove(index);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override public void onCancelled(DatabaseError databaseError) {

            }
        };

        dbRef.addChildEventListener(childListener); }
}
