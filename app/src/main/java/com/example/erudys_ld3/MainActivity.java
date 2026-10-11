package com.example.erudys_ld3;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    /** Kiek laiko (ms) rodomas Snackbar su duomenu santrauka. */
    private static final int SNACKBAR_DURATION_MS = 6000;

    private View root;
    private TextInputLayout tilName;
    private TextInputEditText etName;
    private MultiAutoCompleteTextView actvDepartment;
    private RatingBar ratingBar;
    private TimePicker timePicker;
    private DatePicker datePicker;
    private Spinner spinnerCity;
    private MaterialSwitch switchRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);
        actvDepartment = findViewById(R.id.actv_department);
        ratingBar = findViewById(R.id.rating_bar);
        timePicker = findViewById(R.id.time_picker);
        datePicker = findViewById(R.id.date_picker);
        spinnerCity = findViewById(R.id.spinner_city);
        switchRegister = findViewById(R.id.switch_register);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        timePicker.setIs24HourView(true);

        setUpDepartments();
        setUpCities();

        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tilName.setError(null);
            }
        });

        btnSave.setOnClickListener(v -> onSaveClicked());
    }

    /** Padaliniu sarasas nuskaitomas is strings.xml resursu failo. */
    private void setUpDepartments() {
        String[] departments = getResources().getStringArray(R.array.departments);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_list_item_1, departments);
        actvDepartment.setAdapter(adapter);
        actvDepartment.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
        actvDepartment.setThreshold(1);

        actvDepartment.setOnItemClickListener((parent, view, position, id) ->
                Toast.makeText(this,
                        getString(R.string.toast_selected, parent.getItemAtPosition(position)),
                        Toast.LENGTH_SHORT).show());

        // Paspaudus tuscia lauka is karto parodomas visas sarasas.
        actvDepartment.setOnClickListener(v -> actvDepartment.showDropDown());
        TextInputLayout tilDepartment = findViewById(R.id.til_department);
        tilDepartment.setEndIconOnClickListener(v -> actvDepartment.showDropDown());
    }

    /** Miestu sarasas taip pat saugomas strings.xml faile. */
    private void setUpCities() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.cities, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCity.setAdapter(adapter);
    }

    private void onSaveClicked() {
        String name = etName.getText() == null ? "" : etName.getText().toString().trim();
        if (name.isEmpty()) {
            tilName.setError(getString(R.string.err_name));
            return;
        }
        tilName.setError(null);

        String summary = buildSummary(name);
        showSummarySnackbar(name, summary);
    }

    /** Surenka visu komponentu reiksmes i viena eilute. */
    private String buildSummary(String name) {
        String department = actvDepartment.getText().toString().trim();
        while (department.endsWith(",")) {
            department = department.substring(0, department.length() - 1).trim();
        }

        int difficulty = (int) ratingBar.getRating();
        String time = String.format(Locale.getDefault(), "%02d:%02d",
                timePicker.getHour(), timePicker.getMinute());
        String date = String.format(Locale.getDefault(), "%04d-%02d-%02d",
                datePicker.getYear(), datePicker.getMonth() + 1, datePicker.getDayOfMonth());
        String city = String.valueOf(spinnerCity.getSelectedItem());
        String registered = getString(switchRegister.isChecked() ? R.string.yes : R.string.no);

        return getString(R.string.summary_format,
                name, difficulty, time, date, department, city, registered);
    }

    /**
     * Parodo santrauka Snackbar lange. Paspaudus patvirtinimo uzrasa duomenys
     * irasomi i vidine atminti, o pasibaigus laikui parodomas perspejimas.
     */
    private void showSummarySnackbar(String name, String summary) {
        Snackbar snackbar = Snackbar.make(root, summary, SNACKBAR_DURATION_MS);

        TextView text = snackbar.getView()
                .findViewById(com.google.android.material.R.id.snackbar_text);
        text.setMaxLines(5);

        snackbar.setAction(R.string.snack_action, v -> saveToFile(name, summary));
        snackbar.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (event == DISMISS_EVENT_TIMEOUT) {
                    Snackbar.make(root, R.string.msg_timeout, Snackbar.LENGTH_LONG).show();
                }
            }
        });
        snackbar.show();
    }

    /** Failo pavadinimas sudaromas is EditText turinio pasalinus tarpus. */
    private void saveToFile(String name, String summary) {
        String fileName = name.replaceAll("\\s+", "") + ".txt";
        try (FileOutputStream out = openFileOutput(fileName, MODE_PRIVATE)) {
            out.write(summary.getBytes(StandardCharsets.UTF_8));
            String path = new File(getFilesDir(), fileName).getAbsolutePath();
            Snackbar.make(root, getString(R.string.msg_saved, path), Snackbar.LENGTH_LONG).show();
        } catch (IOException e) {
            Snackbar.make(root, getString(R.string.msg_save_error, String.valueOf(e.getMessage())),
                    Snackbar.LENGTH_LONG).show();
        }
    }
}
