package com.example.dwelventory;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AddEditActivity extends AppCompatActivity implements TagFragment.OnFragmentInteractionListener{
    // All views
    EditText nameButton;
    EditText dateButton;
    EditText makeButton;
    EditText modelButton;
    MaterialButton serialNumButton;
    EditText estValButton;
    EditText commentButton;
    MaterialButton photoButton;
    MaterialButton confirmButton;
    MaterialButton editTagButton;
    MaterialButton tagDisplay1Button;
    MaterialButton tagDisplay2Button;
    MaterialButton tagDisplay3Button;
    // Required inputs
    private String name;
    private Date date;
    private String make;
    private String model;
    private int estValue;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private Item item;

    private ArrayList<Tag> tagsToApply;
//    private String comment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.edit_item_fragment);
        // set up
        editTagButton = findViewById(R.id.edit_tag_button);
        tagDisplay1Button = findViewById(R.id.tag_display_1);
        tagDisplay2Button = findViewById(R.id.tag_display_2);
        tagDisplay3Button  =findViewById(R.id.tag_display_3);

        tagDisplay3Button.setVisibility(View.GONE);
        tagDisplay2Button.setVisibility(View.GONE);
        tagDisplay1Button.setVisibility(View.GONE);

        nameButton = findViewById(R.id.item_name_button);
        dateButton = findViewById(R.id.date_button);
        makeButton = findViewById(R.id.make_button);
        modelButton = findViewById(R.id.model_button);
        serialNumButton = findViewById(R.id.serial_number_button);
        estValButton = findViewById(R.id.estimated_val_button);
        commentButton = findViewById(R.id.comment_button);
        photoButton = findViewById(R.id.photo_button);
        confirmButton = findViewById(R.id.confirm_button);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        int position = intent.getIntExtra("position", -1);
        int requestCode = intent.getIntExtra("requestCode", -1);


        if (mode.equals("edit")){
            item = (Item) intent.getParcelableExtra("item");

            // fill edit texts with information
            assert item != null;
            nameButton.setText(item.getDescription());
            Date date = (Date) intent.getSerializableExtra("date");
            Log.d("aeTag", "um Date is" + date);
            tagsToApply = intent.getParcelableArrayListExtra("tags");
            if(tagsToApply == null){
                tagsToApply = new ArrayList<>();
                Log.d("", "onCreate: SEE HERE 1" + tagsToApply);
            }
            Log.d("", "onCreate: SEE HERE " + tagsToApply);
            item.setTags(tagsToApply);

            // Now display any tags that are already applied. Up to 3
            int numTags = tagsToApply.size();
            int i = 0;
            while (i <= 2 || i < numTags){
                if (i == 0){
                    tagDisplay1Button.setText(tagsToApply.get(i).getTagName());
                    tagDisplay1Button.setVisibility(View.VISIBLE);
                }
                else if (i == 1){
                    tagDisplay2Button.setText(tagsToApply.get(i).getTagName());
                    tagDisplay2Button.setVisibility(View.VISIBLE);
                }
                else if(i == 3){
                    tagDisplay3Button.setText(tagsToApply.get(i).getTagName());
                    tagDisplay3Button.setVisibility(View.VISIBLE);
                }
            }

            assert date != null;
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
            // Format the Date object as a string
            String strDate = dateFormat.format(date);
            dateButton.setText(strDate);
            makeButton.setText(item.getMake());
            modelButton.setText(item.getModel());
            Log.d("aeTag", "before serial");
            Log.d("aeTag", "serial is "+item.getSerialNumber());
            serialNumButton.setText(String.valueOf(item.getSerialNumber()));
            Log.d("aeTag", "before est");
            estValButton.setText(String.valueOf(item.getEstValue()));
            Log.d("aeTag", "before comment");
            commentButton.setText(item.getComment());
            Log.d("aeTag", "made it to the end");
        }

        editTagButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mode.equals("edit")) {
                    Log.d("", "onClick: The current tags.." + tagsToApply);
                    TagFragment newFragment = TagFragment.newInstance(mAuth.getUid(),tagsToApply);
                    newFragment.show(getSupportFragmentManager(), "TAG_FRAG");
                }else{
                    TagFragment newFragment = TagFragment.newInstance(mAuth.getUid());
                    newFragment.show(getSupportFragmentManager(), "TAG_FRAG");
                }
            }
        });


        confirmButton.setOnClickListener(v -> {
            // check for valid inputs
            if (reqInputsValid()){
                // take info and make item object
                Log.d("editTag", "before making the new item, date is " + date);
                
                Item item = new Item(name, date, make, model, estValue);
                item.setTags(tagsToApply);


                // put it in intent
                Intent updatedIntent = new Intent();
                if(item.getTags() == null){
                  ArrayList<Tag>  emptyTagSet = new ArrayList<>();
                  item.setTags(emptyTagSet);
                }
                // go back to main activity
                updatedIntent.putExtra("tags",item.getTags());
                updatedIntent.putExtra("item", item);
                updatedIntent.putExtra("date", date);
                updatedIntent.putExtra("mode", mode);
                updatedIntent.putExtra("position", position );
                updatedIntent.putExtra("requestCode", requestCode);

                setResult(818, updatedIntent);
                Log.d("aeTag", "finishing aeActivity...");
                finish();
            }
        });
    }
    /**
     * This checks all the required inputs are filled out properly
     * @return true or false whether or not inputs are valid
     */
    private boolean reqInputsValid(){
        boolean valid = true;
        // check name
        name = nameButton.getText().toString().trim();
        if (name.isEmpty()) {
            // handle empty field
            nameButton.setError("Field cannot be empty");
            nameButton.requestFocus();
            valid = false;
        }
        // check date
        String strDate = dateButton.getText().toString();
        if (strDate.isEmpty()) {
            // handle empty field
            dateButton.setError("Field cannot be empty");
            dateButton.requestFocus();
            valid = false;
        }
        if (!isDateValid(strDate)){
            // handle date format
            dateButton.setError("Date format must be (mm-dd-yyyy)");
            dateButton.requestFocus();
            valid = false;
        }
        // check make
        make = makeButton.getText().toString().trim();
        if (make.isEmpty()) {
            // handle empty field
            makeButton.setError("Field cannot be empty");
            makeButton.requestFocus();
            valid = false;
        }
        // check model
        model = modelButton.getText().toString();
        if (model.isEmpty()) {
            // handle empty field
            modelButton.setError("Field cannot be empty");
            modelButton.requestFocus();
            valid = false;
        }
        // check estimated value
        String ev = estValButton.getText().toString();
        if (ev.isEmpty()){
            estValButton.setError("Field cannot be empty");
            estValButton.requestFocus();
            valid = false;
        }
        else{
            estValue = Integer.parseInt(ev);
        }
        // All inputs valid!!!
        return valid;
    }

    private boolean isDateValid(String dateStr) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
        dateFormat.setLenient(false); // Disallow lenient date parsing
        try {
            date = dateFormat.parse(dateStr);
            return true; // Date is valid
        } catch (ParseException e) {
            return false; // Date is invalid
        }
    }

    @Override
    public void onCloseAction() {
        TagFragment tagFragment = (TagFragment) getSupportFragmentManager().findFragmentByTag("TAG_FRAG");
        tagFragment.dismiss();
    }

    @Override
    public void onTagApplyAction(ArrayList<Tag> applyTags) {
        TagFragment tagFragment = (TagFragment) getSupportFragmentManager().findFragmentByTag("TAG_FRAG");
        tagFragment.dismiss();
        Intent intent = getIntent();
        String mode = intent.getStringExtra("mode");
        TagListEditor editor = new TagListEditor();
        if (mode.equals("edit")){
            editor.checkSingleItemTagAddition(tagsToApply,applyTags);
            item.setTags(tagsToApply);
        }else{
            if (tagsToApply == null || applyTags.size() == 0){
                tagsToApply = new ArrayList<>();
            }
            item.setTags(tagsToApply);
    }
        tagDisplay3Button.setVisibility(View.GONE);
        tagDisplay2Button.setVisibility(View.GONE);
        tagDisplay1Button.setVisibility(View.GONE);

        int numTags = tagsToApply.size();
        int i = 0;
        while (i <= 2 || i < numTags){
            if (i == 0){
                tagDisplay1Button.setText(tagsToApply.get(i).getTagName());
                tagDisplay1Button.setVisibility(View.VISIBLE);
            }
            else if (i == 1){
                tagDisplay2Button.setText(tagsToApply.get(i).getTagName());
                tagDisplay2Button.setVisibility(View.VISIBLE);
            }
            else if(i == 3){
                tagDisplay3Button.setText(tagsToApply.get(i).getTagName());
                tagDisplay3Button.setVisibility(View.VISIBLE);
            }
        }
}

    @Override
    public void onTagDeletion(Tag deletedTag) {
        return;
    }
}
