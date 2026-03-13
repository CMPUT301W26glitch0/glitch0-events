package com.example.cmput301_app.organizer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

import com.bumptech.glide.Glide;
import com.example.cmput301_app.R;
import com.example.cmput301_app.database.EventDB;
import com.example.cmput301_app.database.OrganizerDB;
import com.example.cmput301_app.model.Event;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.UUID;

public class CreateEventActivity extends AppCompatActivity {
    private static final String TAG = "CreateEventActivity";
    private TextInputEditText etName, etDescription, etLocation, etPrice, etCapacity, etWaitlistLimit;
    private AutoCompleteTextView actCategory;
    private TextInputLayout tilName, tilDescription, tilLocation, tilCategory, tilPrice, tilCapacity, tilWaitlistLimit;
    private Button btnPickDate, btnPublish, btnPickRegOpen, btnPickRegClose;
    private ImageView ivPosterPreview;
    private FrameLayout loadingOverlay;
    private TextView tvLoadingText;
    private Uri posterUri;
    private Calendar eventCalendar, regOpenCalendar, regCloseCalendar;
    private boolean dateSet = false, openSet = false, closeSet = false;
    private EventDB eventDB;
    private OrganizerDB organizerDB;
    private FirebaseStorage storage;
    private FirebaseAuth mAuth;
    private String existingEventId = null;
    private Event existingEvent = null;

    private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    private static final String[] CATEGORIES = { "Sports", "Gamble", "Arts", "Kids", "Seniors", "Music", "Education" };

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia = registerForActivityResult(
            new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    posterUri = uri;
                    ivPosterPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(uri).into(ivPosterPreview);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_event);

        eventDB = new EventDB();
        organizerDB = new OrganizerDB();
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();

        eventCalendar = Calendar.getInstance();
        regOpenCalendar = Calendar.getInstance();
        regCloseCalendar = Calendar.getInstance();

        initViews();
        setupCategorySpinner();
        setupListeners();

        existingEventId = getIntent().getStringExtra("eventId");
        if (existingEventId != null) {
            loadExistingEvent();
        }
    }

    private void initViews() {
        etName = findViewById(R.id.et_event_name);
        etDescription = findViewById(R.id.et_event_description);
        etLocation = findViewById(R.id.et_event_location);
        etPrice = findViewById(R.id.et_event_price);
        etCapacity = findViewById(R.id.et_event_capacity);
        etWaitlistLimit = findViewById(R.id.et_waitlist_limit);
        actCategory = findViewById(R.id.act_event_category);
        tilName = findViewById(R.id.til_name);
        tilDescription = findViewById(R.id.til_description);
        tilLocation = findViewById(R.id.til_location);
        tilCategory = findViewById(R.id.til_category);
        tilPrice = findViewById(R.id.til_price);
        tilCapacity = findViewById(R.id.til_capacity);
        tilWaitlistLimit = findViewById(R.id.til_waitlist_limit);
        btnPickDate = findViewById(R.id.btn_pick_date);
        btnPickRegOpen = findViewById(R.id.btn_pick_reg_open);
        btnPickRegClose = findViewById(R.id.btn_pick_reg_close);
        btnPublish = findViewById(R.id.btn_publish_event);
        ivPosterPreview = findViewById(R.id.iv_poster_preview);
        loadingOverlay = findViewById(R.id.loading_overlay);
        tvLoadingText = findViewById(R.id.tv_loading_text);

        View mainView = findViewById(android.R.id.content);
        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void loadExistingEvent() {
        btnPublish.setText("Update Event");
        if (tvLoadingText != null)
            tvLoadingText.setText("Updating event...");

        eventDB.getEvent(existingEventId, event -> {
            if (event != null) {
                existingEvent = event;
                etName.setText(event.getName());
                etDescription.setText(event.getDescription());
                etLocation.setText(event.getLocation());
                actCategory.setText(event.getCategory(), false);
                etPrice.setText(String.valueOf(event.getPrice()));
                etCapacity.setText(String.valueOf(event.getCapacity()));
                
                if (event.getWaitingListLimit() > 0) {
                    etWaitlistLimit.setText(String.valueOf(event.getWaitingListLimit()));
                }
                etPrice.setText(String.format(Locale.getDefault(), "%.2f", event.getPrice()));
                etCapacity.setText(String.valueOf(event.getCapacity()));

                if (event.getPosterUrl() != null && !event.getPosterUrl().isEmpty()) {
                    ivPosterPreview.setVisibility(View.VISIBLE);
                    Glide.with(this).load(event.getPosterUrl()).into(ivPosterPreview);
                }

                if (event.getDate() != null) {
                    eventCalendar.setTime(event.getDate().toDate());
                    btnPickDate.setText("Event Date: " + dateTimeFormat.format(eventCalendar.getTime()));
                    dateSet = true;
                }
                if (event.getRegistrationOpen() != null) {
                    regOpenCalendar.setTime(event.getRegistrationOpen().toDate());
                    btnPickRegOpen.setText("Reg Open: " + dateTimeFormat.format(regOpenCalendar.getTime()));
                    openSet = true;
                }
                if (event.getRegistrationClose() != null) {
                    regCloseCalendar.setTime(event.getRegistrationClose().toDate());
                    btnPickRegClose.setText("Reg Close: " + dateTimeFormat.format(regCloseCalendar.getTime()));
                    closeSet = true;
                }
            }
        }, e -> Toast.makeText(this, "Failed to load event", Toast.LENGTH_SHORT).show());
    }

    private void setupCategorySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        actCategory.setAdapter(adapter);
    }

    private void setupListeners() {
        findViewById(R.id.btn_select_poster).setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnPickDate.setOnClickListener(v -> showDateTimePicker(eventCalendar, btnPickDate, "Event Date", 1));
        btnPickRegOpen.setOnClickListener(v -> showDateTimePicker(regOpenCalendar, btnPickRegOpen, "Reg Open", 2));
        btnPickRegClose.setOnClickListener(v -> showDateTimePicker(regCloseCalendar, btnPickRegClose, "Reg Close", 3));

        btnPublish.setOnClickListener(v -> validateAndPublish());
    }

    private void showDateTimePicker(Calendar calendar, Button button, String label, int type) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);

                button.setText(label + ": " + dateTimeFormat.format(calendar.getTime()));
                if (type == 1)
                    dateSet = true;
                if (type == 2)
                    openSet = true;
                if (type == 3)
                    closeSet = true;

                validateDateOrder();
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        if (type == 3 && openSet) {
            datePickerDialog.getDatePicker().setMinDate(regOpenCalendar.getTimeInMillis());
        } else if (type == 1 && closeSet) {
            datePickerDialog.getDatePicker().setMinDate(regCloseCalendar.getTimeInMillis());
        }

        datePickerDialog.show();
    }

    private boolean validateDateOrder() {
        if (openSet && closeSet) {
            if (!regOpenCalendar.before(regCloseCalendar)) {
                Toast.makeText(this, "Registration Open must be BEFORE Registration Close!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        if (closeSet && dateSet) {
            if (!regCloseCalendar.before(eventCalendar)) {
                Toast.makeText(this, "Registration Close must be BEFORE Event Start!", Toast.LENGTH_SHORT).show();
                return false;
            }
        }
        return true;
    }

    private void validateAndPublish() {
        if (!dateSet || !openSet || !closeSet) {
            Toast.makeText(this, "Please set all dates (Event, Open, Close)", Toast.LENGTH_LONG).show();
            return;
        }

        if (!validateDateOrder()) {
            return;
        }

        String name = etName.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String category = actCategory.getText().toString().trim();
        String priceStr = etPrice.getText().toString().trim();
        String capStr = etCapacity.getText().toString().trim();
        String limitStr = etWaitlistLimit.getText().toString().trim();

        boolean isValid = true;
        
        // ... previous validations ...
        if (name.isEmpty()) { tilName.setError("Required"); isValid = false; } else tilName.setError(null);
        if (description.isEmpty()) { tilDescription.setError("Required"); isValid = false; } else tilDescription.setError(null);
        if (location.isEmpty()) { tilLocation.setError("Required"); isValid = false; } else tilLocation.setError(null);
        if (category.isEmpty()) { tilCategory.setError("Required"); isValid = false; } else tilCategory.setError(null);

        if (priceStr.isEmpty()) {
            tilPrice.setError("Required");
            isValid = false;
        } else {
            try {
                double price = Double.parseDouble(priceStr);
                if (price < 0) { tilPrice.setError("Invalid Price"); isValid = false; }
                else tilPrice.setError(null);
            } catch (Exception e) { tilPrice.setError("Invalid Number"); isValid = false; }
        }

        long capacity = 0;
        if (capStr.isEmpty()) {
            tilCapacity.setError("Required");
            isValid = false;
        } else {
            try {
                capacity = Long.parseLong(capStr);
                if (capacity <= 0) { tilCapacity.setError("Must be > 0"); isValid = false; }
                else tilCapacity.setError(null);
            } catch (Exception e) { tilCapacity.setError("Invalid Number"); isValid = false; }
        }

        long waitlistLimit = -1;
        if (!limitStr.isEmpty()) {
            try {
                waitlistLimit = Long.parseLong(limitStr);
                if (waitlistLimit <= capacity) {
                    tilWaitlistLimit.setError("Must be > Event Capacity");
                    isValid = false;
                } else {
                    tilWaitlistLimit.setError(null);
                }
            } catch (Exception e) {
                tilWaitlistLimit.setError("Invalid Number");
                isValid = false;
            }
        } else {
            tilWaitlistLimit.setError(null);
        }

        if (!isValid) return;

        Event event = (existingEvent != null) ? existingEvent : new Event();
        event.setName(name);
        event.setDescription(description);
        event.setLocation(location);
        event.setCategory(category);
        event.setPrice(Double.parseDouble(priceStr));
        event.setCapacity(capacity);
        event.setWaitingListLimit(waitlistLimit);
        event.setDate(new Timestamp(eventCalendar.getTime()));
        event.setRegistrationOpen(new Timestamp(regOpenCalendar.getTime()));
        event.setRegistrationClose(new Timestamp(regCloseCalendar.getTime()));

        if (existingEvent == null) {
            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            event.setOrganizerId(uid);
        }

        showLoading(true);

        if (posterUri != null) {
            uploadPosterAndSaveEvent(event);
        } else {
            saveEventToFirestore(event);
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        btnPublish.setEnabled(!show);
    }

    private void uploadPosterAndSaveEvent(Event event) {
        String posterPath = "event_posters/" + UUID.randomUUID().toString() + ".jpg";
        StorageReference ref = storage.getReference().child(posterPath);
        ref.putFile(posterUri).addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl().addOnSuccessListener(uri -> {
            event.setPosterUrl(uri.toString());
            saveEventToFirestore(event);
        })).addOnFailureListener(e -> {
            Log.e(TAG, "Poster upload failed", e);
            saveEventToFirestore(event);
        });
    }

    private void saveEventToFirestore(Event event) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (event.getOrganizerId() == null)
            event.setOrganizerId(uid);

        if (existingEventId == null) {
            eventDB.createEvent(event, savedEvent -> {
                String orgId = savedEvent.getOrganizerId();
                if (orgId == null)
                    orgId = uid;

                organizerDB.addOrganizedEvent(orgId, savedEvent.getEventId(),
                        aVoid -> {
                            showLoading(false);
                            navigateToQRDisplay(savedEvent.getEventId());
                        },
                        e -> {
                            Log.e(TAG, "Failed to link event to organizer", e);
                            showLoading(false);
                            navigateToQRDisplay(savedEvent.getEventId());
                        });
            }, e -> {
                showLoading(false);
                Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            eventDB.updateEvent(event, aVoid -> {
                showLoading(false);
                navigateToDetails(existingEventId);
            }, e -> {
                showLoading(false);
                Toast.makeText(this, "Firestore Update Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void navigateToQRDisplay(String id) {
        Intent intent = new Intent(this, QRDisplayActivity.class);
        intent.putExtra("eventId", id);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void navigateToDetails(String id) {
        Intent intent = new Intent(this, OrganizerEventDetailsActivity.class);
        intent.putExtra("eventId", id);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
