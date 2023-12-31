package com.example.dwelventory;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.lang.reflect.Array;
import java.util.HashMap;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/***
 * The startup activity for Dwelventory
 * 
 * @Author
 *         CMPUT 301 FALL 2023 TEAM33
 */
public class MainActivity extends AppCompatActivity
        implements TagFragment.OnFragmentInteractionListener, FilterFragment.FilterFragmentListener {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private CollectionReference usersRef;
    private CollectionReference itemsRef;
    private ArrayList<Item> dataList;
    private boolean initialSpinnerCheck = true;
    private ArrayAdapter<Item> itemAdapter;
    private ActivityResultLauncher<Intent> addEditActivityResultLauncher;
    private int ADD_ACTIVITY_CODE = 8;
    private int EDIT_ACTIVITY_CODE = 18;
    private int ADD_EDIT_CODE_OK = 818;
    private int EXIST_CODE = 33;
    private int DOES_NOT_EXIST_CODE = 363;
    private Spinner sortSpinner;
    // private FloatingActionButton addButton;
    private ImageButton addButton;
    private TextView totalCost;
    private boolean reverseOrder;
    public int estTotalCost = 0;
    private ListView finalItemList;
    ArrayAdapter<Item> finalItemAdapter;
    private TextView appTitle;

    // Store active filters in variables below to allow for multiple filtering
    private ArrayList<String> keyword_input_saved = new ArrayList<String>();
    private Date start_date_saved= null;
    private Date end_date_saved = null;
    private ArrayList<String> make_input_saved = new ArrayList<String>();
    private ArrayList<Tag> tag_input_saved = new ArrayList<Tag>();
    private boolean filterApplied = false;

    private ArrayList<String> photos;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase authentication
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
        FirebaseUser user = mAuth.getCurrentUser();
        appTitle = findViewById(R.id.app_title);
        if (user == null) {
            signOnAnonymously();
        } else {
            Toast.makeText(MainActivity.this, "Already signed in",
                    Toast.LENGTH_SHORT).show();
            checkUsers(mAuth.getCurrentUser());
        }

        while (mAuth.getCurrentUser() == null) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
        }
        user = mAuth.getCurrentUser();
        Log.d("itemTag", "after user");
        String path = "/users/" + user.getUid() + "/items";
        // String path = "/users/rQ2PrfCOKsYkdi1bfzqvLJVZOqq1/items";
        Log.d("itemTag", "path:" + path);
        itemsRef = db.collection(path);
        dataList = new ArrayList<>();

        itemsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("itemTag", error.toString());
                    return;
                }
                if (value != null) {
                    dataList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        Log.d("itemTag", "size of value is " + value.size());
                        // Log.d("itemTag", user.getUid());
                        String storedRefID = doc.getId();
                        Log.d("itemTag", String.format("Item(%s) fetched", storedRefID));
                        Log.d("itemTag", "added default");
                        String name = doc.get("description", String.class);
                        Log.d("itemTag", String.format("Itemname(%s) fetched", name));
                        Date date = doc.get("date", Date.class);
                        String make = doc.get("make", String.class);
                        String model = doc.get("model", String.class);
                        int serial = doc.get("serialNumber", int.class);
                        int estValue = doc.get("estValue", int.class);
                        ArrayList<String> photos = (ArrayList<String>) doc.get("photos");
                        String comment = doc.get("comment", String.class);
                        // get tags from fire base
                        ArrayList<String> tags = (ArrayList<String>) doc.get("tags");
                        Log.d("result", tags.toString());
                        ArrayList<Tag> realTags = makeTagList(tags);
                        Item item = new Item(name, date, make, model, serial, estValue, comment, photos);
                        item.setTags(realTags);
                        if (!storedRefID.equals("null")) {
                            Log.d("itemTag1",
                                    String.format("Item(%s) was not null", storedRefID) + storedRefID.getClass());
                            item.setItemRefID(UUID.fromString(storedRefID));
                            dataList.add(item);
                        }
                    }
                    applyActiveFilters();
                    itemAdapter.notifyDataSetChanged();
                    setTotal(dataList);
                }
            }
        });

        totalCost = findViewById(R.id.total_cost);
        String text = getString(R.string.totalcost, estTotalCost);
        totalCost.setText(text);
        addButton = findViewById(R.id.add_item_button);

        // ArrayList<Item> dataList = new ArrayList<>();

        // fake data
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        String date11 = "7-Jun-2013";
        String date22 = "28-Oct-2023";
        Date date1;
        try {
            date1 = formatter.parse(date11);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Date date2;
        try {
            date2 = formatter.parse(date22);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        itemAdapter = new ItemList(this, dataList);
        ListView itemList = findViewById(R.id.item_list);
        itemList.setAdapter(itemAdapter);

        itemAdapter = new ItemList(this, dataList);
        itemList = findViewById(R.id.item_list);
        itemList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        itemList.setAdapter(itemAdapter);

        // Declare itemList as new final variable
        // (This variable is used only for the longClickListener)
        finalItemList = itemList;
        finalItemAdapter = itemAdapter;

        itemList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            /**
             * Get the number of items that are selected by checking whether checkbox is
             * checked.
             * 
             * @author Abhi
             * @author Maggie
             * @param selected_count
             *                       TextView to have it's text updated to the number of
             *                       items that have been selected
             * @return count
             *         int of the selected count items, if count == 0, the tag and delete
             *         btn
             *         will not be displayed
             **/
            public int getSelectedCount(TextView selected_count) {
                int count = 0;
                for (int j = 0; j < itemAdapter.getCount(); j++) {
                    View view_temp = finalItemList.getChildAt(j);
                    if (view_temp != null) {
                        CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
                        if (checkBox.isChecked()) {
                            count++;
                        }
                    }
                }
                selected_count.setText("Selected Items : " + count);
                return count;
            }

//            public void clearCheckboxes(){
//                for (int j = 0; j < itemAdapter.getCount(); j++) {
//                    View view_temp = finalItemList.getChildAt(j);
//                    if (view_temp != null) {
//                        CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
//                        if (checkBox.isChecked()) {
//                            checkBox.setChecked(false);
//                        }
//                    }
//                }
//                CheckBox select_All = findViewById(R.id.selectAll_checkbox);
//                select_All.setChecked(false);
//            }

            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                /*
                 * View checkBoxLayout = view.findViewById(R.id.checkbox);
                 * checkBoxLayout.setVisibility(View.VISIBLE);
                 */
                clearCheckboxes();

                for (int j = 0; j < itemAdapter.getCount(); j++) {
                    View view_temp = finalItemList.getChildAt(j);
                    if (view_temp != null) {
                        CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
                        checkBox.setVisibility(View.VISIBLE);
                    }
                }

                RelativeLayout select_items = findViewById(R.id.selectMultipleitems);
                select_items.setVisibility(View.VISIBLE);
                TextView selected_count = findViewById(R.id.selectedItems);
                ImageButton closebtn = findViewById(R.id.closebtn);
                ImageButton deletebtn = findViewById(R.id.deletebtn);
                ImageButton tagButton = findViewById(R.id.multiple_set_tags_button);
                CheckBox select_All = findViewById(R.id.selectAll_checkbox);
                addButton.setVisibility(View.GONE);
                appTitle.setVisibility(View.GONE);
                deletebtn.setVisibility(View.GONE);
                tagButton.setVisibility(View.VISIBLE);
                select_All.setChecked(false);


                for (int j = 0; j < itemAdapter.getCount(); j++) {
                    View view_temp = finalItemList.getChildAt(j);
                    if (view_temp != null) {
                        CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
                        checkBox.setVisibility(View.VISIBLE);
                        checkBox.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                int count = getSelectedCount(selected_count);
                                if (count == 1 || select_All.isChecked()){
                                    deletebtn.setVisibility(View.VISIBLE);
                                } else if(count == itemAdapter.getCount()){
                                    select_All.setChecked(true);
                                    deletebtn.setVisibility(View.VISIBLE);
                                } else if (count != itemAdapter.getCount()){
                                    select_All.setChecked(false);
                                    deletebtn.setVisibility(View.GONE);
                                }
                                else{
                                    deletebtn.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                }
                closebtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        select_items.setVisibility(View.GONE);
                        addButton.setVisibility(View.VISIBLE);
                        appTitle.setVisibility(View.VISIBLE);

                        for (int j = 0; j < itemAdapter.getCount(); j++) {
                            View view_temp = finalItemList.getChildAt(j);
                            if (view_temp != null) {
                                CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
                                checkBox.setChecked(false);
                                checkBox.setVisibility(View.GONE);
                                getSelectedCount(selected_count);
                            }
                        }
                        clearCheckboxes();

                        select_All.setChecked(false);
                        deletebtn.setVisibility(View.GONE);

                    }
                });

                deletebtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Log.d("delete all", "before loop: "+String.valueOf(itemAdapter.getCount()));
                        int total = itemAdapter.getCount() + 1;
                        for (int j = 0; j < total ; j++) {
                            View view_temp = finalItemList.getChildAt(j);
                            if (view_temp != null) {
                                CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
                                if (select_All.isChecked()) {   // if select all is selected delete all the children at 0
                                    // get item and its id
                                    Item deleteItem = dataList.get(0);
                                    Log.d("delete ALL", "going to delete: " + deleteItem.getDescription());
                                    UUID refId = deleteItem.getItemRefID();
                                    // remove from list
                                    finalItemAdapter.remove(dataList.get(0));
//                                    finalItemAdapter.notifyDataSetChanged();
                                    checkBox.setChecked(false);
                                    // remove from firebase
                                    Log.d("delete ALL", "1 going to delete: " + deleteItem.getDescription());
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    String path = "/users/" + user.getUid() + "/items/" + refId.toString();
                                    DocumentReference itemDocRef = db.document(path);
                                    itemDocRef.delete();
                                    getSelectedCount(selected_count);
                                } else if (checkBox.isChecked() && (!select_All.isChecked())) {
                                    // get item and its id
                                    Item deleteItem = dataList.get(j);
                                    Log.d("delete one", "going to delete: " + deleteItem.getDescription());
                                    UUID refId = deleteItem.getItemRefID();
                                    // remove from list
                                    finalItemAdapter.remove(dataList.get(j));
//                                    finalItemAdapter.notifyDataSetChanged();
                                    checkBox.setChecked(false);
                                    // remove from firebase
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    String path = "/users/" + user.getUid() + "/items/" + refId.toString();
                                    DocumentReference itemDocRef = db.document(path);
                                    itemDocRef.delete();
                                    getSelectedCount(selected_count);

                                }
                            }

//                            clearCheckboxes();

                        }
                        select_All.setChecked(false);
                        finalItemAdapter.notifyDataSetChanged();
                    }
                });

                select_All.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (select_All.isChecked()) {
                            for (int j = 0; j < finalItemList.getCount(); j++) {
                                View view1 = finalItemList.getChildAt(j);
                                CheckBox checkBox = view1.findViewById(R.id.checkbox);
                                checkBox.setChecked(true);
                                deletebtn.setVisibility(View.VISIBLE);
                                tagButton.setVisibility(View.VISIBLE);
                                getSelectedCount(selected_count);
                            }
                        }
                        if (!select_All.isChecked()) {
                            for (int j = 0; j < finalItemList.getCount(); j++) {
                                View view1 = finalItemList.getChildAt(j);
                                CheckBox checkBox = view1.findViewById(R.id.checkbox);
                                checkBox.setChecked(false);
                                deletebtn.setVisibility(View.GONE);
                                // tagButton.setVisibility(View.INVISIBLE);
                                getSelectedCount(selected_count);
                            }
                        }
                    }
                });

                tagButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TagFragment newFragment = TagFragment.newInstance(mAuth.getUid(), "edit");
                        newFragment.show(getSupportFragmentManager(), "TAG_FRAG");
                    }
                });

                return true;

            }
        });

        itemList = finalItemList;
        itemAdapter = finalItemAdapter;
        itemList.setAdapter(itemAdapter);
        // itemAdapter.notifyDataSetChanged();

        // final FloatingActionButton addButton = findViewById(R.id.add_item_button);

        // Code fragment below is for filtering

        // final FloatingActionButton addButton = findViewById(R.id.add_item_button);
        final ImageButton addButton = findViewById(R.id.add_item_button);


        Spinner filterSpinner = findViewById(R.id.filter_spinner);
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.filter_spinner_options,
                android.R.layout.simple_spinner_item);

        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setSelection(0);
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                for(int i = 0 ; i < finalItemList.getCount(); i++){
                    if(finalItemList.getChildAt(0) != null){
                        View view2 = finalItemList.getChildAt(i);
                        CheckBox temp_checkbox = view2.findViewById(R.id.checkbox);
                        temp_checkbox.setVisibility(View.INVISIBLE);
                    }
                }
                RelativeLayout rl = findViewById(R.id.selectMultipleitems);
                rl.setVisibility(View.GONE);
                appTitle.setVisibility(View.VISIBLE);
                addButton.setVisibility(View.VISIBLE);


                if (position > 0 && !(initialSpinnerCheck)) {
                    String filter = parent.getItemAtPosition(position).toString();
                    FilterFragment filterFrag = FilterFragment.newInstance(filter, mAuth.getUid());
                    filterFrag.show(getSupportFragmentManager(), "FilterFragment");
                } else if (initialSpinnerCheck) {
                    initialSpinnerCheck = false;
                }
                filterSpinner.setSelection(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        sortSpinner = findViewById(R.id.sort_spinner);
        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.sort_array,
                android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String sort = parent.getItemAtPosition(position).toString();
                switch (sort) {
                    case "Date":
                        ItemSorter.sortDate(dataList, reverseOrder);
                        break;
                    case "Description":
                        ItemSorter.sortDescription(dataList, reverseOrder);
                        break;
                    case "Make":
                        ItemSorter.sortMake(dataList, reverseOrder);
                        break;
                    case "Estimated Value":
                        ItemSorter.sortEstValue(dataList, reverseOrder);
                        break;
                    case "Tags":
                        ItemSorter.sortTag(dataList, reverseOrder);
                        break;
                }
                itemAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        sortSpinner.setAdapter(sortAdapter);

        Spinner orderSpinner;

        orderSpinner = findViewById(R.id.order_spinner);
        ArrayAdapter<CharSequence> orderAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.order_array,
                android.R.layout.simple_spinner_item);
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        orderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String order = parent.getItemAtPosition(position).toString();
                if (order.equals("Descending")) {
                    reverseOrder = true;
                } else {
                    reverseOrder = false;
                }

                String sort = sortSpinner.getSelectedItem().toString();
                switch (sort) {
                    case "Date":
                        ItemSorter.sortDate(dataList, reverseOrder);
                        break;
                    case "Description":
                        ItemSorter.sortDescription(dataList, reverseOrder);
                        break;
                    case "Make":
                        ItemSorter.sortMake(dataList, reverseOrder);
                        break;
                    case "Estimated Value":
                        ItemSorter.sortEstValue(dataList, reverseOrder);
                        break;
                    case "Tags":
                        ItemSorter.sortTag(dataList, reverseOrder);
                        break;
                }
                itemAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        orderSpinner.setAdapter(orderAdapter);

        addEditActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("resultTag", "activity result opened");
                    Log.d("resultTag", "result code: " + result.getResultCode());
                    if (result.getResultCode() == ADD_EDIT_CODE_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            // Extract item and create tag sub collection
                            Item item = data.getParcelableExtra("item");
                            // Get and set date bc its weird
                            Date date = (Date) data.getSerializableExtra("date");
                            item.setDate(date);
                            // Get and set optional parameters
                            int serialCode = data.getIntExtra("serialCode", -1);
                            if (serialCode == EXIST_CODE) {
                                int serial = Integer.parseInt(data.getStringExtra("serialNo"));
                                item.setSerialNumber(serial);
                            } else if (serialCode == DOES_NOT_EXIST_CODE) {
                                Log.d("please king julien", "no serial code attached to item");
                                Log.d("please king julien", String.valueOf(item.getSerialNumber()));
                            }
                            // Request code for handling
                            int requestCode = data.getIntExtra("requestCode", -1);
                            Log.d("resultTag", "request code: " + requestCode);
                            // Get tags and set them to the item
                            ArrayList<Tag> tags = data.getParcelableArrayListExtra("tags");
                            ArrayList<String> stringTags = makeStringTagList(tags); // THIS IS FOR FIREBASE ONLY
                            ArrayList<String> photoPaths = data.getStringArrayListExtra("applied_photos");
                            Log.d("ADDEDITPHOTOS9", "BACK TO MAIN HERES APPLIED PHOTOS" + photoPaths);
                            item.setTags(tags); // set tags
                            item.setPhotos(photoPaths);
                            Log.d("# result from ae", "after setting tags" + String.valueOf(item.getTags()));
                            Log.d("ADDEDITPHOTOS10", "BACK TO MAIN HERES APPLIED PHOTOS" + item.getPhotos());
                            if (requestCode == ADD_ACTIVITY_CODE) {
                                // Handle the result for adding
                                Log.d("resultTag", "i am about to add the item");
                                item.setItemRefID();
                                Log.d("itemTag", "New Item RefID: " + item.getItemRefID());
                                dataList.add(item);
                                estTotalCost += item.getEstValue();
                                // set item in firebase
                                itemsRef.document(String.valueOf(item.getItemRefID())).set(item.toMap());

                                // set STRING tags to items
                                HashMap<String, Object> map = new HashMap<>();
                                map.put("tags", stringTags);
                                itemsRef.document(String.valueOf(item.getItemRefID())).update(map);

                                // set photo remote cloud storage paths to items
                                HashMap<String, Object> photoMap = new HashMap<>();
                                map.put("photos", item.getPhotos());
                                itemAdapter.notifyDataSetChanged();
                                Log.d("tagtag", "onCreate: tags " + item.getTags());
                            } else if (requestCode == EDIT_ACTIVITY_CODE) {
                                Log.d("photome", "I am getting the correct photos..." + item.getPhotos());
                                // Handle the result for editing
                                Log.d("resultTag", "i am about to edit the item");
                                int position = data.getIntExtra("position", -1);
                                String itemRefId = data.getStringExtra("itemRefID");
                                Log.d("itemTag", "from intent RefID: " + itemRefId);
                                Log.d("itemTag", "from editActivity RefID: " + item.getItemRefID());
                                item.setItemRefID(UUID.fromString(itemRefId));
                                Log.d("itemTag", "after setting RefID: " + item.getItemRefID());
                                dataList.remove(position);
                                dataList.add(position, item);
                                Log.d("# item in handler", "position:" + position + " " + item.getItemRefID());
                                Log.d("# handling edit result", "after setting tags" + String.valueOf(item.getTags()));
                                // set item in firebase
                                Log.d("please king julien", String.valueOf(item.getSerialNumber()));
                                itemsRef.document(String.valueOf(item.getItemRefID())).set(item.toMap());
                                // set STRING tags to items

                                HashMap<String, Object> photoMap = new HashMap<>();
                                itemsRef.document(String.valueOf(item.getItemRefID())).set(item.toMap());

                                // set STRING tags to items
                                itemAdapter.notifyDataSetChanged();
                            }
                            String cost = getString(R.string.totalcost, estTotalCost);
                            totalCost.setText(cost);
                        }
                    }
                });

        // View and/or edit the item when clicked
        itemList.setOnItemClickListener((adapterView, view, i, l) -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            intent.putExtra("mode", "edit");
            Log.d("mainTag", "position: " + i);
            Item itemToCopy = dataList.get(i);
            Log.d("# item", "position:" + i + " " + itemToCopy.getItemRefID());
            ArrayList<Tag> copyTags = itemToCopy.getTags();
            Log.d("# sending to ae activity in edit mode",
                    String.valueOf(itemToCopy.getMake()) + " " + String.valueOf(copyTags));
            Item copyItem = makeCopy(itemToCopy);

            intent.putExtra("item", copyItem);
            intent.putExtra("date", copyItem.getDate());
            intent.putExtra("position", i);
            intent.putExtra("requestCode", EDIT_ACTIVITY_CODE);
            intent.putExtra("tags", copyTags);
            String itemRefID = itemToCopy.getItemRefID().toString();
            Log.d("itemTag", "RefID going to edit activity: " + itemRefID);
            intent.putExtra("itemRefID", itemRefID);
            intent.putExtra("send_photos", itemToCopy.getPhotos());
            addEditActivityResultLauncher.launch(intent);
        });
        // go to add activity
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditActivity.class);
            intent.putExtra("mode", "add");
            intent.putExtra("requestCode", ADD_ACTIVITY_CODE);
            addEditActivityResultLauncher.launch(intent);
        });
    }

    /**
     * This makes a copy of the item
     * 
     * @param item the item object to be copied
     * 
     * @return copyItem an Item object with the same values as the input item
     */
    public Item makeCopy(Item item) {
        Log.d("mainTag", "in copy ");
        assert item != null;
        String itemName = item.getDescription();
        Log.d("mainTag", "name is" + itemName);
        Date itemDate = item.getDate();
        String itemMake = item.getMake();
        String itemModel = item.getModel();
        int itemSerial = item.getSerialNumber();
        int itemValue = item.getEstValue();
        String itemComment = item.getComment();
        ArrayList<String> itemPhotos = item.getPhotos();
        Log.d("mainTag", "Date is" + itemDate);
        Log.d("mainTag", "Make is " + itemMake);
        // Item copyItem = new Item(itemName, itemDate, itemMake, itemModel, itemValue);
        Item copyItem = new Item(itemName, itemDate, itemMake, itemModel, itemSerial, itemValue, itemComment,
                itemPhotos);

        return copyItem;
    }

    /**
     * This calculates the total cost of all the items and then
     * sets the textview to that cost
     * 
     * @param dataList the arraylist containing the items
     */
    public void setTotal(ArrayList<Item> dataList) {
        estTotalCost = 0;
        for (int i = 0; i < dataList.size(); i++) {
            Item item = dataList.get(i);
            int val = item.getEstValue();
            estTotalCost += val;
            String cost = getString(R.string.totalcost, estTotalCost);
            totalCost.setText(cost);
        }
        if (dataList.size() == 0) {
            totalCost.setText("Total Cost: $0");
        }
    }

    /**
     * This creates an arraylist of strings of tag names
     * from an arraylist of tsg objects
     * 
     * @param tags
     * @return stringTags
     */
    public ArrayList<String> makeStringTagList(ArrayList<Tag> tags) {
        ArrayList<String> stringTags = new ArrayList<>();
        for (int i = 0; i < tags.size(); i++) {
            stringTags.add(tags.get(i).getTagName());
        }
        return stringTags;
    }

    /**
     * This creates an arraylist of tags
     * from an arraylist of tag name strings
     * 
     * @param stringTags
     * @return tags
     */
    public ArrayList<Tag> makeTagList(ArrayList<String> stringTags) {
        ArrayList<Tag> tags = new ArrayList<>();
        for (int i = 0; i < stringTags.size(); i++) {
            Tag tag = new Tag(stringTags.get(i));
            tags.add(tag);
        }
        return tags;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signOnAnonymously();
        } else {
            Toast.makeText(MainActivity.this, "Already signed in",
                    Toast.LENGTH_SHORT).show();
            checkUsers(mAuth.getCurrentUser());
        }
    }

    /**
     * This method will attempt to sign on anonymously, if the user is not already
     * signed in
     */

    private void signOnAnonymously() {
        Log.d("NULL", "sign on");
        // Log.d("NULL1", String.valueOf(mAuth.signInAnonymously()));
        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                Log.d("NULL", "completed");
                if (task.isSuccessful()) {
                    // Sign in succeeds
                    Log.d("AnonymousAuth", "signInAnonymously:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    Toast.makeText(MainActivity.this, "Authentication Success",
                            Toast.LENGTH_SHORT).show();
                    checkUsers(user);
                } else {
                    // Sign in fails
                    Log.w("AnonymousAuth", "signInAnonymously:failure", task.getException());
                    Toast.makeText(MainActivity.this, "Authentication Failed",
                            Toast.LENGTH_SHORT).show();
                }
            }
        })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d("NULL", "listening failure");
                    }
                });
    }

    /**
     * This method checks the Firestore database to see if a corresponding 'users'
     * document exists
     *
     * @param user This is the given user currently accessing the app/database
     */
    private void checkUsers(FirebaseUser user) {
        DocumentReference doc = db.collection("users").document(user.getUid());
        doc.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    // make item collection after verifying user
                    itemsRef = db.collection("users").document(user.getUid()).collection("items");
                    if (document.exists()) {
                        Log.d("userCheck", "User document exists");
                    } else {
                        Log.d("userCheck", "No such document");
                        // create a new document for the anonymous user if they do not already have one
                        // the new HashMap is to just create an empty document
                        // as we need this document in place to serve as a path for sub-collections
                        usersRef.document(user.getUid()).set(new HashMap<String, Object>());
                    }
                } else {
                    Log.d("userCheck", "get failed with ", task.getException());
                }
            }
        });
    }

    /***
     * This overriden method closes the TagFragment instance if it exists.
     */
    @Override
    public void onCloseAction() {
        TagFragment tagFragment = (TagFragment) getSupportFragmentManager().findFragmentByTag("TAG_FRAG");
        tagFragment.dismiss();
    }

    /***
     * This method applies a set of Tags to 1 or more items. Any Tags that are
     * currently not associated
     * with an Item will not be readded allowing for unique Tag classifiers
     * 
     * @param applyTags
     *                  An ArrayList of Tags representing the set of Tags we want
     *                  associated to all 1 or more Item.
     */
    @Override
    public void onTagApplyAction(ArrayList<Tag> applyTags) {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        String date22 = "28-Oct-2023";
        Date date2;
        TagFragment tagFragment = (TagFragment) getSupportFragmentManager().findFragmentByTag("TAG_FRAG");
        tagFragment.dismiss();

        TagListEditor editor = new TagListEditor();
        for (int j = 0; j < itemAdapter.getCount(); j++) {
            View view_temp = finalItemList.getChildAt(j);
            if (view_temp != null) {
                CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
                // checkBox.setVisibility(View.GONE);
                if (checkBox.isChecked()) {
                    // Must process the tags for this item.;
                    Item item = dataList.get(j);
                    ArrayList<Tag> tags = item.getTags();
                    item.setTags(editor.checkMultipleItemTagAddition(tags, applyTags));
                    itemsRef.document(String.valueOf(item.getItemRefID())).set(item.toMap());
                }
            }
        }

        try {
            date2 = formatter.parse(date22);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        Item item3 = new Item("Jinora", date2, "Pygmy Goat", "Caramel w/ Black Markings", 200);
        item3.setTags(applyTags);
        List<Tag> tags = item3.getTags();
        if (tags.size() >= 2) {
            Log.d("tag", "onTagApplyAction: " + tags.get(0).getTagName() + tags.get(1).getTagName());
        } else {
            Log.d("tag", "Not enough tags in the list");

        }
    }

    /***
     * This method deletes a Tag from all Items in the Item dataList if the Tag is
     * deleted from the
     * database itself. This not leaving any dangling Tag references.
     * 
     * @param deletedTag
     *                   A Tag object depicting the Tag we want to delete from the
     *                   set of all Items.
     */
    @Override
    public void onTagDeletion(Tag deletedTag) {
        // check all the items in the listview. and if the item has the tag that was
        // defined to be
        // deleted then delete it from the arraylist of tags associated with the item!!
        TagListEditor checker = new TagListEditor();
        for (Item currentItem : dataList) {
            checker.checkDeletion(currentItem.getTags(), deletedTag);
            currentItem.setTags(checker.checkDeletion(currentItem.getTags(), deletedTag));
            itemsRef.document(String.valueOf(currentItem.getItemRefID())).set(currentItem.toMap());
        }
    }

    public void deleteItems(ArrayList<Item> dataList, ArrayList<Item> toremove) {
        if (toremove.size() == 0) {
            Toast.makeText(MainActivity.this, "Select items to delete",
                    Toast.LENGTH_SHORT).show();
        } else {
            for (Item item : toremove) {
                dataList.remove(item);
                itemAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * This method saves user input for filtering by item makes and calls a function to apply the filter.
     *
     * @param makeInput
     *          User input item makes to filter by
     */
    @Override
    public void onMakeFilterApplied(ArrayList<String> makeInput) {
        make_input_saved = makeInput;
        applyActiveFilters();
    }

    /**
     * This method saves user input for filtering by date and calls a function to apply the filter.
     *
     * @param start
     *          User input start date
     * @param end
     *          User input end date
     */
    @Override
    public void onDateFilterApplied(Date start, Date end) {
        start_date_saved = start;
        end_date_saved = end;
        applyActiveFilters();

    }

    /**
     * This method saves user input for filtering by description and calls a function to
     * apply the filter.
     *
     * @param keywords
     *          User input item description to filter by
     */
    @Override
    public void onKeywordFilterApplied(ArrayList<String> keywords) {
        keyword_input_saved = keywords;
        applyActiveFilters();
    }

    /**
     * This method saves user input for filtering by tags and calls a function to apply the filter.
     *
     * @param filterTags
     *          User input tags to filter by
     */
    @Override
    public void onTagFilterApplied(ArrayList<Tag> filterTags) {
        tag_input_saved = filterTags;
        applyActiveFilters();

    }

    /**
     * This function is run when the use clicks on the "Clear Filter" option. It
     * retrieves all items from firebase and restores them in the data list to display on screen.
     */
    public void onClearFilterApplied() {
        View view = finalItemList.getChildAt(0);
        CheckBox temp_checkbox = view.findViewById(R.id.checkbox);
        temp_checkbox.setVisibility(View.INVISIBLE);
        estTotalCost = 0;
        dataList.clear();
        make_input_saved.clear();
        start_date_saved = null;
        end_date_saved = null;
        keyword_input_saved.clear();
        tag_input_saved.clear();
        itemAdapter.notifyDataSetChanged();

        itemsRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    dataList.clear();
                    for (QueryDocumentSnapshot doc : task.getResult()) {
                        Item item = new Item(
                                doc.getString("description"),
                                doc.getDate("date"),
                                doc.getString("make"),
                                doc.getString("model"),
                                doc.getLong("estValue").intValue());
                        item.setSerialNumber(doc.getLong("serialNumber").intValue());
                        item.setItemRefID(UUID.fromString(doc.getId()));
                        ArrayList<String> stringTags = (ArrayList<String>) doc.get("tags");
                        ArrayList<Tag> itemTags = makeTagList(stringTags);
                        ArrayList<String> photos = (ArrayList<String>) doc.get("photos");
                        item.setPhotos(photos);
                        item.setTags(itemTags);
                        dataList.add(item);
                        estTotalCost += doc.getLong("estValue").intValue();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setTotal(dataList);
                            clearCheckboxes();
                            itemAdapter.notifyDataSetChanged();
                        }
                    });

                }
            }
        });
//        itemAdapter.notifyDataSetChanged();
    }

    /**
     * Filters the datalist depending on the users make filtering input
     * @param items
     *          list of current items in data list
     * @param makes
     *          List of user input for filtering by make
     * @return
     */
    private ArrayList<Item> filterByMake(ArrayList<Item> items, ArrayList<String> makes) {
        ArrayList<Item> filteredItems = new ArrayList<>();
        for (Item item : items) {
            if (makes.contains(item.getMake())) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    /**
     * Filters the datalist depending on the users date filtering input
     * @param items
     *          list of current items in data list
     * @param start
     *          User input for start date
     * @param end
     *          User input for end date
     * @return
     */
    private ArrayList<Item> filterByDate(ArrayList<Item> items, Date start, Date end) {
        ArrayList<Item> filteredItems = new ArrayList<>();
        for (Item item : items) {
            if ((item.getDate().compareTo(start) > 0 || item.getDate().compareTo(start) == 0)
                    && item.getDate().compareTo(end) < 0) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    /**
     * Filters the datalist depending on the users item description filtering input
     * @param items
     *          list of current items in data list
     * @param keywords
     *          List of user input for filtering by description
     * @return
     */
    private ArrayList<Item> filterByKeyword(ArrayList<Item> items, ArrayList<String> keywords) {
        ArrayList<Item> filteredItems = new ArrayList<>();
        for (Item item : items) {
            if (keywords.contains(item.getDescription())) {
                filteredItems.add(item);
            }
        }
        return filteredItems;
    }

    /**
     * Filters the datalist depending on the users tags filtering input
     * @param items
     *          list of current items in data list
     * @param tags
     *          List of user input for filtering by tags
     * @return
     */
    private ArrayList<Item> filterByTags(ArrayList<Item> items, ArrayList<Tag> tags) {
        ArrayList<Item> filteredItems = new ArrayList<>();

        ArrayList<String> inputTags = new ArrayList<>(makeStringTagList(tags));

        for (Item item : items) {
            ArrayList<String> itemTags = makeStringTagList(item.getTags());
            boolean contains_tags = true;
            for (String inputTag : inputTags){
                if(!itemTags.contains(inputTag)){
                    Log.w("tagFilter", item.getDescription());
                    Log.w("FilterDebug", "Input Tags: " + inputTags.toString());
                    Log.w("FilterDebug", "Item Tags: " + itemTags.toString());
                    contains_tags = false;
                    break;
                }
            }
            if(contains_tags){
                filteredItems.add(item);
            }

        }
        return filteredItems;
    }
    public void clearCheckboxes(){
        for (int j = 0; j < itemAdapter.getCount(); j++) {
            View view_temp = finalItemList.getChildAt(j);
            if (view_temp != null) {
                CheckBox checkBox = view_temp.findViewById(R.id.checkbox);
                if (checkBox.isChecked()) {
                    checkBox.setChecked(false);
                }
            }
        }
        CheckBox select_All = findViewById(R.id.selectAll_checkbox);
        select_All.setChecked(false);
    }

    /**
     * Applies all current filters to the dataList and updates it.
     */
    private void applyActiveFilters() {
        ArrayList<Item> filteredList = new ArrayList<>(dataList); // Start with the full list
        clearCheckboxes();

        // Apply each filter only if it's active (not empty or null)
        if (!make_input_saved.isEmpty()) {
            filteredList = filterByMake(filteredList, make_input_saved);
        }
        if (start_date_saved != null && end_date_saved != null) {
            filteredList = filterByDate(filteredList, start_date_saved, end_date_saved);
        }
        if (!keyword_input_saved.isEmpty()) {
            filteredList = filterByKeyword(filteredList, keyword_input_saved);
        }
        if (!tag_input_saved.isEmpty()) {
            filteredList = filterByTags(filteredList, tag_input_saved);
        }

        // Update the adapter with the filtered list
        itemAdapter.clear();
        itemAdapter.addAll(filteredList);

        itemAdapter.notifyDataSetChanged();
        setTotal(filteredList); // Update total cost based on the filtered list

    }
}
